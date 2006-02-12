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
 * AnnounementReader.java
 *
 * Created on 1. Mai 2004, 10:11
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.util.*;

/**
 *
 * @author  roland
 */
public class ContentReader
{
	private static final String progressSuffix = ".laszloprogress.ser";

	protected Announcement announcement;
	protected BookingAnnouncement xmlAnnouncement;
	protected int tsize;
	protected int totalNumberOfPackets;
	protected int lastReceivedSequence = -1;
	protected ForwardBitChunkList partsToRetrieve;
	protected int received;
	protected boolean done = false;

	protected ContentReader()
	{
	}

	public ContentReader (Announcement announcement, BookingAnnouncement xmlAnnouncement, int received, ForwardBitChunkList partsToRetrieve) throws ProtocolException
	{
		this.announcement = announcement;
		this.xmlAnnouncement = xmlAnnouncement;
		this.received = received;
		this.partsToRetrieve = partsToRetrieve;
		done = true;
		try
		{
			String blksizeStr = announcement.getDetail ("blksize");
			String tsizeStr = announcement.getDetail ("tsize");
			int blksize = Integer.parseInt (blksizeStr);
			tsize = Integer.parseInt (tsizeStr);
			totalNumberOfPackets = (tsize % blksize == 0) ? tsize / blksize : tsize / blksize +1;
		}
		catch (Exception e)
		{
			throw new ProtocolException ("Something in this announcement packet is invalid: " + announcement);
		}
	}

	private static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	protected String dataDump (byte[] data, int num)
	{
		StringBuffer buf = new StringBuffer (num * 3);
		for (int i=0; i<data.length && i < num; i++)
		{
			byte b = data [i];
			int upperNibble = ((b & 0xf0) >> 4);
			int lowerNibble = ((b & 0x0f) >> 0);
			buf.append (" ").append (hexDigits [upperNibble]).append (hexDigits [lowerNibble]);
		}
		return buf.toString();
	}

	public ForwardBitChunkList getPartsToReceive()
	{
		return partsToRetrieve;
	}

	public int getTotalBytes()
	{
		return tsize;
	}

	public int getTotalNumberOfPackets()
	{
		return totalNumberOfPackets;
	}

	public int getLastReceivedSequence()
	{
		return lastReceivedSequence;
	}

	public int getReceivedBytes()
	{
		return received;
	}

	public Announcement getAnnouncement()
	{
		return announcement;
	}

	public BookingAnnouncement getXmlAnnouncement()
	{
		return xmlAnnouncement;
	}

	public boolean isDone()
	{
		return done;
	}

	private StringBuffer appendLocalFileName (StringBuffer toAppendTo)
	{
		StringBuffer name = (toAppendTo != null) ? toAppendTo : new StringBuffer();
		int firstIndex = name.length();
		name.append (announcement.getFullName().replace ('\\', File.separatorChar));
		if ((name.length() > firstIndex+1) && (name.charAt (1) == ':'))
		{
			name.delete (firstIndex, firstIndex+2);
		}
		if (name.length() == firstIndex)
			name.append ("nonamefile");
		if (name.charAt (firstIndex) != (File.separatorChar))
			name.insert (firstIndex, File.separatorChar);
		name.insert (firstIndex, "recv");
		name.insert (firstIndex, File.separatorChar);
		Settings settings = Settings.getSettings();
		name.insert (firstIndex, settings.getWorkDirectory());
		return name;
	}

	public String getLocalFileName()
	{
		return appendLocalFileName (new StringBuffer()).toString();
	}

	private String getProgressFileName()
	{
		return appendLocalFileName (new StringBuffer()).append (progressSuffix).toString();
	}

