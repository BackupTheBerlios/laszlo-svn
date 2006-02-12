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
 * MulticastSocketPool.java
 *
 * Created on 11. Juni 2004, 23:35
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.net.*;
import java.util.*;

import de.boerde.blueparrot.text.*;

/**
 *
 * @author  roland
 */
public class MulticastSocketPool implements Settings.SettingsChangedListener
{
	private Hashtable multicastToSocketInfo;
	private SocketTimeoutHandler timeoutThread;
	private NetworkInterface nic;
	private boolean usePool;

	public class SocketInfo
	{
		private String multicast;
		private MulticastSocket socket;
		private long timingOut;
		private boolean isJoined;
		private Announcement currentOwner;
		private InetAddress multicastAddress;

		protected SocketInfo (Announcement announcement) throws IOException, ProtocolException
		{
			currentOwner = announcement;
			multicast = announcement.getDetail ("multicast");
			StringTokenizer tok = new StringTokenizer (multicast, ",");
			int port;
			try
			{
				String contentip = tok.nextToken();
				multicastAddress = inetAddressFormat.parse (contentip);
				String portstr = tok.nextToken();
				port = Integer.parseInt (portstr);
				String unknownstr = tok.nextToken();
				if (!"1".equals (unknownstr))
				{
					GUIMain.logger.warning("third parameter of multicast is not '1': " + announcement);
				}
			}
			catch (Exception e)
			{
				throw new ProtocolException ("Something in this announcement packet is invalid: " + announcement);
			}
			socket = new MulticastSocket (port);
			socket.setNetworkInterface (nic);

			this.multicastAddress = multicastAddress;
			timingOut = -1;
			isJoined = false;
		}

		protected Announcement getCurrentOwner()
		{
			return currentOwner;
		}

		public MulticastSocket getSocket()
		{
			return socket;
		}

		public void setTimingOut (long timingOut)
		{
			this.timingOut = timingOut;
		}

		public void addTimingOut (long addTimeOut)
		{
			timingOut = System.currentTimeMillis() + addTimeOut;
		}

		public void addTimingOut (String timeOutString)
		{
			try
			{
				this.timingOut = (Long.parseLong (timeOutString) * 1000) + System.currentTimeMillis();
			}
			catch (NumberFormatException e)
			{
				this.timingOut = System.currentTimeMillis();
			}
		}

		public long getTimingOut()
		{
			return timingOut;
		}

		public boolean isTimedOut()
		{
			return (timingOut > 0) && (timingOut > System.currentTimeMillis());
		}

		public void joinGroup() throws IOException
		{
			if (!isJoined)
			{
				//GUIMain.logger.info("--- joining " + multicast);
				socket.joinGroup (multicastAddress);
				isJoined = true;
			}
		}

		public void close()
		{
			if (usePool)
			{
				if (currentOwner == null)
					return;

				String timeout = currentOwner.getDetail ("timeout");
				addTimingOut (timeout);
				currentOwner = null;
			}
			else
			{
				try
				{
					currentOwner = null;
					realClose();
				}
				catch (IOException e)
				{
					GUIMain.logger.severe(e.getMessage());
				}
			}
		}

		protected void realClose() throws IOException
		{
			synchronized (MulticastSocketPool.this)
			{
				multicastToSocketInfo.remove (multicast);
			}

			if (currentOwner != null)
			{
				GUIMain.logger.warning("multicast socket " + currentOwner.getDetail ("multicast") + " is physically closed but seems to be still in use by " + currentOwner.getFullName());
			}

			if (isJoined)
			{
				//GUIMain.logger.info("--- leaving " + multicast);
				socket.leaveGroup (multicastAddress);
				isJoined = false;
			}
			socket.close();
		}
	}

	private class SocketTimeoutHandler extends Thread
	{
		private boolean shouldRun = true;

		protected SocketTimeoutHandler()
		{
			setPriority (MIN_PRIORITY);
		}

