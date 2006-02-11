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
 * ReceiveOptionsPanel.java
 *
 * Created on 5. Mai 2004, 20:41
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class DirectoryOptionsPanel extends JPanel implements OptionsPanel, DocumentListener
{
	private JTextField dirField;
	private JButton dirChooserButton;
	private JFileChooser dirChooser;
	private OptionsWindow window;

	public DirectoryOptionsPanel (final OptionsWindow window)
	{
		super (new GridBagLayout());
		this.window = window;
		Settings settings = Settings.getSettings();
		String workDirectory = settings.getWorkDirectory();
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.fill = GridBagConstraints.NONE;
		labelConstraints.gridwidth =1;
		labelConstraints.insets = new Insets (2, 2, 2, 2);
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.anchor = GridBagConstraints.CENTER;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.gridwidth = 1;
		fieldConstraints.weightx = 10;
		fieldConstraints.insets = new Insets (2, 2, 2, 0);
		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.anchor = GridBagConstraints.LINE_START;
		buttonConstraints.fill = GridBagConstraints.NONE;
		buttonConstraints.gridwidth = GridBagConstraints.REMAINDER;
		buttonConstraints.insets = new Insets (2, 0, 2, 2);
		JLabel dirLabel = new JLabel ("Work Directory: ");
		dirField = new JTextField (workDirectory);
		dirField.getDocument().addDocumentListener (this);
		dirLabel.setLabelFor (dirField);
		JButton dirChooserButton = new JButton ("...");
		dirChooserButton.addActionListener (new FileChooserActionListener());
		dirChooser = new JFileChooser();
		dirChooser.setApproveButtonToolTipText ("Set the Lazlo work directory");
		dirChooser.setApproveButtonText ("Set");
		dirChooser.setApproveButtonMnemonic ('S');
		dirChooser.setDialogTitle ("Laszlo work directory");
		dirChooser.setDialogType (dirChooser.CUSTOM_DIALOG);
		dirChooser.setFileSelectionMode (dirChooser.DIRECTORIES_ONLY);
		dirChooser.setSelectedFile (new File (workDirectory));
		GridBagConstraints warningConstraints = new GridBagConstraints();
		warningConstraints.anchor = GridBagConstraints.CENTER;
		warningConstraints.fill = GridBagConstraints.VERTICAL;
		warningConstraints.gridwidth =3;
		warningConstraints.insets = new Insets (6, 6, 6, 6);
		add (dirLabel, labelConstraints);
		add (dirField, fieldConstraints);
		add (dirChooserButton, buttonConstraints);
		add (new JLabel ("<html><b>Warning:</b> This directory must be under full control by Laszlo.<br>Laszlo may create, change and delete files in this directory<br>without further notice. When you change the directory,<br>make sure you choose one that is big enough and empty."), warningConstraints);
	}

	public String getTabName()
	{
		return "Directory";
	}

	public void applySettings()
	{
		Settings settings = Settings.getSettings();
		settings.setWorkDirectory (dirField.getText());
	}

	public void revertSettings()
	{
		Settings settings = Settings.getSettings();
		dirField.setText (settings.getWorkDirectory());
	}

	public boolean isChangeInUI()
	{
		Settings settings = Settings.getSettings();
		return !dirField.getText().equals (settings.getWorkDirectory());
	}

	private void textFieldChanged()
	{
		window.anOptionChangedInUI();
	}

	public void changedUpdate (DocumentEvent e)
	{
		textFieldChanged();
	}

	public void insertUpdate (DocumentEvent e)
	{
		textFieldChanged();
	}

	public void removeUpdate (DocumentEvent e)
	{
		textFieldChanged();
	}

	private class FileChooserActionListener implements ActionListener
	{
		public void actionPerformed (ActionEvent e)
		{
			File dir = new File (dirField.getText());
			if (dir.exists() && dir.isDirectory())
				dirChooser.setSelectedFile (dir);
			int selection = dirChooser.showDialog (DirectoryOptionsPanel.this, "Set");
			if (selection == dirChooser.APPROVE_OPTION)
			{
				dirField.setText (dirChooser.getSelectedFile().getAbsolutePath());
			}
		}
	}
}
