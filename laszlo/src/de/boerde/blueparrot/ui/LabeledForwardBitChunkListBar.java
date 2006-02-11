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
 * LabeledBitSetBar.java
 *
 * Created on 3. Mai 2004, 21:03
 */

package de.boerde.blueparrot.ui;

import javax.swing.*;

import de.boerde.blueparrot.util.*;

/**
 *
 * @author  roland
 */
public class LabeledForwardBitChunkListBar extends JPanel
{
	private JLabel label;
	private ForwardBitChunkListBar bitSetBar;

	/** Creates a new instance of LabeledBitSetBar */
	public LabeledForwardBitChunkListBar ()
	{
		bitSetBar = new ForwardBitChunkListBar();
		init ("");
	}

	public LabeledForwardBitChunkListBar (int maxBit)
	{
		bitSetBar = new ForwardBitChunkListBar (maxBit);
		init ("");
	}

	public LabeledForwardBitChunkListBar (int maxBit, String text)
	{
		bitSetBar = new ForwardBitChunkListBar (maxBit);
		init (text);
	}

	public LabeledForwardBitChunkListBar (ForwardBitChunkList bitSet)
	{
		bitSetBar = new ForwardBitChunkListBar (bitSet);
		init ("");
	}

	public LabeledForwardBitChunkListBar (ForwardBitChunkList bitSet, String text)
	{
		bitSetBar = new ForwardBitChunkListBar (bitSet);
		init (text);
	}

	public LabeledForwardBitChunkListBar (ForwardBitChunkList bitSet, int maxBit)
	{
		bitSetBar = new ForwardBitChunkListBar (bitSet, maxBit);
		init ("");
	}

	public LabeledForwardBitChunkListBar (ForwardBitChunkList bitSet, int maxBit, String text)
	{
		bitSetBar = new ForwardBitChunkListBar (bitSet, maxBit);
		init (text);
	}

	private void init (String text)
	{
		setLayout (new OverlayLayout (this));
		label = new JLabel (text);
		label.setHorizontalAlignment (SwingConstants.CENTER);
		label.setVerticalAlignment (SwingConstants.CENTER);
		label.setAlignmentX (CENTER_ALIGNMENT);
		label.setAlignmentY (CENTER_ALIGNMENT);
		label.setOpaque (false);
		add (label);
		add (bitSetBar);
	}

	public JLabel getLabel()
	{
		return label;
	}

	public ForwardBitChunkListBar getBar()
	{
		return bitSetBar;
	}
}
