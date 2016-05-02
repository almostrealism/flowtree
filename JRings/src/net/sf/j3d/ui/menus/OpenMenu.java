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



import java.io.*;
import javax.swing.*;

import net.sf.j3d.io.*;
import com.almostrealism.raytracer.engine.*;
import net.sf.j3d.ui.event.*;

import java.beans.ExceptionListener;

/**
  An OpenMenu object extends JMenu and provides menu items for open scene data stored in a file.
*/

public class OpenMenu extends JMenu implements EventListener, EventGenerator {
  private Scene scene;
  
  private EventHandler handler;
  
  private JMenu openSceneMenu, openSurfaceMenu;
  
  private JMenuItem openXMLSceneItem;
  private JMenuItem openXMLSurfaceItem, openRAWSurfaceItem, openGTSSurfaceItem;

	private class CustomExceptionListener implements ExceptionListener {
		public boolean thrown = false;
		
		public void exceptionThrown(Exception e) {
			this.thrown = true;
		}
	}
	
	/**
	  Constructs a new OpenMenu object.
	*/
	
	public OpenMenu(Scene scn) {
		super("Open");
		
		this.scene = scn;
		
		this.openSceneMenu = new JMenu("Scene");
		this.openSurfaceMenu = new JMenu("Surface");
		
		this.openXMLSceneItem = new JMenuItem("XML Encoded Scene");
		
		this.openXMLSurfaceItem = new JMenuItem("XML Encoded Surface");
		this.openRAWSurfaceItem = new JMenuItem("RAW Encoded Surface");
		this.openGTSSurfaceItem = new JMenuItem("GTS Encoded Surface");
		
		this.openSceneMenu.add(this.openXMLSceneItem);
		
		this.openSurfaceMenu.add(this.openXMLSurfaceItem);
		this.openSurfaceMenu.add(this.openRAWSurfaceItem);
		this.openSurfaceMenu.add(this.openGTSSurfaceItem);
		
		this.add(this.openSceneMenu);
		this.add(this.openSurfaceMenu);
		
		this.openXMLSceneItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				openScene(FileDecoder.XMLEncoding);
			}
		});
		
		this.openXMLSurfaceItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				openSurface(FileDecoder.XMLEncoding);
			}
		});
		
		this.openRAWSurfaceItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				openSurface(FileDecoder.RAWEncoding);
			}
		});
		
		this.openGTSSurfaceItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				openSurface(FileDecoder.GTSEncoding);
			}
		});
	}
	
	/**
	  Allows the user to select a file that stores scene data and opens that file. It then fires the required events.
	*/
	
	public void openScene(final int encoding) {
		final JFileChooser fileChooser = new JFileChooser();
		int selected = fileChooser.showOpenDialog(null);
		
		if (selected == JFileChooser.APPROVE_OPTION) {
			final JFrame frame = new JFrame("Loading");
			frame.setSize(250, 70);
			frame.getContentPane().setLayout(new java.awt.FlowLayout());
			frame.getContentPane().add(new JLabel("Loading scene file..."));
			
			frame.setVisible(true);
			
			final Thread loader = new Thread(new Runnable() {
				public void run() {
					CustomExceptionListener listener = new CustomExceptionListener();
					
					try {
						Scene newScene = FileDecoder.decodeSceneFile(fileChooser.getSelectedFile(), encoding, true, listener);
						if (handler != null) handler.fireEvent(new SceneOpenEvent(newScene));
					} catch (FileNotFoundException fnf) {
						JOptionPane.showMessageDialog(null, "There was a FileNotFoundException thrown by threeD.io.FileDecoder",
										"File Not Found", JOptionPane.ERROR_MESSAGE);
					} catch (IOException ioe) {
						JOptionPane.showMessageDialog(null, "There was an IOException thrown by threeD.io.FileDecoder",
										"IO Error", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						e.printStackTrace();
						listener.thrown = true;
					}
					
					if (listener.thrown)
						JOptionPane.showMessageDialog(null, "Some errors occured while loading the file.", "Error", JOptionPane.ERROR_MESSAGE);
					
					frame.setVisible(false);
				}
			});
			
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent event) {
					loader.stop();
					System.gc();
				}
			});
			
			frame.getContentPane().add(cancelButton);
			
			loader.start();
		}
	}
	
	/**
	  Allows the user to select a file that stores surface data and adds the constructed Surface object to the Scene object
	  stored by this OpenMenu object. It then fires the required events.
	*/
	
	public void openSurface(final int encoding) {
		final JFileChooser fileChooser = new JFileChooser();
		int selected = fileChooser.showOpenDialog(null);
		
		if (selected == JFileChooser.APPROVE_OPTION) {
			final JFrame frame = new JFrame("Loading");
			frame.setSize(250, 70);
			frame.getContentPane().setLayout(new java.awt.FlowLayout());
			frame.getContentPane().add(new JLabel("Loading surface file..."));
			
			frame.setVisible(true);
				
			final Thread loader = new Thread(new Runnable() {
				public void run() {
					CustomExceptionListener listener = new CustomExceptionListener();
					
					try {
						Surface newSurface = FileDecoder.decodeSurfaceFile(fileChooser.getSelectedFile(), encoding, true, listener);
						scene.addSurface(newSurface);
						
						if (handler != null) handler.fireEvent(new SurfaceAddEvent(newSurface));
					} catch (FileNotFoundException fnf) {
						JOptionPane.showMessageDialog(null, "There was a FileNotFoundException thrown by threeD.io.FileDecoder",
										"File Not Found", JOptionPane.ERROR_MESSAGE);
					} catch (IOException ioe) {
						JOptionPane.showMessageDialog(null, "There was an IOException thrown by threeD.io.FileDecoder",
										"IO Error", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						e.printStackTrace();
						listener.thrown = true;
					}
					
					if (listener.thrown)
						JOptionPane.showMessageDialog(null, "Some errors occured while loading the file.", "Error", JOptionPane.ERROR_MESSAGE);
					
					frame.setVisible(false);
				}
			});
			
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent event) {
					loader.stop();
					System.gc();
				}
			});
			
			frame.getContentPane().add(cancelButton);
			
			loader.start();
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
	  Sets the EventHandler object used by this OpenMenu object. Setting this to null will deactivate event reporting.
	*/
	
	public void setEventHandler(EventHandler handler) {
		this.handler = handler;
	}
	
	/**
	  Returns the EventHandler object used by this OpenMenu object.
	*/
	
	public EventHandler getEventHandler() {
		return this.handler;
	}
}