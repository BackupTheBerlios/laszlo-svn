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
 * WebcasterNames.java
 *
 * Created on 13. Juni 2004, 14:02
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.boerde.blueparrot.xml.XMLHelpers;

/**
 * 
 * @author roland
 */
public class ProviderNames {
	private Hashtable idsToDisplayNames;

	/** Creates a new instance of WebcasterNames */
	private ProviderNames() {
		idsToDisplayNames = new Hashtable();
		idsToDisplayNames.put("a", "@Europe");
		idsToDisplayNames.put("b", "@Germany");
		idsToDisplayNames.put("c", "@Italy");
		Settings settings = Settings.getSettings();
		String dir = settings.getWorkDirectory();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					dir + File.separatorChar + "providerNames.utf8"), "UTF-8"));
			while (true) {
				String id = in.readLine();
				String name = in.readLine();
				if (id == null && name == null) {
					break;
				}
				idsToDisplayNames.put(id, name);
			}
		} catch (IOException e) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					GUIMain.getLogger().severe(e.getMessage());
				}
			}
		}
	}

	public synchronized String getDisplayName(String id) {
		String result = (String) idsToDisplayNames.get(id);
		if (result == null)
			result = id;
		return result;
	}

	public synchronized void update(String localFileName) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			DocumentBuilder xmlDocumentBuilder = factory.newDocumentBuilder();
			File file = new File(localFileName);
			Document xmlDoc = xmlDocumentBuilder.parse(file);
			Element root = xmlDoc.getDocumentElement();
			NodeList webcasters = root.getElementsByTagName("WEBCASTER");
			boolean changed = false;
			for (int n = 0; n < webcasters.getLength(); n++) {
				Node node = webcasters.item(n);
				if (node instanceof Element) {
					Element webcaster = (Element) node;
					if ("WEBCASTER".equals(webcaster.getTagName())) {
						NodeList idElements = webcaster
								.getElementsByTagName("ID");
						StringBuffer buffer = new StringBuffer();
						for (int i = 0; i < idElements.getLength(); i++) {
							Node idNode = idElements.item(i);
							if (idNode instanceof Element) {
								buffer.append(XMLHelpers
										.getContentText((Element) idNode));
							}
						}
						String id = buffer.toString();
						NodeList nameElements = webcaster
								.getElementsByTagName("NAME");
						buffer = new StringBuffer();
						for (int i = 0; i < nameElements.getLength(); i++) {
							Node nameNode = nameElements.item(i);
							if (nameNode instanceof Element) {
								buffer.append(XMLHelpers
										.getContentText((Element) nameNode));
							}
						}
						String name = buffer.toString();
						if (!name.equals(idsToDisplayNames.get(id))) {
							idsToDisplayNames.put(id, name);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				save();
			}
		} catch (ParserConfigurationException e) {
			GUIMain.getLogger().severe(e.getMessage());
		} catch (SAXException e) {
			GUIMain.getLogger().severe(
					"Update Provider Names parse error on " + localFileName
							+ ": " + e.getMessage());
		} catch (IOException e) {
			GUIMain.getLogger().severe(
					"Update Provider Names I/O error on " + localFileName
							+ ": " + e.getMessage());
		}
	}

	private void save() {
		Settings settings = Settings.getSettings();
		String dir = settings.getWorkDirectory();
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
					dir + File.separatorChar + "providerNames.utf8"), "UTF-8"));
			Enumeration en = idsToDisplayNames.keys();
			while (en.hasMoreElements()) {
				String id = (String) en.nextElement();
				String name = (String) idsToDisplayNames.get(id);
				out.println(id);
				out.println(name);
			}
		} catch (IOException e) {
			GUIMain.getLogger().severe(e.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static ProviderNames theProviderNames = new ProviderNames();

	public static ProviderNames getProviderNames() {
		return theProviderNames;
	}
}
