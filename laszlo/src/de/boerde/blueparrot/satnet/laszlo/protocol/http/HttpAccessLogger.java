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
 * HttpAccessLogger.java
 *
 * Created on 8. August 2004, 17:55
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;
import java.text.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.text.*;

/**
 *
 * @author  roland
 */
public class HttpAccessLogger
{
	private StringBuffer logLine = new StringBuffer ("");
	private PrintWriter writer;
	private final InetAddressFormat inetAddressFormat = new InetAddressFormat();
	private final DateFormat logDateFormat = new SimpleDateFormat ("[dd/MMM/yyyy HH:mm:ss Z]", Locale.ENGLISH);
	private final NumberFormat twoDecimalFormat = new DecimalFormat ("00");
	private Calendar lastLogEntry;

	/** Creates a new instance of HttpAccessLogger */
	public HttpAccessLogger()
	{
		lastLogEntry = Calendar.getInstance();
		reopenFile ('d');
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook (new Thread()
			{
				public void run()
				{
					writer.close();
				}
			}
		);
	}

	private void reopenFile (char rollInterval)
	{
		try
		{
			if (writer != null)
			{
				writer.close();
				writer = null;
			}
			Settings settings = Settings.getSettings();
			String workDir = settings.getWorkDirectory();
			File dir = new File (workDir);
			if (!dir.exists())
			{
				dir.mkdirs();
			}
			String baseName = "access";
			StringBuffer fullName = new StringBuffer (workDir);
			fullName.append (File.separatorChar);
			fullName.append (baseName);
			fullName.append ('-');
			switch (rollInterval)
			{
				case 'd':
				case 'D':
				{
					fullName.append (lastLogEntry.get (Calendar.YEAR));
					fullName.append ('-');
					fullName.append (twoDecimalFormat.format (lastLogEntry.get (Calendar.MONTH) +1));
					fullName.append ('-');
					fullName.append (twoDecimalFormat.format (lastLogEntry.get (Calendar.DAY_OF_MONTH)));
					break;
				}
				case 'w':
				case 'W':
				{
					fullName.append (lastLogEntry.get (Calendar.YEAR));
					fullName.append ("-week-");
					fullName.append (twoDecimalFormat.format (lastLogEntry.get (Calendar.WEEK_OF_YEAR)));
					break;
				}
				case 'm':
				case 'M':
				{
					fullName.append (lastLogEntry.get (Calendar.YEAR));
					fullName.append ('-');
					fullName.append (twoDecimalFormat.format (lastLogEntry.get (Calendar.MONTH) +1));
					break;
				}
				case 'y':
				case 'Y':
				{
					fullName.append (lastLogEntry.get (Calendar.YEAR));
					break;
				}
			}
			fullName.append (".log");
			writer = new PrintWriter (new FileWriter (fullName.toString(), true));
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
		}		
	}

	public void log (HTTPProxyThread.InternalRequestInfo request, HTTPProxyThread.InternalResponseInfo response) throws IOException
	{
		if (writer == null)
			return;

		String template = "%h %l %u %t \"%r\" %>s %b";
		logLine.setLength (0);
		for (int p=0; p<template.length(); p++)
		{
			char c = template.charAt (p);
			if (c == '%')
			{
				do
				{
					p++;
					c = template.charAt (p);
				}
				while (((p) < template.length()) && !Character.isLetter (c) && (c != '%'));
				if (p < template.length())
				{
					c = template.charAt (p);
					switch (c)
					{
						case '%':
						{
							logLine.append ('%');
							break;
						}
						case 'a':
						{
							logLine.append (inetAddressFormat.format (request.getInetAddress()));
							break;
						}
						case 'b':
						{
							logLine.append ("??");
							break;
						}
						case 'B':
						{
							logLine.append ("??");
							break;
						}
						case 'h':
						{
							logLine.append (request.getInetAddress().getHostName());
							break;
						}
						case 'l':
						{
							logLine.append ('-');
							break;
						}
						case 'r':
						{
							logLine.append (request.getMethod());
							logLine.append (' ');
							logLine.append (request.getUri());
							logLine.append (' ');
							logLine.append (request.getProtocol());
							break;
						}
						case 's':
						{
							logLine.append (response.getStatus());
							break;
						}
						case 't':
						{
							Date date = response.getDate();
							if (date == null)
								date = new Date();
							logLine.append (logDateFormat.format (date));
							break;
						}
						case 'u':
						{
							logLine.append ('-');
							break;
						}
						default:
						{
							logLine.append ("unimplemented{%");
							logLine.append (c);
							logLine.append (")}");
							break;
						}
					}
				}
			}
			else
			{
				logLine.append (c);
			}
		}
		Calendar newLogEntry = Calendar.getInstance();
		newLogEntry.setTimeInMillis (System.currentTimeMillis());
		int newYear = newLogEntry.get (Calendar.YEAR);
		int newMonth = newLogEntry.get (Calendar.MONTH);
		int newWeek = newLogEntry.get (Calendar.WEEK_OF_YEAR);
		int newDay = newLogEntry.get (Calendar.DAY_OF_MONTH);
		synchronized (this)
		{
			int lastYear = lastLogEntry.get (Calendar.YEAR);
			int lastMonth = lastLogEntry.get (Calendar.MONTH);
			int lastWeek = lastLogEntry.get (Calendar.WEEK_OF_YEAR);
			int lastDay = lastLogEntry.get (Calendar.DAY_OF_MONTH);
			char rollInterval = 'd';
			lastLogEntry = newLogEntry;
			switch (rollInterval)
			{
				case 'd':
				case 'D':
				{
					if (lastYear != newYear || lastMonth != newMonth || lastDay != newDay)
					{
						reopenFile (rollInterval);
					}
					break;
				}
				case 'w':
				case 'W':
				{
					if (lastYear != newYear || lastWeek != newWeek)
					{
						reopenFile (rollInterval);
					}
					break;
				}
				case 'm':
				case 'M':
				{
					if (lastYear != newYear || lastMonth != newMonth)
					{
						reopenFile (rollInterval);
					}
					break;
				}
				case 'y':
				case 'Y':
				{
					if (lastYear != newYear)
					{
						reopenFile (rollInterval);
					}
					break;
				}
			}
			writer.println (logLine);
		}
	}

	private static HttpAccessLogger theHttpAccessLogger = new HttpAccessLogger();
	public static HttpAccessLogger getHttpAccessLogger()
	{
		return theHttpAccessLogger;
	}
}
