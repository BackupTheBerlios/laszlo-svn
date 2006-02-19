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
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import de.boerde.blueparrot.satnet.laszlo.protocol.http.CacheContentManager;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.ContentManagerInfo;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.Producer;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.RequestInfo;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.ResponseInfo;

/**
 * 
 * @author roland
 */
public class ReceivedAdsGenerator implements Producer {
	public ReceivedAdsGenerator() {
	}

	public void doGet(RequestInfo request, ResponseInfo response)
			throws IOException {
		StringBuffer cacheUrlBuffer = new StringBuffer(request.getUri());
		int startOfOriginalUrl = cacheUrlBuffer.indexOf("/ads_received/");
		if (startOfOriginalUrl >= 0) {
			cacheUrlBuffer.delete(0, startOfOriginalUrl
					+ "/ads_received/".length());
		}
		int hostDirEnd = cacheUrlBuffer.indexOf("/");
		if (hostDirEnd <= 0) {
			response.sendError(403);
			return;
		}
		synchronized (hostdirMap) {
			if (!hostdirMap.contains(cacheUrlBuffer.substring(0, hostDirEnd))) {
				response.sendError(403);
				return;
			}
		}
		cacheUrlBuffer.insert(0, "http://");
		String cacheUrl = cacheUrlBuffer.toString();
		CacheContentManager cacheManager = CacheContentManager
				.getCacheContentManager();
		ContentManagerInfo info = cacheManager.get(cacheUrl);
		Hashtable headers = info.getHeaders();
		Enumeration headerEnum = headers.keys();
		while (headerEnum.hasMoreElements()) {
			String headerName = (String) headerEnum.nextElement();
			String headerValue = (String) headers.get(headerName);
			response.setHeader(headerName, headerValue);
		}
		InputStream inStream = info.getContent();
		OutputStream outStream = response.getOutputStream();
		byte[] buffer = new byte[2048];
		while (true) {
			int len = inStream.read(buffer);
			if (len < 0)
				break;
			outStream.write(buffer, 0, len);
		}
	}

	void addAdvertisingHostdir(String hostdir) {
		synchronized (hostdirMap) {
			hostdirMap.add(hostdir);
		}
	}

	private HashSet hostdirMap = new HashSet();
}
