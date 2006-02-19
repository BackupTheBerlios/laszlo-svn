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
 * AnnounementReader.java
 *
 * Created on 1. Mai 2004, 10:11
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * 
 * @author roland
 */
public class AnnouncementReader implements Settings.SettingsChangedListener {
	private NetworkInterface nic;

	private MulticastSocket socket;

	private InetAddress announceaddress;

	private int announceport;

	private DatagramPacket packet;

	private boolean isInGroup;

	private boolean settingsChanged;

	// final private String transmitCharset = "Windows-1252";

	/** Creates a new instance of AnnounementReader */
	public AnnouncementReader() throws IOException {
		Settings.getSettings().addSettingsChangedListener(this);
		isInGroup = false;
		try {
			reinitialize();
		} catch (Exception e) {
			GUIMain.getLogger().severe(e.getMessage());
		}
	}

	private synchronized void reinitialize() throws IOException {
		Settings settings = Settings.getSettings();
		int buffersize = settings.getAnnouncementBufferSize();
		packet = new DatagramPacket(new byte[buffersize], buffersize);
		nic = settings.getDVBInterface();
		announceaddress = settings.getMulticastIP();
		announceport = settings.getAnnouncementPort();
		socket = new MulticastSocket(announceport);
		socket.setNetworkInterface(nic);
		socket.joinGroup(announceaddress);
		socket.setReceiveBufferSize(settings.getSocketReceiveBufferSize());
		isInGroup = true;
		settingsChanged = false;
	}

	public Announcement getTransmission() throws IOException {
		boolean received = false;
		while (!received) {
			try {
				socket.receive(packet);
				received = true;
			} catch (SocketException e) {
				if (settingsChanged)
					reinitialize();
				else
					throw e;
			}
		}
		return new Announcement(packet);
	}

	public synchronized void close() throws IOException {
		Settings.getSettings().removeSettingsChangedListener(this);
		if (isInGroup) {
			socket.leaveGroup(announceaddress);
			socket.close();
			isInGroup = false;
		}
	}

	public synchronized boolean isClosed() {
		return socket.isClosed();
	}

	public void finalize() throws IOException {
		close();
	}

	public synchronized void settingsChanged(Settings newSettings) {
		if (!(nic.equals(newSettings.getDVBInterface())
				&& announceaddress.equals(newSettings.getMulticastIP()) && (announceport == newSettings
				.getAnnouncementPort()))) {
			settingsChanged = true;
			try {
				socket.leaveGroup(announceaddress);
				socket.close();
			} catch (IOException e) {
				GUIMain.getLogger().severe(e.getMessage());
			}
		}
	}
}
