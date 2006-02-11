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
 * IndexContentManager.java
 *
 * Created on 12. Juni 2004, 21:03
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class ProxyFetchingContentManager extends AbstractFetchingContentManager implements Producer
{
	private Vector openConnections;

	/** Creates a new instance of IndexContentManager */
	public ProxyFetchingContentManager()
	{
		openConnections = new Vector();
	}

	protected ConnectionInfo getConnection (RequestInfo request) throws IOException
	{
		ConnectionInfo connection;
		synchronized (openConnections)
		{
			if (openConnections.isEmpty())
			{
				Settings settings = Settings.getSettings();
				String proxyHost = settings.getUpstreamHttpProxyHost();
				int proxyPort = settings.getUpstreamHttpProxyPort();
				SocketChannel socketChannel = SocketChannel.open (new InetSocketAddress (proxyHost, proxyPort));
				connection = new ConnectionInfo (socketChannel);
			}
			else
			{
				connection = (ConnectionInfo) openConnections.get (0);
				connection.setPooled (true);
				connection.setReusedAgain (false);
				openConnections.remove (0);
			}
		}
		return connection;
	}

	protected void releaseConnection (ConnectionInfo connInfo) throws IOException
	{
		if (connInfo.isReusedAgain())
		{
			synchronized (openConnections)
			{
				openConnections.add (connInfo);
			}
		}
		else
		{
			connInfo.close();
		}
	}

	protected void addRemoteRequestHeaders (ConnectionInfo info, Writer output) throws IOException
	{
		output.write ("Proxy-Connection: Keep-Alive\r\n");
	}

	protected boolean checkRemoteResponsetHeader (ConnectionInfo info, String headerName, String headerValue)
	{
		if (headerName.equalsIgnoreCase ("Proxy-Connection"))
		{
			info.setReusedAgain (headerValue.equalsIgnoreCase ("keep-alive"));
			return false;
		}
		else
		{
			return true;
		}
	}

	private static ProxyFetchingContentManager theProxyFetchingContentManager = new ProxyFetchingContentManager();

	public static ProxyFetchingContentManager getProxyFetchingContentManager()
	{
		return theProxyFetchingContentManager;
	}
}
