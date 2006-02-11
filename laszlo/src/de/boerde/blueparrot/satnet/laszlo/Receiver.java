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
 * Victor.java
 *
 * Created on 1. Mai 2004, 19:35
 */

package de.boerde.blueparrot.satnet.laszlo;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  roland
 */
public class Receiver extends Thread implements ContentReader.TransmissionListener
{
	private AnnouncementReader reader;
	private ReceptionFilter filter;
	private Set currentContentReaders;
	private boolean running;

	/**
	* @param args the command line arguments
	*/
	public static void main(String[] args) throws IOException
	{
		Receiver recv = new Receiver();
		recv.start();
	}

	public Receiver() throws IOException
	{
		reader = new AnnouncementReader();
		filter = new ReceptionFilter();
		currentContentReaders = new HashSet();
		Runtime runtime = Runtime.getRuntime();
		running = true;
		runtime.addShutdownHook (new SaveHook());
	}

	public void run()
	{
		MulticastSocketPool multicastSocketPool = MulticastSocketPool.getMulticastSocketPool();
		while (running)
		{
			try
			{
				Announcement a = reader.getTransmission();
				String multicast = a.getDetail ("multicast");
				String timeout = a.getDetail ("timeout");
				ReceptionFilter.Response shouldReceive = filter.shouldReceive (a);
				if (shouldReceive.getResult())
				{
					System.out.println ("Starting: " + a.getFullName() +  " (" + a.getDetail ("tsize") + " bytes)");
					String name = a.getPlainName();
					try
					{
						ReceiverContentReaderThread t = new ReceiverContentReaderThread (a);
						ReceiverContentReader contentReader = t.getContentReader();
						currentContentReaders.add (contentReader);
						contentReader.addTransmissionListener (this);
						notifyNewTransmission (a, contentReader);
						t.start();
					}
					catch (IOException e)
					{
						if (running)
						{
							System.err.println ("Error in : " + a);
							e.printStackTrace (System.err);
						}
					}
					catch (ProtocolException e)
					{
						e.printStackTrace (System.err);
					}
				}
				else
				{
					System.out.println ("Skipping: " + a.getFullName() +  " (" + a.getDetail ("tsize") + " bytes) " + shouldReceive.getReason());
					multicastSocketPool.checkSocket (a);
				}
				multicastSocketPool.checkTimeouts();
			}
			catch (IOException e)
			{
				if (running)
					e.printStackTrace (System.err);
			}
		}
		try
		{
			if (!reader.isClosed())
				reader.close();
		}
		catch (IOException e)
		{
		}
	}

	private Vector newTransmissionListeners = new Vector();

	public void addNewTransmissionListener (NewTransmissionListener listener)
	{
		newTransmissionListeners.add (listener);
	}

	public void removeNewTransmissionListener (NewTransmissionListener listener)
	{
		newTransmissionListeners.remove (listener);
	}

	private synchronized void notifyNewTransmission (Announcement announcement, ContentReader reader)
	{
		NewTransmissionEvent evt = new NewTransmissionEvent (announcement, reader);
		Iterator listeners = ((Vector) newTransmissionListeners.clone()).iterator();
		while (listeners.hasNext())
		{
			NewTransmissionListener listener = (NewTransmissionListener) listeners.next();
			listener.newTransmission (evt);
		}
	}

	public void removeContentReader (ContentReader reader)
	{
		synchronized (currentContentReaders)
		{
			currentContentReaders.remove (reader);
		}
	}

	public void transmissionCompleted (ContentReader.TransmissionEvent evt)
	{
		removeContentReader (evt.getContentReader());
	}

	public void transmissionIncomplete (ContentReader.TransmissionEvent evt)
	{
		removeContentReader (evt.getContentReader());
	}

	public void transmissionExpired (de.boerde.blueparrot.satnet.laszlo.ContentReader.TransmissionEvent evt)
	{
		removeContentReader (evt.getContentReader());
	}

	public static class NewTransmissionEvent
	{
		private Announcement announcement;
		private ContentReader reader;
		NewTransmissionEvent (Announcement announcement, ContentReader reader)
		{
			this.announcement = announcement;
			this.reader = reader;
		}

		public Announcement getAnnouncement()
		{
			return announcement;
		}

		public ContentReader getContentReader()
		{
			return reader;
		}
	}

	public static interface NewTransmissionListener
	{
		abstract public void newTransmission (NewTransmissionEvent evt);
	}

	private class SaveHook extends Thread
	{
		protected SaveHook()
		{
			super ("Receiver SaveHook");
		}

		public void run()
		{
			running = false;
			try
			{
				if (!reader.isClosed())
					reader.close();
			}
			catch (IOException e)
			{
			}
/*
// Now done my the MultiCastSocketPool...
			synchronized (currentContentReaders)
			{
				Iterator iter = currentContentReaders.iterator();
				while (iter.hasNext())
				{
					ReceiverContentReader contentReader = (ReceiverContentReader) iter.next();
					synchronized (contentReader)
					{
						if (!contentReader.isDone())
							contentReader.interrupt();
					}
				}
			}
*/
			int shutdownTries = 5;
			int shutdownWait = 500;
			while (!currentContentReaders.isEmpty() && (shutdownTries > 0))
			{
				try
				{
					Thread.sleep (shutdownWait);
				}
				catch (InterruptedException e)
				{
				}
				shutdownTries--;
			}
			System.out.println ("hook: finish");
		}
	}
}
