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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.boerde.blueparrot.satnet.laszlo.Settings;
import de.boerde.blueparrot.ui.InetAddressTextField;
import de.boerde.blueparrot.ui.NetworkInterfaceComboBox;

/**
 * 
 * @author roland
 */
public class ReceiveOptionsPanel extends JPanel implements OptionsPanel,
		ItemListener, DocumentListener, ChangeListener {
	private NetworkInterfaceComboBox localInterfaceField;

	private InetAddressTextField multicastAddressField;

	private JSpinner announcementPortField;

	private JCheckBox receiveUseMulticastPoolField;

	private OptionsWindow window;

	/** Creates a new instance of ReceiveOptionsPanel */
	public ReceiveOptionsPanel(OptionsWindow window) {
		super(new GridBagLayout());
		this.window = window;
		Settings settings = Settings.getSettings();
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.fill = GridBagConstraints.NONE;
		labelConstraints.gridwidth = GridBagConstraints.RELATIVE;
		labelConstraints.insets = new Insets(2, 2, 2, 2);
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.anchor = GridBagConstraints.LINE_START;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.insets = new Insets(2, 2, 2, 2);
		GridBagConstraints receiverConstraints = new GridBagConstraints();
		receiverConstraints.anchor = GridBagConstraints.CENTER;
		receiverConstraints.fill = GridBagConstraints.HORIZONTAL;
		receiverConstraints.gridwidth = GridBagConstraints.REMAINDER;
		JLabel localInterfaceLabel = new JLabel("DVB Interface: ");
		localInterfaceField = new NetworkInterfaceComboBox();
		localInterfaceField.setNetworkInterface(settings.getDVBInterface());
		localInterfaceField.addItemListener(this);
		localInterfaceLabel.setLabelFor(localInterfaceField);
		add(localInterfaceLabel, labelConstraints);
		add(localInterfaceField, fieldConstraints);
		JPanel receiverPanel = new JPanel(new GridBagLayout());
		JLabel multicastAddressLabel = new JLabel("Multicast IP: ");
		multicastAddressField = new InetAddressTextField();
		multicastAddressField.setInetAddress(settings.getMulticastIP());
		multicastAddressField.getDocument().addDocumentListener(this);
		multicastAddressLabel.setLabelFor(multicastAddressField);
		receiverPanel.add(multicastAddressLabel, labelConstraints);
		receiverPanel.add(multicastAddressField, fieldConstraints);
		JLabel announcementPortLabel = new JLabel("Announcement Port: ");
		announcementPortField = new JSpinner(new SpinnerNumberModel(settings
				.getAnnouncementPort(), 0, 65535, 1));
		announcementPortField.setEditor(new JSpinner.NumberEditor(
				announcementPortField, "#####0"));
		announcementPortField.addChangeListener(this);
		announcementPortLabel.setLabelFor(announcementPortField);
		receiverPanel.add(announcementPortLabel, labelConstraints);
		receiverPanel.add(announcementPortField, fieldConstraints);
		add(receiverPanel, receiverConstraints);
		receiverPanel = new JPanel(new GridBagLayout());
		JLabel receiveUseMulticastPoolLabel = new JLabel("Use Multicast Pool");
		receiveUseMulticastPoolField = new JCheckBox();
		receiveUseMulticastPoolField.setSelected(settings
				.getReceiveUseMulticastPool());
		receiveUseMulticastPoolField.addChangeListener(this);
		receiveUseMulticastPoolLabel.setLabelFor(receiveUseMulticastPoolField);
		receiverPanel.add(receiveUseMulticastPoolField, labelConstraints);
		receiverPanel.add(receiveUseMulticastPoolLabel, fieldConstraints);
		add(receiverPanel, receiverConstraints);
	}

	public String getTabName() {
		return "Receive";
	}

	public void applySettings() {
		Settings settings = Settings.getSettings();
		settings.setDVBInterface(localInterfaceField.getNetworkInterface());
		settings.setMulticastIP(multicastAddressField.getInetAddress());
		Object portVal = announcementPortField.getValue();
		if ((portVal != null) && (portVal instanceof Number))
			settings.setAnnouncementPort(((Number) portVal).intValue());
		settings.setReceiveUseMulticastPool(receiveUseMulticastPoolField
				.isSelected());
	}

	public void revertSettings() {
		Settings settings = Settings.getSettings();
		localInterfaceField.setNetworkInterface(settings.getDVBInterface());
		multicastAddressField.setInetAddress(settings.getMulticastIP());
		announcementPortField.setValue(new Integer(settings
				.getAnnouncementPort()));
		receiveUseMulticastPoolField.setSelected(settings
				.getReceiveUseMulticastPool());
	}

	public boolean isChangeInUI() {
		Settings settings = Settings.getSettings();
		return !(settings.getDVBInterface() != null
				&& settings.getDVBInterface().equals(
						localInterfaceField.getNetworkInterface())
				&& settings.getMulticastIP().equals(
						multicastAddressField.getInetAddress())
				&& (((Number) announcementPortField.getValue()).intValue() == settings
						.getAnnouncementPort()) && (settings
				.getReceiveUseMulticastPool() == receiveUseMulticastPoolField
				.isSelected()));
	}

	private void somethingChanged() {
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
