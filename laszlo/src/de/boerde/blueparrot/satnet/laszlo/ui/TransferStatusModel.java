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
 * TransferStatusModel.java
 *
 * Created on 3. Mai 2004, 23:29
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class TransferStatusModel extends AbstractTableModel implements Receiver.NewTransmissionListener, ContentReader.TransmissionListener, Runnable, ComponentListener
{
	final public static int LABEL = 0;
	final public static int PROGRESS = 1;

	private Vector transferData;
	//private Receiver receiver;

	/** Creates a new instance of TransferStatusModel */
	public TransferStatusModel (Receiver receiver, JTable table)
	{
		//this.receiver = receiver;
		Vector oldTransfers = ContentReader.getAllContentReaders();
		transferData = new Vector (oldTransfers.size());
		receiver.addNewTransmissionListener (this);
		for (int i=oldTransfers.size()-1; i>=0; i--)
		{
			ContentReader reader = (ContentReader) oldTransfers.get (i);
			transferData.add (reader);
			reader.addTransmissionListener (this);
		}
		table.addComponentListener (this);
		thread = new Thread (this);
		thread.start();
	}

	public int getColumnCount ()
	{
		return 2;
	}

	public synchronized int getRowCount ()
	{
		int size = transferData.size();
		return size;
	}

	public synchronized Object getValueAt (int rowIndex, int columnIndex)
	{
		if (rowIndex < transferData.size())
			return transferData.get (rowIndex);
		else
			return null;
	}

	public synchronized void newTransmission (Receiver.NewTransmissionEvent evt)
	{
		String fullName = evt.getAnnouncement().getFullName();
		for (int i=transferData.size()-1; i>=0; i--)
		{
			ContentReader oldRecord = (ContentReader) transferData.get (i);
			if (oldRecord.getAnnouncement().getFullName().equals (fullName))
			{
				transferData.remove (i);
				fireTableRowsDeleted (i, i);
				break;
			}
		}
		ContentReader reader = evt.getContentReader();
		reader.addTransmissionListener (this);
		transferData.add (0, reader);
		fireTableRowsInserted (0, 0);
	}

	public void transmissionCompleted (ContentReader.TransmissionEvent evt)
	{
		ContentReader reader = evt.getContentReader();
		synchronized (this)
		{
			int index = transferData.indexOf (reader);
			if (index >= 0)
				fireTableRowsUpdated (index, index);
		}
		try
		{
			Thread.sleep (900);
		}
		catch (InterruptedException e)
		{
		}
		synchronized (this)
		{
			int index = transferData.indexOf (reader);
			if (index >= 0)
			{
				transferData.remove (index);
				fireTableRowsDeleted (index, index);
			}
			reader.removeTransmissionListener (this);
		}
	}

	public synchronized void transmissionIncomplete (ContentReader.TransmissionEvent evt)
	{
		ContentReader reader = evt.getContentReader();
		int index = transferData.indexOf (reader);
		if (index >= 0)
		{
			ContentReader theReader = (ContentReader) transferData.get (index);
			transferData.remove (index);
			Announcement announcement = theReader.getAnnouncement();
			if (announcement.getDetail ("fileid") != null)
			{
				int lastIndex;
				for (lastIndex=index; lastIndex<transferData.size(); lastIndex++)
				{
					ContentReader curReader = (ContentReader) transferData.get (lastIndex);
					if (curReader.isDone())
						break;
				}
				transferData.add (lastIndex, theReader);
				fireTableRowsUpdated (index, lastIndex);
			}
			else
			{
				fireTableRowsDeleted (index, index);
			}
		}
	}

	public synchronized void transmissionExpired (ContentReader.TransmissionEvent evt)
	{
		ContentReader reader = evt.getContentReader();
		int index = transferData.indexOf (reader);
		if (index >= 0)
		{
			transferData.remove (index);
			fireTableRowsDeleted (index, index);
		}
		reader.removeTransmissionListener (this);
	}

	private Thread thread;

	public void run()
	{
		int updateFrequency = 500;
		while (true)
		{
			try
			{
				Thread.sleep (updateFrequency);
			}
			catch (InterruptedException e)
			{
				if (thread == null)
					break;
			}
			fireTableDataChanged();
		}
	}

	public synchronized void componentShown (ComponentEvent e)
	{
		if (thread == null)
		{
			thread = new Thread (this);
			thread.start();
		}
	}

	public synchronized void componentHidden (ComponentEvent e)
	{
		if (thread != null)
		{
			Thread oldThread = thread;
			thread = null;
			oldThread.interrupt();
		}
	}

	public void componentMoved (ComponentEvent e)
	{
	}

	public void componentResized (ComponentEvent e)
	{
	}
}
