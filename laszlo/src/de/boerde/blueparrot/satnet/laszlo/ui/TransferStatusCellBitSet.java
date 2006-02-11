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
 * TransferStatusCellBitSet.java
 *
 * Created on 3. Mai 2004, 23:09
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.text.*;
import de.boerde.blueparrot.ui.*;
import de.boerde.blueparrot.util.*;

/**
 *
 * @author  roland
 */
public class TransferStatusCellBitSet extends LabeledForwardBitChunkListBar implements TableCellRenderer
{
	private ForwardBitChunkListBar bar;
	private JLabel label;

	final private static ForwardBitChunkList dummy = new ForwardBitChunkList();

	/** Creates a new instance of TransferStatusCellBitSet */
	public TransferStatusCellBitSet ()
	{
		bar = getBar();
		label = getLabel();
		bar.setBarOrientation (ForwardBitChunkListBar.HORIZONTAL);
	}

	public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		Color barBackground;
		Color barForeground;
		Color background = isSelected ? table.getSelectionBackground() : table.getBackground();
		Color foreground = isSelected ? table.getSelectionForeground() : table.getForeground();
		if (value instanceof ContentReader)
		{
			ContentReader reader = (ContentReader) value;
			//Announcement announcement = reader.getAnnouncement();
			int totalSize = reader.getTotalBytes();
			int receivedSize = reader.getReceivedBytes();
			int missingSize = totalSize - receivedSize;
			bar.setMaxBit (reader.getTotalNumberOfPackets());
			bar.setCurrentBit (reader.getLastReceivedSequence());
			bar.setData (reader.getPartsToReceive());
			StringBuffer text = percentageFormat.format ((double) receivedSize / (double) totalSize, new StringBuffer(), zeroFieldPos).append (" of ");
			sizeFormat.format (totalSize, text, zeroFieldPos);
			if (!reader.isDone())
			{
				barBackground = green;
				barForeground = red;
				if (missingSize > 0)
				{
					text.append (" (");
					sizeFormat.format (missingSize, text, zeroFieldPos);
					text.append (" to go)");
				}
			}
			else
			{
				background = background.brighter();
				foreground = foreground.brighter();
				barBackground = lightGreen;
				barForeground = lightRed;
				if (missingSize > 0)
				{
					text.append (" (");
					sizeFormat.format (missingSize, text, zeroFieldPos);
					text.append (" missing)");
				}
			}
			label.setText (text.toString());
 		}
		else
		{
			label.setText ("");
			bar.setMaxBit (1);
			bar.setCurrentBit (-1);
			bar.setData (dummy);
			barBackground = table.getBackground();
			barForeground = table.getForeground();
		}
		label.setBackground (background);
		label.setForeground (foreground);
		bar.setBackground (barBackground);
		bar.setForeground (barForeground);
		return this;
	}

	private static DecimalFormat percentageFormat = new DecimalFormat ("##0%");
	private static ComputerSizeFormat sizeFormat = new ComputerSizeFormat();
	private static FieldPosition zeroFieldPos = new FieldPosition (0);
	private static Color green = new Color (0x00ff00);
	private static Color red = new Color (0xff0000);
	private static Color lightGreen = new Color (0x99ff99);
	private static Color lightRed = new Color (0xff9999);
}
