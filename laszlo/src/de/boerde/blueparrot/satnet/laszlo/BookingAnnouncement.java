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
 * BookingAnnouncement.java
 *
 * Created on 19. Mai 2004, 20:21
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;

/**
 *
 * @author  roland
 */
public class BookingAnnouncement implements Serializable
{
	private Hashtable contents;

	/** Creates a new instance of BookingAnnouncement */
	public BookingAnnouncement()
	{
		contents = new Hashtable();
	}

	public BookingAnnouncement (Element xml)
	{
		contents = new Hashtable();
		xml2Hashtable (xml, contents);
	}

	public void setContents (Element xml)
	{
		contents = new Hashtable();
		xml2Hashtable (xml, contents);
	}

	private static void xml2Hashtable (Element xml, Hashtable hashtable)
	{
		NodeList children = xml.getChildNodes();
		for (int c=0; c<children.getLength(); c++)
		{
			Node child = children.item (c);
			if ((child instanceof Element) && (child.getNodeType() == Node.ELEMENT_NODE))
			{
				Element childElement = (Element) child;
				//NodeList subchildren = xml.getChildNodes();
				boolean subchildrenAlreadyWentDown = false;
//				for (int s=0; s<subchildren.getLength(); s++)
				{
//					Node subchild = subchildren.item (s);
					Node subchild = child.getFirstChild();
					if ((subchild instanceof Element) && (subchild.getNodeType() == Node.ELEMENT_NODE))
					{
						if (!subchildrenAlreadyWentDown)
						{
							subchildrenAlreadyWentDown = true;
							Hashtable subHashtable = new Hashtable();
							xml2Hashtable (childElement, subHashtable);
							String tagName = childElement.getTagName();
							//	System.out.println ("H " + tagName + " " + subchild);
							Object existing = hashtable.get (tagName);
							if (existing == null)
							{
								hashtable.put (tagName, subHashtable);
							}
							else if (existing instanceof Vector)
							{
								Vector existingVector = (Vector) existing;
								existingVector.add (subHashtable);
							}
							else
							{
								Vector newVector = new Vector();
								newVector.add (existing);
								newVector.add (subHashtable);
								hashtable.put (tagName, newVector);
							}
						}
					}
					else if ((subchild instanceof Text) && (subchild.getNodeType() == Node.TEXT_NODE))
					{
						Text subText = (Text) subchild;
						String tagName = childElement.getTagName();
						//System.out.println ("T " + tagName + " " + subText.getData());
						Object existing = hashtable.get (tagName);
						if (existing == null)
						{
							hashtable.put (tagName, subText.getData());
						}
						else if (existing instanceof Vector)
						{
							Vector existingVector = (Vector) existing;
							existingVector.add (subText.getData());
						}
						else
						{
							Vector newVector = new Vector();
							newVector.add (existing);
							newVector.add (subText.getData());
							hashtable.put (tagName, newVector);
						}
					}
//					else
						//System.out.println ("N " + subchild);
				}
			}
		}
	}

	public String getOneValue (String path)
	{
		return getOneValue (contents, path);
	}

	private static String getOneValue (Hashtable hashtable, String remainingPath)
	{
		int pos = remainingPath.indexOf ('/');
		boolean isLast = pos < 0;
		String token = isLast ? remainingPath : remainingPath.substring (0, pos);
		Object obj = hashtable.get (token);
		if (obj == null)
			return null;

		if (obj instanceof Vector)
		{
			//String newRemain = remainingPath.substring (pos+1);
			Vector vector = (Vector) obj;
			for (int v=0; v<vector.size(); v++)
			{
				Object part = vector.get (v);
				if (!isLast && (part instanceof Hashtable))
				{
					String result = getOneValue ((Hashtable) part, remainingPath.substring (pos+1));
					if (result != null)
					{
						return result;
					}
				}
				if (isLast && (part instanceof String))
				{
					return (String) part;
				}
			}
		}
		if (!isLast && (obj instanceof Hashtable))
		{
			return getOneValue ((Hashtable) obj, remainingPath.substring (pos+1));
		}
		else if (isLast && (obj instanceof String))
		{
			return (String) obj;
		}
		else
			return null;
	}

	public String getUrl()
	{
		return getOneValue ("URL");
	}

	public String getServiceName()
	{
		return getOneValue ("SERVICE_NAME");
	}

	public String getPackageName()
	{
		return getOneValue ("PACKAGE_NAME");
	}

	public int getSize()
	{
		try
		{
			return Integer.parseInt (getOneValue ("SIZE"));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	public String getDescription()
	{
		return getOneValue ("DESCRIPTION");
	}

	public String getIndexHtml()
	{
		return getOneValue ("INDEX_HTML");
	}

	public long getTransmissionTime()
	{
		String value = getOneValue ("TRANSMISSION_TIME");
		return getTimeFromYYYYMMDDHHMM (value);
	}

	public long getUpdatedTime()
	{
		String value = getOneValue ("UPDATE_INFO/UPDATED");
		return getTimeFromYYYYMMDDHHMM (value);
	}

	private static long getTimeFromYYYYMMDDHHMM (String value)
	{
		if (value.length() == 12)
		{
			try
			{
				int year = Integer.parseInt (value.substring (0, 4));
				int month = Integer.parseInt (value.substring (4, 6));
				int day = Integer.parseInt (value.substring (6, 8));
				int hour = Integer.parseInt (value.substring (8, 10));
				int minute = Integer.parseInt (value.substring (10, 12));
				GregorianCalendar calendar = new GregorianCalendar (year, month-1-GregorianCalendar.JANUARY, day, hour, minute, 0);
				return calendar.getTimeInMillis();
			}
			catch (NumberFormatException e)
			{
				return 0;
			}
		}
		else
			return 0;
	}

	public String toString()
	{
		return contents.toString();
	}
}
