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
 * ResourceFetchingProducer.java
 *
 * Created on 13. Juni 2004, 01:36
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;
import java.net.*;

/**
 *
 * @author  roland
 */
public class ResourceFetchingProducer implements Producer
{
	private String virtualRoot;
	private String resourceRoot;

	/** Creates a new instance of ResourceFetchingProducer */
	public ResourceFetchingProducer (String virtualRoot, String resourceRoot)
	{
		if (virtualRoot.endsWith ("/"))
			this.virtualRoot = virtualRoot;
		else
			this.virtualRoot = virtualRoot + "/";

		if (resourceRoot.endsWith ("/"))
			this.resourceRoot = resourceRoot;
		else
			this.resourceRoot = resourceRoot + "/";
	}

	public void doGet (RequestInfo request, ResponseInfo response) throws IOException
	{
		String uri = request.getUri();
		if (uri.startsWith ("http://"))
		{
			uri = uri.substring (uri.indexOf ("/", "http://".length()));
		}

		String plainName = uri.substring (virtualRoot.length());
		if (plainName.indexOf ("..") >= 0)
		{
			response.sendError (400);
			return;
		}
		URL url = getClass().getClassLoader().getResource (resourceRoot + plainName);
		if (url == null)
		{
			response.sendError (404);
			return;
		}
		URLConnection conn = url.openConnection();
		MimeTypes mimeTypes = MimeTypes.getMimeTypes();
		String type = mimeTypes.classifyEnding (uri);
		if (type != null)
		{
			response.setHeader ("Content-Type", type);
		}
		int length = conn.getContentLength();
		if (length >= 0)
		{
			response.setIntHeader ("Content-Length", length);
		}
		long lastModified = conn.getLastModified();
		response.setDateHeader ("Last-Modified", lastModified);
		long ifModifiedSince = request.getDateHeader ("If-Modified-Since");
		if ((ifModifiedSince >= 0) && (ifModifiedSince < lastModified))
		{
			response.setStatus (304);
		}
		else
		{
			response.setStatus (200);
			byte[] buffer = new byte [8192];
			InputStream in = conn.getInputStream();
			OutputStream out = response.getOutputStream();
			while (true)
			{
				int num = in.read (buffer);
				if (num >= 0)
				{
					out.write (buffer, 0, num);
				}
				else
				{
					break;
				}
			}
			in.close();
		}
	}
}
