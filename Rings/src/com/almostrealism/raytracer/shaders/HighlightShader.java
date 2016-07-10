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

package com.almostrealism.raytracer.shaders;

import com.almostrealism.util.Editable;
import com.almostrealism.util.Producer;
import com.almostrealism.util.Vector;
import com.almostrealism.util.graphics.ColorProducer;
import com.almostrealism.util.graphics.RGB;

/**
 * A HighlightShader object provides a shading method for highlights on surfaces.
 * The HighlightShader class uses a phong shading algorithm.
 * 
 * @author Mike Murray
 */
public class HighlightShader extends ShaderSet implements Shader, Editable {
  private static final String propNames[] = {"Highlight Color", "Highlight Exponent"};
  private static final String propDesc[] = {"The base color for the highlight", "The exponent used to dampen the highlight (phong exponent)"};
  private static final Class propTypes[] = {ColorProducer.class, Double.class};
  
  private ColorProducer highlightColor;
  private double highlightExponent;

	/**
	 * Constructs a new HighlightShader object using white as a highlight color
	 * and 1.0 as a highlight exponent.
	 */
	public HighlightShader() {
		this.setHighlightColor(new RGB(1.0, 1.0, 1.0));
		this.setHighlightExponent(1.0);
	}
	
	/**
	 * Constructs a new HighlightShader object using the specified highlight color
	 * and highlight exponent.
	 */
	public HighlightShader(ColorProducer color, double exponent) {
		this.setHighlightColor(color);
		this.setHighlightExponent(exponent);
	}
	
	/**
	 * Method specified by the Shader interface.
	 */
	public RGB shade(ShaderParameters p) {
		RGB lightColor = p.getLight().getColorAt(p.getPoint());
		
		Vector n = p.getSurface().getNormalAt(p.getPoint());
		n.divideBy(n.length());
		Vector h = p.getViewerDirection().add(p.getLightDirection());
		h = h.divide(h.length());
		
		RGB color = new RGB(0.0, 0.0, 0.0);
		
		RGB hc = this.getHighlightColor().evaluate(new Object[] {p});
		if (super.size() > 0) hc.multiplyBy(super.shade(p));
		
		if (p.getSurface().getShadeFront()) {
			double c = h.dotProduct(n);
			c = Math.pow(c, this.getHighlightExponent());
			
			color.addTo((lightColor.multiply(hc)).multiply(c));
		}
		
		if (p.getSurface().getShadeBack()) {
			double c = h.dotProduct(n.minus());
			c = Math.pow(c, this.getHighlightExponent());
			
			color.addTo((lightColor.multiply(hc)).multiply(c));
		}
		
		return color;
	}
	
	/**
	 * @throws IllegalArgumentException  If args[0] is not a ShaderParameters object.
	 * @return  this.shade(args[0]).
	 */
	public RGB evaluate(Object args[]) {
	    if (args[0] instanceof ShaderParameters) {
	        return this.shade((ShaderParameters)args[0]);
	    } else {
	        throw new IllegalArgumentException("Illegal argument: " + args[0]);
	    }
	}
	
	/**
	 * Sets the color used for the highlight shaded by this HighlightShader object
	 * to the color represented by the specifed RGB object.
	 */
	public void setHighlightColor(ColorProducer color) { this.highlightColor = color; }
	
	/**
	 * Sets the highlight exponent (phong exponent) used by this HighlightShader object.
	 */
	public void setHighlightExponent(double exp) { this.highlightExponent = exp; }
	
	/**
	 * Returns the color used for the highlight shaded by this HighlightShader object
	 * as an ColorProducer object.
	 */
	public ColorProducer getHighlightColor() { return this.highlightColor; }
	
	/**
	 * Returns the highlight exponent (phong exponent) used by this HighlightShader object.
	 */
	public double getHighlightExponent() { return this.highlightExponent; }
	
	/**
	 * Returns an array of String objects with names for each editable property of this HighlightShader object.
	 */
	public String[] getPropertyNames() { return HighlightShader.propNames; }
	
	/**
	 * Returns an array of String objects with descriptions for each editable property of this HighlightShader object.
	 */
	public String[] getPropertyDescriptions() { return HighlightShader.propDesc; }
	
	/**
	 * Returns an array of Class objects representing the class types of each editable property of this HighlightShader object.
	 */
	public Class[] getPropertyTypes() { return HighlightShader.propTypes; }
	
	/**
	 * Returns the values of the properties of this HighlightShader object as an Object array.
	 */
	public Object[] getPropertyValues() {
		return new Object[] {this.highlightColor, new Double(this.highlightExponent)};
	}
	
	/**
	 * Sets the value of the property of this HighlightShader object at the specified index to the specified value.
	 * 
	 * @throws IllegalArgumentException  If the object specified is not of the correct type.
	 * @throws IndexOutOfBoundsException  If the index specified does not correspond to an editable property
	 *                                    of this HighlightShader object.
	 */
	public void setPropertyValue(Object value, int index) {
		if (index == 0) {
			if (value instanceof ColorProducer)
				this.setHighlightColor((ColorProducer)value);
			else
				throw new IllegalArgumentException("Illegal argument: " + value.toString());
		} else if (index == 1) {
			if (value instanceof Double)
				this.setHighlightExponent(((Double)value).doubleValue());
			else
				throw new IllegalArgumentException("Illegal argument: " + value.toString());
		} else {
			throw new IndexOutOfBoundsException("Index out of bounds: " + index);
		}
	}
	
	/**
	 * Sets the values of properties of this HighlightShader object to those specified.
	 * 
	 * @throws IllegalArgumentException  If one of the objects specified is not of the correct type.
	 *                                   (Note: none of the values after the erroneous value will be set)
	 * @throws IndexOutOfBoundsException  If the length of the specified array is longer than permitted.
	 */
	public void setPropertyValues(Object values[]) {
		for (int i = 0; i < values.length; i++) {
			this.setPropertyValue(values[i], i);
		}
	}
	
	/**
	 * @return  {highlight color}.
	 */
	public Producer[] getInputPropertyValues() { return new Producer[] {this.highlightColor}; }
	
	/**
	 * Sets the values of properties of this HighlightShader object to those specified.
	 * 
	 * @throws IllegalArgumentException  If the Producer object specified is not of the correct type.
	 * @throws IndexOutOfBoundsException  If the lindex != 0;
	 */
	public void setInputPropertyValue(int index, Producer p) {
		if (index == 0)
			this.setPropertyValue(p, 0);
		else
			throw new IndexOutOfBoundsException("Index out of bounds: " + index);
	}
	
	/**
	 * Returns "Highlight Shader".
	 */
	public String toString() { return "Highlight Shader"; }
}
