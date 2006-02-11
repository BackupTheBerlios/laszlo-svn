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
 * RequestInfo.java
 *
 * Created on 12. Juni 2004, 22:18
 */

package de.boerde.blueparrot.satnet.laszlo.protocol.http;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author  roland
 */
public interface RequestInfo
{
	public String getMethod();
	public String getUri();
	public String getProtocol();
	public String getHeader (String name);
	public int getIntHeader (String name);
	public long getDateHeader (String name);
	public InetAddress getInetAddress();
	public Iterator getAllHeaderLines();
	public InputStream getInputStream();
}
