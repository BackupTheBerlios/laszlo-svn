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

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 *
 * @author  roland
 */
public class XMLAnnouncementManager
{
	private Hashtable xmlAnnouncements;
	private DocumentBuilder xmlDocumentBuilder;

	/** Creates a new instance of XMLAnnouncmentManager */
	private XMLAnnouncementManager()
	{
		xmlAnnouncements = new Hashtable();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating (false);
		try
		{
			xmlDocumentBuilder = factory.newDocumentBuilder();
			File dir = findAnnouncementDir();
			if (dir != null)
			{
				File[] announcementFiles = dir.listFiles();
				if (announcementFiles != null)
				{
					for (int a=0; a<announcementFiles.length; a++)
					{
						File file = announcementFiles [a];
						if (file.getName().endsWith (".xml"))
						{
							String fileName = file.getAbsolutePath();
							GUIMain.logger.info("Reading old XML announcement from " + fileName);
							newXMLAnnouncementList (fileName);
						}
					}
				}
			}
		}
		catch (ParserConfigurationException e)
		{
			GUIMain.logger.severe(e.getMessage());
		}
	}

	public synchronized void newXMLAnnouncementList (String localFile)
	{
		newXMLAnnouncementList (new File (localFile));
	}

	public synchronized void newXMLAnnouncementList (File localFile)
	{
		if (xmlDocumentBuilder == null)
			return;

		try
		{
			Document xmlDoc = xmlDocumentBuilder.parse (localFile);
			Element root = xmlDoc.getDocumentElement();
			NodeList packages = root.getElementsByTagName ("PACKAGE");
			for (int n=0; n<packages.getLength(); n++)
			{
				Node node = packages.item (n);
				if (node instanceof Element)
				{
					Element pkg = (Element) node;
					if ("PACKAGE".equals (pkg.getTagName()))
					{
						BookingAnnouncement ann = new BookingAnnouncement (pkg);
						xmlAnnouncements.put (ann.getPackageName(), ann);
//						xmlAnnouncements.put (name,  pkg);
					}
				}
			}
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
		}
		catch (SAXException e)
		{
			GUIMain.logger.severe("In file " + localFile + ": " + e.getMessage());
		}
	}

	public BookingAnnouncement getXMLAnnouncement (String transferName)
	{
		return (BookingAnnouncement) xmlAnnouncements.get (transferName);
	}

	public void removeXMLAnnouncement (String transferName)
	{
		xmlAnnouncements.remove (transferName);
	}

/*	public static String getSingleDetail (Element pkgNode, String detailName)
	{
		NodeList packageNames = pkgNode.getElementsByTagName (detailName);
		for (int pn=0; pn<packageNames.getLength(); pn++)
		{
			Node nameNode = packageNames.item (pn);
			if (nameNode instanceof Element)
			{
				Element pkgName = (Element) nameNode;
				return getText (pkgName);
			}
		}
		return "";
	}
*/

	private File findAnnouncementDir()
	{
		Settings settings = Settings.getSettings();
		String rootDir = settings.getWorkDirectory() + File.separator + "recv";
		String wantedDir = "Announcement";
		Stack dirsToProcess = new Stack();
		dirsToProcess.push (new File (rootDir));
		while (!dirsToProcess.isEmpty())
		{
			File currentDir = (File) dirsToProcess.pop();
			File[] subdirs = currentDir.listFiles();
			if (subdirs != null)
			{
				for (int s=0; s<subdirs.length; s++)
				{
					File subdir = subdirs [s];
					if (subdir.isDirectory())
					{
						if (wantedDir.equals (subdir.getName()))
						{
							return subdir;
						}
						else
						{
							dirsToProcess.push (subdir);
						}
					}
				}
			}
		}
		return null;
	}

/*	public static String getText (Element elementNode)
	{
		StringBuffer result = new StringBuffer();
		NodeList children = elementNode.getChildNodes();
		for (int c=0; c<children.getLength(); c++)
		{
			Node child = children.item (c);
			if ((child.getNodeType() == Node.TEXT_NODE) && (child instanceof Text))
			{
				String name = ((Text) child).getData();
				result.append (name);
			}
		}
		return result.toString();
	}
*/

	private static XMLAnnouncementManager theXMLAnnouncementManager = new XMLAnnouncementManager();
	public static XMLAnnouncementManager getXMLAnnouncementManager()
	{
		return theXMLAnnouncementManager;
	}
}
