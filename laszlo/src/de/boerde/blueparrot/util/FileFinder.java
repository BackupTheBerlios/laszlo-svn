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
 * FileFinder.java
 *
 * Created on 16. Mai 2004, 20:13
 */

package de.boerde.blueparrot.util;

import java.io.*;
import java.util.*;

/**
 *
 * @author  roland
 */
public class FileFinder
{
	private File root;
	private FileFilter filter;

	/** Creates a new instance of FileFinder */
	public FileFinder (File root)
	{
		this.root = root;
	}

	public void setFileName (String name)
	{
		filter = new FileNameFilter (name);
	}

	public void setFileFilter (FileFilter filter)
	{
		this.filter = filter;
	}

	public File findOneFile()
	{
		Stack dirs = new Stack();
		dirs.push (root);
		while (!dirs.isEmpty())
		{
			File dir = (File) dirs.pop();
			File[] files = dir.listFiles();
			if (files != null)
			{
				for (int i=0; i<files.length; i++)
				{
					File file = files [i];
					if (file.isDirectory())
					{
						dirs.push (file);
					}
					if ((filter == null) || filter.accept (file))
					{
						return file;
					}
				}
			}
		}
		return null;
	}

	public File[] findAllFiles()
	{
		Vector result = new Vector();
		Stack dirs = new Stack();
		dirs.push (root);
		while (!dirs.isEmpty())
		{
			File dir = (File) dirs.pop();
			File[] files = dir.listFiles();
			if (files != null)
			{
				for (int i=0; i<files.length; i++)
				{
					File file = files [i];
					if (file.isDirectory())
					{
						dirs.push (file);
					}
					if ((filter == null) || filter.accept (file))
					{
						result.add (file);
					}
				}
			}
		}
		File[] files = new File [result.size()];
		result.copyInto (files);
		return files;
	}

	public void findAndTraverse (Traverser traverser)
	{
		Stack dirs = new Stack();
		dirs.push (root);
		while (!dirs.isEmpty())
		{
			File dir = (File) dirs.pop();
			File[] files = dir.listFiles();
			if (files != null)
			{
				for (int i=0; i<files.length; i++)
				{
					File file = files [i];
					if (file.isDirectory())
					{
						dirs.push (file);
					}
					if ((filter == null) || filter.accept (file))
					{
						traverser.traverse (file);
					}
				}
			}
		}
	}

	private class FileNameFilter implements FileFilter
	{
		private String name;

		protected FileNameFilter (String name)
		{
			this.name = name;
		}
		
		public boolean accept (File pathname)
		{
			return pathname.getName().equals (name);
		}
	}

	public interface Traverser
	{
		public void traverse (File file);
	}
}
