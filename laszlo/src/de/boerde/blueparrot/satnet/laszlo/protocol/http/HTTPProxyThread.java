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
 * HTTPProxyThread.java
 *
 * Created on 31. Mai 2004, 14:47
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;

import de.boerde.blueparrot.io.*;
import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.html.*;

/**
 *
 * @author  roland
 */
public class HTTPProxyThread extends Thread
{
	private HttpAccessLogger accessLogger;
	private CacheContentManager contentManager;
	private IndexContentManager localManager;
	private Socket connection;
	private boolean shouldRun = true;
	private OutputStream outStream;
	private LineReadableInputStream inStream;

	private InternalRequestInfo requestInfo;
	private InternalResponseInfo responseInfo;

	private byte[] buffer = new byte [32768];

	private static final byte[] CRLF = { (byte) 13, (byte) 10 };

	/** Creates a new instance of HTTPProxyThread */
	public HTTPProxyThread (Socket connection)
	{
		this.connection = connection;
		requestInfo = new InternalRequestInfo (connection.getInetAddress());
		responseInfo = new InternalResponseInfo (requestInfo);
		accessLogger = HttpAccessLogger.getHttpAccessLogger();
		contentManager = CacheContentManager.getCacheContentManager();
		localManager = IndexContentManager.getIndexContentManager();
		htmlDateFormat.setTimeZone (TimeZone.getTimeZone ("GMT"));
	}

	public void run()
	{
		try
		{
			outStream = new BufferedOutputStream (connection.getOutputStream());
			inStream = new LineReadableInputStream (connection.getInputStream());

			while (shouldRun)
			{
				String line = inStream.readLine();
				if (line == null)
				{
					shouldRun = false;
				}
				else
				{
					StringTokenizer tok = new StringTokenizer (line, " ");
					if (tok.countTokens() >= 3)
					{
						String method = tok.nextToken();
						requestInfo.setMethod (method);
						requestInfo.setUri (tok.nextToken());
						String protocol = tok.nextToken();
						requestInfo.setProtocol (protocol);
						if ("HTTP/1.0".equals (protocol) || "HTTP/1.1".equals (protocol))
						{
							Vector headers = new Vector();
							line = inStream.readLine();
							while ((line != null) && !"".equals (line))
							{
								headers.add (line);
								line = inStream.readLine();
							}
							requestInfo.setHeaders (headers);
							if ("GET".equalsIgnoreCase (method))
							{
								doGet();
							}
							else if ("HEAD".equalsIgnoreCase (method))
							{
								doService();
							}
							else if ("POST".equalsIgnoreCase (method))
							{
								doService();
							}
							else if ("CONNECT".equalsIgnoreCase (method))
							{
								sendError (400, "Bad Request, Method not supported");
							}
							else
							{
								sendError (400, "Bad Method in Request");
							}
						}
						else
						{
							sendError (400, "Bad Request, unknown HTTP protocol version");
						}
					}
					else
					{
						sendError (400, "Bad Request");
					}
				}
				accessLogger.log (requestInfo, responseInfo);
				requestInfo.clear();
				responseInfo.clear();
			}
		}
		catch (SocketTimeoutException e)
		{
			GUIMain.getLogger().info("Timeout HTTP connection with " + connection.getInetAddress());
		}
		catch (SocketException e)
		{
			GUIMain.getLogger().warning("SocketException " + e.getMessage() + " at " + requestInfo.getUri());
		}
		catch (Throwable e)
		{
			GUIMain.getLogger().severe(e.getMessage());
		}
		finally
		{
			try
			{
				if ((connection != null) && !connection.isClosed())
				{
					connection.close();
				}
			}
			catch (Exception e)
			{
				GUIMain.getLogger().severe(e.getMessage());
			}
			finally
			{
				try
				{
					if (outStream != null)
					{
						outStream.close();
					}
				}
				catch (Exception e)
				{
					GUIMain.getLogger().severe(e.getMessage());
				}
				finally
				{
					try
					{
						if (inStream != null)
						{
							inStream.close();
						}
					}
					catch (Exception e)
					{
						GUIMain.getLogger().severe(e.getMessage());
					}
				}
			}
		}
	}

