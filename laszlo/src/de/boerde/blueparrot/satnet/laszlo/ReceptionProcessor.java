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
 * ReceptionProcessor.java
 *
 * Created on 15. Mai 2004, 14:37
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;

/**
 *
 * @author  roland
 */
public class ReceptionProcessor implements Receiver.NewTransmissionListener, ContentReader.TransmissionListener
{
	Receiver receiver;

	/** Creates a new instance of ReceptionProcessor */
	public ReceptionProcessor (Receiver receiver)
	{
		receiver.addNewTransmissionListener (this);
	}

	public void newTransmission (Receiver.NewTransmissionEvent evt)
	{
		ContentReader reader = evt.getContentReader();
		reader.addTransmissionListener (this);
	}

	public void transmissionIncomplete (ContentReader.TransmissionEvent evt)
	{
		ContentReader reader = evt.getContentReader();
		reader.removeTransmissionListener (this);
	}

	public void transmissionCompleted (ContentReader.TransmissionEvent evt)
	{
		ContentReader reader = evt.getContentReader();
		reader.removeTransmissionListener (this);
		String localFile = reader.getLocalFileName();
		Announcement announcement = reader.getAnnouncement();
		//String transferName = announcement.getFullName();
		BookingAnnouncement xmlAnnouncement = reader.getXmlAnnouncement();
		if (localFile.endsWith ("webcasters.xml"))
		{
			ProviderNames providerNames = ProviderNames.getProviderNames();
			providerNames.update (localFile);
		}
		else if (localFile.endsWith (".xml") && (localFile.indexOf (File.separator + "Announcement" + File.separator) >= 0))
		{
			XMLAnnouncementManager xmlAnnouncementManager = XMLAnnouncementManager.getXMLAnnouncementManager();
			xmlAnnouncementManager.newXMLAnnouncementList (localFile);
		}
		else if (xmlAnnouncement != null)
		{
			Thread t = new ExtractorBuilder (localFile, announcement, xmlAnnouncement);
			t.start();
		}
	}

	public void transmissionExpired (de.boerde.blueparrot.satnet.laszlo.ContentReader.TransmissionEvent evt)
	{
	}

	private static class ExtractorBuilder extends Thread
	{
		private String localFile;
		private Announcement announcement;
		private BookingAnnouncement xmlAnnouncement;

		public ExtractorBuilder (String localFile, Announcement announcement, BookingAnnouncement xmlAnnouncement)
		{
			super ("ExtractorBuilder " + localFile);
			this.localFile = localFile;
			this.announcement = announcement;
			this.xmlAnnouncement = xmlAnnouncement;
		}

		public void run()
		{
			try
			{
				Extractor extractor = Extractor.getExtractor();
				PackageManager packageManager = PackageManager.getPackageManager();
				File dir = packageManager.getUniqueSubdir();
				boolean success = extractor.extract (localFile, dir);
				if (success)
				{
					packageManager.setPackageInfo (dir, announcement, xmlAnnouncement);
				}
			}
			catch (Exception e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
		}
	}
}
