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
 * IndexContentManager.java
 *
 * Created on 12. Juni 2004, 21:03
 */

package de.boerde.blueparrot.satnet.laszlo.html;

import java.io.*;
import java.util.*;

import de.boerde.blueparrot.satnet.laszlo.*;
import de.boerde.blueparrot.satnet.laszlo.protocol.http.*;

/**
 *
 * @author  roland
 */
public class IndexContentManager implements Producer
{
	private Hashtable urls;

	/** Creates a new instance of IndexContentManager */
	public IndexContentManager()
	{
		urls = new Hashtable();
		setPath ("", new MainIndexGenerator());
		setPath ("files", new ResourceFetchingProducer ("/files", "de/westfalen/fuldix/roland/satnet/laszlo/html/files/"));
		ReceivedAdsGenerator receivedAdsGenerator = new ReceivedAdsGenerator();
		setPath ("advertising", new AdvertisingGenerator (receivedAdsGenerator));
		setPath ("ads_received", receivedAdsGenerator);
	}

	public void doGet (RequestInfo request, ResponseInfo response) throws IOException
	{
		Settings settings = Settings.getSettings();
		String ownName = "http://" + settings.getHttpOwnPseudoName();
		String uri = request.getUri();
		if (uri.startsWith (ownName + "/"))
		{
			uri = uri.substring (ownName.length());
		}
		else
		{
			if (!uri.startsWith ("/"))
			{
				return;
			}
		}

		int secondSlash = uri.indexOf ("/", 1);
		String name;
		//String remain;
		if (secondSlash > 1)
		{
			name = uri.substring (1,  secondSlash);
			//remain = uri.substring (secondSlash+1);
		}
		else
		{
			name = uri.substring (1);
			//remain = "";
		}
		Object item = urls.get (name);
//System.out.println (uri + "=>" + name + "=>" + item);
		if (item instanceof Producer)
			((Producer) item).doGet (request, response);
	}

	public void setPath (String name, Producer producer)
	{
		urls.put (name, producer);
	}

	public void removePath (String name)
	{
		urls.remove (name);
	}

	private static IndexContentManager theIndexContentManager = new IndexContentManager();

	public static IndexContentManager getIndexContentManager()
	{
		return theIndexContentManager;
	}
}
