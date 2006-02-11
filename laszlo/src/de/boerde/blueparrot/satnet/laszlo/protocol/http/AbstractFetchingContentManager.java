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
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;

import de.boerde.blueparrot.io.*;
import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
abstract public class AbstractFetchingContentManager implements Producer
{
	public void doHead (RequestInfo request, ResponseInfo response) throws IOException
	{
		service (request, response);
	}

	public void doGet (RequestInfo request, ResponseInfo response) throws IOException
	{
		service (request, response);
	}

	public void doPost (RequestInfo request, ResponseInfo response) throws IOException
	{
		service (request, response);
	}

	public void service (RequestInfo request, ResponseInfo response) throws IOException
	{
		String uri = request.getUri();
		if (!uri.startsWith ("http://"))
		{
			response.setStatus (400);
			return;
		}

		String method = request.getMethod();
		int requestBodyLength = 0;
		int bodyLength = -1;
		String responseLine;
		byte[] bytes = new byte [2048];
		LineReadableInputStream in;
		ConnectionInfo connInfo;
		OutputStream outStream;
		do
		{
			connInfo = getConnection (request);
			outStream = connInfo.getOutputStream();
			Writer out = new OutputStreamWriter (outStream, "CP1252");	// try to be 8-bit clean by using the Windows 8bit charset
			if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase (method) && !"HEAD".equalsIgnoreCase (method))
			{
				response.sendError (400, "Unimplemented Method");
				return;
			}
			out.write (method);
			out.write (' ');
			out.write (uri);
			out.write (" HTTP/1.0\r\n");
			Iterator headers = request.getAllHeaderLines();
			while (headers.hasNext())
			{
				String header = (String) headers.next();
				int colonPos = header.indexOf (':');
				if (colonPos > 0)
				{
					String headerName = header.substring (0, colonPos);
					if ((headerName.equalsIgnoreCase ("Proxy-Connection"))
					  ||(headerName.equalsIgnoreCase ("Connection")))
					{
					}
					else if (headerName.equalsIgnoreCase ("Content-Length"))
					{
						requestBodyLength = Integer.parseInt (request.getHeader (headerName));
						out.write (header);
						out.write ("\r\n");
					}
					else
					{
						out.write (header);
						out.write ("\r\n");
					}
				}
			}
			out.write ("Via: Laszlo HTTP Proxy Fetcher\r\n");
			addRemoteRequestHeaders (connInfo, out);
			out.write ("\r\n");
			out.flush();
		} while (connInfo.isPooled() && !connInfo.isAlive());

		if ("POST".equalsIgnoreCase (method))
		{
			InputStream clientIn = request.getInputStream();
			while (true)
			{
				int count = clientIn.read (bytes, 0, requestBodyLength > bytes.length ? bytes.length : requestBodyLength);
				if (count < 0)
					break;

				outStream.write (bytes, 0, count);

				requestBodyLength -= count;
				if (requestBodyLength == 0)
					break;
			}
			outStream.flush();
		}

		in = connInfo.getInputStream();
		responseLine = in.readLine();
		if (responseLine == null)
		{
			response.sendError (503);
			return;
		}
		int responseLineLength = responseLine.length();
		if (responseLineLength < 10)
		{
			response.sendError (503);
			return;
		}
		if ((!"HTTP/".equalsIgnoreCase (responseLine.substring (0, 5)))
		  || (responseLine.charAt (8) != ' '))
		{
			response.sendError (503);
			return;
		}
		int spacePos = responseLine.indexOf (' ', 9);
		String statusStr;
		String statusDesc;
		if (spacePos >= 9)
		{
			statusStr = responseLine.substring (9, spacePos);
			statusDesc = responseLine.substring (spacePos +1);
		}
		else
		{
			statusStr = responseLine.substring (9);
			statusDesc = "";
		}
		int status;
		try
		{
			status = Integer.parseInt (statusStr);
		}
		catch (NumberFormatException e)
		{
			status = 503;
		}
		response.setStatus (status, statusDesc);

		while (true)
		{
			String line = in.readLine();
			if (line == null || "".equals (line))
				break;

			int colonPos = line.indexOf (':');
			if (colonPos > 0)
			{
				String headerName = line.substring (0, colonPos);
				String headerValue = line.substring (colonPos +1).trim();
				if (checkRemoteResponsetHeader (connInfo, headerName, headerValue))
				{
					if (headerName.equalsIgnoreCase ("Content-Length"))
					{
						bodyLength = Integer.parseInt (headerValue);
						response.setHeader (headerName, headerValue);
					}
					else
					{
						response.setHeader (headerName, headerValue);
					}
				}
			}
		}
		OutputStream clientOut = response.getOutputStream();
		if (!"HEAD".equalsIgnoreCase (method) && (status != 304) && (status != 204) && (status >= 200))
		{
			while (bodyLength != 0)
			{
				int count;
				if (bodyLength > 0)
				{
					count = in.read (bytes, 0, bodyLength > bytes.length ? bytes.length : bodyLength);
					bodyLength -= count;
				}
				else
				{
					 count = in.read (bytes);
				}
				if (count < 0)
				{
					break;
				}

				clientOut.write (bytes, 0, count);

				if (bodyLength == 0)
					break;
			}
		}
		clientOut.flush();

		releaseConnection (connInfo);
	}

	abstract protected void releaseConnection (ConnectionInfo connInfo) throws IOException;
	abstract protected ConnectionInfo getConnection(RequestInfo request) throws IOException;
	abstract protected void addRemoteRequestHeaders (ConnectionInfo info, Writer output) throws IOException;
	abstract protected boolean checkRemoteResponsetHeader (ConnectionInfo info, String headerName, String headerValue);

	protected static class ConnectionInfo
	{
		private SocketChannel socketChannel;
		private Socket socket;
		private LineReadableInputStream inputStream;
		private boolean pooled;
		private boolean reusedAgain;

		protected ConnectionInfo (SocketChannel socketChannel) throws IOException
		{
			this.socketChannel = socketChannel;
			socket = socketChannel.socket();
			socket.setKeepAlive (true);
			socket.setSoTimeout (60 * 1000);
			inputStream = new LineReadableInputStream (socket.getInputStream());
			pooled = false;
			reusedAgain = false;
		}

		public SocketAddress getSocketAddress()
		{
			return socket.getRemoteSocketAddress();
		}

		public OutputStream getOutputStream() throws IOException
		{
			return socket.getOutputStream();
		}

		public LineReadableInputStream getInputStream()
		{
			return inputStream;
		}

		public void close() throws IOException
		{
			socketChannel.close();
		}

		public boolean isAlive() throws IOException
		{
			socketChannel.configureBlocking (false);
			ByteBuffer initialByte = ByteBuffer.allocate (1);
			int initialBytesFromConnection = socketChannel.read (initialByte);
			socketChannel.configureBlocking (true);
			if (initialBytesFromConnection == 1)
			{
				inputStream.pushbackAfterPreread (initialByte.get (0));
			}
			return initialBytesFromConnection >= 0;
		}

		public void setReusedAgain (boolean reused)
		{
			reusedAgain = reused;
		}

		public boolean isReusedAgain()
		{
			return reusedAgain;
		}

		public void setPooled (boolean pooled)
		{
			this.pooled = pooled;
		}

		public boolean isPooled()
		{
			return pooled;
		}
	}
}
