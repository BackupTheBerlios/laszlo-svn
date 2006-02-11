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

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.io.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.*;
import de.boerde.blueparrot.util.*;


/**
 *
 * @author  roland
 */
public class HttpProtocolInfo extends ProtocolInfo implements Serializable
{
	private File documentRoot;

	public HttpProtocolInfo (PackageManager.PackageInfo pkgInfo)
	{
		String lastIterationUrl = null;
		String lastIterationIndexHtml = null;
		findIndex:
		for (int iteration=0; ; iteration++)
		{
			final String url = CacheContentManager.rewriteUriToLocal (pkgInfo.getXmlAnnouncement().getUrl(), iteration);
			final String indexHtml = CacheContentManager.rewriteUriToLocal (pkgInfo.getXmlAnnouncement().getIndexHtml(), iteration);
			if (url == null && indexHtml == null)
			{
				break findIndex;
			}
			lastIterationUrl = url;
			lastIterationIndexHtml = indexHtml;

			FileFinder finder = new FileFinder (pkgInfo.getDir());
			finder.setFileFilter (new FileFilter()
				{
					public boolean accept (File file)
					{
						if (file.isDirectory())
						{
							File urlFile = new File (file.getAbsolutePath() + File.separator + url);
							if (urlFile.exists())
							{
								return true;
							}
							else
							{
								File indexFile = new File (file.getAbsolutePath() + File.separator + indexHtml);
								return indexFile.exists();
							}
						}
						else
						{
							return false;
						}
					}
				});
			try
			{
				File result = finder.findOneFile();
				if (result != null)
				{
					documentRoot = result.getCanonicalFile();
					break findIndex;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace (System.err);
			}
		}
	}

	public File getDocumentRoot()
	{
		return documentRoot;
	}
}
