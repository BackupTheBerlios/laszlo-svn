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
 * NetworkInterfaceBlah.java
 *
 * Created on 6. Mai 2004, 19:02
 */

package de.boerde.blueparrot.ui;

import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  roland
 */
public class NetworkInterfaceComboBox extends JComboBox
{
	private NetworkInterface[] interfaces;
	private int selectedInterface;
	private String textFormat = "%d (%i)";
	private String tooltipFormat = "";

	/** Creates a new instance of NetworkInterfaceBlah */
	public NetworkInterfaceComboBox()
	{
		Vector temp = new Vector();
		try
		{
			Enumeration enum = NetworkInterface.getNetworkInterfaces();
			while (enum.hasMoreElements())
			{
				temp.add (enum.nextElement());
			}
			interfaces = new NetworkInterface [temp.size()];
		}
		catch (SocketException e)
		{
			interfaces = new NetworkInterface [1];
		}
		temp.copyInto (interfaces);
		setModel (new DefaultComboBoxModel (interfaces));
		setRenderer (new Renderer());
	}

	public NetworkInterface getNetworkInterface()
	{
		int selectedIndex = getSelectedIndex();
		if (selectedIndex >= 0)
			return interfaces [selectedIndex];
		else
			return null;
	}

	public void setNetworkInterface (NetworkInterface iface)
	{
		if (iface == null)
		{
			this.setSelectedIndex (-1);
			return;
		}

		for (int i=0; i<interfaces.length; i++)
		{
			if (iface.equals (interfaces [i]))
			{
				setSelectedIndex (i);
				break;
			}
		}
	}

	private class Renderer extends JLabel implements ListCellRenderer
	{
		public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			if (value != null && value instanceof NetworkInterface)
			{
				NetworkInterface iface = (NetworkInterface) value;
				setText (formatText (textFormat, iface));
				setToolTipText (formatText (tooltipFormat, iface));
			}
			else
			{
				setText ("<invalid>");
				setToolTipText (null);
			}
			setBackground (isSelected ? list.getSelectionBackground() : list.getBackground());
			setForeground (isSelected ? list.getSelectionForeground() : list.getForeground());
			return this;
		}

		private String formatText (String textFormat, NetworkInterface iface)
		{
			StringBuffer buffer = new StringBuffer (textFormat);
			int index = 0;
			while (true)
			{
				index = buffer.indexOf ("%", index);
				if (index == -1)
					break;

				char formatLetter = buffer.charAt (index+1);
				buffer.delete (index, index+2);
				switch (formatLetter)
				{
					case 'n':
					case 'N':
					{
						String value = iface.getName();
						buffer.insert (index, value);
						index += value.length();
						break;
					}
					case 'd':
					case 'D':
					{
						String value = iface.getDisplayName();
						buffer.insert (index, value);
						index += value.length();
						break;
					}
					case 'i':
					case 'I':
					{
						Enumeration addresses = iface.getInetAddresses();
						if (addresses.hasMoreElements())
						{
							while (addresses.hasMoreElements())
							{
								Object o = addresses.nextElement();
								if (o instanceof InetAddress)
								{
									InetAddress inet = (InetAddress) o;
									String value = inet.getHostAddress();
									buffer.insert (index, value);
									index += value.length();
									if (addresses.hasMoreElements())
									{
										buffer.insert (index, "/");
										index++;
									}
								}
							}
						}
						else
						{
							String value = "<no ip>";
							buffer.insert (index, value);
							index += value.length();
						}
						break;
					}
					case '%':
					{
						buffer.insert (index, '%');
						index ++;
						break;
					}
					default:
					{
						String value = "<invalid format>";
						buffer.insert (index, value);
						index += value.length();
						break;
					}
						
				}
			}
			return buffer.toString();
		}
	}
}
