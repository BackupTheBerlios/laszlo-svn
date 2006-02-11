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
 * MimeTypes.java
 *
 * Created on 10. Juni 2004, 14:11
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.util.*;

/**
 *
 * @author  roland
 */
public class MimeTypes
{
	private Hashtable endingsToTypes;

	/** Creates a new instance of MimeTypes */
	private MimeTypes()
	{
		endingsToTypes = new Hashtable();
		endingsToTypes.put ("html", "text/html");
		endingsToTypes.put ("htm", "text/html");
		endingsToTypes.put ("xhtml", "text/html");
		endingsToTypes.put ("shtml", "text/html");
		endingsToTypes.put ("css", "text/css");
		endingsToTypes.put ("txt", "text/plain");
		endingsToTypes.put ("text", "text/plain");
		endingsToTypes.put ("asc", "text/plain");
		endingsToTypes.put ("diff", "text/plain");
		endingsToTypes.put ("rtf", "text/rtf");
		endingsToTypes.put ("rtx", "text/richtext");
		endingsToTypes.put ("csv", "text/comma-separated-values");
		endingsToTypes.put ("tsv", "text/tab-separated-values");
		endingsToTypes.put ("xml", "text/xml");
		endingsToTypes.put ("xsl", "text/xml");
		endingsToTypes.put ("mml", "text/mathml");
		endingsToTypes.put ("gif", "image/gif");
		endingsToTypes.put ("png", "image/png");
		endingsToTypes.put ("jpg", "image/jpeg");
		endingsToTypes.put ("jpe", "image/jpeg");
		endingsToTypes.put ("jpeg", "image/jpeg");
		endingsToTypes.put ("bmp", "image/bmp");
		endingsToTypes.put ("ief", "image/ief");
		endingsToTypes.put ("pcx", "image/pcx");
		endingsToTypes.put ("tif", "image/tiff");
		endingsToTypes.put ("tiff", "image/tiff");
		endingsToTypes.put ("svg", "image/svg+xml");
		endingsToTypes.put ("svgz", "image/svg+xml");
		endingsToTypes.put ("pdf", "application/pdf");
		endingsToTypes.put ("doc", "application/msword");
		endingsToTypes.put ("ps", "application/postscript");
		endingsToTypes.put ("eps", "application/postscript");
		endingsToTypes.put ("zip", "application/zip");
		endingsToTypes.put ("au", "audio/basic");
		endingsToTypes.put ("snd", "audio/basic");
		endingsToTypes.put ("mid", "audio/midi");
		endingsToTypes.put ("midi", "audio/midi");
		endingsToTypes.put ("kar", "audio/midi");
		endingsToTypes.put ("mpga", "audio/mpeg");
		endingsToTypes.put ("mpega", "audio/mpeg");
		endingsToTypes.put ("mp2", "audio/mpeg");
		endingsToTypes.put ("mp3", "audio/mpeg");
		endingsToTypes.put ("m3u", "audio/mpegurl");
		endingsToTypes.put ("sid", "audio/prd.sid");
		endingsToTypes.put ("dl", "video/dl");
		endingsToTypes.put ("fli", "video/fli");
		endingsToTypes.put ("gl", "video/gl");
		endingsToTypes.put ("mpeg", "video/mpeg");
		endingsToTypes.put ("mpg", "video/mpeg");
		endingsToTypes.put ("mpe", "video/mpeg");
		endingsToTypes.put ("qt", "video/quicktime");
		endingsToTypes.put ("qt", "video/mov");
	}

	public String classifyEnding (String uri)
	{
		int qPos = uri.indexOf ("?");
		if (qPos >= 0)
		{
			uri = uri.substring (0, qPos);
		}

		int lastDotPos = uri.lastIndexOf (".");
		if (lastDotPos >= 0)
		{
			return (String) endingsToTypes.get (uri.substring (lastDotPos+1));
		}
		else
		{
			return null;
		}
	}

	private static MimeTypes theMimeTypes = new MimeTypes();

	public static MimeTypes getMimeTypes()
	{
		return theMimeTypes;
	}
}
