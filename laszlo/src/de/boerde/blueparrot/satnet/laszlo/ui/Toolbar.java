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
 * Toolbar.java
 *
 * Created on 5. Mai 2004, 21:53
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import java.awt.*;
import javax.swing.*;

import de.boerde.blueparrot.ui.*;

/**
 *
 * @author  roland
 */
public class Toolbar extends JToolBar
{
	/** Creates a new instance of Toolbar */
	public Toolbar()
	{
		add (new OptionsAction());
		add (new JavaMemoryGauge());
	}
}
