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
 * ProtocolInfo.java
 *
 * Created on 16. Mai 2004, 18:30
 */

package de.boerde.blueparrot.satnet.laszlo.protocol;

import java.io.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.mail.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.news.*;

/**
 *
 * @author  roland
 */
public class ProtocolInfo implements Serializable
{
	protected static final String MAIL_PROTOCOL = "mail://";
	protected static final String NEWS_PROTOCOL = "news://";
	protected static final String HTTP_PROTOCOL = "http://";

	/** Creates a new instance of ProtocolInfo */
	public ProtocolInfo()
	{
	}

	public static ProtocolInfo getProtocolInfo (PackageManager.PackageInfo pkgInfo)
	{
		String url = pkgInfo.getXmlAnnouncement().getUrl();
		if (url != null)
		{
			if (url.startsWith (MAIL_PROTOCOL))
				return new MailProtocolInfo (pkgInfo);
			else if (url.startsWith (NEWS_PROTOCOL))
				return new NewsProtocolInfo (pkgInfo);
			else if (url.startsWith (HTTP_PROTOCOL))
				return new HttpProtocolInfo (pkgInfo);
		}
		return null;
	}
}
