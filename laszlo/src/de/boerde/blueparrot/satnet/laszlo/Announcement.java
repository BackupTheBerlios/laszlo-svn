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
 * Announcement.java
 *
 * Created on 1. Mai 2004, 16:49
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  roland
 */
public class Announcement implements Serializable
{
	private String fullName;
	private String mustBeOctet;
	private Hashtable details;

	final String charset = "Windows-1252";

	private static final int FIELD_FULLNAME = 0;
	private static final int FIELD_MUSTBEOCTET = 1;
	private static final int FIELD_DETAILNAME = 2;
	private static final int FIELD_DETAILVALUE = 3;

	/** Creates a new instance of Announcement */
	public Announcement (DatagramPacket packet) throws IOException
	{
		decodeDatagram (packet);
	}

	// just for compatibility with Serializable
	protected Announcement()
	{
	}

	private void decodeDatagram (DatagramPacket packet) throws IOException
	{
		byte[] data = packet.getData();
		int offset = packet.getOffset();			
		int startPos = offset +2;
		int length = packet.getLength();
		if ((data [offset] != (byte) 0) || (data [offset+1] != (byte) 2))
		{
			System.err.println ("Warning: First two bytes of datagram are not 0x00 0x02.");
		}

		int field=FIELD_FULLNAME;
		String detailName = "";
		details = new Hashtable();
		for (int pos=2; pos<length; pos++)
		{
			int currentPos = offset + pos;
			byte current = data [currentPos];
			if (current == (byte) 0)
			{
				String str = new String (data, startPos, currentPos-startPos, charset);
				startPos = currentPos+1;
				switch (field)
				{
					case FIELD_FULLNAME:
					{
						fullName = str;
						field = FIELD_MUSTBEOCTET;
						break;
					}
					case FIELD_MUSTBEOCTET:
					{
						mustBeOctet = str;
						if (!str.equals ("octet"))
						{
							System.err.println ("Warnung: second field is not 'octet'. My protocol implementation may not cope with that.");
						}
						field = FIELD_DETAILNAME;
						break;
					}
					case FIELD_DETAILNAME:
					{
						detailName = str;
						field = FIELD_DETAILVALUE;
						break;
					}
					case FIELD_DETAILVALUE:
					{
						details.put (detailName, str);
						field = FIELD_DETAILNAME;
						break;
					}
				}
			}
		}
	}

	public String getFullName()
	{
		return fullName;
	}

	public String getPlainName()
	{
		return fullName.substring (fullName.lastIndexOf ('\\')+1);
	}

	public String getDetail (String name)
	{
		return (String) details.get (name);
	}

	private static boolean areDetailsEqual (Hashtable h1, Hashtable h2, String detailName)
	{
		Object o1 = h1.get (detailName);
		Object o2 = h2.get (detailName);
		return (o1 == null && o2 == null) || ((o1 != null) && o1.equals (o2));
	}

	public boolean equals (Object o)
	{
		if (!(o instanceof Announcement))
			return false;

		Announcement other = (Announcement) o;
		if (((fullName == null && other.fullName == null) || ((fullName != null) && fullName.equals (other.fullName)))
			&& ((mustBeOctet == null && other.mustBeOctet == null) || ((mustBeOctet != null) && mustBeOctet.equals (other.fullName))))
		{
			if (details == null && other.details == null)
				return true;

			if (details == null)
				return false;

			return areDetailsEqual (details, other.details, "tsize")
				&& areDetailsEqual (details, other.details, "blksize")
				&& areDetailsEqual (details, other.details, "fileid");
		}
		else
		{
			return false;
		}
	}

	public String toString()
	{
		StringBuffer result = new StringBuffer (500);
		String newline = System.getProperty ("line.separator");
		result.append ("FullName: ").append (fullName).append (newline);
		result.append ("???: ").append (mustBeOctet).append (newline);
		Enumeration detailKeys = details.keys();
		while (detailKeys.hasMoreElements())
		{
			String key = (String) detailKeys.nextElement();
			String value = (String) details.get (key);
			result.append (key).append (": ").append (value).append (newline);
		}
		return result.toString();
	}
}