	private void doGet() throws IOException
	{
		String uri = requestInfo.getUri();
		if (!uri.startsWith ("http://"))
		{
			if (uri.startsWith ("/"))
			{
				doGetLocal();
				return;
			}
			else
			{
				sendError (503, "URI protocol not supported");
				return;
			}
		}
		String hostName = getHostNameIfOk (uri);
		if (hostName == null)
		{
			sendError (400, "Bad hostname");
			return;
		}
		Settings settings = Settings.getSettings();
		if (hostName.equalsIgnoreCase (settings.getHttpOwnPseudoName()))
		{
			doGetLocal();
			return;
		}

		ContentManagerInfo toDeliver = contentManager.get (uri);
		if (toDeliver == null)
		{
			serviceRemote();
			//sendError (404, "Not found");
			return;
		}

		InputStream fileIn = toDeliver.getContent();
		if (fileIn == null)
		{
			sendError (500, "Internal Error getting content");
			return;
		}

		long ifModifiedSince;
		{
			String ifModifiedSinceHeader = requestInfo.getHeader ("If-Modified-Since");
			try
			{
				if (ifModifiedSinceHeader != null)
				{
					Date ifModifiedSinceDate = htmlDateFormat.parse (ifModifiedSinceHeader);
					ifModifiedSince = ifModifiedSinceDate.getTime();
				}
				else
				{
					ifModifiedSince = Long.MIN_VALUE;
				}
			}
			catch (ParseException e)
			{
				ifModifiedSince = Long.MIN_VALUE;
			}
		}

		boolean keepaliveRequested = "keep-alive".equalsIgnoreCase (requestInfo.getHeader ("Proxy-Connection"));
		long lastModified = toDeliver.getLastModified();
		boolean sendNotChanged = (ifModifiedSince > lastModified);
		if (sendNotChanged)
		{
			writeln ("HTTP/1.0 304 Not modified");
		}
		else
		{
			writeln ("HTTP/1.0 200 Cached file");
		}
		if (keepaliveRequested && (sendNotChanged || toDeliver.isSizeKnown()))
		{
			writeln ("Proxy-Connection: keep-alive");
		}
		else
		{
			writeln ("Proxy-Connection: close");
			shouldRun = false;
		}
		writeln ("Date: " + htmlDateFormat.format (new Date ()));
		if (lastModified != Long.MAX_VALUE)
		{
			writeln ("Last-Modified: " + htmlDateFormat.format (new Date (lastModified)));
		}
		Hashtable outHeaders = toDeliver.getHeaders();
		Enumeration headerNames = outHeaders.keys();
		while (headerNames.hasMoreElements())
		{
			String name = (String) headerNames.nextElement();
			write (name);
			write (": ");
			writeln ((String) outHeaders.get (name));
		}
		writeln ("");
		try
		{
			if (!sendNotChanged)
			{
				while (true)
				{
					int num = fileIn.read (buffer);
					if (num >= 0)
					{
						outStream.write (buffer, 0, num);
					}
					else
					{
						break;
					}
				}
			}			
		}
		finally
		{
			fileIn.close();
			outStream.flush();
		}
	}

	private void doGetLocal() throws IOException
	{
		try
		{
			localManager.doGet (requestInfo, responseInfo);
			outStream.flush();
		}
		finally
		{
			if (responseInfo.hasSendingBegun())
			{
				responseInfo.fillLength();
				outStream.flush();
			}
			else
			{
				sendError (500, "Internal error");
			}
		}
	}

	private void doService() throws IOException
	{
		String uri = requestInfo.getUri();
		if (!uri.startsWith ("http://"))
		{
			if (uri.startsWith ("/"))
			{
				sendError (400, "Only GET for own Host");	// Not implemented (yet?)
				return;
			}
			else
			{
				sendError (503, "URI protocol not supported");
				return;
			}
		}
		String hostName = getHostNameIfOk (uri);
		if (hostName == null)
		{
			sendError (400, "Bad hostname");
			return;
		}
		Settings settings = Settings.getSettings();
		if (hostName.equalsIgnoreCase (settings.getHttpOwnPseudoName()))
		{
			sendError (400, "Only GET for own Host");	// Not implemented (yet?)
		}
		else
		{
			serviceRemote();
		}
	}

	private void serviceRemote() throws IOException
	{
		try
		{
			Settings settings = Settings.getSettings();
			String proxyFetching = settings.getHttpProxyFetching();
			switch (proxyFetching.charAt (0))
			{
				case 'u':
				case 'U':
				{
					ProxyFetchingContentManager.getProxyFetchingContentManager().service (requestInfo, responseInfo);
					break;
				}
				case 'd':
				case 'D':
				{
					DirectFetchingContentManager.getDirectFetchingContentManager().service (requestInfo, responseInfo);
					break;
				}
				default:
				{
					responseInfo.sendError (404);
				}
			}
			outStream.flush();
		}
		catch (UnresolvedAddressException e)
		{
			responseInfo.sendError (503, "Unresolved address");
		}
		catch (SocketException e)
		{
			responseInfo.sendError (503);
		}
		finally
		{
			if (responseInfo.hasSendingBegun())
			{
				responseInfo.fillLength();
				outStream.flush();
			}
			else
			{
				sendError (500, "Internal error");
			}
		}
	}

