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
 * NewsNNTPThread.java
 *
 * Created on 29. Mai 2004, 15:10
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.news;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  roland
 */
public class NewsNNTPThread extends Thread
{
	private GroupManager groupManager;
	private Socket connection;
	private boolean shouldRun = true;
	private OutputStream outStream;
	private BufferedReader inReader;
	private GroupDescription currentGroup;
	private Overview groupOverview;
	private long currentArticle;
	private RandomAccessFile articleFile;

	private static final byte[] CRLF = { (byte) 13, (byte) 10 };

	/** Creates a new instance of NewsNNTPThread */
	public NewsNNTPThread (Socket connection)
	{
		this.connection = connection;
		groupManager = GroupManager.getGroupManager();
	}

	public void run()
	{
		try
		{
			outStream = new BufferedOutputStream (connection.getOutputStream());
			inReader = new BufferedReader (new InputStreamReader (connection.getInputStream(), "US-ASCII"));
			writeln ("201 Laszlo Read-Only NNTP Server ready -- no posting possible");

			while (shouldRun)
			{
				if(connection.isClosed())
				{
					shouldRun = false;
				}
				else
				{
					String line = inReader.readLine();	
					if (line == null)
					{
						shouldRun = false;
					}
					else
					{
						line = line.trim();
						int firstSpace = line.indexOf (" ");
						String cmd;
						String cmdargs;
						if (firstSpace >= 0)
						{
							cmd = line.substring (0, firstSpace);
							cmdargs = line.substring (firstSpace+1).trim();
						}
						else
						{
							cmd = line;
							cmdargs = "";
						}
						if ("ARTICLE".equalsIgnoreCase (cmd))
							handleArticleCommand (cmd, cmdargs);
						else if ("BODY".equalsIgnoreCase (cmd))
							handleArticleCommand (cmd, cmdargs);
						else if ("GROUP".equalsIgnoreCase (cmd))
							handleGroupCommand (cmd, cmdargs);
						else if ("HEAD".equalsIgnoreCase (cmd))
							handleArticleCommand (cmd, cmdargs);
						else if ("HELP".equalsIgnoreCase (cmd))
							handleHelpCommand (cmd, cmdargs);
						else if ("IHAVE".equalsIgnoreCase (cmd))
							handleIHaveCommand (cmd, cmdargs);
						else if ("LAST".equalsIgnoreCase (cmd))
							handleLastCommand (cmd, cmdargs);
						else if ("LIST".equalsIgnoreCase (cmd))
							handleListCommand (cmd, cmdargs);
						else if ("MODE".equalsIgnoreCase (cmd))
							handleModeCommand (cmd, cmdargs);
						else if ("NEWGROUPS".equalsIgnoreCase (cmd))
							handleNewGroupsCommand (cmd, cmdargs);
						else if ("NEWNEWS".equalsIgnoreCase (cmd))
							handleNewNewsCommand (cmd, cmdargs);
						else if ("NEXT".equalsIgnoreCase (cmd))
							handleNextCommand (cmd, cmdargs);
						else if ("POST".equalsIgnoreCase (cmd))
							handlePostCommand (cmd, cmdargs);
						else if ("QUIT".equalsIgnoreCase (cmd))
							handleQuitCommand (cmd, cmdargs);
						else if ("SLAVE".equalsIgnoreCase (cmd))
							handleSlaveCommand (cmd, cmdargs);
						else if ("STAT".equalsIgnoreCase (cmd))
							handleArticleCommand (cmd, cmdargs);
						else if ("XOVER".equalsIgnoreCase (cmd))
							handleXOverCommand (cmd, cmdargs);
						else
							handleUnknownCommand (cmd, cmdargs);
					}
					
					
				}
			}
		}
		catch (SocketTimeoutException e)
		{
			System.err.println ("Info: Timeout NNTP connection with " + connection.getInetAddress());
		}
		catch (Exception e)
		{
			e.printStackTrace (System.err);
		}
		finally
		{
			try
			{
				if ((connection != null) && !connection.isClosed())
				{
					connection.close();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace (System.err);
			}
			finally
			{
				try
				{
					if (outStream != null)
					{
						outStream.close();
					}
				}
				catch (Exception e)
				{
					e.printStackTrace (System.err);
				}
				finally
				{
					try
					{
						if (inReader != null)
						{
							inReader.close();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace (System.err);
					}
					finally
					{
						try
						{
							if (articleFile != null)
							{
								articleFile.close();
							}
						}
						catch (Exception e)
						{
							e.printStackTrace (System.err);
						}
						finally
						{
							if (groupOverview != null)
							{
								groupOverview.close();
							}
						}
					}
				}
			}
		}
	}

	private void handleArticleCommand (String cmd, String args) throws IOException
	{
		if (currentGroup == null)
		{
			writeln ("412 No current newsgroup");
			return;
		}

		NewsProtocolInfo info = currentGroup.getNewsProtocolInfo();
		int infoNum;
		long msgNum = -1;
		String msgId;
		if (args.startsWith ("<") && args.endsWith (">"))
		{
			msgId = args;
			if (groupOverview != null)
			{
				msgNum = groupOverview.getMessageNumberForID (msgId);
			}

			if (msgNum < 0)
			{
				writeln ("430 No such article");
				return;
			}

			infoNum = (int) (msgNum - currentGroup.getFirstMessageNumber());
		}
		else if ("".equals (args))
		{
			msgNum = currentArticle;
			infoNum = (int) (msgNum - currentGroup.getFirstMessageNumber());

			if ((infoNum < 0) || (infoNum >= info.getMsgAmount()))
			{
				writeln ("420 No current article");
				return;
			}

			if (groupOverview != null)
				msgId = groupOverview.getMessageID (msgNum);
			else
				msgId = "";
		}
		else
		{
			msgNum = Long.parseLong (args);
			infoNum = (int) (msgNum - currentGroup.getFirstMessageNumber());

			if ((infoNum < 0) || (infoNum >= info.getMsgAmount()))
			{
				writeln ("423 No such article number");
				return;
			}

			if (groupOverview != null)
				msgId = groupOverview.getMessageID (msgNum);
			else
				msgId = "";
			currentArticle = msgNum;
		}

		try
		{
			articleFile.seek (info.getMsgPositions ()[infoNum]);
		}
		catch (IOException e)
		{
			writeln ("430 No such article");
		}

		if ("STAT".equals (cmd))
		{
			writeln ("223 " + msgNum + " " + msgId + " Article found -- request text separately");
		}
		else if ("HEAD".equals (cmd))
		{
			writeln ("221 " + msgNum + " " + msgId + " Article found -- head follows");
			writeArticle (true, false);
			writeTrailingDot();
		}
		else if ("BODY".equals (cmd))
		{
			writeln ("222 " + msgNum + " " + msgId + " Article found -- body follows");
			writeArticle (false, true);
			writeTrailingDot();
		}
		else
		{
			writeln ("220 " + msgNum + " " + msgId + " Article found -- full text follows");
			writeArticle (true, true);
			writeTrailingDot();
		}
	}

	private void handleGroupCommand (String cmd, String args) throws IOException
	{
		GroupDescription newGroup = groupManager.getGroup (args);
		if (newGroup != null)
		{
			try
			{
				RandomAccessFile oldFile = articleFile;
				Overview oldOverview = groupOverview;

				articleFile = new RandomAccessFile (newGroup.getNewsProtocolInfo().getMsgFile(), "r");
				long first = newGroup.getFirstMessageNumber();
				long last = newGroup.getLastMessageNumber();
				currentGroup = newGroup;
				groupOverview = currentGroup.getOverview();
				if (!groupOverview.open())
				{
					groupOverview = null;
				}
				currentArticle = first;
				writeln ("211 " + (last-first+1) + " " + first + " " + last + " " + newGroup.getGroupName() + " Group selected");
				if (oldFile != null)
				{
					try
					{
						oldFile.close();
					}
					catch (IOException e)
					{
						e.printStackTrace (System.err);
					}
				}
				if (oldOverview != null)
				{
					oldOverview.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace (System.err);
				writeln ("411 requested group does not exist");
			}
		}
		else
			writeln ("411 requested group does not exist");
	}

	private void handleHelpCommand (String cmd, String args) throws IOException
	{
		writeln ("100 help text follows");
		writeln ("");
		writeln ("Laszlo Read-Only NTTP Server");
		writeln ("Only minimum NNTP command are supported");
		writeln ("Group-matching command details are not [fully] implemented yet");
		writeln ("Server is also not suitable for distribution");
		writeln ("");
		writeTrailingDot();
	}

	private void handleIHaveCommand (String cmd, String args) throws IOException
	{
		writeln ("435 no articles can be sent at all");
	}

	private void handleLastCommand (String cmd, String args) throws IOException
	{
		if (currentGroup != null)
		{
			if (currentArticle >= 0)
			{
				NewsProtocolInfo info = currentGroup.getNewsProtocolInfo();
				int noInInfo = (int) (currentArticle - currentGroup.getFirstMessageNumber());
				if ((noInInfo > 0) && (currentArticle < info.getMsgAmount()))
				{
					currentArticle--;
					noInInfo--;
					String msgId = (groupOverview != null) ? groupOverview.getMessageID (currentArticle) : "";
					writeln ("223 " + currentArticle + " " + msgId + " Article set -- request text separately");
				}
				else
					writeln ("422 Current group has no last article");
			}
			else
				writeln ("420 No current article");
		}
		else
			writeln ("412 No current newsgroup");
	}

	private void handleListCommand (String cmd, String args) throws IOException
	{
		if ("".equals (args) || "ACTIVE".equalsIgnoreCase (args))
		{
			synchronized (groupManager)
			{
				writeln ("215 Newsgroup list follows");
				Iterator iter = groupManager.iterator();
				while (iter.hasNext())
				{
					GroupDescription desc = (GroupDescription) iter.next();
					long first = desc.getFirstMessageNumber();
					long last = desc.getLastMessageNumber();
					writeln (desc.getGroupName() + " " + last + " " + first + " n");
				}
				writeTrailingDot();
			}
		}
		else
		{
			writeln ("503 Known command but unknown arguments");
		}
	}
	private void handleModeCommand (String cmd, String args) throws IOException
	{
		if ("READER".equalsIgnoreCase (args))
		{
			writeln ("201 Laszlo Read-Only NNTP Server ready -- no posting possible");
		}
		else
		{
			writeln ("503 Known command but unknown arguments");
		}
	}

	private void handleNextCommand (String cmd, String args) throws IOException
	{
		if (currentGroup != null)
		{
			if (currentArticle >= 0)
			{
				NewsProtocolInfo info = currentGroup.getNewsProtocolInfo();
				int noInInfo = (int) (currentArticle - currentGroup.getFirstMessageNumber());
				if ((noInInfo >= 0) && (currentArticle < info.getMsgAmount()-1))
				{
					currentArticle++;
					noInInfo++;
					String msgId = (groupOverview != null) ? groupOverview.getMessageID (currentArticle) : "";
					writeln ("223 " + currentArticle + " " + msgId + " Article set -- request text separately");
				}
				else
					writeln ("421 Current group has no next article");
			}
			else
				writeln ("420 No current article");
		}
		else
			writeln ("412 No current newsgroup");
	}

	private void handleNewGroupsCommand (String cmd, String args) throws IOException
	{
		long decideTime = parseNNTPTime (args);
		synchronized (groupManager)
		{
			writeln ("231 List of newsgroups");
			Iterator iter = groupManager.iterator();
			while (iter.hasNext())
			{
				GroupDescription desc = (GroupDescription) iter.next();
				if (desc.getCreatedTime() > decideTime)
				{
					long first = desc.getFirstMessageNumber();
					long last = desc.getLastMessageNumber();
					writeln (desc.getGroupName() + " " + last + " " + first + " n");
				}
			}
			writeTrailingDot();
		}
	}

	private void handleNewNewsCommand (String cmd, String args) throws IOException
	{
		writeln ("230 List of message ids");
		writeTrailingDot();
	}

	private void handlePostCommand (String cmd, String args) throws IOException
	{
		writeln ("440 Posting not possible with this server");
	}

	private void handleQuitCommand (String cmd, String args) throws IOException
	{
		shouldRun = false;
		writeln ("205 Connection close, good bye");
	}

	private void handleSlaveCommand (String cmd, String args) throws IOException
	{
		writeln ("202 Slave status noted, but no special treatment");
	}

	private void handleXOverCommand (String cmd, String args) throws IOException
	{
		if (currentGroup == null)
		{
			writeln ("412 No current newsgroup");
			return;
		}

		long from;
		long to;
		if ("".equals (args))
		{
			if ((currentArticle >= currentGroup.getFirstMessageNumber()) && (currentArticle <= currentGroup.getLastMessageNumber()))
			{
				from = currentArticle;
				to = currentArticle;
			}
			else
			{
				writeln ("420 No current article");
				return;
			}
		}
		else
		{
			try
			{
				int minusIndex = args.indexOf ('-');
				if (minusIndex >= 0)
				{
					from = Long.parseLong (args.substring (0, minusIndex));
					if (minusIndex+1 == args.length())
					{
						to = currentGroup.getLastMessageNumber();
					}
					else
					{
						to = Long.parseLong (args.substring (minusIndex+1));
					}
				}
				else
				{
					from = Long.parseLong (args);
					to = from;
				}
			}
			catch (NumberFormatException e)
			{
				writeln ("420 Bad article number/range");
				return;
			}
		}

		writeln ("224 Overview information follows");
		if (groupOverview != null)
		{
			groupOverview.writeOverview (outStream, from, to);
		}
		writeTrailingDot();
	}

	private void handleUnknownCommand (String cmd, String args) throws IOException
	{
		writeln ("500 Command not Recognized");
	}

	private void write (String str) throws IOException
	{
		byte[] bytes = str.getBytes ("US-ASCII");
		outStream.write (bytes);
	}

	private void writeln (String str) throws IOException
	{
		if (str.startsWith ("."))
			write (".");	// Encode leading dot as double-dot
		write (str);
		outStream.write (CRLF);
		outStream.flush();
	}

	private void writeTrailingDot() throws IOException
	{
		write (".\r\n");
		outStream.flush();
	}

	private void writeArticle (boolean showHeader, boolean showBody)
	{
		try
		{
			byte[] data = new byte [4];
			int num = articleFile.read (data);
			if (num == 4)
			{
				int toGo = (((data[0] & 0xff) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff));
				byte[] buffer = new byte [32];
				byte lastByte = 0;
				boolean atStartOfLine = true;
				boolean lineHasContents = false;
				boolean isInBody = false;
				try
				{
					while (toGo > 0)
					{
						num = articleFile.read (buffer, 0, (toGo > buffer.length ? buffer.length : toGo));
						toGo -= num;
						boolean writing = (showHeader && !isInBody) || (showBody && isInBody);
						int startIndex = 0;
						for (int i=0; i<num; i++)
						{
							byte current = buffer [i];
							switch (current)
							{
								case (byte) '\n':
								{
									if (lastByte != (byte) '\r')
									{
										if (writing)
										{
											outStream.write (buffer, startIndex, i-startIndex);
											outStream.write (CRLF);
										}
										startIndex = i+1;
									}
									else if (!lineHasContents)
									{
										if (writing)
										{
											outStream.write (buffer, startIndex, i-startIndex+1);
										}
										startIndex = i+1;
									}
									if (!lineHasContents)
									{
										isInBody = true;
										if (!showBody)
										{
											return;
										}
									}
									atStartOfLine = true;
									lineHasContents = false;
									break;
								}
								case (byte) '\r':
								{
									atStartOfLine = false;
									break;
								}
								case (byte) '.':
								{
									if (atStartOfLine)
									{
										if (writing)
										{
											outStream.write (buffer, startIndex, i-startIndex+1);
											outStream.write ((byte) '.');
										}
										startIndex = i+1;
									}
									atStartOfLine = false;
									lineHasContents = true;
									break;
								}
								default:
								{
									atStartOfLine = false;
									lineHasContents = true;
								}
							}
							lastByte = current;
							writing = (showHeader && !isInBody) || (showBody && isInBody);
						}
						if (writing)
						{
							outStream.write (buffer, startIndex, num-startIndex);
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace (System.err);
				}
				finally
				{
					if (lineHasContents)
						outStream.write (CRLF);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace (System.err);
		}
	}

	private static long parseNNTPTime (String args)
	{
		try
		{
			GregorianCalendar calendar;
			if (args.indexOf ("GMT") == 14)
				calendar = new GregorianCalendar (TimeZone.getTimeZone ("GMT"));
			else
				calendar = new GregorianCalendar();

			int currentYear = calendar.get (calendar.YEAR);
			int currentSimpleYear = currentYear % 100;
			int currentCentury = currentYear - currentSimpleYear;
			int simpleYear = Integer.parseInt (args.substring (0, 2));
			int year;
			if (simpleYear > currentSimpleYear + 50)
				year = currentCentury -100 + simpleYear;
			else if (simpleYear < currentSimpleYear - 50)
				year = currentCentury +100 + simpleYear;
			else
				year = currentCentury + simpleYear;

			calendar.set (calendar.YEAR, year);
			calendar.set (calendar.MONTH, Integer.parseInt (args.substring (2, 4)) -1);	// January is 0
			calendar.set (calendar.DAY_OF_MONTH, Integer.parseInt (args.substring (4, 6)));
			calendar.set (calendar.HOUR_OF_DAY, Integer.parseInt (args.substring (7, 9)));
			calendar.set (calendar.MINUTE, Integer.parseInt (args.substring (9, 11)));
			calendar.set (calendar.SECOND, Integer.parseInt (args.substring (11, 13)));
			return calendar.getTimeInMillis();
		}
		catch (Exception e)
		{
			return -1;
		}
	}
}
