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
 * MainIndexGenerator.java
 *
 * Created on 6. Juni 2004, 17:28
 */

package de.boerde.blueparrot.satnet.laszlo.html;

import java.io.IOException;
import java.io.PrintWriter;

import de.boerde.blueparrot.satnet.laszlo.protocol.http.Producer;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.RequestInfo;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.ResponseInfo;

/**
 * 
 * @author roland
 */
public class MainIndexGenerator extends HtmlGenerator implements Producer {
	public MainIndexGenerator() {
	}

	public void doGet(RequestInfo request, ResponseInfo response)
			throws IOException {
		BufferOutput buf = makeWriter();
		PrintWriter out = buf.getPrintWriter();
		writeHeadSection(out, "Laszlo Index Page");
		out.println("<frameset rows=\"95,*\">");
		out
				.println("<frame src=\"/providers\" name=\"castersView\" scrolling=\"no\"/>");
		out.println("<frameset cols=\"23%,*\">");
		out
				.println("<frame src=\"/files/blank.html\" name=\"castersCategoryView\" />");
		out
				.println("<frame src=\"/files/blank.html\" name=\"categorysPackageView\" />");
		out.println("</frameset>");
		out.println("</frameset>");
		writeClosingHtmlSection(out);
		flushWriter(response, buf);
	}
}
