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

package com.almostrealism.raytracer.io;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Stack;

import org.almostrealism.space.Vector;
import org.almostrealism.util.graphics.RGB;

import com.almostrealism.flow.Resource;
import com.almostrealism.rayshade.Shader;
import com.almostrealism.raytracer.engine.AbstractSurface;
import com.almostrealism.raytracer.engine.Scene;
import com.almostrealism.raytracer.engine.Surface;
import com.almostrealism.raytracer.engine.SurfaceGroup;
import com.almostrealism.raytracer.engine.SurfaceWrapper;
import com.almostrealism.raytracer.primitives.Mesh;
import com.almostrealism.raytracer.primitives.Triangle;

/**
 * The FileDecoder class provides static methods for decoding scene and surface data
 * that has been stored in a file.
 */
public class FileDecoder {
  /** The integer code for an XML encoding. */
  public static final int XMLEncoding = 2;
  
  /** The integer code for a RAW encoding. */
  public static final int RAWEncoding = 4;
  
  /** The integer code for a RGB list encoding. */
  public static final int RGBListEncoding = 7;
  
  /** The integer code for a GTS encoding. */
  public static final int GTSEncoding = 8;
  
  /** The integer code for a ply encoding. */
  public static final int PLYEncoding = 16;

	/**
	 * Decodes the scene data stored in the file represented by the specified File object
	 * using the encoding specified by the integer encoding code and returns the new Scene object.
	 * If ui is true only SurfaceUI objects will be used. The specified ExceptionListener is notified
	 * if an exception occurs when using an XMLDecoder.
	 * This method returns null if the encoding is not supported.
	 * 
	 * @throws IOException  If an IO error occurs.
	 * @throws FileNotFoundException  If the file is not found.
	 */
	public static Scene decodeSceneFile(File file, int encoding, boolean ui, ExceptionListener listener, Surface s) throws IOException {
		return FileDecoder.decodeScene(new FileInputStream(file), encoding, ui, listener, s);
	}
	
	public static Scene decodeSceneFile(File file, int encoding, boolean ui,
									ExceptionListener listener)
									throws IOException {
		return FileDecoder.decodeScene(new FileInputStream(file), encoding, ui, listener, null);
	}
	
	public static Scene decodeScene(InputStream fileIn, int encoding,
									boolean ui, ExceptionListener listener) throws IOException {
		return FileDecoder.decodeScene(fileIn, encoding, ui, listener, null);
	}
	
	public static Scene decodeScene(Resource res, int encoding,
									ExceptionListener listener) throws IOException {
		return FileDecoder.decodeScene(res.getInputStream(), encoding, false, listener, null);
	}
	
