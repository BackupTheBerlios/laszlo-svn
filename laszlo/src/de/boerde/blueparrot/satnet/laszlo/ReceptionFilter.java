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
 * ReceptionFilter.java
 *
 * Created on 15. Mai 2004, 14:23
 */

package de.boerde.blueparrot.satnet.laszlo;

/**
 *
 * @author  roland
 */
public class ReceptionFilter
{
	/** Creates a new instance of ReceptionFilter */
	public ReceptionFilter ()
	{
	}

	public Response shouldReceive (Announcement announcement)
	{
		XMLAnnouncementManager xmlAnnouncementManager = XMLAnnouncementManager.getXMLAnnouncementManager();
		PackageManager pkgManager = PackageManager.getPackageManager();
		String transferName = announcement.getFullName();

		if (transferName.indexOf (":\\P\\1\\") == 1)
			return new Response (true, "seems to be something internal");
		if (transferName.indexOf ("\\Announcement\\") >= 0)
			return new Response (true, "seems to be an announcement file");
		if (transferName.endsWith ("webcasters.xml"))
			return new Response (true, "seems to be the webcasters definitions");

		String fileId = announcement.getDetail ("fileid");
		BookingAnnouncement xmlAnnouncement = xmlAnnouncementManager.getXMLAnnouncement (transferName);
		if (xmlAnnouncement == null)
		{
			return new Response (false, "don't have an XML announcement for this transmission");
		}

		String url = xmlAnnouncement.getUrl();
		PackageManager.PackageInfo pkgInfo = pkgManager.getPackageInfo (url);
		if (pkgInfo != null)
		{
			BookingAnnouncement oldXmlAnnouncement = pkgInfo.getXmlAnnouncement();
			long oldUpdated = oldXmlAnnouncement.getUpdatedTime();
			long updated = xmlAnnouncement.getUpdatedTime();
			if (updated > oldUpdated)
			{
				return new Response (true, "existing transmission, but it was updated");
			}
			else if (updated == 0)
			{
				return new Response (true, "existing transmission, but it does not carry an update timestamp");
			}
			else
			{
				pkgInfo.updateXmlAnnouncement (xmlAnnouncement);
				return new Response (false, "already have this transmission, and it was not updated");
			}
		}

		return new Response (true, "why not receive, seems I do not have this transmission yet");
	}

	public static class Response
	{
		private boolean result;
		private String reason;

		protected Response (boolean result, String reason)
		{
			this.result = result;
			this.reason = reason;
		}

		public boolean getResult()
		{
			return result;
		}

		public String getReason()
		{
			return reason;
		}
	}
}
