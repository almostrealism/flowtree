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

package com.almostrealism.raytracer.camera;

import com.almostrealism.raytracer.Settings;
import com.almostrealism.raytracer.engine.Ray;
import com.almostrealism.util.TransformMatrix;
import com.almostrealism.util.Vector;

/**
 * The OrthographicCamera class provides an orthographic porjection camera.
 * The camera location is, as expected, the location from which the camera views, represented as a vector.
 * This value is by default at the origin. The viewing direction is a vector that represents
 * the direction the camera is viewing. This value is by default aligned to the positive Z axis,
 * or (0.0, 0.0, 1.0). The up direction is a vector that represents the orientation of the camera's "up."
 * This value is by default aligned with the positive y axis or (0.0, 1.0, 0.0).
 * The projection dimensions of the camera are the dimensions of the viewing plane.
 * 
 * @author Mike Murray
 */
public class OrthographicCamera implements Camera {
  private Vector location = new Vector(0.0, 0.0, 0.0);
  private Vector viewDirection = new Vector(0.0, 0.0, 1.0);
  private Vector upDirection = new Vector(0.0, 1.0, 0.0);
  private double projectionX, projectionY;
  
  protected Vector u, v, w;
  
	/**
	 * Constructs a new OrthographicCamera object with the defaults described above.
	 */
	public OrthographicCamera() {
		this.setLocation(new Vector(0.0, 0.0, 0.0));
		this.setViewingDirection(new Vector(0.0, 0.0, 1.0));
		this.setUpDirection(new Vector(0.0, 1.0, 0.0));
		
		this.setProjectionDimensions(3.5, 2.0);
	}
	
	/**
	 * Constructs an OrthographicCamera object with the specified location, viewing direction,
	 * and up direction, but with default projection dimensions as specified above.
	 * 
	 * @param location  Camera location.
	 * @param viewDirection  Camera viewing direction.
	 * @param upDirection  Camera up direction.
	 */
	public OrthographicCamera(Vector location, Vector viewDirection, Vector upDirection) {
		this.setLocation(location);
		this.setViewingDirection(viewDirection);
		this.setUpDirection(upDirection);
		
		this.setProjectionDimensions(3.5, 2.0);
	}
	
	public OrthographicCamera(Vector location, Vector viewDirection, Vector upDirection,
							double projectionX, double projectionY) {
		this.setLocation(location);
		this.setViewingDirection(viewDirection);
		this.setUpDirection(upDirection);
		
		this.setProjectionDimensions(projectionX, projectionY);
	}
	
	/**
	 * Sets the location of this OrthographicCamera object to the specified location.
	 */
	public void setLocation(Vector location) { this.location = location; }
	
	/**
	 * Calls the setViewingDirection() method.
	 */
	public void setViewDirection(Vector viewDirection) { this.setViewingDirection(viewDirection); }
	
	/**
	 * Sets the viewing direction of this PinholeCamera object to the specified viewing direction.
	 * This method automatically updates the camera coordinate system vectors.
	 */
	public void setViewingDirection(Vector viewDirection) {
		if (Settings.produceOutput && Settings.produceCameraOutput) {
			Settings.cameraOut.println("CAMERA (" + this.toString() + "): Viewing direction vector being set to " + viewDirection.toString());
		}
		
		this.viewDirection = viewDirection;
		
		this.updateUVW();
	}
	
	/**
	 * Sets the up direction of this PinholeCamera object to the specified up direction.
	 * This method automatically updates the camera coordinate system vectors.
	 */
	public void setUpDirection(Vector upDirection) {
		if (Settings.produceOutput && Settings.produceCameraOutput) {
			Settings.cameraOut.println("CAMERA: Up direction vector being set to " + upDirection.toString());
		}
		
		this.upDirection = upDirection;
		
		this.updateUVW();
	}
	
	/**
	 * Sets the projection dimensions to the specified projection dimensions.
	 */
	public void setProjectionDimensions(double projectionX, double projectionY) {
		this.projectionX = projectionX;
		this.projectionY = projectionY;
	}
	
	/**
	 * Sets the projection width of this OrthographicCamera object to the specified projection width.
	 */
	public void setProjectionWidth(double projectionX) {
		if (Settings.produceOutput && Settings.produceCameraOutput) {
			Settings.cameraOut.println("CAMERA: Projection width being set to " + projectionX);
		}
		
		this.projectionX = projectionX;
	}
	
	/**
	 * Sets the projection height of this OrthographicCamera object to the specified projection height.
	 */
	public void setProjectionHeight(double projectionY) {
		if (Settings.produceOutput && Settings.produceCameraOutput) {
			Settings.cameraOut.println("CAMERA: Projection height being set to " + projectionY);
		}
		
		this.projectionY = projectionY;
	}
	
	/**
	 * Updates the orthonormal vectors used to describe camera space for this OrthographicCamera object.
	 */
	public void updateUVW() {
		this.w = (this.viewDirection.divide(this.viewDirection.length())).minus();
		
		this.u = this.upDirection.crossProduct(this.w);
		this.u.divideBy(this.u.length());
		
		this.v = this.w.crossProduct(this.u);
		
		if (Settings.produceCameraOutput) {
			Settings.cameraOut.println("CAMERA: U = " + this.u.toString() + ", V = " + this.v.toString() + ", W = " + this.w.toString());
		}
	}
	
	/**
	 * Returns the location of this PinholeCamera object as a Vector object.
	 */
	public Vector getLocation() { return this.location; }
	
	/**
	 * Calls the getViewingDirection() method and returns the result.
	 */
	public Vector getViewDirection() { return this.getViewingDirection(); }
	
	/**
	 * Returns the viewing direction of this PinholeCamera object as a Vector object.
	 */
	public Vector getViewingDirection() { return this.viewDirection; }
	
	/**
	 * Returns the up direction of this PinholeCamera object as a Vector object.
	 */
	public Vector getUpDirection() { return this.upDirection; }
	
	/**
	 * Returns the projection width of this PinholeCamera object as a double value.
	 */
	public double getProjectionWidth() { return this.projectionX; }
	
	/**
	 * Returns the projection height of this PinholeCamera object as a double value.
	 */
	public double getProjectionHeight() { return this.projectionY; }
	
	/**
	 * @return  A TransformMatrix object that can be used to convert coordinates in the coordinate system described by this Camera object
	 * 			to the standard x, y, z coordinates.
	 */
	public TransformMatrix getRotationMatrix() {
	    double matrix[][] = {{this.u.getX(), this.u.getY(), this.u.getZ()},
	            				{this.v.getX(), this.v.getY(), this.v.getZ()},
	            				{this.w.getX(), this.w.getY(), this.w.getZ()}};
	    
	    return new TransformMatrix(matrix);
	}
	
	/**
	 * @see com.almostrealism.raytracer.camera.Camera#rayAt(double, double, int, int)
	 */
	public Ray rayAt(double i, double j, int screenWidth, int screenHeight) {
		double x = this.projectionX * ((i / screenWidth) - 0.5);
		double y = this.projectionY * ((j / screenHeight) - 0.5);
		
		Vector o = new Vector(x, y, 0.0);
		this.getRotationMatrix().getInverse().transform(o, TransformMatrix.TRANSFORM_AS_LOCATION);
		
		return new Ray(o, this.viewDirection);
	}

}
