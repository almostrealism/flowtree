/*
* Copyright (C) 2004-06  Mike Murray
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

package net.sf.j3d.ui.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.MutableTreeNode;

import com.almostrealism.raytracer.engine.Scene;
import com.almostrealism.raytracer.surfaceUI.SurfaceUI;
import net.sf.j3d.ui.event.Event;
import net.sf.j3d.ui.event.EventGenerator;
import net.sf.j3d.ui.event.EventHandler;
import net.sf.j3d.ui.event.EventListener;
import net.sf.j3d.ui.event.SceneCloseEvent;
import net.sf.j3d.ui.event.SceneOpenEvent;
import net.sf.j3d.ui.event.SurfaceAddEvent;
import net.sf.j3d.ui.event.SurfaceEditEvent;
import net.sf.j3d.ui.event.SurfaceRemoveEvent;


/**
 * A SceneTreeNode object allows a Scene object to be represented by a TreeNode component
 * that can be added to a JTree component.
 * 
 * @author  Mike Murray
 */
public class SceneTreeNode implements TreeNode, EventGenerator, EventListener {
  private Scene scene;
  private List children;
  
  private EventHandler handler;

	/**
	 * Constructs a new SceneTreeNode object using the specified Scene object.
	 */
	public SceneTreeNode(Scene scene) {
		this.scene = scene;
		this.children = new ArrayList();
		this.updateChildren();
	}
	
	/**
	 * Returns the Scene object stored by this SceneTreeNode object.
	 */
	public Scene getScene() { return this.scene; }
	
	/**
	 * Updates the children of this SceneTreeNode object so that they match the stored Scene object.
	 */
	public void updateChildren() {
		this.children.clear();
		int l = this.scene.getSurfaces().length;
		
		for(int i = 0; i < l; i++) {
			if (this.scene.getSurface(i) instanceof SurfaceUI)
				this.children.add(new SurfaceTreeNode((SurfaceUI)this.scene.getSurface(i), this));
		}
	}
	
	/**
	 * Method called when an event has been fired.
	 */
	public void eventFired(Event event) {
		if (event instanceof SceneOpenEvent) {
			this.scene = ((SceneOpenEvent)event).getScene();
			this.updateChildren();
		}
		
		if (event instanceof SceneCloseEvent) {
			this.scene = null;
			this.children.clear();
		}
		
		if (event instanceof SurfaceAddEvent || event instanceof SurfaceRemoveEvent) {
			this.updateChildren();
		} else if (event instanceof SurfaceEditEvent && ((SurfaceEditEvent)event).isNameChangeEvent() == true) {
			this.updateChildren();
		}
	}
	
	/**
	 * @return  The children of this SceneTreeNode object as an Enumeration.
	 */
	public java.util.Enumeration children() {
		java.util.Enumeration en = new java.util.Enumeration() {
			private Iterator itr = SceneTreeNode.this.children.iterator();
			
			public boolean hasMoreElements() { return itr.hasNext(); }
			public Object nextElement() throws java.util.NoSuchElementException { return itr.next(); }
		};
		
		return en;
	}
	
	/**
	 * @return  The child of this SceneTreeNode object at the specified index.
	 */
	public javax.swing.tree.TreeNode getChildAt(int index) { return (TreeNode) this.children.get(index); }
	
	/**
	 * @return  The number of children contained by this SceneTreeNode object.
	 */
	public int getChildCount() { return this.children.size(); }
	
	/**
	 * @return  null (A SceneTreeNode is a root node and has no parent).
	 */
	public javax.swing.tree.TreeNode getParent() { return null; }
	
	/**
	 * @return  The index of the specified TreeNode object in this SceneTreeNode object's children.
	 *          If the specified TreeNode object is unmatched -1 will be returned.
	 */
	public int getIndex(javax.swing.tree.TreeNode node) { return this.children.indexOf(node); }
	
	/**
	 * @return  True (A SceneTreeNode always allows children).
	 */
	public boolean getAllowsChildren() { return true; }
	
	/**
	 * @return  False (A SceneTreeNode is never a leaf).
	 */
	public boolean isLeaf() { return false; }
	
	/**
	 * @return  "Scene".
	 */
	public String toString() { return "Scene"; }
	
	/**
	 * Does nothing.
	 */
	public void insert(MutableTreeNode node, int index) { }
	
	/**
	 * Removes the Surface object stored by the Scene backing this SceneTreeNode
	 * at the specified index and updates the children.
	 */
	public void remove(int index) {
		this.scene.removeSurface(index);
		this.updateChildren();
	}
	
	/**
	 * Does nothing.
	 */
	public void remove(MutableTreeNode node) { }
	
	/**
	 * Does nothing.
	 */
	public void setUserObject(Object o) { }
	
	/**
	 * Does nothing.
	 */
	public void removeFromParent() { }
	
	/**
	 * Does nothing.
	 */
	public void setParent(MutableTreeNode node) { }

	public void setEventHandler(EventHandler handler) {
		this.handler = handler;
		
		Iterator itr = this.children.iterator();
		while (itr.hasNext()) ((EventGenerator)itr.next()).setEventHandler(handler);
	}

	public EventHandler getEventHandler() { return this.handler; }
}