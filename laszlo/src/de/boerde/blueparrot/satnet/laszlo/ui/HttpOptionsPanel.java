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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.boerde.blueparrot.satnet.laszlo.Settings;
import de.boerde.blueparrot.ui.InetAddressTextField;

/**
 * 
 * @author roland
 */
public class HttpOptionsPanel extends JPanel implements OptionsPanel,
		ItemListener, DocumentListener, ChangeListener {
	private InetAddressTextField bindAddressField;

	private JSpinner ownPortField;

	private JTextField ownNameField;

	private ProxyModeComboBox fetchModeField;

	private JTextField upstreamProxyHostField;

	private JSpinner upstreamProxyPortField;

	private JLabel upstreamProxyHostLabel;

	private JLabel upstreamProxyPortLabel;

	private OptionsWindow window;

	/** Creates a new instance of ReceiveOptionsPanel */
	public HttpOptionsPanel(OptionsWindow window) {
		super(new GridBagLayout());
		this.window = window;
		Settings settings = Settings.getSettings();
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.fill = GridBagConstraints.NONE;
		labelConstraints.gridwidth = 1;
		labelConstraints.gridx = 1;
		labelConstraints.insets = new Insets(2, 2, 2, 2);
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.gridwidth = 3;
		fieldConstraints.gridx = 2;
		fieldConstraints.weightx = 10;
		fieldConstraints.insets = new Insets(2, 2, 2, 2);
		GridBagConstraints oneLineConstraints = new GridBagConstraints();
		oneLineConstraints.anchor = GridBagConstraints.LINE_START;
		oneLineConstraints.fill = GridBagConstraints.HORIZONTAL;
		oneLineConstraints.gridwidth = 4;
		oneLineConstraints.gridx = 1;
		oneLineConstraints.weightx = 10;
		oneLineConstraints.insets = new Insets(8, 8, 8, 8);
		GridBagConstraints middleFieldConstraints = new GridBagConstraints();
		middleFieldConstraints.anchor = GridBagConstraints.LINE_START;
		middleFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		middleFieldConstraints.gridwidth = 1;
		middleFieldConstraints.gridx = 2;
		middleFieldConstraints.weightx = 10;
		middleFieldConstraints.insets = new Insets(2, 2, 2, 2);
		GridBagConstraints backLabelConstraints = new GridBagConstraints();
		backLabelConstraints.anchor = GridBagConstraints.LINE_END;
		backLabelConstraints.fill = GridBagConstraints.NONE;
		backLabelConstraints.gridwidth = 1;
		backLabelConstraints.gridx = 3;
		backLabelConstraints.insets = new Insets(2, 2, 2, 2);
		GridBagConstraints endFieldConstraints = new GridBagConstraints();
		endFieldConstraints.anchor = GridBagConstraints.LINE_START;
		endFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		endFieldConstraints.gridwidth = 1;
		endFieldConstraints.gridx = 4;
		endFieldConstraints.insets = new Insets(2, 2, 2, 2);
		JLabel fetchModeLabel = new JLabel("Fetch missing: ");
		fetchModeField = new ProxyModeComboBox(settings.getHttpProxyFetching());
		fetchModeField.addItemListener(this);
		fetchModeLabel.setLabelFor(fetchModeField);
		add(fetchModeLabel, labelConstraints);
		add(fetchModeField, fieldConstraints);
		upstreamProxyHostLabel = new JLabel("Proxy Host: ");
		upstreamProxyHostField = new JTextField(settings
				.getUpstreamHttpProxyHost());
		upstreamProxyHostField.getDocument().addDocumentListener(this);
		upstreamProxyHostLabel.setLabelFor(upstreamProxyHostField);
		add(upstreamProxyHostLabel, labelConstraints);
		add(upstreamProxyHostField, middleFieldConstraints);
		upstreamProxyPortLabel = new JLabel(" Port ");
		upstreamProxyPortField = new JSpinner(new SpinnerNumberModel(settings
				.getUpstreamHttpProxyPort(), 1, 65535, 1));
		upstreamProxyPortField.setEditor(new JSpinner.NumberEditor(
				upstreamProxyPortField, "#####0"));
		upstreamProxyPortField.addChangeListener(this);
		upstreamProxyPortLabel.setLabelFor(upstreamProxyPortField);
		add(upstreamProxyPortLabel, backLabelConstraints);
		add(upstreamProxyPortField, endFieldConstraints);
		add(new JSeparator(), oneLineConstraints);
		JLabel ownNameLabel = new JLabel("Own Name: ");
		ownNameField = new JTextField(settings.getHttpOwnPseudoName());
		ownNameField.getDocument().addDocumentListener(this);
		ownNameLabel.setLabelFor(ownNameField);
		add(ownNameLabel, labelConstraints);
		add(ownNameField, fieldConstraints);
		JLabel bindAddressLabel = new JLabel("Bind Address: ");
		bindAddressField = new InetAddressTextField();
		bindAddressField.setInetAddress(settings.getLaszloHttpBindAddress());
		bindAddressField.getDocument().addDocumentListener(this);
		bindAddressLabel.setLabelFor(bindAddressField);
		add(bindAddressLabel, labelConstraints);
		add(bindAddressField, middleFieldConstraints);
		JLabel ownPortLabel = new JLabel(" Port ");
		ownPortField = new JSpinner(new SpinnerNumberModel(settings
				.getLaszloHttpPort(), 1, 65535, 1));
		ownPortField
				.setEditor(new JSpinner.NumberEditor(ownPortField, "#####0"));
		ownPortField.addChangeListener(this);
		ownPortLabel.setLabelFor(ownPortField);
		add(ownPortLabel, backLabelConstraints);
		add(ownPortField, endFieldConstraints);
		setEnabledStates();
	}

	public String getTabName() {
		return "Web";
	}

	public void applySettings() {
		Settings settings = Settings.getSettings();
		settings.setLaszloHttpBindAddress(bindAddressField.getInetAddress());
		Object ownPortVal = ownPortField.getValue();
		if ((ownPortVal != null) && (ownPortVal instanceof Number))
			settings.setLaszloHttpPort(((Number) ownPortVal).intValue());
		settings.setHttpOwnPseudoName(ownNameField.getText().trim());
		settings.setHttpProxyFetching(fetchModeField.getMode());
		settings.setUpstreamHttpProxyHost(upstreamProxyHostField.getText()
				.trim());
		Object upstreamProxyPortVal = upstreamProxyPortField.getValue();
		if ((upstreamProxyPortVal != null)
				&& (upstreamProxyPortVal instanceof Number))
			settings.setUpstreamHttpProxyPort(((Number) upstreamProxyPortVal)
					.intValue());
	}

	public void revertSettings() {
		Settings settings = Settings.getSettings();
		bindAddressField.setInetAddress(settings.getLaszloHttpBindAddress());
		ownPortField.setValue(new Integer(settings.getLaszloHttpPort()));
		ownNameField.setText(settings.getHttpOwnPseudoName());
		fetchModeField.setMode(settings.getHttpProxyFetching());
		upstreamProxyHostField.setText(settings.getUpstreamHttpProxyHost());
		upstreamProxyPortField.setValue(new Integer(settings
				.getUpstreamHttpProxyPort()));
	}

	public boolean isChangeInUI() {
		Settings settings = Settings.getSettings();
		return !(settings.getLaszloHttpBindAddress().equals(
				bindAddressField.getInetAddress())
				&& (((Number) ownPortField.getValue()).intValue() == settings
						.getLaszloHttpPort())
				&& ownNameField.getText().trim().equalsIgnoreCase(
						settings.getHttpOwnPseudoName())
				&& (fetchModeField.getMode().charAt(0) == settings
						.getHttpProxyFetching().charAt(0))
				&& upstreamProxyHostField.getText().trim().equalsIgnoreCase(
						settings.getUpstreamHttpProxyHost()) && (((Number) upstreamProxyPortField
				.getValue()).intValue() == settings.getUpstreamHttpProxyPort()));
	}

	private void setEnabledStates() {
		switch (fetchModeField.getMode().charAt(0)) {
		case 'u':
		case 'U': {
			upstreamProxyHostField.setEnabled(true);
			upstreamProxyPortField.setEnabled(true);
			upstreamProxyHostLabel.setEnabled(true);
			upstreamProxyPortLabel.setEnabled(true);
			break;
		}
		default: {
			upstreamProxyHostField.setEnabled(false);
			upstreamProxyPortField.setEnabled(false);
			upstreamProxyHostLabel.setEnabled(false);
			upstreamProxyPortLabel.setEnabled(false);
		}
		}
	}

	private void somethingChanged() {
		setEnabledStates();
		window.anOptionChangedInUI();
	}

	public void itemStateChanged(ItemEvent e) {
		somethingChanged();
	}

	public void removeUpdate(DocumentEvent e) {
		somethingChanged();
	}

	public void changedUpdate(DocumentEvent e) {
		somethingChanged();
	}

	public void insertUpdate(DocumentEvent e) {
		somethingChanged();
	}

	public void stateChanged(ChangeEvent e) {
		somethingChanged();
	}
}
