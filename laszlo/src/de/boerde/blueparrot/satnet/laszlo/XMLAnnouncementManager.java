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
 * XMLAnnouncmentManager.java
 *
 * Created on 15. Mai 2004, 14:51
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author roland
 */
public class XMLAnnouncementManager {
	private Hashtable xmlAnnouncements;

	private DocumentBuilder xmlDocumentBuilder;

	/** Creates a new instance of XMLAnnouncmentManager */
	private XMLAnnouncementManager() {
		xmlAnnouncements = new Hashtable();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			xmlDocumentBuilder = factory.newDocumentBuilder();
			File dir = findAnnouncementDir();
			if (dir != null) {
				File[] announcementFiles = dir.listFiles();
				if (announcementFiles != null) {
					for (int a = 0; a < announcementFiles.length; a++) {
						File file = announcementFiles[a];
						if (file.getName().endsWith(".xml")) {
							String fileName = file.getAbsolutePath();
							GUIMain.getLogger().info(
									"Reading old XML announcement from "
											+ fileName);
							newXMLAnnouncementList(fileName);
						}
					}
				}
			}
		} catch (ParserConfigurationException e) {
			GUIMain.getLogger().severe(e.getMessage());
		}
	}

	public synchronized void newXMLAnnouncementList(String localFile) {
		newXMLAnnouncementList(new File(localFile));
	}

	public synchronized void newXMLAnnouncementList(File localFile) {
		if (xmlDocumentBuilder == null)
			return;

		try {
			Document xmlDoc = xmlDocumentBuilder.parse(localFile);
			Element root = xmlDoc.getDocumentElement();
			NodeList packages = root.getElementsByTagName("PACKAGE");
			for (int n = 0; n < packages.getLength(); n++) {
				Node node = packages.item(n);
				if (node instanceof Element) {
					Element pkg = (Element) node;
					if ("PACKAGE".equals(pkg.getTagName())) {
						BookingAnnouncement ann = new BookingAnnouncement(pkg);
						xmlAnnouncements.put(ann.getPackageName(), ann);
						// xmlAnnouncements.put (name, pkg);
					}
				}
			}
		} catch (IOException e) {
			GUIMain.getLogger().severe(e.getMessage());
		} catch (SAXException e) {
			GUIMain.getLogger().warning(
					"In file " + localFile + ": " + e.getMessage());
		}
	}

	public BookingAnnouncement getXMLAnnouncement(String transferName) {
		return (BookingAnnouncement) xmlAnnouncements.get(transferName);
	}

	public void removeXMLAnnouncement(String transferName) {
		xmlAnnouncements.remove(transferName);
	}

	private File findAnnouncementDir() {
		Settings settings = Settings.getSettings();
		String rootDir = settings.getWorkDirectory() + File.separator + "recv";
		String wantedDir = "Announcement";
		Stack dirsToProcess = new Stack();
		dirsToProcess.push(new File(rootDir));
		while (!dirsToProcess.isEmpty()) {
			File currentDir = (File) dirsToProcess.pop();
			File[] subdirs = currentDir.listFiles();
			if (subdirs != null) {
				for (int s = 0; s < subdirs.length; s++) {
					File subdir = subdirs[s];
					if (subdir.isDirectory()) {
						if (wantedDir.equals(subdir.getName())) {
							return subdir;
						} else {
							dirsToProcess.push(subdir);
						}
					}
				}
			}
		}
		return null;
	}

	private static XMLAnnouncementManager theXMLAnnouncementManager = new XMLAnnouncementManager();

	public static XMLAnnouncementManager getXMLAnnouncementManager() {
		return theXMLAnnouncementManager;
	}
}
