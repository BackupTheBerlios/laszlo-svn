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
 * TransferStatusPane.java
 *
 * Created on 3. Mai 2004, 22:45
 */

package de.boerde.blueparrot.satnet.laszlo.ui;

import javax.swing.*;
import javax.swing.table.*;

import de.boerde.blueparrot.satnet.laszlo.*;

/**
 *
 * @author  roland
 */
public class TransferStatusPane extends JTable
{
	/** Creates a new instance of TransferStatusPane */
	public TransferStatusPane (Receiver recv)
	{
		setModel (new TransferStatusModel (recv, this));
		TableColumnModel columnModel = getColumnModel();
		columnModel.getColumn (TransferStatusModel.LABEL).setCellRenderer (new TransferStatusCellLabel());
		columnModel.getColumn (TransferStatusModel.PROGRESS).setCellRenderer (new TransferStatusCellBitSet());
		setTableHeader (null);
		setShowHorizontalLines (true);
		setShowVerticalLines (false);
	}
}
