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
 * ComputerSizeFormat.java
 *
 * Created on 9. Mai 2004, 15:44
 */

package de.boerde.blueparrot.text;

import java.text.*;

/**
 *
 * @author  roland
 */
public class ComputerSizeFormat extends Format
{
	/** Creates a new instance of ComputerSizeFormat */
	public ComputerSizeFormat()
	{
	}

	public String format (int size)
	{
		return format (new Integer (size), new StringBuffer(), new FieldPosition (0)).toString();
	}

	public StringBuffer format (int value, StringBuffer toAppendTo, FieldPosition pos)
	{
		return format (new Integer (value), toAppendTo, pos);
	}

	public StringBuffer format (long value, StringBuffer toAppendTo, FieldPosition pos)
	{
		return format (new Long (value), toAppendTo, pos);
	}

	public StringBuffer format (Object obj, StringBuffer toAppendTo, FieldPosition pos)
	{
		if (! (obj instanceof Number))
			throw new IllegalArgumentException ("Not a Number");

		Number number = (Number) obj;
		double value = number.doubleValue();
		int mark=0;
		while (value > 1024 && mark < marks.length)
		{
			mark++;
			value /= 1024;
		}
		DecimalFormat format;
		if (value > 100 || mark == 0)
			format = decimalFormat0;
		else if (value > 10)
			format = decimalFormat1;
		else
			format = decimalFormat2;

		toAppendTo = format.format (new Double (value), toAppendTo, pos);
		if (mark != 0)
			toAppendTo.append (' ');
		toAppendTo.append (marks [mark]);

		return toAppendTo;
	}

	public Object parseObject (String source, ParsePosition pos)
	{
		Object obj = decimalFormat3.parseObject (source, pos);
		if (!(obj instanceof Number))
		{
			pos.setErrorIndex (pos.getIndex());
			return null;
		}

		Number number = (Number) obj;
		double value = number.doubleValue();
		int index = pos.getIndex();
		if (index < source.length() && (source.charAt (index) == ' '))
		{
			index++;
			pos.setIndex (index);
		}
		boolean adjust = false;
		for (int i=marks.length-1; i>= 0; i++)
		{
			if (adjust)
			{
				value *= 1024;
			}
			else
			{
				String m = marks [i];
				int indexPlusMlength = index + m.length();
				if (source.length() >= indexPlusMlength)
				{
					String nextPart = source.substring (index, indexPlusMlength);
					if (nextPart.equalsIgnoreCase (m))
					{
						pos.setIndex (indexPlusMlength);
						adjust = true;
					}
				}
			}
		}
		return new Integer ((int) value);
	}

	private static DecimalFormat decimalFormat0 = new DecimalFormat ("###########0");
	private static DecimalFormat decimalFormat1 = new DecimalFormat ("###########0.0");
	private static DecimalFormat decimalFormat2 = new DecimalFormat ("###########0.00");
	private static DecimalFormat decimalFormat3 = new DecimalFormat ("###########0.000");
	private static String[] marks = { "", "KiB", "MiB", "GiB" };
}
