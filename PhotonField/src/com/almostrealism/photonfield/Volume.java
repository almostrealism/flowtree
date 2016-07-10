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
 * Copyright (C) 2006  Mike Murray
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 */

package com.almostrealism.photonfield;

/**
 * A Volume object represents a volume of space in three dimensions.
 * In combination with an absorber implementaion a Volume instance defines
 * a solid object.
 * 
 * @author Mike Murray
 */
public interface Volume {
	/**
	 * Checks if a point is within this volume.
	 * 
	 * @param x  {x, y, z} - The point in space to test.
	 * @return  True if the point is within this volume, false otherwise.
	 */
	public boolean inside(double x[]);
	
	/**
	 * Calculates the vector normal to the volume at the specfied point.
	 * 
	 * @param x  {x, y, z} - The point in space to calculate the normal.
	 * @return  {x, y, z} - The vector normal to the surface of the volume.
	 */
	public double[] getNormal(double x[]);
	
	/**
	 * Calculates the distance along the line defined by the specified position
	 * and direction vectors that the line intersects with this Volume. This is
	 * a maximum distance that the volume can garuentee that intersection does
	 * not occur. If intersection cannot be caclualated, zero should be returned.
	 * 
	 * @param p  The position.
	 * @param d  The direction.
	 * @return  The distance before intersection occurs.
	 */
	public double intersect(double p[], double d[]);
	
	/**
	 * Returns 2D coordinates on the surface of this volume at the specified point
	 * in 3D.
	 * 
	 * @param xyz  {x, y, z} - Position in spatial coordinates.
	 * @return  {u, v} - Position in surface coordinates (u,v between 0.0 and 1.0).
	 */
	public double[] getSurfaceCoords(double xyz[]);
	
	/**
	 * Returns 3D coordinates on the surface of this volume at the specified point
	 * in 2D surface coordinates.
	 * 
	 * @param uv  {u, v} - Position in surface coordinates (u,v between 0.0 and 1.0).
	 * @return  {x, y, z} - Position in spatial coordinates.
	 */
	public double[] getSpatialCoords(double uv[]);
}