	/**
	 * Decodes the scene data read from the specified InputStream object using the encoding specified by
	 * the integer encoding code and returnes the new Scene object. If ui is true only SurfaceUI objects will be used.
	 * The specified ExceptionListener is notified if an exception occurs when using an XMLDecoder.
	 * This method returns null if the encoding is not supported.
	 */
	public static Scene decodeScene(InputStream fileIn, int encoding, boolean ui, ExceptionListener listener, Surface s) throws IOException {
		if (encoding == FileDecoder.XMLEncoding) {
			XMLDecoder decoder = new XMLDecoder(fileIn);
			decoder.setExceptionListener(listener);
			
			Scene scene = (Scene)decoder.readObject();
			
			if (!ui) {
				Surface sr[] = scene.getSurfaces();
				for (int i = 0; i < sr.length; i++)
					if (sr[i] instanceof SurfaceWrapper)
						sr[i] = ((SurfaceWrapper)sr[i]).getSurface();
			}
			
			return scene;
		} else if (encoding == FileDecoder.RAWEncoding) {
			Scene scene = new Scene();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(fileIn));
			
			String line = in.readLine();
			int lineCount = 0;
			
			t: while (line != null) {
				if (line.startsWith("#") == true) {
					line = in.readLine();
					lineCount++;
				} else {
					break t;
				}
			}
			
			boolean polyData = false;
			int pointCount = 0, polyCount = 0;
			
			if (line.indexOf(" ", line.indexOf(" ") + 1) < 0) {
				polyData = true;
				
				pointCount = Integer.parseInt(line.substring(0, line.indexOf(" ")));
				polyCount = Integer.parseInt(line.substring(line.indexOf(" ") + 1));
				
				line = in.readLine();
				lineCount++;
			}
			
			l: while (line != null) {
				int tCount = 0;
				
				if (line.startsWith("#") == true) {
					line = in.readLine();
					lineCount++;
					
					continue l;
				}
				
				if (polyData == true) {
					Vector points[] = new Vector[pointCount];
					
					for (int p = 0; p < points.length; p++) {
						t: while (true) {
							if (line.startsWith("#") == true) {
								line = in.readLine();
								lineCount++;
							} else {
								break t;
							}
						}
						
						double data[] = new double[3];
						
						boolean broken = false;
						
						i: for (int i = 0; i < data.length; i++) {
							int index = line.indexOf(" ");
							
							if (i == data.length - 1) {
								data[i] = Double.parseDouble(line);
								
								break i;
							}
							
							if (index > 0) {
								data[i] = Double.parseDouble(line.substring(0, index));
								line = line.substring(index + 1);
							} else {
								System.out.println("Line " + lineCount + " of RAW file does not contain the proper number of terms, " +
											"or is not properly delimited");
								
								broken = true;
								break i;
							}
						}
						
						if (broken == false) {
							points[p] = new Vector(data[0], data[1], data[2]);
						}
						
						line = in.readLine();
						lineCount++;
					}
					
					for (int p = 0; p < polyCount; p++) {
						t: while (true) {
							if (line.startsWith("#") == true) {
								line = in.readLine();
								lineCount++;
							} else {
								break t;
							}
						}
						
						int data[] = new int[3];
						
						boolean broken = false;
						
						i: for (int i = 0; i < data.length; i++) {
							int index = line.indexOf(" ");
							
							if (i == data.length - 1) {
								data[i] = Integer.parseInt(line.trim());
								
								break i;
							}
							
							if (index > 0) {
								data[i] = Integer.parseInt(line.substring(0, index).trim());
								line = line.substring(index + 1);
							} else {
								System.out.println("Line " + lineCount + " of RAW file does not contain the proper number of terms, " +
											"or is not properly delimited");
								
								broken = true;
								break i;
							}
						}
						
						if (broken == false) {
							Surface newSurface = null;
							
							newSurface = new Triangle(points[data[0]], points[data[1]], points[data[2]]);
							((AbstractSurface)newSurface).setColor(new RGB(1.0, 1.0, 1.0));
							((AbstractSurface)newSurface).setShaders(new Shader[0]);
							
							if (ui == true) {
								System.out.println("FileDecoder: UI mode no longer supported.");
//								newSurface = SurfaceUIFactory.createSurfaceUI((AbstractSurface)newSurface);
//								((AbstractSurfaceUI)newSurface).setName("Triangle " + p);
							}
							
							scene.addSurface(newSurface);
						}
						
						line = in.readLine();
						lineCount++;
					}
					
					break l;
				} else {
					try {
						double data[] = new double[9];
						
						boolean broken = false;
						
						i: for (int i = 0; i < data.length; i++) {
							int index = line.indexOf(" ");
							
							if (i == data.length - 1) {
								data[i] = Double.parseDouble(line);
								
								break i;
							}
							
							if (index > 0) {
								data[i] = Double.parseDouble(line.substring(0, index));
								line = line.substring(index + 1);
							} else {
								System.out.println("Line " + lineCount + " of RAW file does not contain the proper number of terms, " +
											"or is not properly delimited");
								
								broken = true;
								break i;
							}
						}
						
						if (broken == false) {
							Vector p1 = new Vector(data[0], data[1], data[2]);
							Vector p2 = new Vector(data[3], data[4], data[5]);
							Vector p3 = new Vector(data[6], data[7], data[8]);
							
							Surface newSurface = null;
							
							newSurface = new Triangle(p1, p2, p3);
							((AbstractSurface)newSurface).setColor(new RGB(1.0, 1.0, 1.0));
							((AbstractSurface)newSurface).setShaders(new Shader[0]);
							
							if (ui == true) {
								System.out.println("FileDecoder: UI mode no longer supported.");
//								newSurface = SurfaceUIFactory.createSurfaceUI((AbstractSurface)newSurface);
//								((AbstractSurfaceUI)newSurface).setName("Triangle " + tCount);
							}
							
							scene.addSurface(newSurface);
							tCount++;
						}
					} catch (NumberFormatException nfe) {
						System.out.println("Line " + lineCount + " of RAW file contains nonnumerical data");
					}
				}
				
				line = in.readLine();
				lineCount++;
			}
			
			return scene;
		} else if (encoding == FileDecoder.GTSEncoding) {
			Mesh m = new Mesh();
			if (s != null && s instanceof Mesh) {
				System.out.println(m);
				
				m = (Mesh) s;
			}
			
			BufferedReader in = new BufferedReader(new InputStreamReader(fileIn));
			
			int lineCount = 0;
			String line = null;
			
			int pointCount = 0, edgeCount = 0, triangleCount = 0;
			
			w: while (true) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				if (line.startsWith("#")) continue w;
				
				double d[] = FileDecoder.parseDoubles(line);
				
				pointCount = (int)d[0];
				edgeCount = (int)d[1];
				triangleCount = (int)d[2];
				
				break w;
			}
			
