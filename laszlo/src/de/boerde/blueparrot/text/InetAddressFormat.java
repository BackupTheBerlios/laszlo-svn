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
 * InetAddressFormat.java
 *
 * Created on 8. Mai 2004, 22:06
 */

package de.boerde.blueparrot.text;

import java.net.*;
import java.text.*;

/**
 *
 * @author  roland
 */
public class InetAddressFormat extends Format
{
	/** Creates a new instance of InetAddressFormat */
	public InetAddressFormat()
	{
	}

	public StringBuffer format (Object obj, StringBuffer toAppendTo, FieldPosition pos)
	{
		if (obj instanceof InetAddress)
		{
			return toAppendTo.append (((InetAddress) obj).getHostAddress());
		}
		else
			throw new IllegalArgumentException ("Not an InetAddress.");
	}

	public InetAddress parse (String source) throws ParseException
	{
		return (InetAddress) parseObject (source);
	}

	public InetAddress parse (String source, ParsePosition pos)
	{
		return (InetAddress) parseObject (source, pos);
	}

	public Object parseObject (String source, ParsePosition pos)
	{
		if (source == null)
			return null;

		byte[] bytes = new byte[4];

		Number part = ipPartFormat.parse (source, pos);
		if (part == null)
			return null;
		int partNum = part.intValue();
		int index=pos.getIndex();
		if ((partNum < 0) || (partNum > 255))
		{
			pos.setErrorIndex (index);
			return null;
		}
		bytes [0] = (byte) partNum;

		if (source.charAt (index) != '.')
		{
			pos.setErrorIndex (index);
			return null;
		}
		pos.setIndex (index+1);

		part = ipPartFormat.parse (source, pos);
		if (part == null)
			return null;
		partNum = part.intValue();
		index=pos.getIndex();
		if ((partNum < 0) || (partNum > 255))
		{
			pos.setErrorIndex (index);
			return null;
		}
		bytes [1] = (byte) partNum;

		if (source.charAt (index) != '.')
		{
			pos.setErrorIndex (index);
			return null;
		}
		pos.setIndex (index+1);

		part = ipPartFormat.parse (source, pos);
		if (part == null)
			return null;
		partNum = part.intValue();
		index=pos.getIndex();
		if ((partNum < 0) || (partNum > 255))
		{
			pos.setErrorIndex (index);
			return null;
		}
		bytes [2] = (byte) partNum;

		if (source.charAt (index) != '.')
		{
			pos.setErrorIndex (index);
			return null;
		}
		pos.setIndex (index+1);

		part = ipPartFormat.parse (source, pos);
		if (part == null)
			return null;
		partNum = part.intValue();
		index=pos.getIndex();
		if ((partNum < 0) || (partNum > 255))
		{
			pos.setErrorIndex (index);
			return null;
		}
		bytes [3] = (byte) partNum;

		try
		{
			return InetAddress.getByAddress (bytes);
		}
		catch (UnknownHostException e)
		{
			pos.setErrorIndex (pos.getIndex());
			return null;
		}
	}

	private static DecimalFormat ipPartFormat = new DecimalFormat ("##0");
}
