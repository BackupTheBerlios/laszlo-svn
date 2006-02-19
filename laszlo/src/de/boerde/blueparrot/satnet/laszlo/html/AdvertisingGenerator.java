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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.boerde.blueparrot.satnet.laszlo.BookingAnnouncement;
import de.boerde.blueparrot.satnet.laszlo.GUIMain;
import de.boerde.blueparrot.satnet.laszlo.PackageManager;
import de.boerde.blueparrot.satnet.laszlo.Settings;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.CacheContentManager;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.ContentManagerInfo;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.Producer;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.RequestInfo;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.ResponseInfo;
import de.boerde.blueparrot.xml.XMLHelpers;

/**
 * 
 * @author roland
 */
public class AdvertisingGenerator extends HtmlGenerator implements Producer {
	private DocumentBuilder xmlDocumentBuilder;

	private Random random;

	private ReceivedAdsGenerator receivedAdsGenerator;

	public AdvertisingGenerator(ReceivedAdsGenerator receivedAdsGenerator) {
		this.receivedAdsGenerator = receivedAdsGenerator;
		random = new Random();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		try {
			xmlDocumentBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			GUIMain.getLogger().severe(e.getMessage());
		}
	}

	public void doGet(RequestInfo request, ResponseInfo response)
			throws IOException {
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		BufferOutput buf = makeWriter();
		PrintWriter out = buf.getPrintWriter();
		Settings settings = Settings.getSettings();
		String[] refresh = { "<meta http-equiv=\"Refresh\" content=\"120;url=http://"
				+ settings.getHttpOwnPseudoName() + "/advertising\" />" };
		writeHeadSection(out, "Advertising", refresh);
		out.println("<body>");
		PackageManager packageManager = PackageManager.getPackageManager();
		Iterator iterator = packageManager.iterator();
		Vector ads = new Vector();
		while (iterator.hasNext()) {
			PackageManager.PackageInfo info = (PackageManager.PackageInfo) iterator
					.next();
			BookingAnnouncement xmlAnnouncement = info.getXmlAnnouncement();
			if ("advertising".equalsIgnoreCase(info.getXmlAnnouncement()
					.getServiceName())) {
				ads.add(xmlAnnouncement.getIndexHtml());
			}
		}
		int size = ads.size();
		ContentManagerInfo info = null;
		String indexHtml = null;
		String linkUrl = null;
		while (size > 0) {
			int which = random.nextInt(size);
			indexHtml = (String) ads.get(which);
			if (indexHtml.startsWith("http://")) {
				linkUrl = "/ads_received/"
						+ indexHtml.substring("http://".length());
			} else {
				linkUrl = "/ads_received/" + indexHtml;
				indexHtml = "http://" + indexHtml;
			}
			int hostdirEnd = indexHtml.indexOf('/', "http://".length());
			if (hostdirEnd <= 0) {
				response.sendError(403);
				return;
			}
			receivedAdsGenerator.addAdvertisingHostdir(indexHtml.substring(
					"http://".length(), hostdirEnd));

			CacheContentManager cacheManager = CacheContentManager
					.getCacheContentManager();
			info = cacheManager.get(indexHtml);
			if (info != null) {
				break;
			} else {
				ads.remove(which);
				size = ads.size();
			}
		}
		if (info != null) {
			InputStream xmlStream = info.getContent();
			try {
				Document advInfo = xmlDocumentBuilder.parse(xmlStream);
				String bannerDir;
				{
					StringBuffer bannerBuf = new StringBuffer(linkUrl);
					int indexDirPos = linkUrl.lastIndexOf('/');
					if (indexDirPos >= 0) {
						bannerBuf.delete(indexDirPos + 1, linkUrl.length());
					}
					bannerDir = bannerBuf.toString();
				}
				String clickUrl;
				{
					StringBuffer clickBuf = new StringBuffer(indexHtml);
					int indexDirPos = indexHtml.lastIndexOf('/');
					if (indexDirPos >= 0) {
						clickBuf.delete(indexDirPos + 1, indexHtml.length());
					}
					clickUrl = clickBuf.toString();
				}
				String rich = "";
				NodeList elements = advInfo.getElementsByTagName("RICH_MEDIA");
				for (int e = 0; e < elements.getLength(); e++) {
					Node n = elements.item(e);
					if (n instanceof Element)
						rich = XMLHelpers.getContentText((Element) n);
				}
				// String border = null;
				// elements = advInfo.getElementsByTagName ("BORDER");
				// for (int e=0; e<elements.getLength(); e++)
				// {
				// Node n = elements.item (e);
				// if (n instanceof Element)
				// border = XMLHelpers.getContentText ((Element) n);
				// }
				String scrollbar = null;
				elements = advInfo.getElementsByTagName("SCROLLBAR");
				for (int e = 0; e < elements.getLength(); e++) {
					Node n = elements.item(e);
					if (n instanceof Element)
						scrollbar = XMLHelpers.getContentText((Element) n);
				}
				String size_x = null;
				elements = advInfo.getElementsByTagName("SIZE_X");
				for (int e = 0; e < elements.getLength(); e++) {
					Node n = elements.item(e);
					if (n instanceof Element)
						size_x = XMLHelpers.getContentText((Element) n);
				}
				String size_y = null;
				elements = advInfo.getElementsByTagName("SIZE_Y");
				for (int e = 0; e < elements.getLength(); e++) {
					Node n = elements.item(e);
					if (n instanceof Element)
						size_y = XMLHelpers.getContentText((Element) n);
				}

				out.println("<script language=\"JavaScript\">");
				out.println("function clickOnAd()");
				out.println("{");
				if (!"".equals(rich)) {
					out.print("window.open (\"" + clickUrl + rich
							+ "\", \"satelliteAdvertising\", \"");
					boolean comma = false;
					/*
					 * if (border != null) { if (comma) out.print (","); else
					 * comma=true; if ("false".equals (border)) out.print (",
					 * border=no"); else out.print (", border=yes"); }
					 */
					if (scrollbar != null) {
						if (comma)
							out.print(",");
						else
							comma = true;
						if ("false".equals(scrollbar))
							out.print("scrollbars=no");
						else
							out.print("scrollbars=yes");
					}
					if (size_x != null) {
						if (comma)
							out.print(",");
						else
							comma = true;
						out.print("width=" + size_x);
					}
					if (size_y != null) {
						if (comma)
							out.print(",");
						else
							comma = true;
						out.print("height=" + size_y);
					}
					out.println("\");");
				}
				out.println("}");
				out.println("</script>");

				NodeList banners = advInfo.getElementsByTagName("AD_BANNER");
				for (int b = 0; b < banners.getLength(); b++) {
					Node bannerNode = banners.item(b);
					String banner = null;
					if (!(bannerNode instanceof Element))
						break;
					banner = bannerDir
							+ XMLHelpers.getContentText((Element) bannerNode);

					out
							.println("<iframe src=\""
									+ banner
									+ "\" frameborder=\"0\" scrolling=\"no\" marginwidth=\"0\" marginheight=\"0\" width=\"468\" height=\"60\">");
					out.println(banner + " should have appeared here");
					out.println("</iframe>");
				}
			} catch (SAXException e) {
				out.print("<h3>Error in ad banner ");
				out.print(escapeHtml(indexHtml));
				out.println("</h3>");
				out.println(escapeHtml(e.getMessage()));
			}
		}

		out.println("</body>");
		writeClosingHtmlSection(out);
		flushWriter(response, buf);
	}
}
