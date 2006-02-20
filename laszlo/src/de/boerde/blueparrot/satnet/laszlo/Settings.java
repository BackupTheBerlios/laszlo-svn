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
 * Settings.java
 *
 * Created on 8. Mai 2004, 21:06
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import de.boerde.blueparrot.text.InetAddressFormat;

/**
 * 
 * @author roland
 */
public class Settings extends Properties {
	/** Creates a new instance of Settings */
	private Settings() {
	}

	private static File getSettingsFile() {
		String fileName = GUIMain.getSettingsFileArg();
		if (fileName == null)
			fileName = System.getProperty("user.home") + File.separator
					+ ".lazlo" + File.separator + "lazlo.properties";
		return new File(fileName);
	}

	public void setDVBInterface(NetworkInterface dvbInterface) {
		setProperty("DVBInterface", dvbInterface.getName());
	}

	public NetworkInterface getDVBInterface() {
		String dvbInterfaceSetting = getProperty("DVBInterface");
		NetworkInterface dvbInterface = null;
		if ((dvbInterfaceSetting != null) && !"".equals(dvbInterfaceSetting)) {
			try {
				dvbInterface = NetworkInterface.getByName(dvbInterfaceSetting);
			} catch (SocketException e) {
			}
		}
		if (dvbInterfaceSetting == null) {
			try {
				Enumeration ifaces = NetworkInterface.getNetworkInterfaces();
				int quality = 0;
				while (ifaces.hasMoreElements()) {
					NetworkInterface iface = (NetworkInterface) ifaces
							.nextElement();
					String name = iface.getName().toLowerCase();
					String displayName = iface.getDisplayName().toLowerCase();
					if ((quality < 5) && (name.indexOf("dvb") > -1)) {
						dvbInterface = iface;
						quality = 5;
					} else if ((quality < 4)
							&& (displayName.indexOf("dvb") > -1)) {
						dvbInterface = iface;
						quality = 4;
					} else if ((quality < 3)
							&& (displayName.indexOf("sat") > -1)) {
						dvbInterface = iface;
						quality = 3;
					} else if ((quality < 2)
							&& (displayName.indexOf("b2c2 broadband receiver") > -1)) {
						dvbInterface = iface;
						quality = 2;
					} else if (quality < 1) {
						dvbInterface = iface;
						quality = 1;
					}
				}
				setDVBInterface(dvbInterface);
			} catch (SocketException e) {
				GUIMain.getLogger().severe(e.getMessage());
			}
		}
		return dvbInterface;
	}

	public void setMulticastIP(InetAddress multicastIP) {
		if (getMulticastIP().equals(multicastIP))
			return;

		setProperty("MulticastIP", multicastIP.getHostAddress());
	}

	public InetAddress getMulticastIP() {
		String multicastIPSetting = getProperty("MulticastIP");
		InetAddress multicastIP = null;
		try {
			multicastIP = inetAddressFormat.parse(multicastIPSetting);
		} catch (ParseException e) {
		}
		if (multicastIP == null) {
			byte[] rawIP = { (byte) 228, (byte) 67, (byte) 3, (byte) 0 };
			try {
				multicastIP = InetAddress.getByAddress(rawIP);
			} catch (UnknownHostException e) {
			}
		}
		return multicastIP;
	}

	public void setAnnouncementPort(int port) {
		if (getAnnouncementPort() == port)
			return;

		setProperty("AnnouncementPort", Integer.toString(port));
	}

	public int getAnnouncementPort() {
		String announcementPortSetting = getProperty("AnnouncementPort");
		int announcementPort = 1767;
		try {
			announcementPort = Integer.parseInt(announcementPortSetting);
		} catch (Exception e) {
		}
		return announcementPort;
	}

	public void setWorkDirectory(String workDirectory) {
		if (getWorkDirectory().equals(workDirectory))
			return;

		while (workDirectory.endsWith(File.separator))
			workDirectory = workDirectory.substring(0,
					workDirectory.length() - 1);
		setProperty("WorkDirectory", workDirectory);
	}