	protected void savePartsToRetrieve()
	{
		String fileid = announcement.getDetail ("fileid");
		if (fileid == null)
		{	// no saving of progress information if file does not have an id.
			return;
		}
		ObjectOutputStream out = null;
		try
		{
			out = new ObjectOutputStream (new FileOutputStream (getProgressFileName()));
			out.writeObject (announcement);
			out.writeObject (xmlAnnouncement);
			out.writeInt (received);
			out.writeObject (partsToRetrieve);
//			GUIMain.logger.info("Saved progress information for file " + announcement.getFullName() + " id " + fileid + " tsize " + tsize + " blksize " + announcement.getDetail ("blksize") + " received " + received + " partsToRetrieve " + partsToRetrieve);
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

	protected void loadPartsToRetrieve()
	{
		String fileid = announcement.getDetail ("fileid");
		if (fileid == null)
		{	// no loading of progress information if file does not have an id.
			return;
		}
		File progressFile = new File (getProgressFileName());
		if (!progressFile.exists())
		{	// nothing to do if there is no progress file.
			return;
		}
		ObjectInputStream in = null;
		try
		{
			in = new ObjectInputStream (new FileInputStream (progressFile));
			Announcement savedAnnouncement = (Announcement) in.readObject();
			if (announcement.getFullName().equals (savedAnnouncement.getFullName())
				&& announcement.getDetail ("fileid").equals (savedAnnouncement.getDetail ("fileid"))
				&& announcement.getDetail ("blksize").equals (savedAnnouncement.getDetail ("blksize"))
				&& announcement.getDetail ("tsize").equals (savedAnnouncement.getDetail ("tsize")))
			{
				BookingAnnouncement tempXmlAnnouncement = (BookingAnnouncement) in.readObject();
				int tempReceived = in.readInt();
				ForwardBitChunkList tempPartsToRetrieve = (ForwardBitChunkList) in.readObject();
				xmlAnnouncement = tempXmlAnnouncement;
				received = tempReceived;
				partsToRetrieve = tempPartsToRetrieve;
//				GUIMain.logger.info("Loaded progress information for file " + announcement.getFullName() + " id " + fileid + " tsize " + tsize + " blksize " + announcement.getDetail ("blksize") + " received " + received + " partsToRetrieve " + partsToRetrieve);
			}
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
		}
		catch (Exception e)
		{
			GUIMain.logger.warning("File " + announcement.getFullName() + " id " + fileid + " broken progress file");
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
	}

	protected void deleteProgressFile()
	{
		String fileid = announcement.getDetail ("fileid");
		if (fileid == null)
		{	// no loading of progress information if file does not have an id.
			return;
		}
		File progressFile = new File (getProgressFileName());
		if (!progressFile.exists())
		{	// nothing to do if there is no progress file.
			return;
		}
		progressFile.delete();
	}

	private Vector transmissionListeners = new Vector();

	public void addTransmissionListener (TransmissionListener listener)
	{
		transmissionListeners.add (listener);
	}

	public void removeTransmissionListener (TransmissionListener  listener)
	{
		transmissionListeners.remove (listener);
	}

	protected synchronized void notifyTransmissionCompleted()
	{
		removeContentReaderFromAll (this);
		TransmissionEvent evt = new TransmissionEvent (this);
		Iterator listeners = ((Vector) transmissionListeners.clone()).iterator();
		while (listeners.hasNext())
		{
			TransmissionListener  listener = (TransmissionListener) listeners.next();
			listener.transmissionCompleted (evt);
		}
	}

	protected synchronized void notifyTransmissionIncomplete()
	{
		TransmissionEvent evt = new TransmissionEvent (this);
		Iterator listeners = ((Vector) transmissionListeners.clone()).iterator();
		while (listeners.hasNext())
		{
			TransmissionListener  listener = (TransmissionListener) listeners.next();
			listener.transmissionIncomplete (evt);
		}
		if (announcement.getDetail ("fileid") == null)
		{
			notifyTransmissionExpired();
		}
	}

	protected synchronized void notifyTransmissionExpired()
	{
		TransmissionEvent evt = new TransmissionEvent (this, true);
		Iterator listeners = ((Vector) transmissionListeners.clone()).iterator();
		while (listeners.hasNext())
		{
			TransmissionListener  listener = (TransmissionListener) listeners.next();
			listener.transmissionExpired (evt);
		}
	}

	private static ContentReader loadAnonymousProgressFile (File file)
	{
		ContentReader result = null;
		ObjectInputStream in = null;
		try
		{
			in = new ObjectInputStream (new FileInputStream (file));
			Announcement savedAnnouncement = (Announcement) in.readObject();
			BookingAnnouncement savedXmlAnnouncement = (BookingAnnouncement) in.readObject();
			int savedReceived = in.readInt();
			ForwardBitChunkList savedPartsToRetrieve = (ForwardBitChunkList) in.readObject();
			result = new ContentReader (savedAnnouncement, savedXmlAnnouncement, savedReceived, savedPartsToRetrieve);
		}
		catch (Throwable e)
		{
			file.delete();
			String name = file.getAbsolutePath();
			name = name.substring (0, name.length() - progressSuffix.length());
			File actualFile = new File (name);
			actualFile.delete();
			GUIMain.logger.warning("File " + file.getAbsolutePath() + " broken progress file, deleting");
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
		return result;
	}

	public static Vector getAllContentReaders()
	{
		return allContentReaders;
	}

	private static Vector allContentReaders;
	private static ExpiryThread expiryThread;
	static
	{
		allContentReaders = readAllSavedProgressFiles();
		expiryThread = new ExpiryThread();
		expiryThread.start();
	}

	private static void expireContentReaders()
	{
		Settings settings = Settings.getSettings();
		long decideTime = System.currentTimeMillis() - (60 * 1000 * settings.getExpireIncompleteFileAfterMinutes());
		synchronized (allContentReaders)
		{
			Iterator iter = allContentReaders.iterator();
			while (iter.hasNext())
			{
				ContentReader reader = (ContentReader) iter.next();
				if (reader.isDone())
				{
					String name = reader.getLocalFileName();
					File file = new File (name);
					String progressName = reader.getProgressFileName();
					File progressFile = new File (progressName);
					if ((progressFile.lastModified() < decideTime) && (file.lastModified() < decideTime))
					{
						iter.remove();
						file.delete();
						progressFile.delete();
						reader.notifyTransmissionExpired();
					}
				}
			}
		}
	}

	private static void expireFiles()
	{
		Settings settings = Settings.getSettings();
		final long decideTime = System.currentTimeMillis() - (60 * 1000 * settings.getExpireIncompleteFileAfterMinutes());
		File dir = new File (settings.getWorkDirectory() + File.separatorChar + "recv");
		FileFinder finder = new FileFinder(dir);
		finder.setFileFilter (new FileFilter()
			{
				public boolean accept (File file)
				{
					return file.lastModified() < decideTime;
				}
			});
		File[] expiredFiles = finder.findAllFiles();
		if (expiredFiles != null)
		{
			for (int e=0; e<expiredFiles.length; e++)
			{
				File file = expiredFiles [e];
				file.delete();
			}
		}
	}

	protected void expireSimilarContentReaders()
	{
		if (xmlAnnouncement == null)
			return;

		String myUrl = xmlAnnouncement.getUrl();
		if (myUrl == null)
			return;

		String myLocalName = getLocalFileName();
		synchronized (allContentReaders)
		{
			Iterator iter = allContentReaders.iterator();
			while (iter.hasNext())
			{
				ContentReader reader = (ContentReader) iter.next();
				BookingAnnouncement otherXmlAnnouncement = reader.getXmlAnnouncement();
				if (otherXmlAnnouncement != null)
				{
					if (myUrl.equals (otherXmlAnnouncement.getUrl()))
					{
						String otherName = reader.getLocalFileName();
						if (!myLocalName.equals (otherName))
						{
							File file = new File (otherName);
							String progressName = reader.getProgressFileName();
							File progressFile = new File (progressName);
							iter.remove();
							file.delete();
							progressFile.delete();
						}
						reader.notifyTransmissionExpired();
					}
				}
			}
		}
	}

	protected static void addContentReaderToAll (ContentReader reader)
	{
		synchronized (allContentReaders)
		{
			String localFile = reader.getLocalFileName();
			for (int c=0; c<allContentReaders.size(); c++)
			{
				ContentReader existing = (ContentReader) allContentReaders.get (c);
				if (localFile.equals (existing.getLocalFileName()))
				{
					allContentReaders.remove (c);
					break;
				}
			}
			allContentReaders.add (reader);
		}
	}

	protected static void removeContentReaderFromAll (ContentReader reader)
	{
		synchronized (allContentReaders)
		{
			allContentReaders.remove (reader);
		}
	}

	private static Vector readAllSavedProgressFiles()
	{
		Vector result = new Vector();
		Vector resultFiles = new Vector();
		Settings settings = Settings.getSettings();
		String rootDir = settings.getWorkDirectory() + File.separator + "recv";
		Stack dirsToVisit = new Stack();
		dirsToVisit.push (new File (rootDir));
		while (!dirsToVisit.isEmpty())
		{
			File dir = (File) dirsToVisit.pop();
			File[] files = dir.listFiles();
			if (files != null)
			{
				for (int i=0; i<files.length; i++)
				{
					File file = files [i];
					if (file.isDirectory())
					{
						dirsToVisit.push (file);
					}
					else
					{
						if (file.getName().endsWith (progressSuffix))
						{
							ContentReader reader = loadAnonymousProgressFile (file);
							if (reader != null)
							{
								int pos;
								long fileLastModified = file.lastModified();
								for (pos = 0; pos<result.size(); pos++)
								{
									File fileAtPos = (File) resultFiles.get (pos);
									if (fileLastModified < fileAtPos.lastModified())
										break;
								}
								resultFiles.add (pos, file);
								result.add (pos, reader);
							}
						}
					}
				}
			}
		}
		return result;
	}

	public static class TransmissionEvent
	{
		private ContentReader reader;
		private boolean complete;
		private boolean expired;

		TransmissionEvent (ContentReader reader)
		{
			this.reader = reader;
			complete = reader.partsToRetrieve.isEmpty();
		}

		TransmissionEvent (ContentReader reader, boolean expired)
		{
			this.reader = reader;
			complete = reader.partsToRetrieve.isEmpty();
			this.expired = expired;
		}

		public ContentReader getContentReader()
		{
			return reader;
		}

		public boolean isComplete()
		{
			return complete;
		}

		public boolean isExpired()
		{
			return expired;
		}
	}

	public static interface TransmissionListener
	{
		abstract public void transmissionCompleted (TransmissionEvent evt);
		abstract public void transmissionIncomplete (TransmissionEvent evt);
		abstract public void transmissionExpired (TransmissionEvent evt);
	}

	private static class ExpiryThread extends Thread
	{
		protected ExpiryThread()
		{
		}

		public synchronized void run()
		{
			while (true)
			{
				try
				{
					Settings settings = Settings.getSettings();
					wait (60 * 1000 * settings.getExpireIncompleteFileCheckIntervalMinutes());
					expireContentReaders();
					expireFiles();
				}
				catch (Throwable t)
				{
					GUIMain.logger.severe(t.getMessage());
				}
			}
		}
	}
}
