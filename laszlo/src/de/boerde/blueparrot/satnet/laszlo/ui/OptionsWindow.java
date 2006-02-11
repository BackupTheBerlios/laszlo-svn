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
 * OptionsWindow.java
 *
 * Created on 5. Mai 2004, 21:36
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.ui.*;

/**
 *
 * @author  roland
 */
public class OptionsWindow extends JFrame implements WindowListener, OkCancelApplyPanel.PerformingListener
{
	private OptionsPanel[] panels = { new ReceiveOptionsPanel (this), new DirectoryOptionsPanel (this), new HttpOptionsPanel (this), new NntpOptionsPanel (this) };
	private OkCancelApplyPanel buttonPanel;

	/** Creates a new instance of OptionsWindow */
	public OptionsWindow()
	{
		super ("Laszlo Options");
		Container root = getContentPane();
		root.setLayout (new BorderLayout (4, 4));
		JTabbedPane tabs = new JTabbedPane();
		for (int p=0; p<panels.length; p++)
		{
			OptionsPanel panel = panels [p];
			tabs.addTab (panel.getTabName(), (Component) panel);
		}
		root.add (tabs, BorderLayout.CENTER);
		buttonPanel = new OkCancelApplyPanel();
		buttonPanel.addPerformingListener (this);
		root.add (buttonPanel, BorderLayout.SOUTH);
		addWindowListener (this);
	}

	private static OptionsWindow theWindow = new OptionsWindow();
	static
	{
		theWindow.pack();
	}

	public static synchronized void makeVisible()
	{
		if (theWindow.isVisible())
			theWindow.toFront();
		else
		{
			theWindow.performRevert();
			theWindow.setVisible (true);
		}
	}

	public void anOptionChangedInUI()
	{
		for (int p=0; p<panels.length; p++)
		{
			OptionsPanel panel = panels [p];
			if (panel.isChangeInUI())
			{
				buttonPanel.enableForApplyable();
				return;
			}
		}
		buttonPanel.disableForOriginal();
	}

	private void performRevert()
	{
		for (int p=0; p<panels.length; p++)
		{
			OptionsPanel panel = panels [p];
			panel.revertSettings();
		}
		buttonPanel.disableForOriginal();
	}

	public void performApply (ActionEvent evt)
	{
		for (int p=0; p<panels.length; p++)
		{
			OptionsPanel panel = panels [p];
			panel.applySettings();
		}
		buttonPanel.disableForOriginal();
		Settings settings = Settings.getSettings();
		try
		{
			settings.save();
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog (this, "Error: Could not save configuration.", "Laszlo Error", JOptionPane.ERROR_MESSAGE);
		}
		settings.commitChanges();
	}

	public void performClose (ActionEvent evt)
	{
		setVisible (false);
	}

	public void windowClosing (WindowEvent e)
	{
		setVisible (false);
	}

	public void windowActivated (WindowEvent e)
	{
	}

	public void windowClosed (WindowEvent e)
	{
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