	public String getWorkDirectory() {
		String workDirectory = getProperty("WorkDirectory");
		if ((workDirectory == null) || "".equals(workDirectory)) {
			workDirectory = System.getProperty("user.home")+ File.separator
			+ ".lazlo" + File.separator + "work";
		}
		if (workDirectory.endsWith(File.separator))
			workDirectory = workDirectory.substring(0,
					workDirectory.length() - 1);
		return workDirectory;
	}

	public void setAnnouncementBufferSize(int size) {
		if (getAnnouncementBufferSize() == size)
			return;

		setProperty("AnnouncementBufferSize", String.valueOf(size));
	}

	public int getAnnouncementBufferSize() {
		String sizeStr = getProperty("AnnouncementBufferSize");
		int size = 8192;
		try {
			size = Integer.parseInt(sizeStr);
		} catch (Exception e) {
		}
		return size;
	}

	public void setSocketReceiveBufferSize(int size) {
		if (getSocketReceiveBufferSize() == size)
			return;

		setProperty("SocketReceiveBufferSize", String.valueOf(size));
	}

	public int getSocketReceiveBufferSize() {
		String sizeStr = getProperty("ReceiveBufferSize");
		int size = 32768;
		try {
			size = Integer.parseInt(sizeStr);
		} catch (Exception e) {
		}
		return size;
	}

	public void setReceiveUseMulticastPool(boolean use) {
		if (getReceiveUseMulticastPool() == use)
			return;

		setProperty("ReceiveUseMulticastPool", use ? "true" : "false");
	}

	public boolean getReceiveUseMulticastPool() {
		String useStr = getProperty("ReceiveUseMulticastPool");
		boolean use = false;
		try {
			use = "true".equalsIgnoreCase(useStr);
		} catch (Exception e) {
		}
		return use;
	}

	public void setUnpackCommandline(String extension, String command) {
		if (!extension.startsWith("."))
			return;
		if (getUnpackCommandline(extension).equals(command))
			return;

		setProperty("UnpackCommandline" + extension, command);
	}

	public String getUnpackCommandline(String file) {
		int p = file.lastIndexOf(File.separatorChar) + 1;
		if (p > 0 && p < file.length()) {
			file = file.substring(p);
		}
		String command = null;
		while (command == null || "".equals(command)) {
			command = getProperty("UnpackCommandline" + file);
			if (command == null) {
				command = getProperty("UnpackCommandline" + file.toLowerCase());
			}
			if (command == null) {
				command = getProperty("UnpackCommandline" + file.toUpperCase());
			}
			if (command == null) {
				if (".rar".equals(file)) {
					command = "\"unrar\" x -o+ -inul -ri1 -p- -- \"%%PACKEDFILE%%\" \"%%DESTDIR%%\""
							+ File.separator;
				} else if (".cab".equals(file)) {
					String os = System.getProperty("os.name");
					if (os != null && os.toLowerCase().indexOf("windows") >= 0) {
						command = "\"%ProgramFiles%\\Resource Kit\\extract.exe\" /Y /E /L \"%%DESTDIR%%\" \"%%PACKEDFILE%%\"";
					} else {
						command = "\"cabextract\" -q -d \"%%DESTDIR%%\" \"%%PACKEDFILE%%\"";
					}
				}
			}
			p = file.indexOf('.', 1);
			if (p > 0 && p < file.length()) {
				file = file.substring(p);
			} else {
				break;
			}
		}
		if (command == null) {
			command = getProperty("UnpackCommandline");
		}
		if (command == null) {
			command = "";
		}
		return command;
	}

	public void setExpireIncompleteFileAfterMinutes(int minutes) {
		if (getExpireIncompleteFileAfterMinutes() == minutes)
			return;

		setProperty("ExpireIncompleteFileAfterMinutes", String.valueOf(minutes));
	}

