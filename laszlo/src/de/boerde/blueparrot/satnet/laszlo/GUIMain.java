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

package de.boerde.blueparrot.satnet.laszlo;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.*;
import javax.swing.UIManager;

import de.boerde.blueparrot.satnet.laszlo.protocol.http.HTTPProxyServer;
import de.boerde.blueparrot.satnet.laszlo.protocol.news.GroupManager;
import de.boerde.blueparrot.satnet.laszlo.protocol.news.NewsNNTPServer;
import de.boerde.blueparrot.satnet.laszlo.ui.TransferStatusWindow;

/**
 * 
 * @author roland
 */
public class GUIMain {
	
	private static Logger logger;

	private static String settingsFileArg;

	public static String getSettingsFileArg() {
		return settingsFileArg;
	}

	private static void initLogger() {
		Settings settings = Settings.getSettings();
		logger = Logger.getLogger("laszlo");
		logger.setLevel(settings.getLogLevel());
		Handler logHandler = null;
		String logFile = settings.getLogFile();
		String logWarning = null;
		if (!"".equals(logFile)) {
			try	{
				logHandler = new FileHandler(logFile);
			}
			catch(Exception e) {
				logWarning = e.getMessage();
			}
		}
		if (logHandler == null) {
			logHandler = new ConsoleHandler();
		}
		logHandler.setFormatter(new SimpleFormatter());
		logger.addHandler(logHandler);
		if(logWarning != null) {
			getLogger().warning("Cannot open log file, fall back to console: "
					+ logWarning);
		}
	}
	
	private static void initUI() {
		
		String theme = Settings.getSettings().getTheme();
		try {
			 if ("metal".equals(theme)) {
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			}
			else if ("motif".equals(theme)) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
			}
			else {
				System.setProperty("swing.gtkthemefile",	 theme);
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			}
		}
		catch(Exception e) {
			getLogger().warning(e.getMessage());
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			settingsFileArg = args[0];
		}

		Locale.setDefault(Locale.ENGLISH);
		initLogger();
		initUI();
		
		Receiver recv = new Receiver();
		ReceptionProcessor processor = new ReceptionProcessor(recv);
		TransferStatusWindow frame = new TransferStatusWindow(recv);
		recv.start();
		frame.pack();

		frame.setVisible(true);
		NewsNNTPServer nntpServer = new NewsNNTPServer();
		nntpServer.start();
		HTTPProxyServer httpProxy = new HTTPProxyServer();
		httpProxy.start();
		PackageManager.getPackageManager();
		XMLAnnouncementManager.getXMLAnnouncementManager();
		GroupManager.getGroupManager();
	}

	public static Logger getLogger() {
		return logger;
	}
}
