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
 * Overview.java
 *
 * Created on 30. Mai 2004, 15:06
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.news;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.GUIMain;

/**
 *
 * @author  roland
 */
public class Overview implements Serializable
{
	private RandomAccessFile overviewAccess;
	private int openCount;
	private File overviewFile;
	private long firstNumber;

	private static final String[] overviewInformation = { "Subject:", "From:", "Date:", "Message-ID:", "References:", "Bytes:", "Lines:" };
	private static final int overviewMessageIDPos = 3;
	private static final int overviewBytesPos = 5;
	private static final byte[] CRLF = { (byte) 13, (byte) 10 };

	private static int putIntoOverview (String[] overview, String line)
	{
		int lineLen = line.length();
		for (int i=0; i<overviewInformation.length; i++)
		{
			String overItem = overviewInformation [i];
			int len = overItem.length();
			if ((lineLen >= len) && overItem.equalsIgnoreCase (line.substring (0, len)))
			{
				overview [i] = line.substring (len+1);
				return i;
			}
		}
		return -1;
	}

	/** Creates a new instance of Overview */
	private Overview()
	{
	}

	public Overview (NewsProtocolInfo info)
	{
		overviewFile = new File (info.getMsgFile().getAbsolutePath() + ".laszloOverview.ser");
		createOverview (info);
	}

	private void createOverview (NewsProtocolInfo info)
	{
		int count = info.getMsgAmount();
		long[] msgFilePos = info.getMsgPositions();
		boolean ok = true;
		RandomAccessFile msgAccess = null;
		try
		{
			overviewAccess = new RandomAccessFile (overviewFile, "rw");
			msgAccess = new RandomAccessFile (info.getMsgFile(), "r");
			long overFilePos = count * 8;
			String[] overview = new String [overviewInformation.length];
			for (int i=0; i<count; i++)
			{
				overviewAccess.seek (i * 8);
				overviewAccess.writeLong (overFilePos);
				overviewAccess.seek (overFilePos);
				msgAccess.seek (msgFilePos [i]);
				int numBytes;
				{
					byte[] data = new byte [4];
					//int num = msgAccess.read (data);
					numBytes = (((data[0] & 0xff) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff));
				}
				String line = "";
				Arrays.fill (overview, "");
				do
				{
					String newLine = msgAccess.readLine();

					if (newLine.startsWith ("\t"))
					{
						line = line + " " + newLine.substring (1);
					}
					else
					{
						putIntoOverview (overview, line);
						line = newLine;
					}
				}
				while ((line != null) && !"".equals (line));
				overview [overviewBytesPos] = String.valueOf (numBytes);
				StringBuffer overviewLine = new StringBuffer (256);
				for (int o=0; o<overview.length; o++)
				{
					overviewLine.append ('\t');
					overviewLine.append (overview [o]);
				}
				overviewLine.append ("\r\n");
				overviewAccess.write (overviewLine.toString().getBytes ("ISO-8859-1"));	// Has been read by RandomAccessFile.getLine() so according to Javadocs should be quite close to isolatin1
				overFilePos = overviewAccess.getFilePointer();
			}
			overviewAccess.setLength (overFilePos);
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
			ok = false;
		}
		finally
		{
			try
			{
				overviewAccess.close();
			}
			catch (IOException e)
			{
				GUIMain.logger.severe(e.getMessage());
			}
			finally
			{
				if (msgAccess != null)
				{
					try
					{
						msgAccess.close();
					}
					catch (IOException e)
					{
						GUIMain.logger.severe(e.getMessage());
					}
				}
			}
		}
		if (!ok)
		{
			overviewFile.delete();
		}
	}

	synchronized void setFirstNumber (long firstNumber)
	{
		this.firstNumber = firstNumber;
	}

	private synchronized void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeObject (overviewFile);
		out.writeLong (firstNumber);
	}

	private synchronized void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		if (overviewAccess != null)
			close();
		overviewFile = (File) in.readObject();
		firstNumber = in.readLong();
	}

	public synchronized boolean open()
	{
		try
		{
			if (openCount == 0)
			{
				overviewAccess = new RandomAccessFile (overviewFile, "r");
			}
			openCount++;
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	public synchronized void close()
	{
		try
		{
			openCount--;
			if (openCount <= 0)
			{
				if (overviewAccess != null)
				{
					overviewAccess.close();
					overviewAccess = null;
				}
				openCount = 0;
			}
		}
		catch (IOException e)
		{
		}
	}

	public synchronized void writeOverview (OutputStream out, long from, long to)
	{
		if (overviewAccess == null)
			return;

		try
		{
			int localFrom = (int) (from - firstNumber);
			overviewAccess.seek (localFrom*8);
			long fromPos = overviewAccess.readLong();
			overviewAccess.seek (fromPos);
			for (long num=from; num<=to; num++)
			{
				String overLine = overviewAccess.readLine();
				if ((overLine != null) && !"".equals (overLine))
				{
					out.write (String.valueOf (num).getBytes ("US-ASCII"));
					out.write (overLine.getBytes ("ISO-8859-1"));	// Has been read by RandomAccessFile.getLine() so according to Javadocs should be quite close to isolatin1
					out.write (CRLF);
				}
			}
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
		}
	}

	public synchronized String getMessageID (long number)
	{
		if (overviewAccess == null)
		{
			return "";
		}

		try
		{
			int localNumber = (int) (number - firstNumber);
			overviewAccess.seek (localNumber*8);
			long recordPos = overviewAccess.readLong();
			overviewAccess.seek (recordPos);
			String overviewLine = overviewAccess.readLine();
			StringTokenizer tok = new StringTokenizer (overviewLine, "\t");
			if (tok.countTokens() > overviewMessageIDPos)
			{
				for (int i=1; i<overviewMessageIDPos; i++)
				{
					tok.nextToken();
				}
				return tok.nextToken();
			}
			else
			{
				return "";
			}
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
			return "";
		}
	}

	public synchronized long getMessageNumberForID (String id)
	{
		if (overviewAccess == null)
		{
			return -1;
		}

		try
		{
			overviewAccess.seek (0);
			long firstRecordPos = overviewAccess.readLong();
			overviewAccess.seek (firstRecordPos);
			long messageNumber = firstNumber;
			while (true)
			{
				String overviewLine = overviewAccess.readLine();
				if (overviewLine == null)
					break;

				StringTokenizer tok = new StringTokenizer (overviewLine, "\t");
				if (tok.countTokens() > overviewMessageIDPos)
				{
					for (int i=1; i<overviewMessageIDPos; i++)
					{
						tok.nextToken();
					}
					if (id.equals (tok.nextToken()))
					{
						return messageNumber;
					}
				}
				messageNumber++;
			}
			return -1;
		}
		catch (IOException e)
		{
			GUIMain.logger.severe(e.getMessage());
			return -1;
		}
	}
}
