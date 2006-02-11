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
 * HtmlGenerator.java
 *
 * Created on 6. Juni 2004, 14:59
 */

package de.boerde.blueparrot.satnet.laszlo.html;

import java.io.*;

import de.boerde.blueparrot.satnet.laszlo.protocol.http.*;

/**
 *
 * @author  roland
 */
public class HtmlGenerator
{
	/** Creates a new instance of HtmlGenerator */
	public HtmlGenerator ()
	{
	}

	public static class BufferOutput
	{
		private PrintWriter printWriter;
		private ByteArrayOutputStream byteStream;

		protected BufferOutput() throws IOException
		{
			byteStream = new ByteArrayOutputStream();
			printWriter = new PrintWriter (new OutputStreamWriter (byteStream, "UTF-8"));
		}

		public PrintWriter getPrintWriter()
		{
			return printWriter;
		}

		protected ByteArrayOutputStream getByteStream()
		{
			return byteStream;
		}
	}

	protected static BufferOutput makeWriter() throws IOException
	{
		return new BufferOutput();
	}

	protected static void flushWriter (ResponseInfo response, BufferOutput buffer) throws IOException
	{
		buffer.getPrintWriter().flush();
		ByteArrayOutputStream byteStream = buffer.getByteStream();
		response.setHeader ("Content-Type" , "text/html; charset=UTF-8");
		response.setIntHeader ("Content-Length", byteStream.size());
		OutputStream out = response.getOutputStream();
		byteStream.writeTo (out);
		out.flush();
	}

	protected static void writeHeadSection (PrintWriter out, String title)
	{
		writeHeadSection (out, title, null);
	}

	protected static void writeHeadSection (PrintWriter out, String title, String[] addtlHead)
	{
		out.println ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println ("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"DTD/xhtml1-transitional.dtd\">");
		out.println ("<html>");
		out.println ("<head>");
		if (addtlHead != null)
		{
			for (int i=0; i<addtlHead.length; i++)
			{
				out.println (addtlHead [i]);
			}
		}
		out.println ("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\" />");
		out.println ("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />");
		out.println ("<link href=\"/files/laszlo.css\" title=\"Laszlo Style\" rel=\"stylesheet\" type=\"text/css\" />");
		out.print ("<title>");
		out.print (title);
		out.println ("</title>");
		out.println ("</head>");
	}

	protected static void writeClosingHtmlSection (PrintWriter out)
	{
		out.println ("</html>");
	}

	protected static void makeLink (PrintWriter out, String text, String link, String target)
	{
		out.print ("<a href=\"");
		out.print (link);
		if ((target != null) && !"".matches (target))
		{
			out.print ("\" target=\"");
			out.print (target);
		}
		out.print ("\">");
		out.print (text);
		out.print ("</a>");
	}

	protected static String escapeUrl (String origString)
	{
		StringBuffer result = new StringBuffer (origString);
		for (int pos=0; pos<result.length(); pos++)
		{
			char current = result.charAt (pos);
			if ((current >= '\u0080') || (current == '%'))
			{
				int charCode = (int) current;
				String hexString = Integer.toHexString (charCode);
				int hexStringLen = hexString.length();
				while (hexStringLen < 2)
				{
					hexString = "0" + hexString;
				}
				if (hexStringLen > 2)
				{
					hexString = hexString.substring (hexStringLen -2);
				}
				result.setCharAt (pos, '%');
				result.insert (pos+1, hexString);
				pos += 2;
			}
		}
		return result.toString();
	}

	protected static String escapeHtml (String origString)
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
				String hexString = Integer.toString (charCode);
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
}
