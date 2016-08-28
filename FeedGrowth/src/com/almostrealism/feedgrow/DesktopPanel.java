/*
 * Copyright 2016 Michael Murray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almostrealism.feedgrow;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.almostrealism.raytracer.io.WavefrontObjParser;
import com.almostrealism.receptor.ReceptorRenderPanel;

/**
 * @author  Michael Murray
 */
public class DesktopPanel extends DesktopPanelUI {
    private Point initialClick;
    private JFrame frame;
	
	public DesktopPanel(JFrame parent, Replicator r) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
	    this.frame = parent;

	    addMouseListener(new MouseAdapter() {
	        public void mousePressed(MouseEvent e) {
	            initialClick = e.getPoint();
	            getComponentAt(initialClick);
	        }
	    });
	    
	    addMouseMotionListener(new MouseMotionAdapter() {
	        @Override
	        public void mouseDragged(MouseEvent e) {

	            // get location of Window
	            int thisX = frame.getLocation().x;
	            int thisY = frame.getLocation().y;

	            // Determine how much the mouse moved since the initial click
	            int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
	            int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);

	            // Move window to this position
	            int X = thisX + xMoved;
	            int Y = thisY + yMoved;
	            frame.setLocation(X, Y);
	        }
	    });
	    
		r.addLayer("Cube", new WavefrontObjParser(Replicator.class.getClassLoader().getResourceAsStream("models/Cube.obj")).getMesh());
		
		toolBar.add(new QuitAction());
		toolBar.add(r.getCanvasAction());
		toolBar.add(r.getLayersAction());
		toolBar.add(r.getSamplerAction());
		toolBar.add(r.getFeedbackAction());
		
		ReceptorRenderPanel raytracer = new ReceptorRenderPanel(r.getScene());
		renderPanel.add(raytracer, BorderLayout.CENTER);
		raytracer.render();
	}
	
	private static class QuitAction extends AbstractAction {
		public QuitAction() {
			super("Quit", new ImageIcon(QuitAction.class.getResource("/icons/x.png")));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) { System.exit(0); }
	}
}
