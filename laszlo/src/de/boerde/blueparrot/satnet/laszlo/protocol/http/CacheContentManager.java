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
 * ContentManager.java
 *
 * Created on 31. Mai 2004, 15:59
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import de.boerde.blueparrot.satnet.laszlo.GUIMain;
import de.boerde.blueparrot.satnet.laszlo.PackageManager;
import de.boerde.blueparrot.satnet.laszlo.protocol.ProtocolInfo;

/**
 * 
 * @author roland
 */
public class CacheContentManager implements ContentManager,
		PackageManager.PackageListener {
	private CacheContentManager() {
		PackageManager pkgManager = PackageManager.getPackageManager();
		synchronized (hostdirMap) {
			pkgManager.addPackageListener(this);
			Iterator packages = pkgManager.iterator();
			while (packages.hasNext()) {
				PackageManager.PackageInfo info = (PackageManager.PackageInfo) packages
						.next();
				packageAdded(info);
			}
		}
	}

	public void packageAdded(PackageManager.PackageInfo info) {
		synchronized (hostdirMap) {
			ProtocolInfo protoInfo = info.getProtocolInfo();
			if (protoInfo instanceof HttpProtocolInfo) {
				File subdir = ((HttpProtocolInfo) protoInfo).getDocumentRoot();
				if ((subdir != null) && subdir.isDirectory()) {
					File[] hostdirs = subdir.listFiles();
					if (hostdirs != null) {
						for (int h = 0; h < hostdirs.length; h++) {
							File hostdir = hostdirs[h];
							String hostdirName = hostdir.getName()
									.toLowerCase();
							Vector hostdirVector = (Vector) hostdirMap
									.get(hostdirName);
							if (hostdirVector == null) {
								hostdirVector = new Vector();
								hostdirMap.put(hostdirName, hostdirVector);
							}
							hostdirVector.add(hostdir);
						}
					}
				}
			}
		}
	}

	public void packageRemoved(PackageManager.PackageInfo info) {
		synchronized (hostdirMap) {
			ProtocolInfo protoInfo = info.getProtocolInfo();
			if (protoInfo instanceof HttpProtocolInfo) {
				File subdir = ((HttpProtocolInfo) protoInfo).getDocumentRoot();
				if ((subdir != null) && subdir.isDirectory()) {
					File[] hostdirs = subdir.listFiles();
					if (hostdirs != null) {
						for (int h = 0; h < hostdirs.length; h++) {
							File hostdir = hostdirs[h];
							String hostdirName = hostdir.getName();
							Vector hostdirVector = (Vector) hostdirMap
									.get(hostdirName);
							if (hostdirVector != null) {
								hostdirVector.remove(hostdir);
								if (hostdirVector.isEmpty()) {
									hostdirMap.remove(hostdirName);
								}
							}
						}
					}
				}
			}
		}
	}

	public void packageUpdated(PackageManager.PackageInfo oldInfo,
			PackageManager.PackageInfo newInfo) {
		synchronized (hostdirMap) {
			packageRemoved(oldInfo);
			packageAdded(newInfo);
		}
	}

	public ContentManagerInfo get(String uri) {
		if (!uri.startsWith("http://")) {
			return null;
		}

		// Settings settings = Settings.getSettings();
		String hostName = getHostNamePart(uri).toLowerCase();
		ContentManagerInfo result = new ContentManagerInfo();
		String mimeTypeAccordingToEnding = MimeTypes.getMimeTypes()
				.classifyEnding(uri);
		if (mimeTypeAccordingToEnding != null) {
			result.setHeader("Content-Type", mimeTypeAccordingToEnding);
		}
		File resultFile = null;

		findFile: for (int iteration = 0;; iteration++) {
			String localUri = rewriteUriToLocal(uri, iteration);
			if (localUri == null)
				break;

			synchronized (hostdirMap) {
				Vector hostdirVector = (Vector) hostdirMap.get(hostName);
				if (hostdirVector != null) {
					for (int v = 0; v < hostdirVector.size(); v++) {
						File hostdir = (File) hostdirVector.get(v);
						String hostdirAbsolutePath = hostdir.getAbsolutePath();
						try {
							File testFile = new File(hostdirAbsolutePath
									+ File.separatorChar
									+ localUri.substring(hostName.length() + 1));
							testFile = testFile.getCanonicalFile();
							String testFileAbsolutePath = testFile
									.getAbsolutePath();
							if (testFileAbsolutePath
									.startsWith(hostdirAbsolutePath)) {
								// make sure we stay in the correct host directory
								// and do not navigate out of it via any ".." or
								// similar constructions, to prevent javascript
								// attacks
								int hostdirAbsolutePathLen = hostdirAbsolutePath
										.length();
								if ((testFileAbsolutePath.length() == hostdirAbsolutePathLen)
										|| (testFileAbsolutePath
												.charAt(hostdirAbsolutePathLen) == File.separatorChar)) {
									if (testFile.exists()) {
										resultFile = testFile;
										break findFile;
									}
								}
							}
						} catch (IOException e) {
							GUIMain.getLogger().severe(e.getMessage());
						}
					}
				}
			}
		}

		if (resultFile == null)
			return null;

		if (resultFile.isDirectory()) {
			File fileResultFile = null;
			for (int w = 0; w < welcomeNames.length; w++) {
				fileResultFile = new File(resultFile.getAbsolutePath()
						+ File.separator + welcomeNames[w]);
				if (fileResultFile.exists() && fileResultFile.isFile()) {
					result.setHeader("Content-Type", welcomeTypes[w]);
					resultFile = fileResultFile;
				}
			}
		}

		if (resultFile.isFile()) {
			result.setFile(resultFile);
			return result;
		} else {
			return null;
		}
	}

	private static String getHostNamePart(String uri) {
		// assert uri.startsWith ("http://");
		final int startInUri = "http://".length();
		int endOfHostname = uri.indexOf("/", startInUri);
		if (endOfHostname < 0)
			endOfHostname = uri.length();

		return uri.substring(startInUri, endOfHostname);
	}

	static String rewriteUriToLocal(String uri, int iteration) {
		int charsAmbiguous = 0;
		StringBuffer uriBuffer = new StringBuffer(uri);
		if (uri.startsWith("http://")) {
			uriBuffer.delete(0, "http://".length());
		}

		// convert the hostname part to lowercase (our way to assure
		// case-insensitivity...
		int endOfHostname = uriBuffer.indexOf("/");
		if (endOfHostname < 0)
			endOfHostname = uriBuffer.length();
		uriBuffer.replace(0, endOfHostname, uriBuffer.substring(0,
				endOfHostname).toLowerCase());

		boolean qMarkSeen = false;
		for (int i = 0; i < uriBuffer.length(); i++) {
			char current = uriBuffer.charAt(i);
			switch (current) {
			case '?': {
				qMarkSeen = true;
				uriBuffer.setCharAt(i, '-');
				break;
			}
			case ':':
			case '*':
			case '|': {
				uriBuffer.setCharAt(i, '-');
				break;
			}
			case '/': {
				if (!qMarkSeen) {
					uriBuffer.setCharAt(i, File.separatorChar);
				}
				break;
			}
			case '\\': {
				if (!qMarkSeen) {
					uriBuffer.setCharAt(i, File.separatorChar);
				} else {
					GUIMain.getLogger().warning(
							"###### URI with \\. UNIMPLEMENTED. Please check how to handle this: "
									+ uri);
					break;
				}
			}
			case '%': {
				try {
					if (i + 2 < uriBuffer.length()) {
						char d1 = uriBuffer.charAt(i + 1);
						char d2 = uriBuffer.charAt(i + 2);
						char c = (char) ((hexDigitToNumber(d1) << 4) | hexDigitToNumber(d2));
						if (iteration > 0) {
							if (((iteration - 1) & (1 << charsAmbiguous)) == 0)	{
								uriBuffer.setCharAt(i, c);
								uriBuffer.delete(i + 1, i + 3);
								if (c != '%') {
									i--; // to loop over the same character again
								}
							}
							charsAmbiguous++;
						}
					}
				} catch (NumberFormatException e) {
					// do nothing; just keep the original character
				}
				break;
			}
			}
		}

		if ((1 << charsAmbiguous) > (iteration)) {
			return uriBuffer.toString();
		} else {
			return null;
		}
	}

	private static final int hexDigitToNumber(final char c) {
		switch (c) {
		case '0': {
			return 0;
		}
		case '1': {
			return 1;
		}
		case '2': {
			return 2;
		}
		case '3': {
			return 3;
		}
		case '4': {
			return 4;
		}
		case '5': {
			return 5;
		}
		case '6': {
			return 6;
		}
		case '7': {
			return 7;
		}
		case '8': {
			return 8;
		}
		case '9': {
			return 9;
		}
		case 'a':
		case 'A': {
			return 10;
		}
		case 'b':
		case 'B': {
			return 11;
		}
		case 'c':
		case 'C': {
			return 12;
		}
		case 'd':
		case 'D': {
			return 13;
		}
		case 'e':
		case 'E': {
			return 14;
		}
		case 'f':
		case 'F': {
			return 15;
		}
		default: {
			throw new NumberFormatException(String.valueOf(c));
		}
		}
	}

	private Hashtable hostdirMap = new Hashtable();

	private static final String[] welcomeNames = { "index.htm", "index.html",
			"welcome.htm", "welcome.html" };

	private static final String[] welcomeTypes = { "text/html", "text/html",
			"text/html", "text/html" };

	private static CacheContentManager theCacheContentManager = new CacheContentManager();

	public static CacheContentManager getCacheContentManager() {
		return theCacheContentManager;
	}
}