		public void run()
		{
			while (shouldRun)
			{
				synchronized (MulticastSocketPool.this)
				{
					long now = System.currentTimeMillis();
					Enumeration infoEnum = multicastToSocketInfo.keys();
					while (infoEnum.hasMoreElements())
					{
						String multicast = (String) infoEnum.nextElement();
						SocketInfo info = (SocketInfo) multicastToSocketInfo.get (multicast);
						long timingOut = info.getTimingOut();
						if ((timingOut > 0) && (timingOut < now))
						{
							try
							{
								info.realClose();
							}
							catch (IOException e)
							{
								GUIMain.logger.severe(e.getMessage());
							}
							multicastToSocketInfo.remove (multicast);
						}
					}
				}
				try
				{
					synchronized (this)
					{
						wait (10000);
					}
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	private class ExitHook extends Thread
	{
		public void run()
		{
			closeAllSockets();
		}
	}

	/** Creates a new instance of MulticastSocketPool */
	private MulticastSocketPool()
	{
		Settings settings = Settings.getSettings();
		multicastToSocketInfo = new Hashtable();
		nic = settings.getDVBInterface();
		usePool = settings.getReceiveUseMulticastPool();
		settings.addSettingsChangedListener (this);
		timeoutThread = new SocketTimeoutHandler();
		timeoutThread.start();
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook (new ExitHook());
	}

	public synchronized SocketInfo getSocket (Announcement announcement) throws IOException, ProtocolException
	{
		String multicast = announcement.getDetail ("multicast");
		SocketInfo socketInfo = null;
		if (usePool)
		{
			socketInfo = (SocketInfo) multicastToSocketInfo.get (multicast);
			if (socketInfo != null)
			{
				if (socketInfo.getCurrentOwner() != null)
				{
					socketInfo.getSocket().setSoTimeout (1);	// in case someone's still trying to read, better time out soon...
					GUIMain.logger.warning("Socket " + multicast + " ist retrieved but still in use by " + announcement.getFullName());
				}
				socketInfo.setTimingOut (-1);	// Don't bother as long as the ContentReader is active on the socket
			}
		}
		if (socketInfo == null)
		{
			socketInfo = new SocketInfo (announcement);
			multicastToSocketInfo.put (multicast, socketInfo);
		}
		return socketInfo;
	}

	public synchronized void checkSocket (Announcement announcement) throws IOException
	{
		String multicast = announcement.getDetail ("multicast");
		SocketInfo socketInfo = null;
		if(multicast != null)
		    socketInfo = (SocketInfo)multicastToSocketInfo.get(multicast);
		if (socketInfo != null)
		{
			if (socketInfo.getCurrentOwner() != null)
			{
				socketInfo.getSocket().setSoTimeout (1);	// in case someone's still trying to read, better time out soon...
				GUIMain.logger.warning("Socket " + multicast + " is checked but still in use by " + announcement.getFullName());
			}
			long timeoutTime = 0;
			try
			{
				String timeoutStr = announcement.getDetail ("timeout");
				String tsizeStr = announcement.getDetail ("tsize");
				String bitrateStr = announcement.getDetail ("bitrate");
				int timeout = Integer.parseInt (timeoutStr);
				timeoutTime = timeout * (long) 1000;
				int tsize = Integer.parseInt (tsizeStr);
				int bitrate = Integer.parseInt (bitrateStr);
				long transferTime = tsize  * (long) 1000 / bitrate;
				timeoutTime += transferTime;
			}
			catch (Exception e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
			socketInfo.addTimingOut (timeoutTime);
		}
	}

	public void checkTimeouts()
	{
		synchronized (timeoutThread)
		{
			timeoutThread.notify();
		}
	}

	private synchronized void closeAllSockets()
	{
		Enumeration infoEnum = multicastToSocketInfo.keys();
		while (infoEnum.hasMoreElements())
		{
			String multicast = (String) infoEnum.nextElement();
			SocketInfo info = (SocketInfo) multicastToSocketInfo.get (multicast);
			try
			{
				info.realClose();
			}
			catch (IOException e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
		}
		multicastToSocketInfo.clear();
	}

	public void settingsChanged (Settings newSettings)
	{
		boolean newUsePool = newSettings.getReceiveUseMulticastPool();
		if (nic.equals (newSettings.getDVBInterface()) && (usePool == newUsePool))
			return;

		if (!usePool)
			return;

		usePool = newUsePool;
		closeAllSockets();
	}

	private static MulticastSocketPool theMulticastSocketPool = new MulticastSocketPool();

	public static MulticastSocketPool getMulticastSocketPool()
	{
		return theMulticastSocketPool;
	}

	private final InetAddressFormat inetAddressFormat = new InetAddressFormat();
}
