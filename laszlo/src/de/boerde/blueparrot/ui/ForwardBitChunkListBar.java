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
 * BitSetProgressBar.java
 *
 * Created on 3. Mai 2004, 19:27
 */

package de.boerde.blueparrot.ui;

import java.awt.*;
import javax.swing.*;

import de.boerde.blueparrot.util.*;

/**
 *
 * @author  roland
 */
public class ForwardBitChunkListBar extends JComponent
{
	private ForwardBitChunkList bitSet;
	private int maxBit;
	private int currentBit = -1;
	private Color currentBitColor = Color.blue;
	private Color currentBitLighterColor = Color.blue.brighter();
	private int barOrientation = USE_COMPONENTORIENTATION;

	final private static int ORIENTATION_MIN = 0;
	final public static int USE_COMPONENTORIENTATION = 0;
	final public static int HORIZONTAL = 1;
	final public static int VERTICAL = 2;
	final private static int ORIENTATION_MAX = 2;

	/** Creates a new instance of BitSetProgressBar */
	public ForwardBitChunkListBar ()
	{
		this (8);
	}

	public ForwardBitChunkListBar (int maxBit)
	{
		this (new ForwardBitChunkList(), maxBit);
	}

	public ForwardBitChunkListBar (ForwardBitChunkList bitSet)
	{
		this (bitSet, (bitSet != null && bitSet.length() > 0) ? bitSet.length() : 1);
	}

	public ForwardBitChunkListBar (ForwardBitChunkList bitSet, int maxBit)
	{
		if (bitSet == null)
		{
			throw new IllegalArgumentException ("BitSet cannot be null");
		}
		if (maxBit <= 0)
		{
			throw new IllegalArgumentException ("maxBit must be >0 but was given as " + maxBit);
		}
		this.bitSet = bitSet;
		this.maxBit = maxBit;
	}

	public void setData (ForwardBitChunkList bitSet)
	{
		if (bitSet == null)
		{
			throw new IllegalArgumentException ("ForwardBitChunkList cannot be null");
		}
		ForwardBitChunkList oldBitSet = this.bitSet;
		this.bitSet = bitSet;
		firePropertyChange ("bitSet", oldBitSet, bitSet);
		if ((bitSet != oldBitSet) && !bitSet.equals (oldBitSet))
		{
			revalidate();
			repaint();
		}
	}

	public ForwardBitChunkList getData()
	{
		return bitSet;
	}

	public void setMaxBit (int maxBit)
	{
		if (maxBit <= 0)
		{
			throw new IllegalArgumentException ("maxBit must be >0 but was given as " + maxBit);
		}
		int oldMaxBit = this.maxBit;
		this.maxBit = maxBit;
		firePropertyChange ("maxBit", oldMaxBit, maxBit);
		if (maxBit != oldMaxBit)
		{
			revalidate();
			repaint();
		}
	}

	public int getMaxBit()
	{
		return maxBit;
	}

	public void setCurrentBit (int currentBit)
	{
		int oldCurrentBit = this.currentBit;
		this.currentBit = currentBit;
		firePropertyChange ("currentBit", oldCurrentBit, currentBit);
		if (currentBit != oldCurrentBit)
		{
			revalidate();
			repaint();
		}
	}

	public int getCurrentBit()
	{
		return currentBit;
	}

	public void setBarOrientation (int barOrientation)
	{
		if (barOrientation < ORIENTATION_MIN || barOrientation > ORIENTATION_MAX)
		{
			throw new IllegalArgumentException ("Invalid Bar Orientation " + barOrientation);
		}
		int oldBarOrientation = this.barOrientation;
		this.barOrientation = barOrientation;
		firePropertyChange ("barOrientation", oldBarOrientation, barOrientation);
		if (barOrientation != oldBarOrientation)
		{
			revalidate();
			repaint();
		}
	}

	public int getBarOrientation()
	{
		return barOrientation;
	}

