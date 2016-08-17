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

package com.almostrealism.raytracer.primitives;

import org.almostrealism.space.Intersection;
import org.almostrealism.space.Ray;
import org.almostrealism.space.TransformMatrix;
import org.almostrealism.space.Vector;
import org.almostrealism.texture.RGB;

import com.almostrealism.raytracer.engine.AbstractSurface;
import com.almostrealism.raytracer.engine.ParticleGroup;

/**
 * A Plane object represents an plane in 3d space.
 */
public class Plane extends AbstractSurface implements ParticleGroup {
  /** Integer code for XY plane. **/
  public static final int XY = 2;
  
  /** Integer code for XZ plane. **/
  public static final int XZ = 4;
  
  /** Integer code for YZ plane. **/
  public static final int YZ = 8;
  
  private int type;

	/**
	 * Constructs a Plane object that represents an XY plane that is black.
	 */
	public Plane() {
		super();
		this.setType(Plane.XY);
	}
	
	/**
	 * Constructs a Plane object that represents a Plane with the orientation specified by an integer code.
	 */
	public Plane(int type) {
		super();
		this.setType(type);
	}
	
	/**
	 * Constructs a Plane object that represents an XY plane with the specified color.
	 */
	public Plane(int type, RGB color) {
		super(new Vector(0.0, 0.0, 0.0), 1.0, color);
		this.setType(type);
	}
	
	/**
	 * Sets the orientation of this Plane object to the orientation specified by the integer type code.
	 * 
	 * @throws IllegalArgumentException  If the specified type code is not valid.
	 */
	public void setType(int type) {
		if (type == Plane.XY || type == Plane.XZ || type == Plane.YZ)
			this.type = type;
		else
			throw new IllegalArgumentException("Illegal type code: " + type);
	}
	
	/**
	 * Returns the integer code for the orientation of this Plane object.
	 */	
	public int getType() {
		return this.type;
	}
	
	/**
	 * Returns a Vector object that represents the vector normal to this plane at the point represented by the specified Vector object.
	 */
	public Vector getNormalAt(Vector point) {
		Vector normal = null;
		
		if (this.type == Plane.XY)
			normal = new Vector(0.0, 0.0, 1.0);
		else if (this.type == Plane.XZ)
			normal = new Vector(0.0, 1.0, 0.0);
		else if (this.type == Plane.YZ)
			normal = new Vector(1.0, 0.0, 0.0);
		else
			return null;
		
		super.getTransform(true).transform(normal, TransformMatrix.TRANSFORM_AS_NORMAL);
		
		return normal;
	}
	
	/**
	 * Returns true if the ray represented by the specified Ray object intersects the plane represented by this Plane object in real space.
	 */
	public boolean intersect(Ray ray) {
		ray.transform(this.getTransform(true).getInverse());
		
		Vector d = ray.getDirection();
		
		if (this.type == Plane.XY && d.getZ() == this.getLocation().getZ())
			return false;
		else if (this.type == Plane.XZ && d.getY() == this.getLocation().getY())
			return false;
		else if (this.type == Plane.YZ && d.getX() == this.getLocation().getX())
			return false;
		else
			return true;
	}
	
	/**
	 * Returns an Intersection object representing the points along the ray represented by the specified Ray object that intersection
	 * between the ray and the plane represented by this Plane object occurs.
	 */
	public Intersection intersectAt(Ray ray) {
		ray.transform(this.getTransform(true).getInverse());
		
		double t[] = new double[1];
		
		Vector o = ray.getOrigin();
		Vector d = ray.getDirection();
		
		if (this.type == Plane.XY)
			t[0] = (this.getLocation().getZ() - o.getZ()) / d.getZ();
		else if (this.type == Plane.XZ)
			t[0] = (this.getLocation().getY() - o.getY()) / d.getY();
		else if (this.type == Plane.YZ)
			t[0] = (this.getLocation().getX() - o.getX()) / d.getX();
		else
			return null;
		
		return new Intersection(ray, this, t);
	}

    /**
     * @see com.almostrealism.raytracer.engine.ParticleGroup#getParticleVertices()
     */
    public double[][] getParticleVertices() {
        if (this.type == Plane.XY) {
            return new double[][] {{10.0, 10.0, 0.0}, {10.0, -10.0, 0.0}, {-10.0, 10.0, 0.0}, {-10.0, -10.0, 0.0}};
        } else if (this.type == Plane.XZ) {
            return new double[][] {{10.0, 0.0, 10.0}, {10.0, 0.0, -10.0}, {-10.0, 0.0, 10.0}, {-10.0, 0.0, -10.0}};
        } else if (this.type == Plane.YZ) {
            return new double[][] {{0.0, 10.0, 10.0}, {0.0, 10.0, -10.0}, {0.0, -10.0, 10.0}, {0.0, -10.0, -10.0}};
        } else {
            return null;
        }
    }
}
