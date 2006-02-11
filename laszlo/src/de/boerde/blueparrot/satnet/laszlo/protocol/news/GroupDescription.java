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
 * GroupDescription.java
 *
 * Created on 23. Mai 2004, 19:16
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.news;

import java.io.*;

/**
 *
 * @author  roland
 */
public class GroupDescription implements Serializable
{
	private String groupName;
	private long firstMessageNumber;
	private long lastMessageNumber;
	private String lastMessageID;
	private NewsProtocolInfo newsInfo;
	private Overview overview;
	private long createdTime;
	private long updatedTime;
	private long updatedFirstMessage;

	/** Creates a new instance of GroupDescription */
	GroupDescription()
	{
	}

	GroupDescription (String groupName)
	{
		this.groupName = groupName;
		firstMessageNumber = 1;
		lastMessageNumber = 0;
		createdTime = System.currentTimeMillis();
		updatedTime = System.currentTimeMillis();
		updatedFirstMessage = 0;
	}

	GroupDescription (String groupName, NewsProtocolInfo newsInfo)
	{
		this.groupName = groupName;
		this.newsInfo = newsInfo;
		firstMessageNumber = 1;
		int lastMessageNumber = newsInfo.getMsgAmount() -1;
		this.lastMessageNumber = lastMessageNumber +1;
		overview = new Overview (newsInfo);
		overview.setFirstNumber (firstMessageNumber);
		createdTime = System.currentTimeMillis();
		updatedTime = System.currentTimeMillis();
		updatedFirstMessage = firstMessageNumber;
		if (lastMessageNumber >= 0)
		{
			overview.open();
			lastMessageID = overview.getMessageID (this.lastMessageNumber);
			overview.close();
		}
	}

	public synchronized void setInfo (NewsProtocolInfo newNewsInfo)
	{
//System.out.println ("### before: first " + firstMessageNumber + " last " + lastMessageNumber);
		overview = new Overview (newNewsInfo);
		overview.open();

		int newLastMessageNumber = newNewsInfo.getMsgAmount() -1;
		long shift;
		if (newLastMessageNumber >= 0)
		{
			if (lastMessageID != null)
			{
				shift = overview.getMessageNumberForID (lastMessageID);
				if (shift < 0)
				{
					shift = lastMessageNumber - firstMessageNumber +1;
				}
				else
				{
					shift = lastMessageNumber - firstMessageNumber - shift;
				}
			}
			else
			{
				shift = lastMessageNumber - firstMessageNumber +1;
			}
		}
		else
		{
			shift = 0;
			lastMessageID = null;
		}
		firstMessageNumber += shift;
		lastMessageNumber = firstMessageNumber + newLastMessageNumber;
//System.out.println ("### after: first " + firstMessageNumber + " last " + lastMessageNumber + " shift " + shift);
		newsInfo = newNewsInfo;
		overview.setFirstNumber (firstMessageNumber);
		lastMessageID = overview.getMessageID (lastMessageNumber);
		overview.close();
		updatedTime = System.currentTimeMillis();
		updatedFirstMessage += shift;
	}

	public long getFirstMessageNumber()
	{
		return firstMessageNumber;
	}

	public long getLastMessageNumber()
	{
		return lastMessageNumber;
	}

	public String getGroupName()
	{
		return groupName;
	}

	public NewsProtocolInfo getNewsProtocolInfo()
	{
		return newsInfo;
	}

	public Overview getOverview()
	{
		return overview;
	}

	public long getCreatedTime()
	{
		return createdTime;
	}

	public long getUpdatedTime()
	{
		return updatedTime;
	}

	public long getUpdatedFirstMessage()
	{
		return updatedFirstMessage;
	}
}