	public void paint (Graphics g)
	{
		int height = getHeight();
		int width = getWidth();
		Color background = getBackground();
		Color foreground = getForeground();
		g.setColor (background);
		g.fillRect (0, 0, width, height);
		g.setColor (foreground);
		boolean verticalOrientation;
		if (barOrientation == USE_COMPONENTORIENTATION)
		{
			verticalOrientation = !getComponentOrientation().isHorizontal();
		}
		else
		{
			verticalOrientation = barOrientation == VERTICAL;
		}
		double bitLength = (double) (verticalOrientation ? height : width) / (double) maxBit;
		for (int i=bitSet.nextSetBit(0), clearIndex; i>=0 && i<maxBit; i=bitSet.nextSetBit(clearIndex))
		{
			clearIndex = bitSet.nextClearBit (i);
			int pos1 = (int) (i*bitLength);
			int pos2;
			if ((clearIndex >= 0) && (clearIndex < maxBit))
				pos2 = (int) (clearIndex*bitLength) - pos1;
			else
				pos2 = (int) (maxBit*bitLength) - pos1;
			if (pos2 == 0)
				pos2 = 1;
			if (verticalOrientation)
			{
				g.fillRect (0, pos1, width, pos2);
			}
			else
			{
				g.fillRect (pos1, 0, pos2, height);
			}
		}
		if (currentBit >= 0 && currentBit < maxBit)
		{
			int pos = (int) ((currentBit+1)*bitLength)-1;
			if (verticalOrientation)
			{
				g.setColor (currentBitLighterColor);
				g.drawLine (0, pos-1, width-1, pos-1);
				g.setColor (currentBitColor);
				g.drawLine (0, pos, width-1, pos);
			}
			else
			{
				g.setColor (currentBitLighterColor);
				g.drawLine (pos-1, 0, pos-1, height-1);
				g.setColor (currentBitColor);
				g.drawLine (pos, 0, pos, height-1);
			}
		}
	}

	public Color getCurrentBitColor()
	{
		return currentBitColor;
	}

	public void setCurrentBitColor (Color color)
	{
		currentBitColor = color;
		currentBitLighterColor = color.brighter();
	}

	public static void main (String[] args)
	{
		JFrame frame = new JFrame ("ForwardBitChunkBar Test");
		Container root = frame.getContentPane();
		root.setLayout (new GridLayout (0, 1));
		java.util.Random random = new java.util.Random();
		ForwardBitChunkList[] set = new ForwardBitChunkList [30];
		ForwardBitChunkListBar[] bars = new ForwardBitChunkListBar [set.length];
		int[] len = new int [set.length];
		for (int i=0; i<set.length; i++)
		{
			int max = random.nextInt (500)+1;
			set [i] = new ForwardBitChunkList (max/3, 2*max/3);
			len [i] = max;
			ForwardBitChunkListBar bar = new ForwardBitChunkListBar (set[i], max+1);
			bar.setPreferredSize (new Dimension (800, 10));
			bar.setBackground (Color.green);
			bar.setForeground (Color.red);
			bar.setBarOrientation (HORIZONTAL);
			bars[i] = bar;
			root.add (bar);
		}
		frame.pack();

		frame.setVisible (true);

		System.out.println ("Start");
		for (int i=0; i<set.length; i++)
		{
			System.out.println ("Bar " + i);
			int l=len[i];
			for (int j=0; j<l; j++)
			{
				int value = random.nextInt (5);
				if (value < 4)
				{
					set[i].set (j);
					root.repaint();
				}
				else
				{
					set[i].clear (j);
//					System.out.print (j + " ");
				}
				bars[i].setCurrentBit (j);
				try
				{
					Thread.sleep (10);
				}
				catch (InterruptedException e)
				{
				}
			}
			bars[i].setCurrentBit (-1);
			System.out.println (set[i]);
		}
	}
}
