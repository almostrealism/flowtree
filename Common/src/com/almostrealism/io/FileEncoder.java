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

package com.almostrealism.io;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;

import com.almostrealism.raytracer.Settings;
import com.almostrealism.raytracer.camera.PinholeCamera;
import com.almostrealism.raytracer.camera.ThinLensCamera;
import com.almostrealism.raytracer.engine.AbstractSurface;
import com.almostrealism.raytracer.engine.Scene;
import com.almostrealism.raytracer.engine.Surface;
import com.almostrealism.raytracer.lighting.AmbientLight;
import com.almostrealism.raytracer.lighting.DirectionalAmbientLight;
import com.almostrealism.raytracer.lighting.Light;
import com.almostrealism.raytracer.lighting.PointLight;
import com.almostrealism.raytracer.lighting.RectangularLight;
import com.almostrealism.raytracer.lighting.SphericalLight;
import com.almostrealism.raytracer.primitives.Cone;
import com.almostrealism.raytracer.primitives.Cylinder;
import com.almostrealism.raytracer.primitives.Mesh;
import com.almostrealism.raytracer.primitives.Plane;
import com.almostrealism.raytracer.primitives.Polynomial;
import com.almostrealism.raytracer.primitives.Sphere;
import com.almostrealism.raytracer.primitives.Triangle;
import com.almostrealism.util.Vector;
import com.almostrealism.util.graphics.GraphicsConverter;
import com.almostrealism.util.graphics.RGB;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

// TODO  Add GIF encoding.
// TODO  Add GTS and RAW encoding.

/**
 * The FileEncoder class provides static methods for encoding Scene and Surface objects
 * and storing them in local files.
 */
public class FileEncoder {

  /** The integer code for an XML encoding. */
  public static final int XMLEncoding = 2;
  
  /** The integer code for a GTS encoding. */
  public static final int GTSEncoding = 3;
  
  /** The integer code for a PPM image encoding. */
  public static final int PPMEncoding = 4;
  
  /** The integer code for a PIX image encoding. */
  public static final int PIXEncoding = 5;
  
  /** The integer code for a JPEG image encoding. */
  public static final int JPEGEncoding = 6;
  
  /** The integer code for an RGB list image encoding. */
  public static final int RGBListEncoding = 7;

	/**
	 * Encodes the specified Scene object using the encoding specified by the integer encoding code
	 * and saves the encoded data in the file represented by the specified File object.
	 * If the encoding code is not recognized, the method returns.
	 */
	public static void encodeSceneFile(Scene scene, File file, int encoding) throws IOException {
		if (file.exists() != true) {
			if (!file.createNewFile()) {
				System.out.println("FileEncoder: Unable to create " + file);
				return;
			}
		}
		
		FileOutputStream fileOut = new FileOutputStream(file);
		
		if (encoding == FileEncoder.XMLEncoding) {
			XMLEncoder encoder = new XMLEncoder(fileOut);
			
			FileEncoder.configureEncoder(encoder);
			
			encoder.writeObject(scene);
			encoder.close();
		}
	}
	
	/**
	 * Encodes the specified Surface object using the encoding specified by the integer encoding code
	 * and saves the encoded data in the file represented by the specified File object.
	 * If the encoding code is not recognized, the method returns.
	 */
	public static void encodeSurfaceFile(Surface surface, File file, int encoding) throws IOException {
		if (file.exists() != true) {
			file.createNewFile();
		}
		
		FileOutputStream fileOut = new FileOutputStream(file);
		
		if (encoding == FileEncoder.XMLEncoding) {
			XMLEncoder encoder = new XMLEncoder(fileOut);
			
			FileEncoder.configureEncoder(encoder);
			
			encoder.writeObject(surface);
			encoder.close();
		} else if (encoding == FileEncoder.GTSEncoding) {
			Mesh m = null;
			
			if (surface instanceof AbstractSurface)
				m = ((AbstractSurface)surface).triangulate();
			else
				return;
			
			Vector v[] = m.getVectors();
			Triangle t[] = m.getTriangles();
			
			PrintStream p = new PrintStream(fileOut);
			
			p.println("# GTS output generated by Rings version " + Settings.version);
			p.println("# " + new Date());
			p.println(v.length + " " + 3 * t.length + " " + t.length);
			
			for (int i = 0; i < v.length; i++) {
				p.println(v[i].getX() + " " + v[i].getY() + " " + v[i].getZ());
			}
			
			for (int i = 0; i < t.length; i++) {
				Vector tv[] = t[i].getVertices();
				p.println(m.indexOf(tv[0]) + " " + m.indexOf(tv[1]));
				p.println(m.indexOf(tv[1]) + " " + m.indexOf(tv[2]));
				p.println(m.indexOf(tv[2]) + " " + m.indexOf(tv[0]));
			}
			
			int j = 0;
			for (int i = 0; i < t.length; i++) p.println(j++ + " " + j++ + " " + j++);
			
			p.flush();
			p.close();
		}
	}
	
