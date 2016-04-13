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

package net.sf.j3d.raytracer.engine;

import net.sf.j3d.raytracer.shaders.Shader;
import net.sf.j3d.raytracer.shaders.ShaderParameters;
import net.sf.j3d.util.Vector;
import net.sf.j3d.util.graphics.RGB;

/**
  The Surface interface is implemented by any 3d object which may be intersected by a 3d ray.
  These objects must supply methods for calculating ray-surface intersections.
*/

public interface Surface {
	/**
	  Returns true if the front side of this Surface object should be shaded.
	  The "front side" is the side that the Vector object returned by the getNormalAt()
	  method for this Surface object points outward from.
	*/
	
	public boolean getShadeFront();
	
	/**
	  Returns true if the back side of this Surface object should be shaded.
	  The "back side" is the side that the vector opposite the Vector object
	  returned by the getNormalAt() method for this Surface object points outward from.
	*/
	
	public boolean getShadeBack();
	
	/**
	  Returns the color of this Surface object at the specified point as an RGB object.
	*/
	
	public RGB getColorAt(Vector point);
	
	/**
	  Returns a Vector object that represents the vector normal to the 3d surface at the point
	  represented by the specified Vector object. 
	*/
	
	public Vector getNormalAt(Vector point);
	
	/**
	  Returns true if the ray intersects the 3d surface in real space.
	*/
	
	public boolean intersect(Ray ray);
	
	/**
	  Returns an Intersection object that represents the values for t that solve the vector equation p = o + t * d
	  where p is a point of intersection of the specified ray and the surface.
	*/
	
	public Intersection intersectAt(Ray ray);
	
	/**
	  Returns an RGB object representing the color of this surface at the specified point
	  based on the specified parameters.
	  
	  @see Shader
	*/
	
	public RGB shade(ShaderParameters parameters);
}
