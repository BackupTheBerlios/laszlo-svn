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
 * XMLHelpers.java
 *
 * Created on 3. Oktober 2004, 14:31
 */

package de.boerde.blueparrot.xml;

import org.w3c.dom.*;

/**
 *
 * @author  roland
 */
public class XMLHelpers
{
	/** Creates a new instance of XMLHelpers */
	private XMLHelpers ()
	{
	}

	public static String getContentText (Element elementNode)
	{
		StringBuffer result = new StringBuffer();
		NodeList children = elementNode.getChildNodes();
		for (int c=0; c<children.getLength(); c++)
		{
			Node child = children.item (c);
			if ((child.getNodeType() == Node.TEXT_NODE) && (child instanceof Text))
			{
				String name = ((Text) child).getData();
				result.append (name);
			}
		}
		return result.toString();
	}	
}