	/**
	 * Encodes the image represented by the specified RGB array using the encoding specified by the integer encoding code
	 * and saves the encoded data in the file represented by the specified File object.
	 * If the encoding code is not recognized, the method returns.
	 */
	public static void encodeImageFile(RGB image[][], File file, int encoding)
						throws IOException {
		OutputStream o = new FileOutputStream(file);
		writeImage(image, o, encoding);
		o.flush();
		o.close();
	}
	
	public static void writeImage(RGB image[][], OutputStream o, int encoding)
						throws IOException {
		if (encoding == FileEncoder.RGBListEncoding) {
			ObjectOutputStream out = new ObjectOutputStream(o);
			
			for (int i = 0; i < image.length; i++) {
				for (int j = 0; j < image[i].length; j++) {
					if (image[i][j] == null) {
						new RGB(0.0, 0.0, 0.0).writeExternal(out);
					} else {
						image[i][j].writeExternal(out);
					}
				}
			}
			
			out.flush();
		} if (encoding == FileEncoder.PPMEncoding) {
			java.io.PrintWriter out = new java.io.PrintWriter(o);
			
			out.println("P3");
			out.println("# Generated by Rings FileEncoder (Version " + Settings.version + ")");
			out.println("# " + Settings.writtenByInfo);
			
			out.println(image.length + " " + image[0].length);
			out.println("255");
			
			for (int j = 0; j < image[0].length; j++) {
				for (int i = 0; i < image.length; i++) {
					if (image[i][j] == null) {
						out.println("0 0 0");
					} else {
						int r = (int)(255 * image[i][j].getRed());
						int g = (int)(255 * image[i][j].getGreen());
						int b = (int)(255 * image[i][j].getBlue());
						out.println(r + " " + g + " " + b);
					}
				}
			}
			
			out.flush();
			
			if (out.checkError() == true)
				throw new IOException("IO error while writing image data");
		} else if (encoding == FileEncoder.PIXEncoding) {
			int w = image.length;
			int h = image[0].length;
			
			byte b[] = new byte[4 * w * h + 10];
			
			b[0] = (byte)(w >> 8);
			b[1] = (byte)w;
			b[2] = (byte)(h >> 8);
			b[3] = (byte)h;
			b[9] = 24;
			
			int index = 10;
			
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					b[index++] = 1;
					b[index++] = (byte)(255 * image[i][j].getBlue());
					b[index++] = (byte)(255 * image[i][j].getGreen());
					b[index++] = (byte)(255 * image[i][j].getRed());
				}
			}
			
			o.write(b);
		} else if (encoding == FileEncoder.JPEGEncoding) {
		    BufferedImage bimg = new BufferedImage(image.length, image[0].length, BufferedImage.TYPE_INT_ARGB);
		    Graphics g = bimg.createGraphics();
		    
		    g.drawImage(GraphicsConverter.convertToAWTImage(image), 0, 0, null);
		    
		    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(o);
		    JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bimg);
		    param.setQuality(1.0f, true);
		    encoder.encode(bimg, param);
		}
	}
	
	private static void configureEncoder(XMLEncoder encoder) {
		encoder.setPersistenceDelegate(PinholeCamera.class, new CameraPersistenceDelegate());
		encoder.setPersistenceDelegate(ThinLensCamera.class, new CameraPersistenceDelegate());
		
		encoder.setPersistenceDelegate(AbstractSurface.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Sphere.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Cylinder.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Cone.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Plane.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Polynomial.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Triangle.class, new SurfacePersistenceDelegate());
		encoder.setPersistenceDelegate(Mesh.class, new SurfacePersistenceDelegate());
//		encoder.setPersistenceDelegate(AbstractSurfaceUI.class, new SurfacePersistenceDelegate());
//		encoder.setPersistenceDelegate(SurfaceUIFactory.SurfaceUIImpl.class, new SurfacePersistenceDelegate());
		
		encoder.setPersistenceDelegate(Light.class, new LightPersistenceDelegate());
		encoder.setPersistenceDelegate(AmbientLight.class, new LightPersistenceDelegate());
		encoder.setPersistenceDelegate(DirectionalAmbientLight.class, new LightPersistenceDelegate());
		encoder.setPersistenceDelegate(PointLight.class, new LightPersistenceDelegate());
		encoder.setPersistenceDelegate(SphericalLight.class, new LightPersistenceDelegate());
		encoder.setPersistenceDelegate(RectangularLight.class, new LightPersistenceDelegate());
	}
}
