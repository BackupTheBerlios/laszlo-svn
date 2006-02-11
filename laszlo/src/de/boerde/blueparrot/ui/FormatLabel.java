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


package de.boerde.blueparrot.ui;

import java.awt.*;
import javax.swing.*;

public class FormatLabel extends JLabel
{
	private JLabel formatLabel;

	public FormatLabel (String formatString)
	{
		formatLabel = new JLabel (formatString);
		formatLabel.setFont (getFont());
	}

	public void setFont (Font font)
	{
		super.setFont (font);
		if (formatLabel != null)
		{
			formatLabel.setFont (getFont());
		}
	}

	public Dimension getMaximumSize()
	{
		Dimension actualMaximumSize = super.getMaximumSize();
		Dimension formatMaximumSize = formatLabel.getMaximumSize();
		if (actualMaximumSize.width > formatMaximumSize.width)
			return actualMaximumSize;
		else
			return formatMaximumSize;
	}

	public Dimension getMinimumSize()
	{
		return formatLabel.getMinimumSize();
	}

	public Dimension getPreferredSize()
	{
		Dimension actualPreferredSize = super.getPreferredSize();
		Dimension formatPreferredSize = formatLabel.getPreferredSize();
		if (actualPreferredSize.width > formatPreferredSize.width)
			return actualPreferredSize;
		else
			return formatPreferredSize;
	}
}
