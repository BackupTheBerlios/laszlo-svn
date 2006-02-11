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
 * ContentReaderThread.java
 *
 * Created on 1. Mai 2004, 23:15
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;

/**
 *
 * @author  roland
 */
public class ReceiverContentReaderThread extends Thread
{
	private Announcement announcement;
	private ReceiverContentReader contentReader;

	/** Creates a new instance of ContentReaderThread */
	public ReceiverContentReaderThread (Announcement announcement) throws IOException, ProtocolException
	{
		this.announcement = announcement;
		contentReader = new ReceiverContentReader (announcement);
	}

	public ReceiverContentReader getContentReader()
	{
		return contentReader;
	}

	public void run()
	{
		try
		{
			contentReader.getTransmission();
		}
		catch (IOException e)
		{
			System.err.println ("Error in: " + announcement);
			e.printStackTrace (System.err);
		}
	}
}
