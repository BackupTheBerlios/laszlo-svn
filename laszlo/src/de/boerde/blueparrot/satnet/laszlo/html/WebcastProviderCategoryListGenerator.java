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
 * WebcastProviderCategoryListGenerator.java
 *
 * Created on 6. Juni 2004, 14:51
 */

package de.boerde.blueparrot.satnet.laszlo.html;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.*;

/**
 *
 * @author  roland
 */
public class WebcastProviderCategoryListGenerator extends HtmlGenerator implements Producer
{
	private String name;
	//private String title;
	private Hashtable categories;

	public WebcastProviderCategoryListGenerator (String name, String title, Hashtable categories)
	{
		this.name = name;
		//this.title = title;
		this.categories = categories;
	}

	public void doGet (RequestInfo request, ResponseInfo response) throws IOException
	{
		BufferOutput buf = makeWriter();
		PrintWriter out = buf.getPrintWriter();
		Settings settings = Settings.getSettings();
		String[] refresh = { "<meta http-equiv=\"Refresh\" content=\"60;url=http://" + settings.getHttpOwnPseudoName() + "/" + name + "\" />" };
		writeHeadSection (out, "Categories", refresh);
		out.println ("<body>");
		if (categories != null)
		{
			Vector[] sortedServiceCategories = new Vector [wantedServices.length];
			for (int s=0; s<wantedServices.length; s++)
			{
				sortedServiceCategories [s] = new Vector();
			}
			synchronized (categories)
			{
				Enumeration catEnum = categories.keys();
				while (catEnum.hasMoreElements())
				{
					String category = (String) catEnum.nextElement();
					for (int s=0; s<wantedServices.length; s++)
					{
						if (categoryHasService (categories, category, wantedServices [s]))
						{
							sortedServiceCategories [s].add (category);
						}
					}
				}
			}
			for (int s=0; s<wantedServices.length; s++)
			{
				Vector sortedCategories = sortedServiceCategories [s];
				Collections.sort (sortedCategories);
				out.println ("<ul>");
				for (int c=0; c<sortedCategories.size(); c++)
				{
					String category = (String) sortedCategories.get (c);
					String linkName = name + "-" + category;
					out.print ("<li>");
					makeLink (out, category, linkName, "categorysPackageView");
					out.println ("</li>");
				}
				out.println ("</ul>");
			}
		}
		out.println ("</body>");
		writeClosingHtmlSection (out);
		flushWriter (response, buf);
	}

	private boolean categoryHasService (Hashtable categories, String category, String service)
	{
		Vector categorysPackages = (Vector) categories.get (category);
		Iterator packages = categorysPackages.iterator();
		while (packages.hasNext())
		{
			PackageManager.PackageInfo info = (PackageManager.PackageInfo) packages.next();
			if (service.equals (info.getXmlAnnouncement().getServiceName()))
			{
				return true;
			}
		}
		return false;
	}

/*	private boolean categoryHasServiceOtherThan (Hashtable categories, String category, String service)
	{
		Vector categorysPackages = (Vector) categories.get (category);
		Iterator packages = categorysPackages.iterator();
		while (packages.hasNext())
		{
			PackageManager.PackageInfo info = (PackageManager.PackageInfo) packages.next();
			if (!service.equals (info.getXmlAnnouncement().getServiceName()))
			{
				return true;
			}
		}
		return false;
	}
*/
	private static String[] wantedServices = { "webcasting", "newscasting" };
}
