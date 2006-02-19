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
 * GroupManager.java
 *
 * Created on 29. Mai 2004, 18:14
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.news;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class GroupManager
{
	private Vector groups = new Vector();

	/** Creates a new instance of GroupManager */
	private GroupManager()
	{
		load();
	}

	private synchronized void load()
	{
		Settings settings = Settings.getSettings();
		File groupInfoFile = new File (settings.getWorkDirectory() + File.separator + "laszloNewsGroups.ser");
		if (groupInfoFile.exists())
		{
			ObjectInputStream in = null;
			try
			{
				in = new ObjectInputStream (new FileInputStream (groupInfoFile));
				groups = (Vector) in.readObject();
			}
			catch (Exception e)
			{
				GUIMain.getLogger().severe(e.getMessage());
			}
			finally
			{
				if (in != null)
				{
					try
					{
						in.close();
					}
					catch (Exception e)
					{
						GUIMain.getLogger().severe(e.getMessage());
					}
				}
			}
		}
	}

	private synchronized void save()
	{
		Settings settings = Settings.getSettings();
		File groupInfoFile = new File (settings.getWorkDirectory() + File.separator + "laszloNewsGroups.ser");
		ObjectOutputStream out = null;
		try
		{
			out = new ObjectOutputStream (new FileOutputStream (groupInfoFile));
			out.writeObject (groups);
		}
		catch (Exception e)
		{
			GUIMain.getLogger().severe(e.getMessage());
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (Exception e)
				{
					GUIMain.getLogger().severe(e.getMessage());
				}
			}
		}
	}

	public synchronized void merge (GroupDescription desc)
	{
		String name = desc.getGroupName();
		for (int i=0; i<groups.size(); i++)
		{
			GroupDescription curDesc = (GroupDescription) groups.get (i);
			int comp = name.compareToIgnoreCase (curDesc.getGroupName());
			if (comp == 0)
			{
				groups.set (i, desc);
				save();
				return;
			}
			else if (comp < 0)
			{
				groups.add (i, desc);
				save();
				return;
			}
		}
		groups.add (desc);
		save();
	}

	public synchronized GroupDescription getGroup (String name)
	{
		if (name == null)
			return null;

		int lower = 0;
		int upper = groups.size() -1;
		while (lower <= upper)
		{
			int middle = (upper+lower) / 2;
			GroupDescription desc = (GroupDescription) groups.get (middle);
			int comp = name.compareToIgnoreCase (desc.getGroupName());
			if (comp > 0)
			{
				lower = middle + 1;
			}
			else if (comp < 0)
			{
				upper = middle -1;
			}
			else
			{
				return desc;
			}
		}
		return null;
	}

	public synchronized Iterator iterator()
	{
		return groups.iterator();
	}

	private static GroupManager theGroupManager = new GroupManager();

	public static GroupManager getGroupManager()
	{
		return theGroupManager;
	}
}
