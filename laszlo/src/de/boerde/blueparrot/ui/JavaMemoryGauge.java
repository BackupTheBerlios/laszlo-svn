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
 * JavaMemoryGauge.java
 *
 * Created on 9. Mai 2004, 19:34
 */

package de.boerde.blueparrot.ui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;

import de.boerde.blueparrot.text.*;

/**
 *
 * @author  roland
 */
public class JavaMemoryGauge extends JPanel
{
	private int gaugeOrientation = USE_COMPONENTORIENTATION;
	private Diagram diagram = new Diagram();
	private JLabel label = new JLabel();

	final private static int ORIENTATION_MIN = 0;
	final public static int USE_COMPONENTORIENTATION = 0;
	final public static int HORIZONTAL = 1;
	final public static int VERTICAL = 2;
	final private static int ORIENTATION_MAX = 2;

	/** Creates a new instance of JavaMemoryGauge */
	public JavaMemoryGauge()
	{
		super (new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets (2, 2, 2, 2);
		constraints.weightx = 1;
		constraints.weighty = 1;
		JPanel innerPanel = new JPanel();
		innerPanel.setLayout (new OverlayLayout (innerPanel));
		label.setOpaque (false);
		label.setAlignmentX (Component.CENTER_ALIGNMENT);
		label.setAlignmentY (Component.CENTER_ALIGNMENT);
		label.setHorizontalAlignment (SwingConstants.CENTER);
		label.setVerticalAlignment (SwingConstants.CENTER);
		diagram.setAlignmentX (Component.CENTER_ALIGNMENT);
		diagram.setAlignmentY (Component.CENTER_ALIGNMENT);
		innerPanel.add (label);
		innerPanel.add (diagram);
		add (innerPanel, constraints);
		addComponentListener (new ComponentListener());
		thread = new UpdateThread();
		thread.start();
	}

	public void setGaugeOrientation (int gaugeOrientation)
	{
		if (gaugeOrientation < ORIENTATION_MIN || gaugeOrientation > ORIENTATION_MAX)
		{
			throw new IllegalArgumentException ("Invalid Gauge Orientation " + gaugeOrientation);
		}
		int oldGaugeOrientation = this.gaugeOrientation;
		this.gaugeOrientation = gaugeOrientation;
		firePropertyChange ("gaugeOrientation", oldGaugeOrientation, gaugeOrientation);
		if (gaugeOrientation != oldGaugeOrientation)
		{
			revalidate();
			repaint();
		}
	}

	public int getGaugeOrientation()
	{
		return gaugeOrientation;
	}

	private class Diagram extends JPanel
	{
		public void paint (Graphics g)
		{
			int height = getHeight();
			int width = getWidth();
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			long inuseMemory = totalMemory - freeMemory;
			Color max = Color.green;
			Color total = Color.yellow;
			Color inuse = Color.red;
			boolean verticalOrientation = isActuallyVertical();
			double bitLength = (double) (verticalOrientation ? height : width) / (double) maxMemory;
			int inuseWidth = (int) (inuseMemory * bitLength);
			int totalWidth = (int) (totalMemory * bitLength);
			//int freeWidth = (int) (freeMemory * bitLength);

			if (verticalOrientation)
			{
				g.setColor (inuse);
				g.fillRect (0, 0, width, inuseWidth);
				g.setColor (total);
				g.fillRect (0, inuseWidth, width, totalWidth-inuseWidth);
				g.setColor (max);
				g.fillRect (0, totalWidth, width, height-totalWidth);
			}
			else
			{
				g.setColor (inuse);
				g.fillRect (0, 0, inuseWidth, height);
				g.setColor (total);
				g.fillRect (inuseWidth, 0, totalWidth-inuseWidth, height);
				g.setColor (max);
				g.fillRect (totalWidth, 0, width-totalWidth, height);
			}
		}
	}

	private boolean isActuallyVertical()
	{
		boolean verticalOrientation;
		if (gaugeOrientation == USE_COMPONENTORIENTATION)
		{
			verticalOrientation = !getComponentOrientation().isHorizontal();
		}
		else
		{
			verticalOrientation = gaugeOrientation == VERTICAL;
		}
		return verticalOrientation;
	}

	private Thread thread;

	private class UpdateThread extends Thread
	{
		UpdateThread()
		{
			super ("JavaMemoryGauge update thread");
		}

		public void run ()
		{
			Runtime runtime = Runtime.getRuntime();
			int updateFrequency = 0;
			try
			{
				updateFrequency = Integer.parseInt (System.getProperty ("fuldix.JavaMemoryGauge.update"));
			}
			catch (Throwable t)
			{
			}
			if (updateFrequency <= 0)
			{
				updateFrequency = 800;
			}
			while (true)
			{
				try
				{
					Thread.sleep (updateFrequency);
				}
				catch (InterruptedException e)
				{
					if (thread == null)
						break;
				}
				if (thread != this)
					break;

				long maxMemory = runtime.maxMemory();
				long totalMemory = runtime.totalMemory();
				long freeMemory = runtime.freeMemory();
				long inuseMemory = totalMemory - freeMemory;
				StringBuffer text = new StringBuffer();
				String separator;
				if (isActuallyVertical())
				{
					separator = "\n";
				}
				else
				{
					separator = " / ";
				}
				sizeFormat.format (inuseMemory, text, zeroFieldPos);
				text.append (separator);
				sizeFormat.format (totalMemory, text, zeroFieldPos);
				text.append (separator);
				sizeFormat.format (maxMemory, text, zeroFieldPos);
				label.setText (text.toString());
				repaint();
			}
		}
	}

	private class ComponentListener extends ComponentAdapter
	{
		public void componentShown (ComponentEvent e)
		{
			synchronized (JavaMemoryGauge.this)
			{
				if (thread == null)
				{
					thread = new UpdateThread();
					thread.start();
				}
			}
		}

		public void componentHidden (ComponentEvent e)
		{
			synchronized (JavaMemoryGauge.this)
			{
				if (thread != null)
				{
					Thread oldThread = thread;
					thread = null;
					oldThread.interrupt();
				}
			}
		}
	}

	private static ComputerSizeFormat sizeFormat = new ComputerSizeFormat();
	private static FieldPosition zeroFieldPos = new FieldPosition (0);
}
