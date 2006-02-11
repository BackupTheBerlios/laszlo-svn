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
 * InetAddressTextField.java
 *
 * Created on 6. Mai 2004, 22:35
 */

package de.boerde.blueparrot.ui;

import java.awt.*;
import java.net.*;
import java.text.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 *
 * @author  roland
 */
public class InetAddressTextField extends JFormattedTextField
{
	private JTextField formatField;

	/** Creates a new instance of InetAddressTextField */
	public InetAddressTextField()
	{
		super (new Formatter());
		formatField = new JTextField ("000.000.000.000");
		formatField.setFont (getFont());
		setHorizontalAlignment (RIGHT);
	}

	public InetAddress getInetAddress()
	{
		return (InetAddress) getValue();
	}

	public void setInetAddress (InetAddress inetAddress)
	{
		setValue (inetAddress);
	}

	public void setFont (Font font)
	{
		super.setFont (font);
		if (formatField != null)
		{
			formatField.setFont (getFont());
		}
	}

	public Dimension getMaximumSize()
	{
		Dimension actualMaximumSize = super.getMaximumSize();
		Dimension formatMaximumSize = formatField.getMaximumSize();
		if (actualMaximumSize.width > formatMaximumSize.width)
			return actualMaximumSize;
		else
			return formatMaximumSize;
	}

	public Dimension getMinimumSize()
	{
		return formatField.getMinimumSize();
	}

	public Dimension getPreferredSize()
	{
		Dimension actualPreferredSize = super.getPreferredSize();
		Dimension formatPreferredSize = formatField.getPreferredSize();
		if (actualPreferredSize.width > formatPreferredSize.width)
			return actualPreferredSize;
		else
			return formatPreferredSize;
	}

	public static class Formatter extends DefaultFormatter
	{
		public String valueToString (Object value) throws ParseException
		{
			if (value == null)
				return "";

			if (!(value instanceof InetAddress))
				throw new ParseException ("Not an InetAddress", 0);

			return ((InetAddress) value).getHostAddress();
		}

		public Object stringToValue (String string) throws ParseException
		{
			if (string == null)
				throw new ParseException ("Null String", 0);
			int points=0;
			int index=0;
			byte[] bytes = new byte[4];
			while (index < string.length())
			{
				int pointIndex = string.indexOf ('.', index);
				int component = points;
				if (pointIndex == -1)
				{
					pointIndex = string.length();
				}
				else
				{
					points++;
					if (points > 3)
					{
						throw new ParseException ("Too many dots", pointIndex);
					}
				}
				String numberstr = string.substring (index, pointIndex);
				try
				{
					int number = Integer.parseInt (numberstr);
					if (number < 0 || number > 255)
						throw new ParseException ("Number is not in range 0..255", index);
					bytes [component] = (byte) number;
				}
				catch (NumberFormatException e)
				{
					throw new ParseException ("Not A number", index);
				}
				index = pointIndex +1;
			}
			if (points < 3)
			{
				throw new ParseException ("Not three dots", 0);
			}
			try
			{
				return InetAddress.getByAddress (bytes);
			}
			catch (UnknownHostException e)
			{
				throw new ParseException ("Unknown host", 0);
			}
		}
	}
}
