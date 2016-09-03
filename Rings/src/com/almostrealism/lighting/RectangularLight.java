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

package com.almostrealism.lighting;

import org.almostrealism.color.RGB;
import org.almostrealism.space.TransformMatrix;
import org.almostrealism.space.Vector;

import com.almostrealism.rayshade.Shader;
import com.almostrealism.raytracer.primitives.Plane;

/**
 * A RectangularLight object provides PointLight samples that are randomly distributed
 * across the a plane surface. The location of the plane is used for the upper left corner
 * of the area to sample.
 * 
 * @author Mike Murray
 */
public class RectangularLight extends Plane implements SurfaceLight {
  private double width, height;
  private double intensity;
  
  private int samples;
	
  	/**
  	 * Constructs a new RectangularLight object.
  	 */
	public RectangularLight() {
		this.samples = 1;
		
		super.setShaders(new Shader[0]);
	}
	
	/**
	 * Constructs a new RectangularLight object.
	 * 
	 * @param width  Width of rectangle.
	 * @param height  Hieght of rectangle.
	 */
	public RectangularLight(double width, double height) {
		this.width = width;
		this.height = height;
		
		this.samples = 1;
		
		super.setShaders(new Shader[0]);
	}
	
	/**
	 * Sets the number of samples to use for this RectangularLight object.
	 * 
	 * @param samples
	 */
	public void setSampleCount(int samples) { this.samples = samples; }
	
	/**
	 * @return  The number of samples to use for this RectangularLight object.
	 */
	public int getSampleCount() { return this.samples; }
	
	/**
	 * @see com.almostrealism.lighting.SurfaceLight#getSamples(int)
	 */
	public Light[] getSamples(int total) {
		Light l[] = new Light[total];
		
		double in = this.intensity / total;
		
		for (int i = 0; i < total; i++) {
			double x = 0.0, y = 0.0, z = 0.0;
			
			if (super.getType() == Plane.XY) {
				x = Math.random() * this.width;
				y = Math.random() * this.height;
				z = 0.0;
			} else if (super.getType() == Plane.XZ) {
				x = Math.random() * this.width;
				y = 0.0;
				z = Math.random() * this.height;
			} else if (super.getType() == Plane.YZ) {
				x = 0.0;
				y = Math.random() * this.width;
				z = Math.random() * this.height;
			}
			
			Vector p = new Vector(x, y, z);
			super.getTransform(true).transform(p, TransformMatrix.TRANSFORM_AS_LOCATION);
			
			l[i] = new PointLight(p, in, (RGB) super.getColorAt(p));
		}
		
		return l;
	}
	
	/**
	 * @see com.almostrealism.lighting.SurfaceLight#getSamples()
	 */
	public Light[] getSamples() { return this.getSamples(this.samples); }
	
	/**
	 * Sets the width of the rectangular area of this RectangularLight object.
	 */
	public void setWidth(double width) { this.width = width; }
	
	/**
	 * Sets the height of the rectangular area of this RectangularLight object.
	 */
	public void setHeight(double height) { this.height = height; }
	
	/**
	 * @return  The width of the rectangular area of this RectangularLight object.
	 */
	public double getWidth() { return this.width; }
	
	/**
	 * @return  The width of the rectangular area of this RectangularLight object.
	 */
	public double getHeight() { return this.height; }
	
	/**
	 * @see com.almostrealism.lighting.Light#setIntensity(double)
	 */
	public void setIntensity(double intensity) { this.intensity = intensity; }
	
	/**
	 * @see com.almostrealism.lighting.Light#getIntensity()
	 */
	public double getIntensity() { return this.intensity; }
	
	/**
	 * @return  "Rectangular Light".
	 */
	public String toString() { return "Rectangular Light"; }
}
