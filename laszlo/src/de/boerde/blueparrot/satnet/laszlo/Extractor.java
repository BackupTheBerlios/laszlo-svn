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
 * Extractor.java
 *
 * Created on 15. Mai 2004, 17:53
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.util.*;

/**
 *
 * @author  roland
 */
public class Extractor
{
	/** Creates a new instance of Extractor */
	private Extractor()
	{
	}

	public boolean extract (String localFile, File destDir) throws java.io.IOException
	{
		Runtime runtime = Runtime.getRuntime();
		Settings settings = Settings.getSettings();
		StringBuffer command = new StringBuffer (settings.getUnpackCommandline (localFile));
		for (int pos = command.indexOf ("%%PACKEDFILE%%"); pos >= 0; pos=command.indexOf ("%%PACKEDFILE%%", pos))
		{
			command.delete (pos, pos+"%%PACKEDFILE%%".length());
			command.insert (pos, localFile);
		}
		for (int pos = command.indexOf ("%%DESTDIR%%"); pos >= 0; pos=command.indexOf ("%%DESTDIR%%", pos))
		{
			command.delete (pos, pos+"%%DESTDIR%%".length());
			command.insert (pos, destDir.getAbsolutePath());
		}

		Vector commandParts = new Vector();
		boolean isInQuoteMarks = false;
		int cutPos=0;
		int pos;
		for (pos=0; pos<command.length(); pos++)
		{
			char posChar = command.charAt (pos);
			switch (posChar)
			{
				case '"':
				{
					command.delete (pos, pos+1);
					pos--;
					isInQuoteMarks = !isInQuoteMarks;
					break;
				}
				case '*':
				{	// escape character
					command.delete (pos, pos+1);
					break;
				}
				case ' ':
				{
					if (!isInQuoteMarks)
					{
						commandParts.add (command.substring (cutPos, pos));
						cutPos = pos+1;
					}
					break;
				}
			}
		}
		if (pos > cutPos)
			commandParts.add (command.substring (cutPos, command.length()));

		String[] commandArr = new String [commandParts.size()];
		commandParts.copyInto (commandArr);

/*
System.out.print ("###");
for (int i=0; i<commandArr.length; i++)
{
	System.out.print (commandArr [i]);
	System.out.print ("!");
}
System.out.println();
*/

		boolean success = false;
		Process process = runtime.exec (commandArr);
		try
		{
/*			{
				String lastLine = "";
				BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream()));
				while (true)
				{
					String line = reader.readLine();
					if (line == null)
						break;

					lastLine = line;
					if (line.equals ("All OK"))
					{
						success = true;
					}
				}
				System.out.println (localFile + " ###LastLine--> " + lastLine);
			}
*/
			process.waitFor();
			File sourceFile = new File (localFile);
			sourceFile.delete();
			//System.out.println (localFile + " ###exitValue " + process.exitValue());
			success = (process.exitValue() == 0);
			if (!success)
			{
				GUIMain.logger.warning("Error while unpacking " + localFile);
				// Remove destination dir in case of error
				Stack filesToDelete = new Stack();
				Stack dirsToProcess = new Stack();
				dirsToProcess.push (destDir);
				while (!dirsToProcess.isEmpty())
				{
					File dir = (File) dirsToProcess.pop();
					if ((dir != null) && dir.isDirectory())
					{
						File[] files = dir.listFiles();
						if (files != null)
						{
							for (int f=0; f<files.length; f++)
							{
								File file = files [f];
								if (file != null)
								{
									if (file.isDirectory())
									{
										dirsToProcess.push (file);
									}
									filesToDelete.push (file);
								}
							}
						}
					}
				}
				while (!filesToDelete.isEmpty())
				{
					File file = (File) filesToDelete.pop();
					file.delete();
				}
			}
		}
		catch (InterruptedException e)
		{
			GUIMain.logger.severe(e.getMessage());
		}
		return success;
	}

	private static Extractor theExtractor = new Extractor();

	public static Extractor getExtractor()
	{
		return theExtractor;
	}
}