	public int getExpireIncompleteFileAfterMinutes() {
		String minutesStr = getProperty("ExpireIncompleteFileAfterMinutes");
		int minutes = 12 * 60; // 1/2day
		try {
			minutes = Integer.parseInt(minutesStr);
		} catch (Exception e) {
		}
		return minutes;
	}

	public void setExpireIncompleteFileCheckIntervalMinutes(int minutes) {
		if (getExpireIncompleteFileCheckIntervalMinutes() == minutes)
			return;

		setProperty("ExpireIncompleteFileCheckIntervalMinutes", String
				.valueOf(minutes));
	}

	public int getExpireIncompleteFileCheckIntervalMinutes() {
		String minutesStr = getProperty("ExpireIncompleteFileCheckIntervalMinutes");
		int minutes = 5;
		try {
			minutes = Integer.parseInt(minutesStr);
		} catch (Exception e) {
		}
		return minutes;
	}

	public void setExpirePackageAfterMinutes(int minutes) {
		if (getExpirePackageAfterMinutes() == minutes)
			return;

		setProperty("ExpirePackageAfterMinutes", String.valueOf(minutes));
	}

	public int getExpirePackageAfterMinutes() {
		String minutesStr = getProperty("ExpirePackageAfterMinutes");
		int minutes = 14 * 24 * 60; // 2 weeks
		try {
			minutes = Integer.parseInt(minutesStr);
		} catch (Exception e) {
		}
		return minutes;
	}

	public void setExpirePackageCheckIntervalMinutes(int minutes) {
		if (getExpirePackageCheckIntervalMinutes() == minutes)
			return;

		setProperty("ExpirePackageCheckIntervalMinutes", String
				.valueOf(minutes));
	}

	public int getExpirePackageCheckIntervalMinutes() {
		String minutesStr = getProperty("ExpirePackageCheckIntervalMinutes");
		int minutes = 60;
		try {
			minutes = Integer.parseInt(minutesStr);
		} catch (Exception e) {
		}
		return minutes;
	}

	public void setLaszloNntpPort(int port) {
		if (getLaszloNntpPort() == port)
			return;

		setProperty("LaszloNntpPort", String.valueOf(port));
	}

	public int getLaszloNntpPort() {
		String portStr = getProperty("LaszloNntpPort");
		int port = 1119;
		try {
			port = Integer.parseInt(portStr);
		} catch (Exception e) {
		}
		return port;
	}

	public void setLaszloNntpPortBacklog(int portBacklog) {
		if (getLaszloNntpPortBacklog() == portBacklog)
			return;

		setProperty("LaszloNntpPortBacklog", String.valueOf(portBacklog));
	}

	public int getLaszloNntpPortBacklog() {
		String portBacklogStr = getProperty("LaszloNntpPortBacklog");
		int portBacklog = 5;
		try {
			portBacklog = Integer.parseInt(portBacklogStr);
		} catch (Exception e) {
		}
		return portBacklog;
	}

	public void setLaszloNntpBindAddress(InetAddress bindAddress) {
		if (getLaszloNntpBindAddress().equals(bindAddress))
			return;

		setProperty("LaszloNntpBindAddress", bindAddress.getHostAddress());
	}

