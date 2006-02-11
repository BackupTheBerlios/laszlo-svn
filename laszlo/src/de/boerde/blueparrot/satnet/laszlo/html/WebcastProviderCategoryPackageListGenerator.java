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
 * WebcastProviderCategoryPackageListGenerator.java
 *
 * Created on 6. Juni 2004, 14:51
 */

package de.boerde.blueparrot.satnet.laszlo.html;

import java.io.*;
import java.text.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.*;

/**
 *
 * @author  roland
 */
public class WebcastProviderCategoryPackageListGenerator extends HtmlGenerator implements Producer
{
	private String name;
	private String category;
	private String caster;
	private Vector packages;

	DateFormat dateFormat = DateFormat.getDateTimeInstance (DateFormat.FULL, DateFormat.SHORT);

	public WebcastProviderCategoryPackageListGenerator (String name, String category, String caster, Vector packages)
	{
		this.name = name;
		this.category = category;
		this.caster = caster;
		this.packages = packages;
	}

	public void doGet (RequestInfo request, ResponseInfo response) throws IOException
	{
		BufferOutput buf = makeWriter();
		PrintWriter out = buf.getPrintWriter();
		Settings settings = Settings.getSettings();
		ProviderNames providerNames = ProviderNames.getProviderNames();
		String[] refresh = { "<meta http-equiv=\"Refresh\" content=\"60;url=http://" + settings.getHttpOwnPseudoName() + "/" + name + "\" />" };
		writeHeadSection (out, "Package List", refresh);
		out.println ("<body>");
		out.print ("<h1>");
		out.print (category);
		out.print (" ");
		out.print (providerNames.getDisplayName (caster));
		out.println ("</h1>");
		if (packages != null)
		{
			synchronized (packages)
			{
				Enumeration packEnum = packages.elements();
				while (packEnum.hasMoreElements())
				{
					PackageManager.PackageInfo info = (PackageManager.PackageInfo) packEnum.nextElement();
					BookingAnnouncement announcement = info.getXmlAnnouncement();
					String url = announcement.getUrl();
					String index = announcement.getIndexHtml();
					String link;
					if (index != null)
					{
						if (!index.startsWith ("http://"))
						{
							index = "http://" + index;
						}
						// decide what to put as the Link.
						// Problem:
						// The index file that the announcement points to sometimes does not exist.
						CacheContentManager contentManager = CacheContentManager.getCacheContentManager();
						link = (contentManager.get (index) != null) ? index : url;
					}
					else
					{
						link = url;
					}
					out.println ("<div>");
					out.print ("<h2>");
					makeLink (out, url, link, "_blank");
					out.println ("</h2>");
					out.print ("<p class=\"updated\">Last updated: ");
					out.print (dateFormat.format (new Date (announcement.getUpdatedTime())).toString());
					out.println ("</p>");
					out.println ("<p>");
					out.println (announcement.getDescription());
					out.println ("<p>");
					out.println ("</div>");
				}
			}
		}

		out.println ("</body>");
		writeClosingHtmlSection (out);
		flushWriter (response, buf);
	}
}
