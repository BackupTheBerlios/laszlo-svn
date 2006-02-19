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
 * PackageLinksGenerator.java
 *
 * Created on 5. Juni 2004, 21:10
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import de.boerde.blueparrot.satnet.laszlo.html.IndexContentManager;
import de.boerde.blueparrot.satnet.laszlo.html.WebcastProviderCategoryListGenerator;
import de.boerde.blueparrot.satnet.laszlo.html.WebcastProviderCategoryPackageListGenerator;
import de.boerde.blueparrot.satnet.laszlo.html.WebcastProviderListGenerator;

/**
 * 
 * @author roland
 */
public class PackageOrganizer {
	private Hashtable webcasters;

	private IndexContentManager htmlManager;

	PackageOrganizer() {
		htmlManager = IndexContentManager.getIndexContentManager();
		webcasters = new Hashtable();
		htmlManager.setPath("providers", new WebcastProviderListGenerator(
				"providers", webcasters));
	}

	synchronized void initialize(PackageManager packageManager) {
		boolean concurrentChange;
		do {
			try {
				concurrentChange = false;
				webcasters = new Hashtable();
				htmlManager.setPath("providers",
						new WebcastProviderListGenerator("providers",
								webcasters));
				Iterator allPackages = packageManager.iterator();
				while (allPackages.hasNext()) {
					PackageManager.PackageInfo info = (PackageManager.PackageInfo) allPackages
							.next();
					setPackageInfo(info);
				}
			} catch (ConcurrentModificationException e) {
				concurrentChange = true;
			}
		} while (concurrentChange);
	}

	public synchronized void setPackageInfo(PackageManager.PackageInfo info) {
		BookingAnnouncement announcement = info.getXmlAnnouncement();
		String webcaster = announcement.getOneValue("WEBCASTER_ID");
		String category = announcement.getOneValue("CATEGORY");
		/*
		 * String category = announcement.getOneValue
		 * ("PROFILE/CATEGORY/CATEGORY_NAME"); if ((category == null) ||
		 * "".equals (category)) { category = announcement.getOneValue
		 * ("CATEGORY"); }
		 */
		// String service = announcement.getServiceName();
		Hashtable categories;
		synchronized (webcasters) {
			categories = (Hashtable) webcasters.get(webcaster);
			if (categories == null) {
				categories = new Hashtable();
				webcasters.put(webcaster, categories);
				String webName = "list-" + webcaster;
				htmlManager.setPath(webName,
						new WebcastProviderCategoryListGenerator(webName,
								webcaster, categories));
			}
		}
		Vector packages;
		synchronized (categories) {
			packages = (Vector) categories.get(category);
			if (packages == null) {
				packages = new Vector();
				categories.put(category, packages);
				String webName = "list-" + webcaster + "-" + category;
				htmlManager.setPath(webName,
						new WebcastProviderCategoryPackageListGenerator(
								webName, category, webcaster, packages));
			}
		}
		// String url = announcement.getUrl();
		long updated = announcement.getUpdatedTime();
		synchronized (packages) {
			for (int pos = 0; pos < packages.size(); pos++) {
				PackageManager.PackageInfo otherInfo = (PackageManager.PackageInfo) packages
						.get(pos);
				// int comp = url.compareToIgnoreCase
				// (otherInfo.getXmlAnnouncement().getUrl());
				long comp = (otherInfo.getXmlAnnouncement().getUpdatedTime() - updated);
				if (comp < 0) {
					packages.add(pos, info);
					return;
				}
				/*
				 * else if (comp == 0) { packages.set (pos, info); return; }
				 */
			}
			packages.add(info);
		}
	}

	public synchronized void removePackageInfo(PackageManager.PackageInfo info) {
		BookingAnnouncement announcement = info.getXmlAnnouncement();
		String webcaster = announcement.getOneValue("WEBCASTER_ID");
		String category = announcement.getOneValue("CATEGORY");
		/*
		 * String category = announcement.getOneValue
		 * ("PROFILE/CATEGORY/CATEGORY_NAME"); if ((category == null) ||
		 * "".equals (category)) { category = announcement.getOneValue
		 * ("CATEGORY"); }
		 */
		// String service = announcement.getServiceName();
		Hashtable categories;
		synchronized (webcasters) {
			categories = (Hashtable) webcasters.get(webcaster);
			if (categories == null) {
				return;
			}
			Vector packages;
			synchronized (categories) {
				packages = (Vector) categories.get(category);
				if (packages == null) {
					return;
				}

				String url = announcement.getUrl();
				synchronized (packages) {
					for (int pos = 0; pos < packages.size(); pos++) {
						PackageManager.PackageInfo otherInfo = (PackageManager.PackageInfo) packages
								.get(pos);
						if (url.equalsIgnoreCase(otherInfo.getXmlAnnouncement()
								.getUrl())) {
							packages.remove(pos);
							break;
						}
					}
					if (packages.isEmpty()) {
						categories.remove(category);
					}
				}
				if (categories.isEmpty()) {
					webcasters.remove(webcaster);
				}
			}
		}
	}
}
