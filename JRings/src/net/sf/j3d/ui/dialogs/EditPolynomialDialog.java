/*
* Copyright (C) 2004-05  Mike Murray
*
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License (version 2)
*  as published by the Free Software Foundation.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*/

package net.sf.j3d.ui.dialogs;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import com.almostrealism.raytracer.engine.Surface;
import com.almostrealism.raytracer.primitives.Polynomial;
import com.almostrealism.raytracer.primitives.PolynomialTerm;
import com.almostrealism.raytracer.surfaceUI.SurfaceUI;
import net.sf.j3d.ui.event.DialogCloseEvent;
import net.sf.j3d.ui.event.Event;
import net.sf.j3d.ui.event.EventGenerator;
import net.sf.j3d.ui.event.EventHandler;
import net.sf.j3d.ui.event.EventListener;
import net.sf.j3d.ui.event.SurfaceEditEvent;
import net.sf.j3d.ui.event.SurfaceEvent;
import net.sf.j3d.ui.event.SurfaceRemoveEvent;


/**
 * An EditPolynomialDialog object can be used to allow a user to edit the terms of a SurfaceUI object
 * that wraps a Polynomial object.
 */
public class EditPolynomialDialog extends JPanel implements Dialog, EventListener, EventGenerator {
  private Polynomial poly;
  
  private boolean open;
  
  private EventHandler handler;
  
  private PolynomialTermsListModel listModel;
  
  private JFrame frame;
  private JPanel termsPanel, buttonPanel;
  private JList termsList;
  private JScrollPane termsListScrollPane;
  private JButton addTermButton, editTermButton, removeTermButton, doneButton;

	/**
	 * Constructs a new EditPolynomialDialog object that can be used to edit the specified SurfaceUI object.
	 * 
	 * @throws IllegalArgumentException  If the specified SurfaceUI object does not wrap a Polynomial object.
	 */
	public EditPolynomialDialog(SurfaceUI polynomial) {
		Surface s = polynomial.getSurface();
		
		if (s instanceof Polynomial)
			this.poly = (Polynomial)s;
		else
			throw new IllegalArgumentException("SurfaceUI object does not wrap a Polynomial object.");
		
		this.termsPanel = new JPanel();
		this.termsPanel.setBorder(new TitledBorder(LineBorder.createGrayLineBorder(), "Terms:"));
		
		this.buttonPanel = new JPanel(new java.awt.FlowLayout());
		
		this.listModel = new PolynomialTermsListModel(this.poly);
		this.termsList = new JList(this.listModel);
		this.termsListScrollPane = new JScrollPane(this.termsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		this.addTermButton = new JButton("Add Term");
		this.editTermButton = new JButton("Edit Term");
		this.removeTermButton = new JButton("Remove Term");
		this.doneButton = new JButton("Done");
		
		this.termsPanel.add(this.termsListScrollPane);
		
		this.buttonPanel.add(this.addTermButton);
		this.buttonPanel.add(this.editTermButton);
		this.buttonPanel.add(this.removeTermButton);
		this.buttonPanel.add(this.doneButton);
		
		this.add(this.termsPanel, java.awt.BorderLayout.CENTER);
		this.add(this.buttonPanel, java.awt.BorderLayout.SOUTH);
		
		this.frame = new JFrame("Edit Polynomial Function");
		this.frame.setSize(500, 250);
		
		this.frame.getContentPane().add(this);
		
		this.frame.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClsing(java.awt.event.WindowEvent event) {
				closeDialog();
			}
		});
		
		this.addTermButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				PolynomialTerm newTerm = new PolynomialTerm(0.0, 0, 0, 0);
				EditPolynomialDialog.this.poly.addTerm(newTerm);
				
				EditPolynomialTermDialog editDialog = new EditPolynomialTermDialog(EditPolynomialDialog.this.poly, EditPolynomialDialog.this.poly.getTerms().length - 1);
				editDialog.setEventHandler(handler);
				
				editDialog.showDialog();
			}
		});
		
		this.editTermButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				if (termsList.isSelectionEmpty() == true)
					return;
				
				int index = termsList.getSelectedIndex();
				
				EditPolynomialTermDialog editDialog = new EditPolynomialTermDialog(EditPolynomialDialog.this.poly, index);
				editDialog.setEventHandler(handler);
				
				editDialog.showDialog();
			}
		});
		
		this.removeTermButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				if (termsList.isSelectionEmpty() == true)
					return;
				
				int index = termsList.getSelectedIndex();
				
				EditPolynomialDialog.this.poly.removeTerm(index);
				
				if (handler != null) {
					handler.fireEvent(new SurfaceEditEvent(SurfaceEditEvent.dataChangeEvent, EditPolynomialDialog.this.poly));
				}
			}
		});
		
		this.doneButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				closeDialog();
			}
		});
	}
	
	/**
	 * Shows this dialog in a JFrame.
	 */
	public void showDialog() {
		if (this.open == true)
			return;
		
		this.updateTermsList();
		
		this.frame.setVisible(true);
		this.open = true;
	}
	
	/**
	 * Closes (and disposes) this dialog if it is open.
	 */
	public void closeDialog() {
		if (this.open == true) {
			this.frame.setVisible(false);
			this.open = false;
			
			this.frame.dispose();
			
			if (this.handler != null)
				this.handler.fireEvent(new DialogCloseEvent(this));
		}
	}
	
	/**
	 * Updates the list of terms displayed by this dialog so that its contents matches the Polynomial object being edited.
	 */
	public void updateTermsList() {
		this.termsList.repaint();
	}
	
	/**
	 * Method called when an event has been fired.
	 */
	public void eventFired(Event event) {
		if (this.open == false)
			return;
		
		if (event instanceof SurfaceEvent && ((SurfaceEvent)event).getTarget() != this.poly)
			return;
		
		if (event instanceof SurfaceEditEvent) {
			SurfaceEditEvent editEvent = (SurfaceEditEvent)event;
			
			if (editEvent.isDataChangeEvent() == true) {
				this.updateTermsList();
			}
		}
		
		if (event instanceof SurfaceRemoveEvent) {
			this.closeDialog();
		}
	}
	
	/**
	 * Sets the EventHandler object used by this EditPolynomialDialog object to the specified EventHandler object.
	 * Setting this to null will deactivate event reporting.
	 */
	public void setEventHandler(EventHandler handler) {
		this.handler = handler;
		
		if (this.handler != null) {
			this.handler.addListener(this.listModel);
		}
	}
	
	/**
	 * Returns the EventHandler object used by this EditPolynomialDialog object.
	 */
	public EventHandler getEventHandler() {
		return this.handler;
	}
}
