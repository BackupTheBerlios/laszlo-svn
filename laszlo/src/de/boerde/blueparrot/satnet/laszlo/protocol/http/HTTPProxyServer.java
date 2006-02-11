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
 * HTTPProxyServer.java
 *
 * Created on 31. Mai 2004, 14:28
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;
import java.net.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class HTTPProxyServer extends Thread implements Settings.SettingsChangedListener
{
	//private HttpAccessLogger accessLogger = HttpAccessLogger.getHttpAccessLogger();	// not really needed but to make sure that the class is loaded and initialized _here_
	private InetAddress bindAddress;
	private int port;
	private int backlog;
	private int clientTimeout;
	private boolean shouldRun = true;
	private ServerSocket socket;

	/** Creates a new instance of HTTPProxyServer */
	public HTTPProxyServer()
	{
		Settings settings = Settings.getSettings();
		bindAddress = settings.getLaszloHttpBindAddress();
		port = settings.getLaszloHttpPort();
		backlog = settings.getLaszloHttpPortBacklog();
		clientTimeout = 1000 * settings.getLaszloHttpClientTimeoutSeconds();
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
								e.printStackTrace (System.err);
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
					HTTPProxyThread thread = new HTTPProxyThread (connection);
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
					catch (IOException ex)
					{
						ex.printStackTrace (System.err);
					}
					finally
					{
						socket = null;
					}
				}
				else
				{
					e.printStackTrace (System.err);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace (System.err);
			}
			finally
			{
				if (socket == null)
				{
					try
					{
						Thread.sleep (1000);
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}
		Settings settings = Settings.getSettings();
		settings.removeSettingsChangedListener (this);
	}

	public synchronized void settingsChanged (Settings newSettings)
	{
		InetAddress newBindAddress = newSettings.getLaszloHttpBindAddress();
		int newPort = newSettings.getLaszloHttpPort();
		int newBacklog = newSettings.getLaszloHttpPortBacklog();
		int newClientTimeout = 1000 * newSettings.getLaszloHttpClientTimeoutSeconds();
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
					ex.printStackTrace (System.err);
				}
			}
			notify();
		}
	}
}