	private void sendError (int code, String message) throws IOException
	{
		writeln ("HTTP/1.0 " + code + " " + message);
		writeln ("Proxy-Connection: close");
		writeln ("Content-Type: text/html");
		writeln ("");
		String method = requestInfo.getMethod();
		if (!"HEAD".equalsIgnoreCase (method))
		{
			writeln ("<html><head><title>Error " + code + ": " + message + "</title></head><body><h1>Error " + code + "</h1><h2>" + message + "</h2>");
			writeln (escapeHtml (method) + " " + escapeHtml (requestInfo.getUri()) + " " + escapeHtml (requestInfo.getProtocol()));
			writeln ("</body></html>");
		}
		outStream.flush();
		shouldRun = false;
	}

	private void write (String str) throws IOException
	{
		byte[] bytes = str.getBytes ("US-ASCII");
		outStream.write (bytes);
	}

	private void writeln (String str) throws IOException
	{
		write (str);
		outStream.write (CRLF);
	}

	private void writeln() throws IOException
	{
		outStream.write (CRLF);
	}

	private static String getHostNameIfOk (String uri)
	{
//		assert uri.startsWith ("http://");
		final int startInUri = "http://".length();
		int endOfHostname = uri.indexOf ("/", startInUri);
		if (endOfHostname < 0)
			endOfHostname = uri.length();

		String hostname = uri.substring (startInUri, endOfHostname);
		if ("".equals (hostname))
			return null;	// hostname must not be empty

		boolean lastWasDot = true;	// beginning of URL is a new hostname component, i.e. as if there was a dot in front of it
		boolean colonSeen = false;	// after the colon only numbers are allowed
		for (int p=0; p<hostname.length(); p++)
		{
			char current = hostname.charAt (p);
			if (colonSeen)
			{
				if ((current >= '0') && (current <= '9'))
				{
				}
				else
				{
					return null;	// invalid character
				}
			}
			else
			{
				if (((current >= 'A') && (current <= 'Z'))
					|| ((current >= 'a') && (current <= 'z'))
					|| ((current >= '0') && (current <= '9'))
					|| (current == '-') || (current == '_'))	// Underscore is actually not allowed by hostname RFC xyz, but it seems used for some internal purposes of the sat receiving that is not supposed to map to "real" names
				{
					lastWasDot = false;
				}
				else if (current == '.')
				{
					if (lastWasDot)
						return null;	// two dots following each other are not allowed in a hostname
					else
						lastWasDot = true;
				}
				else if (current == ':')
				{
					colonSeen = true;
				}
				else
				{
					return null;	// invalid character
				}
			}
		}
		return hostname;	// all possible wrong cases have been sorted out earlier
	}

	private static String escapeHtml (String origString)
	{
		StringBuffer result = new StringBuffer (origString);
		for (int pos=0; pos<result.length(); pos++)
		{
			char current = result.charAt (pos);
			if (current < '\u0080')
			{
				switch (current)
				{
					case '<':
					{
						result.setCharAt (pos, '&');
						result.insert (pos+1, "lt;");
						pos += "lt;".length();
						break;
					}
					case '>':
					{
						result.setCharAt (pos, '&');
						result.insert (pos+1, "gt;");
						pos += "gt;".length();
						break;
					}
					case '&':
					{
						result.insert (pos+1, "amp;");
						pos += "amp;".length();
						break;
					}
				}
			}
			else
			{
				int charCode = (int) current;
				String hexString = Integer.toHexString (charCode);
				int hexStringLen = hexString.length();
				result.setCharAt (pos, '&');
				result.insert (pos+1, '#');
				result.insert (pos+2, hexString);
				result.insert (pos+2+hexStringLen, ';');
				pos += hexStringLen +2;
			}
		}
		return result.toString();
	}

	public class InternalRequestInfo implements  RequestInfo
	{
		private String method;
		private String uri;
		private String protocol;
		private Vector headers;
		private InetAddress inetAddress;

		protected InternalRequestInfo (InetAddress inetAddress)
		{
			this.inetAddress = inetAddress;
		}

		protected void clear()
		{
			method = null;
			uri = null;
			protocol = null;
			if (headers != null)
				headers.clear();
		}

		protected void setMethod (String method)
		{
			this.method = method;
		}

		protected void setUri (String uri)
		{
			this.uri = uri;
		}

		protected void setProtocol (String protocol)
		{
			this.protocol = protocol;
		}

		protected void setHeaders (Vector headers)
		{
			this.headers = headers;
		}

		public String getMethod ()
		{
			return method;
		}

		public String getUri()
		{
			return uri;
		}

		public String getProtocol()
		{
			return protocol;
		}

		public Iterator getAllHeaderLines()
		{
			return headers.iterator();
		}

