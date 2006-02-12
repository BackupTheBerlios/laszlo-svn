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
 * ContentManagerInfo.java
 *
 * Created on 12. Juni 2004, 01:03
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;
import java.net.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.GUIMain;

/**
 *
 * @author  roland
 */
public class ContentManagerInfo
{
	private InputStream content;
	private Hashtable headers;
	private boolean sizeKnown;
	private long lastModified;

	ContentManagerInfo()
	{
		headers = new Hashtable();
		sizeKnown = false;
		lastModified = Long.MAX_VALUE;
	}

	public InputStream getContent()
	{
		return content;
	}

	public Hashtable getHeaders()
	{
		return headers;
	}

	public boolean isSizeKnown()
	{
		return sizeKnown;
	}

	public long getLastModified()
	{
		return lastModified;
	}

	protected void setFile (File file)
	{
		try
		{
			content = new FileInputStream (file);
			headers.put ("Content-Length", String.valueOf (file.length()));
			lastModified = file.lastModified();
			sizeKnown = true;
		}
		catch (FileNotFoundException e)
		{
			GUIMain.logger.severe("Error: Not found: " + file);
		}
	}

	protected void setUrl (URLConnection connection)
	{
		try
		{
			content = connection.getInputStream();
			int size = connection.getContentLength();
			if (size >= 0)
			{
				headers.put ("Content-Length", String.valueOf (size));
				sizeKnown = true;
			}
			lastModified = connection.getLastModified();
		}
		catch (IOException e)
		{
			GUIMain.logger.severe("Error getting Resource: " + e.getMessage());
		}
	}

	protected void setHeader (String name, String value)
	{
		headers.put (name, value);
	}

	protected void setLastModified (long lastModified)
	{
		this.lastModified = lastModified;
	}
}
