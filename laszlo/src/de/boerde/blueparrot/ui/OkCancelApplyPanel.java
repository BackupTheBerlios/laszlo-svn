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
 * OkCancelApplyPanel.java
 *
 * Created on 9. Mai 2004, 00:07
 */

package de.boerde.blueparrot.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  roland
 */
public class OkCancelApplyPanel extends JPanel
{
	private Action okAction;
	private Action cancelAction;
	private Action applyAction;
	private Vector performingListeners = new Vector();

	/** Creates a new instance of OkCancelApplyPanel */
	public OkCancelApplyPanel()
	{
		super (new BorderLayout());
		JPanel innerPanel = new JPanel (new GridLayout (1, 3, 4, 4));
		okAction = new OkAction();
		JButton okButton = new JButton (okAction);
		innerPanel.add (okButton);
		cancelAction = new CancelAction();
		JButton cancelButton = new JButton (cancelAction);
		innerPanel.add (cancelButton);
		applyAction = new ApplyAction();
		JButton applyButton = new JButton (applyAction);
		innerPanel.add (applyButton);
		add (innerPanel, BorderLayout.EAST);
	}

	public void disableForOriginal()
	{
		applyAction.setEnabled (false);
	}

	public void enableForApplyable()
	{
		applyAction.setEnabled (true);
	}

	private class OkAction extends AbstractAction
	{
		OkAction()
		{
			super ("OK");
		}

		public void actionPerformed (ActionEvent e)
		{
			notifyApply();
			notifyClose();
		}
	}

	private class CancelAction extends AbstractAction
	{
		CancelAction()
		{
			super ("Cancel");
		}

		public void actionPerformed (ActionEvent e)
		{
			notifyClose();
		}
	}

	private class ApplyAction extends AbstractAction
	{
		ApplyAction()
		{
			super ("Apply");
		}

		public void actionPerformed (ActionEvent e)
		{
			notifyApply();
		}
	}

	public void addPerformingListener (PerformingListener listener)
	{
		synchronized (performingListeners)
		{
			performingListeners.add (listener);
		}
	}

	public void removePerformingListener (PerformingListener listener)
	{
		synchronized (performingListeners)
		{
			performingListeners.remove (listener);
		}
	}

	private void notifyApply()
	{
		ActionEvent evt = new ActionEvent (this, 0, "apply");
		Iterator listeners;
		synchronized (performingListeners)
		{
			listeners = ((Vector) performingListeners.clone()).iterator();
		}
		while (listeners.hasNext())
		{
			PerformingListener listener = (PerformingListener) listeners.next();
			listener.performApply (evt);
		}
	}

	private void notifyClose()
	{
		ActionEvent evt = new ActionEvent (this, 0, "close");
		Iterator listeners;
		synchronized (performingListeners)
		{
			listeners = ((Vector) performingListeners.clone()).iterator();
		}
		while (listeners.hasNext())
		{
			PerformingListener listener = (PerformingListener) listeners.next();
			listener.performClose (evt);
		}
	}

	public interface PerformingListener extends EventListener
	{
		abstract public void performApply (ActionEvent e);
		abstract public void performClose (ActionEvent e);
	}
}
