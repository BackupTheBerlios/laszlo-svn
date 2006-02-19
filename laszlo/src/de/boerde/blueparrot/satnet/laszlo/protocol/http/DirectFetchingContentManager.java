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

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import de.boerde.blueparrot.satnet.laszlo.GUIMain;

/**
 * 
 * @author roland
 */
public class DirectFetchingContentManager extends
		AbstractFetchingContentManager implements Producer {
	private Hashtable openConnections;

	/** Creates a new instance of IndexContentManager */
	public DirectFetchingContentManager() {
		openConnections = new Hashtable();
		Thread timeoutThread = new Thread() {
			public void run() {
				try {
					while (true) {
						sleep(300 * 1000);
						checkAllTimeouts();
					}
				} catch (InterruptedException e) {
					GUIMain.getLogger().severe(e.getMessage());
				}
			}
		};
	}

	protected ConnectionInfo getConnection(RequestInfo request)
			throws IOException {
		String uri = request.getUri();
		if (!"http://".equalsIgnoreCase(uri.substring(0, "http://".length()))) {
			throw new IOException("Unsupported protocol in URI " + uri);
		}

		String hostAndPort;
		int pos = uri.indexOf('/', "http://".length());
		if (pos >= 0)
			hostAndPort = uri.substring("http://".length(), pos);
		else
			hostAndPort = uri.substring("http://".length());
		pos = hostAndPort.indexOf(':');
		String host;
		int port;
		if (pos > 0) {
			host = hostAndPort.substring(0, pos);
			port = Integer.parseInt(hostAndPort.substring(pos + 1));
			if (port <= 0)
				throw new NumberFormatException(
						"Negative Port numbers not allowed");
		} else {
			host = hostAndPort;
			port = 80;
		}

		InetSocketAddress socketAddress = new InetSocketAddress(host, port);
		TimeoutConnectionInfo connection;
		synchronized (openConnections) {
			Vector hostConnections = (Vector) openConnections
					.get(socketAddress);
			if (hostConnections == null || hostConnections.isEmpty()) {
				SocketChannel socketChannel = SocketChannel.open(socketAddress);
				connection = new TimeoutConnectionInfo(socketChannel);
			} else {
				long now = System.currentTimeMillis();
				do {
					connection = (TimeoutConnectionInfo) hostConnections.get(0);
					hostConnections.remove(0);
				} while ((connection.getTimeoutTime() <= now)
						&& !hostConnections.isEmpty());
				if (hostConnections.isEmpty()) {
					SocketChannel socketChannel = SocketChannel
							.open(socketAddress);
					connection = new TimeoutConnectionInfo(socketChannel);
				}
				connection.setPooled(true);
				connection.setReusedAgain(false);
				if (hostConnections.isEmpty()) {
					openConnections.remove(socketAddress);
				}
			}
		}
		return connection;
	}

	protected void releaseConnection(ConnectionInfo connInfo)
			throws IOException {
		if (connInfo.isReusedAgain()) {
			SocketAddress socketAddress = connInfo.getSocketAddress();
			synchronized (openConnections) {
				Vector hostConnections = (Vector) openConnections
						.get(socketAddress);
				if (hostConnections == null) {
					hostConnections = new Vector();
					openConnections.put(socketAddress, hostConnections);
				}
				hostConnections.add(connInfo);
			}
		} else {
			connInfo.close();
		}
	}

	protected void addRemoteRequestHeaders(ConnectionInfo info, Writer output)
			throws IOException {
		output.write("Connection: Keep-Alive\r\n");
	}

	protected boolean checkRemoteResponsetHeader(ConnectionInfo info,
			String headerName, String headerValue) {
		if (headerName.equalsIgnoreCase("Connection")) {
			if (headerValue.equalsIgnoreCase("keep-alive")) {
				info.setReusedAgain(true);
				TimeoutConnectionInfo tinfo = (TimeoutConnectionInfo) info;
				if (tinfo.getTimeoutTime() == 0) {
					tinfo
							.setTimeoutTime(System.currentTimeMillis() + 30 * 1000);
				}
			} else {
				info.setReusedAgain(false);
			}
			return false;
		} else if (headerName.equalsIgnoreCase("Keep-Alive")) {
			StringTokenizer tok = new StringTokenizer(headerValue, ",");
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int pos = token.indexOf('=');
				if (pos > 0) {
					String name = token.substring(0, pos).trim();
					String value = token.substring(pos + 1).trim();
					if ("timeout".equalsIgnoreCase(name)) {
						try {
							int timeout = Integer.parseInt(value);
							if (timeout > 0) {
								TimeoutConnectionInfo tinfo = (TimeoutConnectionInfo) info;
								tinfo.setTimeoutTime(System.currentTimeMillis()
										+ timeout * 1000);
							}
						} catch (NumberFormatException e) {
							GUIMain.getLogger().severe(e.getMessage());
						}
					}
				}
			}
			return false;
		} else {
			return true;
		}
	}

	private void checkAllTimeouts() {
		synchronized (openConnections) {
			long now = System.currentTimeMillis();
			Enumeration addresses = openConnections.keys();
			while (addresses.hasMoreElements()) {
				SocketAddress address = (SocketAddress) addresses.nextElement();
				Vector hostConnections = (Vector) openConnections.get(address);
				for (int c = hostConnections.size() - 1; c >= 0; c--) {
					TimeoutConnectionInfo info = (TimeoutConnectionInfo) hostConnections
							.get(c);
					if (info.getTimeoutTime() <= now) {
						try {
							info.close();
						} catch (IOException e) {
							GUIMain.getLogger().severe(e.getMessage());
						}
						hostConnections.remove(c);
					}
				}
				if (hostConnections.isEmpty()) {
					openConnections.remove(address);
				}
			}
		}
	}

	protected class TimeoutConnectionInfo extends ConnectionInfo {
		private long timeoutTime;

		protected TimeoutConnectionInfo(SocketChannel socketChannel)
				throws IOException {
			super(socketChannel);
		}

		public void setTimeoutTime(long timeoutTime) {
			this.timeoutTime = timeoutTime;
		}

		public long getTimeoutTime() {
			return timeoutTime;
		}

		public boolean isTimedOut() {
			return timeoutTime <= System.currentTimeMillis();
		}
	}

	private static DirectFetchingContentManager theDirectFetchingContentManager = new DirectFetchingContentManager();

	public static DirectFetchingContentManager getDirectFetchingContentManager() {
		return theDirectFetchingContentManager;
	}
}