		public String getHeader (String name)
		{
			String findWord = name + ":";
			for (int i=0; i<headers.size(); i++)
			{
				String header = (String) headers.get (i);
				if (header.startsWith (findWord))
				{
					return header.substring (findWord.length()).trim();
				}
			}
			return null;
		}

		public int getIntHeader (String name)
		{
			String str = getHeader (name);
			if (str == null)
				return -1;

			return Integer.parseInt (str);
		}

		public long getDateHeader (String name)
		{
			try
			{
				String str = getHeader (name);
				if (str == null)
					return -1;

				return htmlDateFormat.parse (str).getTime();
			}
			catch (ParseException e)
			{
				throw new IllegalArgumentException ("Header value cannot be parsed as a date");
			}
		}

		public InetAddress getInetAddress()
		{
			return inetAddress;
		}

		public InputStream getInputStream()
		{
			return inStream;
		}
	}

	public class InternalResponseInfo implements ResponseInfo
	{
		private int status;
		private String statusString;
		private Hashtable headers;
		private boolean headerWasSent;
		private RequestInfo requestInfo;
		private Date date;

		protected InternalResponseInfo (RequestInfo requestInfo)
		{
			status = 200;
			statusString = "OK";
			headerWasSent = false;
			headers = new Hashtable();
			this.requestInfo = requestInfo;
		}

		protected void clear()
		{
			status = 200;
			statusString = "OK";
			headerWasSent = false;
			headers.clear();
		}

		public void setHeader (String name, String value)
		{
			headers.put (name, value);
		}

		public void setIntHeader (String name, int value)
		{
			setHeader (name, String.valueOf (value));
		}

		public void setDateHeader (String name, long value)
		{
			setHeader (name, htmlDateFormat.format (new Date (value)));
		}

		public void setDateHeader (String name, Date date)
		{
			setHeader (name, htmlDateFormat.format (date));
		}

		public void setStatus (int status)
		{
			setStatus (status, (String) statusTexts.get (new Integer (status)));
		}

		public void setStatus (int status, String statusString)
		{
			this.status = status;
			this.statusString = statusString;
		}

		protected void setDate()
		{
			date = new Date();
			setDateHeader ("Date", date);
		}

		public void sendError (int status) throws IOException
		{
			sendError (status, (String) statusTexts.get (new Integer (status)));
		}

		public void sendError (int status, String statusString) throws IOException
		{
			this.status = status;
			this.statusString = statusString;
			HTTPProxyThread.this.sendError (status, statusString);
			headerWasSent = true;
		}

		protected boolean hasSendingBegun()
		{
			return headerWasSent;
		}

		protected void fillLength()
		{
			// no implementaion (yet), requires a custom (counting) output stream
		}

		String getHeader (String name)
		{
			return (String) headers.get (name);
		}

		int getStatus()
		{
			return status;
		}

		Date getDate()
		{
			return date;
		}

		private void sendHeader() throws IOException
		{
			if (!headerWasSent)
			{
				setDate();
				boolean lengthIsKnown = false;
				switch (status)
				{
					case 304:
					{
						lengthIsKnown = true;
						break;
					}
				}
				write ("HTTP/1.0 ");
				write (String.valueOf (status));
				if (statusString != null)
				{
					write (" ");
					writeln (statusString);
				}
				else
				{
					writeln();
				}
				Enumeration theHeaders = headers.keys();
				while (theHeaders.hasMoreElements())
				{
					String name = (String) theHeaders.nextElement();
					String value = (String) headers.get (name);
					if ("Content-Length".equalsIgnoreCase (name))
					{
						lengthIsKnown = true;
					}
					write (name);
					write (": ");
					writeln (value);
				}
				boolean keepaliveRequested = (requestInfo != null) ? "keep-alive".equalsIgnoreCase (requestInfo.getHeader ("Proxy-Connection")) : false;
				if (keepaliveRequested && lengthIsKnown)
				{
					writeln ("Proxy-Connection: keep-alive");
				}
				else
				{
					writeln ("Proxy-Connection: close");
					shouldRun = false;
				}
				writeln();
				headerWasSent = true;
			}
		}

		public OutputStream getOutputStream() throws IOException
		{
			if (!headerWasSent)
			{
				sendHeader();
			}
			return outStream;
		}
	}

	private final DateFormat htmlDateFormat = new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);	

	private static Hashtable statusTexts = new Hashtable();
	static
	{
		statusTexts.put (new Integer (200), "OK");
		statusTexts.put (new Integer (304), "Not Modified");
		statusTexts.put (new Integer (400), "Bad Request");
		statusTexts.put (new Integer (404), "Not Found");
		statusTexts.put (new Integer (500), "Internal Error");
		statusTexts.put (new Integer (503), "Gateway Error");
	}
}
