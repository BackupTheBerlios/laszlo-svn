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
 * TransferStatusWindow.java
 *
 * Created on 3. Mai 2004, 22:47
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class TransferStatusWindow extends JFrame implements WindowListener
{
	private Toolbar toolbar;
	private TransferStatusPane statusPane;

	/** Creates a new instance of TransferStatusWindow */
	public TransferStatusWindow (Receiver recv)
	{
		super ("Laszlo Status");
		addWindowListener (this);
		Container root = getContentPane();
		root.setLayout (new BorderLayout());
		toolbar = new Toolbar();
		root.add (toolbar, BorderLayout.NORTH);
		statusPane = new TransferStatusPane (recv);
		JScrollPane scrollPane = new JScrollPane (statusPane);
		scrollPane.setHorizontalScrollBarPolicy (scrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy (scrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		root.add (scrollPane, BorderLayout.CENTER);
	}

	public TransferStatusPane getStatusPane()
	{
		return statusPane;
	}

	public void windowActivated (WindowEvent e)
	{
	}

	public void windowClosed (WindowEvent e)
	{
	}

	public void windowClosing (WindowEvent e)
	{
		setVisible (false);
		System.exit (0);
	}

	public void windowDeactivated (WindowEvent e)
	{
	}

	public void windowDeiconified (WindowEvent e)
	{
	}

	public void windowIconified (WindowEvent e)
	{
	}

	public void windowOpened (WindowEvent e)
	{
	}
}
