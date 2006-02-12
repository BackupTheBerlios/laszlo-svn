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
 * NewsNNTPServer.java
 *
 * Created on 23. Mai 2004, 13:14
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.news;

import java.io.*;
import java.net.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class NewsNNTPServer extends Thread implements Settings.SettingsChangedListener
{
	private InetAddress bindAddress;
	private int port;
	private int backlog;
	private int clientTimeout;
	private boolean shouldRun = true;
	private ServerSocket socket;

	/** Creates a new instance of NewsNNTPServer */
	public NewsNNTPServer()
	{
		Settings settings = Settings.getSettings();
		bindAddress = settings.getLaszloNntpBindAddress();
		port = settings.getLaszloNntpPort();
		backlog = settings.getLaszloNntpPortBacklog();
		clientTimeout = 1000 * settings.getLaszloNntpClientTimeoutSeconds();
		settings.addSettingsChangedListener (this);
	}

	public void run()
	{
		while (shouldRun)
		{
			try
			{
				synchronized (this)
				{
					if (socket == null)
					{
						if ((port > 0) && (backlog >= 0))
						{
							try
							{
								socket = new ServerSocket (port, backlog, bindAddress);
							}
							catch (Exception e)
							{
								GUIMain.logger.severe(e.getMessage());
								wait();
							}
						}
						else
						{
							wait();
						}
					}
				}

				if (socket != null)
				{
					Socket connection = socket.accept();
					connection.setSoTimeout (clientTimeout);
					NewsNNTPThread thread = new NewsNNTPThread (connection);
					thread.start();
				}
			}
			catch (SocketException e)
			{
				if ((socket != null) && !socket.isClosed())
				{
					try
					{
						socket.close();
					}
					catch (IOException e1)
					{
						GUIMain.logger.severe(e1.getMessage());
					}
					finally
					{
						socket = null;
					}
				}
				else
				{
					GUIMain.logger.severe(e.getMessage());
				}
			}
			catch (Exception e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
		}
		Settings settings = Settings.getSettings();
		settings.removeSettingsChangedListener (this);
	}

	public synchronized void settingsChanged (Settings newSettings)
	{
		InetAddress newBindAddress = newSettings.getLaszloNntpBindAddress();
		int newPort = newSettings.getLaszloNntpPort();
		int newBacklog = newSettings.getLaszloNntpPortBacklog();
		int newClientTimeout = 1000 * newSettings.getLaszloNntpClientTimeoutSeconds();
		if ((port != newPort) || (backlog != newBacklog) || (clientTimeout != newClientTimeout) || !bindAddress.equals (newBindAddress))
		{
			port = newPort;
			backlog = newBacklog;
			bindAddress = newBindAddress;
			clientTimeout = newClientTimeout;
			if (socket != null)
			{
				try
				{
					socket.close();
				}
				catch (IOException ex)
				{
					GUIMain.logger.severe(ex.getMessage());
				}
			}
			notify();
		}
	}
}