			int edges[][] = new int[edgeCount][2];
			
			i: for (int i = 0; i < pointCount; ) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				if (line.startsWith("#")) continue i;
				
				double d[] = FileDecoder.parseDoubles(line);
				m.addVector(new Vector(d[0], d[1], d[2]));
				
				i++;
			}
			
			i: for (int i = 0; i < edgeCount; ) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				if (line.startsWith("#")) continue i;
				
				double d[] = FileDecoder.parseDoubles(line);
				edges[i][0] = (int)d[0] - 1;
				edges[i][1] = (int)d[1] - 1;
				
				i++;
			}
			
			i: for (int i = 0; i < triangleCount; ) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				if (line.startsWith("#")) continue i;
				
				double d[] = FileDecoder.parseDoubles(line);
				
				Stack st = new Stack();
				
				for (int j = 0; j < d.length; j++) {
					int v[] = edges[(int)d[j] - 1];
					Integer v0 = Integer.valueOf(v[0]);
					Integer v1 = Integer.valueOf(v[1]);
					
					if (!st.contains(v0)) st.push(v0);
					if (!st.contains(v1)) st.push(v1);
				}
				
				m.addTriangle(((Integer)st.pop()).intValue(), ((Integer)st.pop()).intValue(), ((Integer)st.pop()).intValue());
				
				i++;
			}
			
			if (ui == true) {
				System.out.println("FileDecoder: UI mode no longer supported.");
//				AbstractSurfaceUI sr[] = {SurfaceUIFactory.createSurfaceUI(m)};
//				sr[0].setName("Mesh (" + m.getTriangles().length + " Triangles)");
//				return new Scene(sr);
			}
			
			return new Scene(new Surface[] {m});
		} else if (encoding == FileDecoder.PLYEncoding) {
			Mesh m = new Mesh();
			if (s != null && s instanceof Mesh) {
				System.out.println(m);
				m = (Mesh) s;
			}
			
			BufferedReader in = new BufferedReader(new InputStreamReader(fileIn));
			
			int lineCount = 0;
			String line = null;
			
			int pointCount = 0, triangleCount = 0;
			
			w: while (true) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				
				if (line.startsWith("element")) {
					if (line.indexOf("vertex") > 0)
						pointCount = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
					if (line.indexOf("face") > 0)
						triangleCount = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
				} else if (line.startsWith("end_header")) {
					break w;
				}
			}
			
			i: for (int i = 0; i < pointCount; ) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				if (line.startsWith("#")) continue i;
				
				double d[] = FileDecoder.parseDoubles(line);
				m.addVector(new Vector(d[0], d[1], d[2]));
				
				i++;
			}
			
			i: for (int i = 0; i < triangleCount; ) {
				line = in.readLine();
				lineCount++;
				if (line == null) return null;
				if (line.startsWith("#")) continue i;
				
				double d[] = FileDecoder.parseDoubles(line);
				
				m.addTriangle((int) d[1], (int) d[2], (int) d[3]);
				
				i++;
			}
			
			if (ui == true) {
				System.out.println("FileDecoder: UI mode no longer supported.");
//				AbstractSurfaceUI sr[] = {SurfaceUIFactory.createSurfaceUI(m)};
//				sr[0].setName("Mesh (" + m.getTriangles().length + " Triangles)");
//				return new Scene(sr);
			}
			
			return new Scene(new Surface[] {m});
		} else {
			return null;
		}
	}
	
	public static Surface decodeSurfaceFile(File file, int encoding,
								boolean ui, ExceptionListener listener)
								throws IOException {
		return FileDecoder.decodeSurfaceFile(file, encoding, ui, listener, null);
	}
	
	/**
	 * Decodes the surface data stored in the file represented by the specified File object
	 * using the encoding specified by the integer encoding code and returns the new Surface object.
	 * If ui is true a SurfaceUI object will be returned. The specified ExceptionListener is notified
	 * if an exception occurs when using an XMLDecoder.
	 * The decodeSurfaceFile method returns null if the encoding is not supported.
	 * 
	 * @throws IOException  If an IO error occurs.
	 * @throws FileNotFoundException  If the file is not found.
	 */
	public static Surface decodeSurfaceFile(File file, int encoding, boolean ui, ExceptionListener listener, Surface s) throws IOException {
		FileInputStream fileIn = new FileInputStream(file);
		
		if (encoding == FileDecoder.XMLEncoding) {
			XMLDecoder decoder = new XMLDecoder(fileIn);
			decoder.setExceptionListener(listener);
			
			return ((Surface)decoder.readObject());
		} else if (encoding == FileDecoder.RAWEncoding) {
			Scene scene = FileDecoder.decodeSceneFile(file, FileDecoder.RAWEncoding, ui, listener, s);
			
			Surface group = new SurfaceGroup(scene.getSurfaces());
			
			if (ui == true) {
				System.out.println("FileDecoder: UI mode no longer supported.");
//				group = SurfaceUIFactory.createSurfaceUI((AbstractSurface)group);
//				((AbstractSurfaceUI)group).setName("Surface (Triangles)");
			}
			
			return group;
		} else if (encoding == FileDecoder.GTSEncoding ||
				encoding == FileDecoder.PLYEncoding) {
			Scene scene = FileDecoder.decodeSceneFile(file, encoding, ui, listener, s);
			return scene.getSurfaces()[0];
		} else {
			return null;
		}
	}
	
	/**
	 * Parses a series of double values separated by spaces and returnes an array containing the values.
	 * 
	 * @throws NumberFormatException  If a number is not correctly formatted.
	 */
	public static double[] parseDoubles(String s) {
		java.util.Vector values = new java.util.Vector();
		
		s = s.concat(" ");
		int index = s.indexOf(" ");
		
		w: while (index >= 0) {
			String t = s.substring(0, index);
			t = t.trim();
			
			if (t.equals("")) break w;
			
			values.addElement(new Double(t));
			
			s = s.substring(index + 1);
			index = s.indexOf(" ");
		}
		
		double d[] = new double[values.size()];
		for (int i = 0; i < d.length; i++) d[i] = ((Double)values.elementAt(i)).doubleValue();
		
		return d;
	}
	
	public static RGB[][] readRGBList(InputStream in, RGB buf[][]) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		String line = null;
		int i = 0, j = 0;
		
		w: while ((line = reader.readLine()) != null) {
			if (j >= buf[i].length) { i++; j = 0; }
			if (i >= buf.length) break w;
			
			String s[] = line.split(" ");
			
			double r = Double.parseDouble(s[0]);
			double g = Double.parseDouble(s[1]);
			double b = Double.parseDouble(s[2]);
			
			buf[i][j] = new RGB(r, g, b);
		}
		
		return buf;
	}
}
