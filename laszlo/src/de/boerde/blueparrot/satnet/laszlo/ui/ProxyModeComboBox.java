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

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import javax.swing.*;

/**
 *
 * @author  roland
 */
public class ProxyModeComboBox extends JComboBox
{
	private static String[] modeLabels = { "Not at all", "Directly", "Via Upstream Proxy" };
	private static String[] modeSettings = { "none", "direct", "upstreamProxy" };

	/** Creates a new instance of NetworkInterfaceBlah */
	public ProxyModeComboBox()
	{
		super (modeLabels);
	}

	public ProxyModeComboBox (String mode)
	{
		super (modeLabels);
		setMode (mode);
	}

	public String getMode()
	{
		int selectedIndex = getSelectedIndex();
		if (selectedIndex >= 0)
			return modeSettings [selectedIndex];
		else
			return modeSettings [0];
	}

	public void setMode (String mode)
	{
		switch (mode.trim().charAt (0))
		{
			case 'd':
			case 'D':
			{
				setSelectedIndex (1);
				break;
			}
			case 'u':
			case 'U':
			{
				setSelectedIndex (2);
				break;
			}
			default:
			{
				setSelectedIndex (0);
			}
		}
	}
}
