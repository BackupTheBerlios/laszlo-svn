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
 * TransferStatusCellLabel.java
 *
 * Created on 3. Mai 2004, 23:09
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class TransferStatusCellLabel extends JLabel implements TableCellRenderer
{
	/** Creates a new instance of TransferStatusCellLabel */
	public TransferStatusCellLabel ()
	{
		setHorizontalAlignment (LEFT);
		setVerticalAlignment (CENTER);
		setAlignmentX (LEFT_ALIGNMENT);
		setAlignmentY (CENTER_ALIGNMENT);
		setHorizontalTextPosition (LEFT);
	}

	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		Color background = isSelected ? table.getSelectionBackground() : table.getBackground();
		Color foreground = isSelected ? table.getSelectionForeground() : table.getForeground();
		if (value instanceof ContentReader)
		{
			ContentReader reader = (ContentReader) value;
			Announcement announcement = reader.getAnnouncement();
			//String transferName = announcement.getFullName();
			BookingAnnouncement pkginfo = reader.getXmlAnnouncement();
			String text = null;
			if (pkginfo != null)
				text = pkginfo.getUrl();
			if (text == null)
				text = announcement.getPlainName();

			if (reader.isDone())
			{
				background = background.brighter();
				foreground = foreground.brighter();
				text = "<" + text + ">";
			}
			setText (text);
 		}
		else
		{
			setText ("");
		}
		setBackground (background);
		setForeground (foreground);
		return this;
	}
}
