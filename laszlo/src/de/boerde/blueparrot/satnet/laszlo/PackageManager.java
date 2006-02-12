/*
Laszlo, a reception software for a satellite-based push service.
Copyright (C) 2004-2006  Roland Fulde

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA 02110-1301, USA.

Project home page: http://laszlo.berlios.de/
*/


/*
 * ContentManager.java
 *
 * Created on 16. Mai 2004, 02:54
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.protocol.*;

/**
 *
 * @author  roland
 */
public class PackageManager
{
	private Hashtable packagesByUrl;
	private PackageOrganizer packageOrganizer;
	private StringBuffer dirNumber;
	private ExpireThread expireThread;

	public PackageManager()
	{
		packagesByUrl = new Hashtable();
		dirNumber = new StringBuffer();
		readOldPackageInfo();
		expireThread = new ExpireThread();
		expireThread.start();
	}

	public PackageOrganizer getPackageOrganizer()
	{
		return packageOrganizer;
	}

	public PackageInfo getPackageInfo (String url)
	{
		synchronized (packagesByUrl)
		{
			return (PackageInfo) packagesByUrl.get (url);
		}
	}

	public void setPackageInfo (File dir, Announcement announcement, BookingAnnouncement xmlAnnouncement)
	{
		PackageInfo info = new PackageInfo (dir, announcement, xmlAnnouncement);
		ProtocolInfo protocolInfo = ProtocolInfo.getProtocolInfo (info);
		info.setProtocolInfo (protocolInfo);
		PackageInfo oldInfo = null;
		synchronized (packagesByUrl)
		{
			String url = xmlAnnouncement.getUrl();
			oldInfo = (PackageInfo) packagesByUrl.get (url);
			packagesByUrl.put (url, info);
			File file = new File (dir.getAbsolutePath() + File.separator + "laszloinfo.ser");
			ObjectOutputStream out = null;
			try
			{
				out = new ObjectOutputStream (new FileOutputStream (file));
				out.writeObject (announcement);
				out.writeObject (xmlAnnouncement);
				out.writeObject (protocolInfo);
			}
			catch (IOException e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
			finally
			{
				try
				{
					if (out != null)
						out.close();
				}
				catch (IOException e)
				{
					GUIMain.logger.severe(e.getMessage());
				}
			}
		}
		if (oldInfo != null)
		{
			packageOrganizer.removePackageInfo (oldInfo);
			packageOrganizer.setPackageInfo (info);
			Vector listeners;
			synchronized (packageListeners)
			{
				listeners = (Vector) packageListeners.clone();
			}
			Iterator iter = (listeners).iterator();
			while (iter.hasNext())
			{
				PackageListener listener = (PackageListener) iter.next();
				listener.packageUpdated (oldInfo, info);
			}
			removePackageDir (oldInfo);
		}
		else
		{
			packageOrganizer.setPackageInfo (info);
			Vector listeners;
			synchronized (packageListeners)
			{
				listeners = (Vector) packageListeners.clone();
			}
			Iterator iter = listeners.iterator();
			while (iter.hasNext())
			{
				PackageListener listener = (PackageListener) iter.next();
				listener.packageAdded (info);
			}
		}
	}

	public Iterator iterator()
	{
		return packagesByUrl.values().iterator();
	}

	private void removePackageDir (PackageInfo info)
	{
		removePackageDir (info.getDir());
	}

	private void removePackageDir (File topDir)
	{
		// delete info file first
		File infoFile = new File (topDir.getAbsolutePath() + File.separator + "laszloinfo.ser");
		infoFile.delete();

		Stack filesToDelete = new Stack();
		Stack dirsToProcess = new Stack();
		filesToDelete.push (topDir);
		dirsToProcess.push (topDir);
		while (!dirsToProcess.isEmpty())
		{
			File dir = (File) dirsToProcess.pop();
			if ((dir != null) && dir.isDirectory())
			{
				File[] files = dir.listFiles();
				if (files != null)
				{
					for (int f=0; f<files.length; f++)
					{
						File file = files [f];
						if (file != null)
						{
							if (file.isDirectory())
							{
								dirsToProcess.push (file);
							}
							filesToDelete.push (file);
						}
					}
				}
			}
		}
		while (!filesToDelete.isEmpty())
		{
			File file = (File) filesToDelete.pop();
			file.delete();
		}
	}

	private static PackageInfo readPackageInfo (File dir)
	{
		PackageInfo info = null;
		File file = new File (dir.getAbsolutePath() + File.separator + "laszloinfo.ser");
		ObjectInputStream in = null;
		try
		{
			if (file.exists())
			{
				in = new ObjectInputStream (new FileInputStream (file));
				Announcement announcement = (Announcement) in.readObject();
				BookingAnnouncement xmlAnnouncement = (BookingAnnouncement ) in.readObject();
				ProtocolInfo protocolInfo = (ProtocolInfo) in.readObject();
				info = new PackageInfo (dir, announcement, xmlAnnouncement);
				info.setProtocolInfo (protocolInfo);
			}
		}
		catch (InvalidClassException e)
		{
			GUIMain.logger.severe("Invalid package information in " + dir);
		}
		catch (Exception e)
		{
			GUIMain.logger.severe(e.getMessage());
			info = null;
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();
			}
			catch (IOException e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
		}
		return info;
	}

	public File getUniqueSubdir()
	{
		synchronized (dirNumber)
		{
			Settings settings = Settings.getSettings();
			String workDir = settings.getWorkDirectory();
			File dir;
			do
			{
				boolean adjust;
				int pos = dirNumber.length() -1;
				do
				{
					adjust = false;
					if (pos < 0)
					{
						dirNumber.insert (0, "0");
					}
					else
					{
						char posChar = dirNumber.charAt (pos);
						switch (posChar)
						{
							case '9':
							{
								dirNumber.setCharAt (pos, 'a');
								break;
							}
							case 'z':
							{
								dirNumber.setCharAt (pos, '0');
								pos--;
								adjust = true;
								break;
							}
							default:
							{
								dirNumber.setCharAt (pos, ++posChar);
							}
						}
					}
				}
				while (adjust);
				dir = new File (workDir + File.separator + "pkgs" + File.separator + dirNumber.toString());
			}
			while (dir.exists());
			dir.mkdirs();
			return dir;
		}
	}

	private void readOldPackageInfo()
	{
		final Vector invalidPkgDirs = new Vector();
		Settings settings = Settings.getSettings();
		String workDir = settings.getWorkDirectory();
		long oldestAllowedTime = System.currentTimeMillis() - (settings.getExpirePackageAfterMinutes() * 60 * 1000);
		File dir = new File (workDir + File.separator + "pkgs");
		File[] subdirs = dir.listFiles();
		if (subdirs != null)
		{
			for (int i=0; i<subdirs.length; i++)
			{
				File subdir = subdirs [i];
				if ((subdir != null) && subdir.isDirectory())
				{
					PackageInfo info = readPackageInfo (subdir);
					if (info != null)
					{
						long updatedTime = info.getXmlAnnouncement().getUpdatedTime();
						if (updatedTime >= oldestAllowedTime)
						{
							BookingAnnouncement announcement = info.getXmlAnnouncement();
							packagesByUrl.put (announcement.getUrl(), info);
						}
						else
						{
							GUIMain.logger.info("Expire: " + info.getXmlAnnouncement().getUrl());
							invalidPkgDirs.add (subdir);
						}
					}
					else
					{
						invalidPkgDirs.add (subdir);
					}
				}
			}
		}
		packageOrganizer = new PackageOrganizer();
		synchronized (packageOrganizer)
		{
			Thread startupThread = new Thread (new Runnable()
			{
				public void run()
				{
					packageOrganizer.initialize (PackageManager.this);
					Iterator iter = invalidPkgDirs.iterator();
					while (iter.hasNext())
					{
						removePackageDir ((File) iter.next());
					}
				}
			}, "Package Manager Startup Thread");
			startupThread.start();
		}
	}

	private class ExpireThread extends Thread
	{
		private ExpireThread()
		{
			super ("Package Manager Expire Thread");
		}

		public void run()
		{
			while (true)
			{
				Settings settings = Settings.getSettings();
				try
				{
					Thread.sleep (settings.getExpirePackageCheckIntervalMinutes() * 60 * 1000);
				}
				catch (InterruptedException e)
				{
					break;
				}
				long oldestAllowedTime = System.currentTimeMillis() - (settings.getExpirePackageAfterMinutes() * 60 * 1000);
				Vector packagesExpired = new Vector();
				synchronized (packagesByUrl)
				{
					Enumeration packages = packagesByUrl.keys();
					while (packages.hasMoreElements())
					{
						String url = (String) packages.nextElement();
						PackageInfo info = (PackageInfo) packagesByUrl.get (url);
						long packageTime = info.getXmlAnnouncement().getUpdatedTime();
						if (packageTime < oldestAllowedTime)
						{
							GUIMain.logger.info("Expire: " + info.getXmlAnnouncement().getUrl());
							packagesByUrl.remove (url);
							packagesExpired.add (info);
							packageOrganizer.removePackageInfo (info);
							Vector listeners;
							synchronized (packageListeners)
							{
								listeners = (Vector) packageListeners.clone();
							}
							Iterator iter = listeners.iterator();
							while (iter.hasNext())
							{
								PackageListener listener = (PackageListener) iter.next();
								listener.packageRemoved (info);
							}
						}
					}
				}
				for (int i=0; i<packagesExpired.size(); i++)
				{
					PackageInfo info = (PackageInfo) packagesExpired.get (i);
					File packageDir = info.getDir();
					removePackageDir (packageDir);
				}
			}
		}
	}

	private Vector packageListeners = new Vector();

	public void addPackageListener (PackageListener listener)
	{
		synchronized (packageListeners)
		{
			packageListeners.add (listener);
		}
	}

	public void removePackageListener (PackageListener listener)
	{
		synchronized (packageListeners)
		{
			packageListeners.remove (listener);
		}
	}

	public static interface PackageListener
	{
		public void packageAdded (PackageInfo info);
		public void packageRemoved (PackageInfo info);
		public void packageUpdated (PackageInfo oldInfo, PackageInfo newInfo);
	}

	private static PackageManager thePackageManager = new PackageManager();

	public static PackageManager getPackageManager()
	{
		return thePackageManager;
	}

	public static class PackageInfo
	{
		private File dir;
		private Announcement announcement;
		private BookingAnnouncement xmlAnnouncement;
		private ProtocolInfo protocolInfo;

		protected PackageInfo (File dir, Announcement announcement, BookingAnnouncement xmlAnnouncement)
		{
			this.dir = dir;
			this.announcement = announcement;
			this.xmlAnnouncement = xmlAnnouncement;
		}

		public File getDir()
		{
			return dir;
		}

		public Announcement getAnnouncement()
		{
			return announcement;
		}

		public BookingAnnouncement getXmlAnnouncement()
		{
			return xmlAnnouncement;
		}

		public void updateXmlAnnouncement (BookingAnnouncement xmlAnnouncement)
		{
			this.xmlAnnouncement = xmlAnnouncement;
			File file = new File (dir.getAbsolutePath() + File.separator + "laszloinfo.ser");
			ObjectOutputStream out = null;
			try
			{
				out = new ObjectOutputStream (new FileOutputStream (file));
				out.writeObject (announcement);
				out.writeObject (xmlAnnouncement);
				out.writeObject (protocolInfo);
			}
			catch (IOException e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
			finally
			{
				try
				{
					if (out != null)
						out.close();
				}
				catch (IOException e)
				{
					GUIMain.logger.severe(e.getMessage());
				}
			}
		}

		public ProtocolInfo getProtocolInfo()
		{
			return protocolInfo;
		}

		protected void setProtocolInfo (ProtocolInfo protocolInfo)
		{
			this.protocolInfo = protocolInfo;
		}
	}
}
