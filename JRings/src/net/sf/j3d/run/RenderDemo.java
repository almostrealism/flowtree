/*
 * Copyright (C) 2005  Mike Murray
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

package net.sf.j3d.run;

import java.io.File;
import java.io.IOException;

import net.sf.j3d.io.FileDecoder;
import net.sf.j3d.io.FileEncoder;
import com.almostrealism.raytracer.camera.ThinLensCamera;
import com.almostrealism.raytracer.engine.AbstractSurface;
import com.almostrealism.raytracer.engine.Intersection;
import com.almostrealism.raytracer.engine.Ray;
import com.almostrealism.raytracer.engine.Scene;
import com.almostrealism.raytracer.engine.Texture;
import com.almostrealism.raytracer.lighting.PointLight;
import com.almostrealism.raytracer.lighting.RectangularLight;
import com.almostrealism.raytracer.primitives.Plane;
import com.almostrealism.raytracer.primitives.Sphere;
import com.almostrealism.raytracer.textures.StripeTexture;
import net.sf.j3d.util.TransformMatrix;
import net.sf.j3d.ui.displays.RenderTestFrame;
import net.sf.j3d.util.Vector;
import net.sf.j3d.util.graphics.RGB;


/**
 * @author Mike Murray
 */
public class RenderDemo {
	public static void main(String[] args) {
		Scene scene = null;
		
		try {
			scene = FileDecoder.decodeSceneFile(new File("CornellBox.xml"), FileDecoder.XMLEncoding, false, null);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		Sphere s = new Sphere();
		s.setLocation(new Vector(0.0, -0.5, 0.0));
		s.setColor(new RGB(0.8, 0.2, 0.2));
		
		TransformMatrix scale = TransformMatrix.createScaleMatrix(1.0, 1.4, 1.0);
		s.addTransform(scale);
		
		Texture randomTex = new Texture() {
			public RGB getColorAt(Vector point) {
				point.setZ(0.0);
				double d = (point.length() * 4.0) % 3;
				
				if (d < 1) {
					return new RGB (0.5 + Math.random() / 2.0, 0.0, 0.0);
				} else if (d < 2) {
					return new RGB (0.0, 0.5 + Math.random() / 2.0, 0.0);
				} else {
					return new RGB (0.0, 0.0, 0.5 + Math.random() / 2.0);
				}
			}

			public RGB getColorAt(Vector point, Object args[]) { return this.getColorAt(point); }
			public RGB evaluate(Object args[]) { return this.getColorAt((Vector) args[0]); }
		};
		
		s.addTexture(randomTex);
		
		AbstractSurface thing = new AbstractSurface() {
			private Plane p = new Plane(Plane.XY);
			
			public Vector getNormalAt(Vector point) { return new Vector(0.0, 0.0, 1.0); }
			
			public boolean intersect(Ray ray) {
				ray.transform(this.getTransform(true).getInverse());
				return this.p.intersect(ray);
			}
			
			public Intersection intersectAt(Ray ray) {
				ray.transform(this.getTransform(true).getInverse());
				
				if (Math.random() > 0.5) {
					return this.p.intersectAt(ray);
				} else {
					return new Intersection(ray, this, new double[0]);
				}
			}
		};
		
		thing.setLocation(new Vector(0.0, 0.0, 5.0));
		thing.setColor(new RGB(1.0, 1.0, 1.0));
		
		StripeTexture stripes = new StripeTexture();
		stripes.setAxis(StripeTexture.XAxis);
		stripes.setStripeWidth(0.25);
		stripes.setFirstColor(new RGB(1.0, 0.0, 0.0));
		stripes.setSecondColor(new RGB(0.0, 0.0, 1.0));
		thing.addTexture(stripes);
		
		scene.addSurface(s);
		scene.addSurface(thing);
		
		RectangularLight rl = new RectangularLight(2.0, 2.0);
		rl.setColor(new RGB(1.0, 1.0, 1.0));
		rl.getLocation().setY(6.0);
		rl.setType(Plane.XZ);
		rl.setIntensity(0.7);
		rl.setSampleCount(6);
		
		PointLight pl1 = new PointLight(new Vector(4.0, 4.0, 3.0), 0.6, new RGB(0.4, 1.0, 0.4));
		PointLight pl2 = new PointLight(new Vector(-4.0, 4.0, 3.0), 0.6, new RGB(1.0, 0.4, 0.4));
		
		PointLight pl3 = new PointLight(new Vector(0.0, 5.0, 4.0), 0.7, new RGB(0.0, 0.0, 1.0)) {
			public RGB getColorAt(Vector p) {
				RGB c = super.getColorAt(p);
				c.multiplyBy(Math.sin(p.subtract(super.getLocation()).length()));
				return c;
			}
		};
		
		scene.addLight(rl);
		scene.addLight(pl1);
		scene.addLight(pl2);
		scene.addLight(pl3);
		
		ThinLensCamera c = new ThinLensCamera();
		c.setLocation(new Vector(0.0, 0.0, 10.0));
		c.setViewDirection(new Vector(0.0, 0.0, -1.0));
		c.setProjectionDimensions(c.getProjectionWidth(), c.getProjectionWidth() * 1.6);
		c.setFocalLength(0.05);
		c.setFocus(10.0);
		c.setLensRadius(0.2);
		
		scene.setCamera(c);
		
		try {
			FileEncoder.encodeSceneFile(scene, new File("RenderDemo.xml"), FileEncoder.XMLEncoding);
			// scene = FileDecoder.decodeSceneFile(new File("RenderDemo.xml"), FileDecoder.XMLEncoding, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		RenderTestFrame f = new RenderTestFrame(scene, 200, 2);
		f.render();
	}
}