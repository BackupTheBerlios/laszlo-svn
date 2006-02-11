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
 * WebcastProviderListGenerator.java
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
public class WebcastProviderListGenerator extends HtmlGenerator implements Producer
{
	private String name;
	private Hashtable casters;

	public WebcastProviderListGenerator (String name, Hashtable casters)
	{
		this.name = name;
		this.casters = casters;
	}

	public void doGet (RequestInfo request, ResponseInfo response) throws IOException
	{
		BufferOutput buf = makeWriter();
		PrintWriter out = buf.getPrintWriter();
		ProviderNames providerNames = ProviderNames.getProviderNames();
		Settings settings = Settings.getSettings();
		String[] refresh = { "<meta http-equiv=\"Refresh\" content=\"240;url=http://" + settings.getHttpOwnPseudoName() + "/" + name + "\" />" };
		writeHeadSection (out, "Webcasters", refresh);
		out.println ("<body>");
		out.println ("<table border=\"0\" width=\"100%\">");
		out.println ("<tr valign=\"middle\"><td>");
		out.println ("<h1>Laszlo Index</h1>");
		out.println ("<p class=\"providers\">");
		int numProviders = 0;
		if (casters != null)
		{
			Vector sortedProviders = new Vector();
			synchronized (casters)
			{
				Enumeration casterEnum = casters.keys();
				while (casterEnum.hasMoreElements())
				{
					String caster = (String) casterEnum.nextElement();
					sortedProviders.add (caster);
				}
			}
			Collections.sort (sortedProviders);
			for (int p=0; p<sortedProviders.size(); p++)
			{
				String caster = (String) sortedProviders.get(p);
				String linkName = "list-" + caster;
				out.print ("<span>");
				makeLink (out, providerNames.getDisplayName (caster), linkName, "castersCategoryView");
				out.print ("</span>");
				out.println();
				numProviders++;
			}
		}
		if (numProviders == 0)
		{
			out.println ("No transmissions have been received yet.");
		}
		out.println ("</p>");
		out.println ("</td>");
		out.println ("<td>");
		out.println ("<iframe src=\"http://" + settings.getHttpOwnPseudoName() + "/advertising\" frameborder=\"0\" scrolling=\"no\" marginwidth=\"0\" marginheight=\"0\" width=\"100%\" height=\"60\">");
		out.println ("[Advertising]");
		out.println ("</iframe>");
		out.println ("</td></tr>");
		out.println ("</table>");
		out.println ("</body>");
		writeClosingHtmlSection (out);
		flushWriter (response, buf);
	}
}
