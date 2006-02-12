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

package de.boerde.blueparrot.satnet.laszlo.protocol.mail;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.*;
import de.boerde.blueparrot.util.*;

/**
 *
 * @author  roland
 */
public class MailProtocolInfo extends ProtocolInfo implements Serializable
{
	private File msgFile;
	private long[] msgPositions;

	public MailProtocolInfo (PackageManager.PackageInfo pkgInfo)
	{
		File dir = pkgInfo.getDir();
		BookingAnnouncement xmlAnnouncement = pkgInfo.getXmlAnnouncement();
		String url = xmlAnnouncement.getUrl();
		String newsgroup = url.substring (MAIL_PROTOCOL.length());
		FileFinder finder = new FileFinder (dir);
		finder.setFileName (newsgroup + ".msg");
		msgFile = finder.findOneFile();

		if (msgFile != null)
		{
			RandomAccessFile msgAccess = null;
			try
			{
				msgAccess = new RandomAccessFile (msgFile, "r");
				long pos = 0;
				byte[] data = new byte [4];
				int num;
				Vector positions = new Vector();
				int count = 0;
				do
				{
					msgAccess.seek (pos);
					num = msgAccess.read (data);
					if (num == 4)
					{
						positions.add (new Long (pos));
						int length = (((data[0] & 0xff) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff));
						pos += length +4;
						count++;
					}
				}
				while (num == 4);

				msgPositions = new long [positions.size()];
				for (int i=0; i<msgPositions.length; i++)
				{
					msgPositions [i] = ((Long) positions.get (i)).longValue();
				}
				GUIMain.logger.info(msgFile.getName() + " has " + count + " messages.");
			}
			catch (IOException e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
			finally
			{
				try
				{
					if (msgAccess  != null)
						msgAccess .close();
				}
				catch (IOException e)
				{
					GUIMain.logger.severe(e.getMessage());
				}
			}
		}
	}
}
