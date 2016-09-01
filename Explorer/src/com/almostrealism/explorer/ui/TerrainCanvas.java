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

package com.almostrealism.explorer.ui;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;

import com.almostrealism.gl.SurfaceCanvas;

public class TerrainCanvas extends SurfaceCanvas {
	public TerrainCanvas() {
		super(null);
	}
	
	public static void main(String args[]) throws IOException {
		TerrainCanvas c = new TerrainCanvas();
//		c.add(new Terrain(new File("Explorer/ne_110m_land/ne_110m_land.shp").toURI().toString()));
		
		c.start();
		
		JFrame frame = new JFrame("Explorer");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(c);
		
		frame.setSize(400, 400);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
