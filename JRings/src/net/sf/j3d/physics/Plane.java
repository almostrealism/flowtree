/*
 * Copyright (C) 2004-06  Mike Murray
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 *
 */

package net.sf.j3d.physics;

import java.awt.Graphics;

import com.almostrealism.raytracer.camera.Camera;
import net.sf.j3d.util.TransformMatrix;
import net.sf.j3d.util.Vector;


/**
 * @author Mike Murray
 */
public class Plane extends com.almostrealism.raytracer.primitives.Plane implements RigidBody {
	private State state;
	
	private TransformMatrix rotateXMatrix, rotateYMatrix, rotateZMatrix;
	
	/**
	 * Constructs a new Plane object using the specified initial rigid body state values.
	 * 
	 * @param x  location
	 * @param r  rotation
	 * @param v  linear velocity
	 * @param w  angular velocity
	 * @param f  force
	 * @param t  torque
	 * @param mass  mass
	 * @param e  coefficient of restitution
	 */
	public Plane(Vector x, Vector r, Vector v, Vector w, Vector f, Vector t, double mass, double e) {
		super(com.almostrealism.raytracer.primitives.Plane.XZ);
		
		this.state = new State();
		this.state.init(x, r, v, w, f, t, mass, new TransformMatrix(new double[][] {{1.0, 0.0, 0.0, 0.0},
				{0.0, 1.0, 0.0, 0.0},
				{0.0, 0.0, 1.0, 0.0},
				{0.0, 0.0, 0.0, 1.0}}), e);
	}
	
	// public void angularImpulse(Vector impulse) {
	//    System.out.println("Angular impulse on plane.");
	//}
	
	/**
	 * @see net.sf.j3d.physics.RigidBody#intersect(net.sf.j3d.physics.RigidBody)
	 */
	public Vector[] intersect(RigidBody b) {
		if (b instanceof Sphere) {
			State s = ((Sphere)b).getState();
			double d = this.state.x.getY() - s.x.getY();
			
			if (Math.abs(d) <= ((Sphere)b).getRadius()) {
				if (d >= 0)
					return new Vector[] {new Vector(s.x.getX(), s.x.getY(), s.x.getZ()), new Vector(0.0, -1.0, 0.0)};
				else
					return new Vector[] {new Vector(s.x.getX(), this.state.x.getY(), s.x.getZ()), new Vector(0.0, 1.0, 0.0)};
			} else {
				return new Vector[0];
			}
		} else {
			return new Vector[0];
		}
	}
	
	/**
	 * @see net.sf.j3d.physics.RigidBody#draw(threeD.raytracer.engine.Camera, java.awt.Graphics, double, double, double)
	 */
	public void draw(Camera c, Graphics g, double ox, double oy, double scale) {
	}
	
	/**
	 * @see net.sf.j3d.physics.RigidBody#updateModel()
	 */
	public void updateModel() {
		// Vector rn = super.r.divide(super.r.length());
		
		// this.rotateXMatrix = TransformMatrix.createRotateXMatrix(Math.acos(rn.getX()));
		// this.rotateYMatrix = TransformMatrix.createRotateYMatrix(Math.acos(rn.getY()));
		// this.rotateZMatrix = TransformMatrix.createRotateZMatrix(Math.acos(rn.getZ()));
		
		super.setLocation(this.state.x);
	}
	
	public State getState() { return this.state; }
}
