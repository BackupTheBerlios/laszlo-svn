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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import de.boerde.blueparrot.util.ForwardBitChunkList;

/**
 * 
 * @author roland
 */
public class ReceiverContentReader extends ContentReader implements
		Settings.SettingsChangedListener {
	private MulticastSocketPool.SocketInfo socketInfo;

	private MulticastSocket socket;

	private int blksize;

	private int timeout;

	protected String workDir;


	public ReceiverContentReader(Announcement announcement) throws IOException,
			ProtocolException {
		this.announcement = announcement;
		Settings settings = Settings.getSettings();
		try {
			String blksizeStr = announcement.getDetail("blksize");
			String tsizeStr = announcement.getDetail("tsize");
			String timeoutStr = announcement.getDetail("timeout");
			blksize = Integer.parseInt(blksizeStr);
			tsize = Integer.parseInt(tsizeStr);
			timeout = Integer.parseInt(timeoutStr);
		} catch (Exception e) {
			throw new ProtocolException(
					"Something in this announcement packet is invalid: "
							+ announcement);
		}
		MulticastSocketPool multicastPool = MulticastSocketPool
				.getMulticastSocketPool();
		socketInfo = multicastPool.getSocket(announcement);
		socket = socketInfo.getSocket();
		socket.setReceiveBufferSize(settings.getSocketReceiveBufferSize());
		if ((timeout <= 0) || (timeout > 7200)) {
			GUIMain.getLogger().warning(
					"unreasonable SoTimeout, using deault instead for "
							+ announcement);
			timeout = 120;
		}
		socket.setSoTimeout(timeout * 1000); // Java uses milliseconds, what
											 // is the timeout value from the
											 // announcement?
		totalNumberOfPackets = (tsize % blksize == 0) ? tsize / blksize : tsize
				/ blksize + 1;
		partsToRetrieve = new ForwardBitChunkList(0, totalNumberOfPackets);
		XMLAnnouncementManager xmlAnnouncementManager = XMLAnnouncementManager
				.getXMLAnnouncementManager();
		String transferName = announcement.getFullName();
		xmlAnnouncement = xmlAnnouncementManager
				.getXMLAnnouncement(transferName);
		expireSimilarContentReaders();
		xmlAnnouncementManager.removeXMLAnnouncement(transferName);
		addContentReaderToAll(this);
	}

	public void getTransmission() throws IOException {
		Settings settings = Settings.getSettings();
		workDir = settings.getWorkDirectory();
		RandomAccessFile out = null;
		File file = new File(getLocalFileName());
		try {
			File dir = file.getParentFile();
			if (!dir.exists())
				dir.mkdirs();
			out = new RandomAccessFile(file, "rw");
			out.setLength(tsize);
			socketInfo.joinGroup();
			loadPartsToRetrieve();
			// largest known transfer mode uses 6 header bytes per network
			// packet
			DatagramPacket packet = new DatagramPacket(new byte[blksize + 6],
					blksize + 6);

			lastReceivedSequence = -1;
			synchronized (socket) {
				while (!partsToRetrieve.isEmpty()) {
					socket.receive(packet);
					int length = packet.getLength();
					if (length >= 2) {
						byte[] data = packet.getData();
						int offset = packet.getOffset();
						if (data[offset + 1] == 3) {
							int sequence;
							switch (data[offset]) {
							case 0: {
								if (length >= 4) {
									sequence = (((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff)) - 1;
									// actual data packets start at 1; we start our counting at zero.
									offset += 4; // skip the four header bytes
									length -= 4;
								} else {
									sequence = -1;
								}
								break;
							}
							case 1: {
								if (length >= 6) {
									sequence = (((data[offset + 2] & 0xff) << 24)
											| ((data[offset + 3] & 0xff) << 16)
											| ((data[offset + 4] & 0xff) << 8) | (data[offset + 5] & 0xff)) - 1;
									// actual data packets start at 1; we start our counting at zero.
									offset += 6; // skip the six header bytes
									length -= 6;
								} else {
									sequence = -1;
								}
								break;
							}
							default: {
								sequence = -1;
							}
							}
							if (sequence >= 0) {
								if (sequence < totalNumberOfPackets) {
									if ((length == blksize)
											|| ((length < blksize) && (sequence == totalNumberOfPackets - 1))) {
										if (sequence > lastReceivedSequence) {
											if (partsToRetrieve.get(sequence)) {
												long filepos = (sequence)
														* blksize;
												out.seek(filepos);
												out.write(data, offset, length);
												partsToRetrieve.clear(sequence);
												received += length;
											}
											lastReceivedSequence = sequence;
											if (sequence == totalNumberOfPackets - 1)
												break;
										} else {
											GUIMain
													.getLogger()
													.warning(
															announcement
																	.getFullName()
																	+ " Sequence "
																	+ sequence
																	+ " came after "
																	+ lastReceivedSequence
																	+ ", possible overlap with other transmission?");
											break;
										}
									} else {
										GUIMain
												.getLogger()
												.warning(
														announcement
																.getFullName()
																+ " Packet length "
																+ length
																+ " does not match blksize "
																+ blksize
																+ ", dump "
																+ dataDump(
																		data,
																		16)
																+ ".");
										break;
									}
								} else {
									GUIMain
											.getLogger()
											.warning(
													announcement.getFullName()
															+ " Invalid sequence number "
															+ sequence
															+ " (numPackets="
															+ totalNumberOfPackets
															+ ", dump "
															+ dataDump(data, 16)
															+ ".");
									break;
								}
							} else {
								GUIMain
										.getLogger()
										.warning(
												announcement.getFullName()
														+ " Unknown or unimplemented transferMode, or short packet. Dump: "
														+ dataDump(data, 16)
														+ ". Announcement was: "
														+ announcement);
								break;
							}
						} else {
							GUIMain
									.getLogger()
									.warning(
											announcement.getFullName()
													+ " Reveived packet does not start with 0x?? 0x03. Dump: "
													+ dataDump(data, 16)
													+ ". Announcement was: "
													+ announcement);
							break;
						}
					} else {
						GUIMain.getLogger().warning(
								announcement.getFullName()
										+ " Short packet received, ignoring");
						break;
					}
				}
			}
		} catch (SocketTimeoutException e) {
			GUIMain.getLogger()
					.warning("Timeout " + announcement.getFullName());
		} catch (SocketException e) {
			GUIMain.getLogger().warning(
					"Socket Exception: " + e.getMessage() + " "
							+ announcement.getFullName());
		} finally {
			if (!partsToRetrieve.isEmpty()) {
				if (announcement.getDetail("fileid") != null)
					savePartsToRetrieve();
				else
					file.delete();
			}

			socketInfo.close();
			if (out != null) {
				out.close();
			}
			lastReceivedSequence = -1;
			if (partsToRetrieve.isEmpty()) {
				deleteProgressFile();
				GUIMain.getLogger().info(
						"Finished: " + announcement.getPlainName());
				notifyTransmissionCompleted();
			} else {
				notifyTransmissionIncomplete();
			}

			done = true;
		}
	}

	public boolean isDone() {
		return done;
	}

	public void interrupt() {
		try {
			socket.setSoTimeout(1); // time out faster...
		} catch (Exception e) {
			GUIMain.getLogger().severe(e.getMessage());
		}
	}

	public void settingsChanged(Settings newSettings) {
		if (!workDir.equals(newSettings.getWorkDirectory())) {
			interrupt();
		}
	}
}
