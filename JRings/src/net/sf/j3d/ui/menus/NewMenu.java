/*
* Copyright (C) 2004  Mike Murray
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

package net.sf.j3d.ui.menus;


import javax.swing.*;

import com.almostrealism.raytracer.engine.*;
import net.sf.j3d.ui.dialogs.*;
import net.sf.j3d.ui.event.*;

/**
  A NewMenu object extends JMenu and provides menu items for creating new scenes and surfaces.
*/

public class NewMenu extends JMenu implements EventListener, EventGenerator {
  private Scene scene;
  
  private EventHandler handler;
  
  private JMenuItem newSceneItem, newSurfaceItem;

	/**
	  Constructs a new NewMenu object.
	*/
	
	public NewMenu(Scene scn) {
		super("New");
		
		this.scene = scn;
		
		this.newSceneItem = new JMenuItem("Scene");
		this.newSurfaceItem = new JMenuItem("Surface");
		
		this.add(this.newSceneItem);
		this.add(this.newSurfaceItem);
		
		this.newSceneItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				newScene();
			}
		});
		
		this.newSurfaceItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				NewSurfaceDialog newDialog = new NewSurfaceDialog(scene);
				newDialog.setEventHandler(handler);
				
				newDialog.showDialog();
			}
		});
	}
	
	/**
	  Creates a new Scene object and fires the required events.
	*/
	
	public void newScene() {
		Scene newScene = new Scene();
		
		if (this.handler != null) {
			this.handler.fireEvent(new SceneOpenEvent(newScene));
		}
	}
	
	/**
	  Method called when an event has been fired.
	*/
	
	public void eventFired(Event event) {
		if (event instanceof SceneOpenEvent) {
			this.scene = ((SceneOpenEvent)event).getScene();
		} else if (event instanceof SceneCloseEvent) {
			this.scene = null;
		}
	}
	
	/**
	  Sets the EventHandler object used by this NewMenu object. Setting this to null will deactivate event reporting.
	*/
	
	public void setEventHandler(EventHandler handler) {
		this.handler = handler;
	}
	
	public EventHandler getEventHandler() {
		return this.handler;
	}
}