	public InetAddress getLaszloNntpBindAddress() {
		String bindAddressSetting = getProperty("LaszloNntpBindAddress");
		InetAddress bindAddress = null;
		try {
			bindAddress = inetAddressFormat.parse(bindAddressSetting);
		} catch (ParseException e) {
		}
		if (bindAddress == null) {
			byte[] rawIP = { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
			try {
				bindAddress = InetAddress.getByAddress(rawIP);
			} catch (UnknownHostException e) {
			}
		}
		return bindAddress;
	}

	public void setLaszloNntpClientTimeoutSeconds(int timeout) {
		if (getLaszloNntpClientTimeoutSeconds() == timeout)
			return;

		setProperty("LaszloNntpClientTimeoutSeconds", String.valueOf(timeout));
	}

	public int getLaszloNntpClientTimeoutSeconds() {
		String timeoutStr = getProperty("LaszloNntpClientTimeoutSeconds");
		int timeout = 10 * 60; // 10 minutes
		try {
			timeout = Integer.parseInt(timeoutStr);
		} catch (Exception e) {
		}
		return timeout;
	}

	public void setLaszloHttpPort(int port) {
		if (getLaszloHttpPort() == port)
			return;

		setProperty("LaszloHttpPort", String.valueOf(port));
	}

	public int getLaszloHttpPort() {
		String portStr = getProperty("LaszloHttpPort");
		int port = 8080;
		try {
			port = Integer.parseInt(portStr);
		} catch (Exception e) {
		}
		return port;
	}

	public void setLaszloHttpPortBacklog(int portBacklog) {
		if (getLaszloHttpPortBacklog() == portBacklog)
			return;

		setProperty("LaszloHttpPortBacklog", String.valueOf(portBacklog));
	}

	public int getLaszloHttpPortBacklog() {
		String portBacklogStr = getProperty("LaszloHttpPortBacklog");
		int portBacklog = 10;
		try {
			portBacklog = Integer.parseInt(portBacklogStr);
		} catch (Exception e) {
		}
		return portBacklog;
	}

	public void setLaszloHttpBindAddress(InetAddress bindAddress) {
		if (getLaszloHttpBindAddress().equals(bindAddress))
			return;

		setProperty("LaszloHttpBindAddress", bindAddress.getHostAddress());
	}

	public InetAddress getLaszloHttpBindAddress() {
		String bindAddressSetting = getProperty("LaszloHttpBindAddress");
		InetAddress bindAddress = null;
		try {
			bindAddress = inetAddressFormat.parse(bindAddressSetting);
		} catch (ParseException e) {
		}
		if (bindAddress == null) {
			byte[] rawIP = { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
			try {
				bindAddress = InetAddress.getByAddress(rawIP);
			} catch (UnknownHostException e) {
			}
		}
		return bindAddress;
	}

	public void setLaszloHttpClientTimeoutSeconds(int timeout) {
		if (getLaszloHttpClientTimeoutSeconds() == timeout)
			return;

		setProperty("LaszloHttpClientTimeoutSeconds", String.valueOf(timeout));
	}

	public int getLaszloHttpClientTimeoutSeconds() {
		String timeoutStr = getProperty("LaszloHttpClientTimeoutSeconds");
		int timeout = 30;
		try {
			timeout = Integer.parseInt(timeoutStr);
		} catch (Exception e) {
		}
		return timeout;
	}

	public void setHttpOwnPseudoName(String ownName) {
		if (getHttpOwnPseudoName().equals(ownName))
			return;

		setProperty("HttpOwnPseudoName", ownName.toLowerCase());
	}

	public String getHttpOwnPseudoName() {
		String ownName = getProperty("HttpOwnPseudoName");
		if ((ownName == null) || "".equals(ownName)) {
			ownName = "laszlo_index";
		}
		return ownName;
	}

	public void setHttpProxyFetching(String mode) {
		if (getHttpProxyFetching().charAt(0) == mode.charAt(0))
			return;

		setProperty("HttpProxyFetching", mode);
	}

	public String getHttpProxyFetching() {
		String mode = getProperty("HttpProxyFetching");
		if ((mode == null) || "".equals(mode)) {
			mode = "none";
		}
		return mode;
	}

	public void setUpstreamHttpProxyHost(String host) {
		if (getUpstreamHttpProxyHost().equals(host))
			return;

		setProperty("UpstreamHttpProxyHost", host.toLowerCase());
	}

	public String getUpstreamHttpProxyHost() {
		String host = getProperty("UpstreamHttpProxyHost");
		if ((host == null) || "".equals(host)) {
			host = "";
		}
		return host;
	}

	public void setUpstreamHttpProxyPort(int port) {
		if (getUpstreamHttpProxyPort() == port)
			return;

		setProperty("UpstreamHttpProxyPort", Integer.toString(port));
	}

	public int getUpstreamHttpProxyPort() {
		String portSetting = getProperty("UpstreamHttpProxyPort");
		int port = 8080;
		try {
			port = Integer.parseInt(portSetting);
		} catch (Exception e) {
		}
		return port;
	}

	public void setLogLevel(Level level) {
		if (getLogLevel().equals(level))
			return;
				
		String strLevel;
		if (Level.SEVERE.equals(level))
			strLevel = "severe";
		else if (Level.WARNING.equals(level))
			strLevel = "warning";
		else
			strLevel = "info";
		setProperty("LogLevel", strLevel);
	}

	public Level getLogLevel() {
		Level level = Level.INFO;
		String strLevel = getProperty("LogLevel");
		if (strLevel != null) {
			if (strLevel.toLowerCase().trim().equals("warning"))
				level = Level.WARNING;
			else if (strLevel.toLowerCase().trim().equals("severe"))
				level = Level.SEVERE;
			else if (strLevel.toLowerCase().trim().equals("off"))
				level = Level.OFF;
		}
		return level;
	}

	public void setLogFile(String fileName) {
		if (getLogFile().equals(fileName))
			return;
		setProperty("Log	File", fileName);
	}
	
	public String getLogFile() {
		String fileName = getProperty("LogFile");
		if (fileName == null)
			return "";
		return fileName;
	}
	
	public String getTheme() {
		String theme = getProperty("Theme");
		String defaultTheme = "metal";
		if (theme != null) {
			if (theme.toLowerCase().trim().equals("motif"))
				return "motif";
			if (theme.toLowerCase().trim().equals("system"))
				return "system";
			try {
				if (new File(theme).isFile()) {
					return theme;
				}
			}
			catch(SecurityException e) {
				return defaultTheme;
			}
		}
		return defaultTheme;
	}
	
	public void setTheme(String theme) {
		if(getTheme().equals(theme))
			return;
		setProperty("Theme", theme);
	}

	
	public void commitChanges() {
		notifySettingsChanged();
	}

	public void save() throws IOException {
		FileOutputStream out = null;
		try {
			File settingsFile = getSettingsFile();
			File settingsDir = settingsFile.getParentFile();
			if (!settingsDir.exists())
				settingsDir.mkdirs();

			out = new FileOutputStream(settingsFile);
			store(out, "Laszlo properties file");
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
			}
		}
	}

	public void load() throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(getSettingsFile());
			this.load(in);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	public void addSettingsChangedListener(SettingsChangedListener listener) {
		listeners.add(listener);
	}

	public void removeSettingsChangedListener(SettingsChangedListener listener) {
		listeners.remove(listener);
	}

	private Vector listeners = new Vector();

	private void notifySettingsChanged() {
		Iterator lis = ((Vector) listeners.clone()).iterator();
		while (lis.hasNext()) {
			SettingsChangedListener listener = (SettingsChangedListener) lis
					.next();
			listener.settingsChanged(this);
		}
	}

	public static interface SettingsChangedListener {
		public void settingsChanged(Settings newSettings);
	}

	private static InetAddressFormat inetAddressFormat = new InetAddressFormat();

	private static Settings theSettings = new Settings();
	static {
		File settingsFile = getSettingsFile();
		if (settingsFile.exists() && settingsFile.isFile()) {
			try {
				theSettings.load();
			} catch (IOException e) {
				GUIMain.getLogger().severe(
						"Laszlo settings could not be loaded: "
								+ e.getMessage());
			}
		}
	}

	public static Settings getSettings() {
		return theSettings;
	}
}
