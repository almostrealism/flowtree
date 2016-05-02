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

package com.almostrealism.raytracer.primitives;

import com.almostrealism.raytracer.engine.AbstractSurface;
import com.almostrealism.raytracer.engine.Intersection;
import com.almostrealism.raytracer.engine.Ray;

import net.sf.j3d.util.Vector;

/**
  A Polynomial object represents a 3d polynomial surface.
*/

public class Polynomial extends AbstractSurface {
  private static double maxIntersectionDistance = 100.0;
  private static double defaultZerosInterval = 0.5;
  private static int defaultZerosRecursions = 4;
  
  private PolynomialTerm terms[];
  
  private Polynomial dx;
  private Polynomial dy;
  private Polynomial dz;

	/**
	  Constructs a new Polynomial object with no terms.
	*/
	
	public Polynomial() {
		this.setTerms(new PolynomialTerm[0]);
	}
	
	/**
	  Constructs a new Polynomial object with the specified terms.
	*/
	
	public Polynomial(PolynomialTerm terms[]) {
		this.setTerms(terms);
	}
	
	/**
	  Sets the terms of this Polynomial object to those specified.
	*/
	
	public void setTerms(PolynomialTerm terms[]) {
		this.terms = terms;
	}
	
	/**
	  Adds the specified PolynomialTerm object to this Polynomial object.
	*/
	
	public void addTerm(PolynomialTerm term) {
		PolynomialTerm newTerms[] = new PolynomialTerm[this.terms.length + 1];
		
		System.arraycopy(this.terms, 0, newTerms, 0, this.terms.length);
		newTerms[newTerms.length - 1] = term;
		
		this.setTerms(newTerms);
	}
	
	/**
	  Removes the PolynomialTerm object stored at the specified index from this Polynomial object.
	*/
	
	public void removeTerm(int index) {
		PolynomialTerm newTerms[] = new PolynomialTerm[this.terms.length - 1];
		
		System.arraycopy(this.terms, 0, newTerms, 0, index);
		if (index != this.terms.length - 1) {
			System.arraycopy(this.terms, index + 1, newTerms, index, this.terms.length - (index + 1));
		}
		
		this.setTerms(newTerms);
	}
	
	/**
	  Returns the terms of this Polynomial object as an array of PolynomialTerm objects.
	*/
	
	public PolynomialTerm[] getTerms() {
		return this.terms;
	}
	
	/**
	  Scans this Polynomial object for like terms and combines them and calculates the terms of the new partial derivatives.
	*/
	
	public void simplify() {
		java.util.Vector newTermsVector = new java.util.Vector();
		
		i: for(int i = 0; i < this.terms.length; i++) {
			if (this.terms[i].getCoefficient() == 0.0)
				continue i;
			
			for(int j = 0; j < newTermsVector.size(); j++) {
				PolynomialTerm aTerm = (PolynomialTerm)newTermsVector.elementAt(j);
				
				if (aTerm.isLikeTerm(this.terms[i])) {
					aTerm.setCoefficient(aTerm.getCoefficient() + this.terms[i].getCoefficient());
					
					continue i;
				}
			}
			
			newTermsVector.addElement(this.terms[i]);
		}
		
		PolynomialTerm newTerms[] = new PolynomialTerm[newTermsVector.size()];
		
		for(int i = 0; i < newTerms.length; i++) {
			newTerms[i] = (PolynomialTerm)newTermsVector.elementAt(i);
		}
		
		this.setTerms(newTerms);
	}
	
	/**
	  Combines the terms of this Polynomial object and the specified Polynomial object
	  and returns the sum as a Polynomial object.
	*/
	
	public Polynomial add(Polynomial polynomial) {
		PolynomialTerm sumTerms[] = new PolynomialTerm[this.terms.length + polynomial.getTerms().length];
		
		System.arraycopy(this.terms, 0, sumTerms, 0, this.terms.length);
		System.arraycopy(polynomial.getTerms(), 0, sumTerms, this.terms.length, polynomial.getTerms().length);
		
		return new Polynomial(sumTerms);
	}
	
	/**
	  Calculates the product of the polynomial function represented by this Polynomial object
	  and that of the specified Polynomial object and returns the result as a Polynomial object.
	*/
	
	public Polynomial multiply(Polynomial polynomial) {
		Polynomial productPolynomials[] = new Polynomial[polynomial.getTerms().length];
		
		for(int i = 0; i < polynomial.getTerms().length; i++) {
			productPolynomials[i] = this.multiply(polynomial.getTerms()[i]);
		}
		
		Polynomial product = new Polynomial();
		
		for(int i = 0; i < productPolynomials.length; i++) {
			product = product.add(productPolynomials[i]);
		}
		
		return product;
	}
	
	/**
	  Calculates the product of the polynomial function represented by this Polynomial object
	  and that of the specified PolynomialTerm object and returns the result as a Polynomial object.
	*/
	
	public Polynomial multiply(PolynomialTerm term) {
		PolynomialTerm productTerms[] = new PolynomialTerm[this.terms.length];
		
		for(int i = 0; i < this.terms.length; i++) {
			productTerms[i] = this.terms[i].multiply(term);
		}
		
		return new Polynomial(productTerms);
	}
	
	/**
	  Multiplies this Polynomial object with itself as many times as indicated by the specified integer value.
	  If power is less than 0, null is returned. The polynomial is simplified after each step.
	*/
	
	public Polynomial expand(int power) {
		if (power < 0) {
			return null;
		} else if (power == 0) {
			Polynomial p = new Polynomial();
			p.addTerm(new PolynomialTerm(1.0, 0, 0, 0));
			
			return p;
		} else if (power == 1) {
			return this;
		}
		
		Polynomial polynomial = this.multiply(this);
		polynomial.simplify();
		
		return polynomial.expand(power - 1);
	}
	
	/**
	  Evaluates the polynomial function represented by this Polynomial object for the specified values.
	*/
	
	public double evaluate(double x, double y, double z) {
		double value = 0;
		
		for(int i = 0; i < this.terms.length; i++) {
			if (this.terms[i] != null)
				value += this.terms[i].evaluate(x, y, z);
		}
		
		return value;
	}
	
	/**
	  Evaluates the gradient of the polynomial funtion represented by this Polynomial object for the specified values.
	*/
	
	public Vector evaluateGradient(double x, double y, double z) {
		Vector gradient = new Vector(this.getDx().evaluate(x, y, z),
						this.getDy().evaluate(x, y, z),
						this.getDz().evaluate(x, y, z));
		
		return gradient;
	}
	
	/**
	  Calculates a series of zeros found by the calculateZero method in the interval from the double value start to the double value end
	  choosing guesses at every interval of the length specified by the double value increment. The Y and Z values and the number of recursions
	  must also be specified.
	*/
	
	public double[] calculateZeros(double start, double end, double increment, double yValue, double zValue, int recursions) {
		java.util.Vector zerosVector = new java.util.Vector();
		
		for(int i = 0; (start + i * increment) < end; i++) {
			if (this.evaluate((start + i * increment), yValue, zValue) * this.evaluate((start + (i + 1) * increment), yValue, zValue) < 0) {
				zerosVector.addElement(new Double(this.calculateZero(start + (i + 0.5) * increment, yValue, zValue, recursions)));
			}
		}
		
		double zeros[] = new double[zerosVector.size()];
		
		for(int i = 0; i < zeros.length; i++) {
			zeros[i] = ((Double)zerosVector.elementAt(i)).doubleValue();
		}
		
		return zeros;
	}
	
	/**
	  Calculates an estimate for an X value that allows the polynomial function represented by this Polynomial object
	  to equal zero when the specified Y and Z values are held constant. A recursive method is used and the number
	  of recursions may be specified (3 or 4 usually works well).
	*/
	
	public double calculateZero(double initialValue, double yValue, double zValue, int recursions) {
		if (recursions <= 0)
			return initialValue;
		else
			return this.calculateZero(initialValue - (this.evaluate(initialValue, yValue, zValue) / this.getDx().evaluate(initialValue, yValue, zValue)),
							yValue, zValue, recursions - 1);
	}
	
	/**
	  Returns a Polynomial object that represents the partial derivative of the polynomial function represented by this Polynomial object
	  with respect to X.
	*/
	
	public Polynomial getDx() {
		if (this.dx == null)
			this.calculateDx();
		
		return this.dx;
	}
	
	/**
	  Returns a Polynomial object that represents the partial derivative of the polynomial function represented by this Polynomial object
	  with respect to Y.
	*/
	
	public Polynomial getDy() {
		if (this.dy == null)
			this.calculateDy();
		
		return this.dy;
	}
	
	/**
	  Returns a Polynomial object that represents the partial derivative of the polynomial function represented by this Polynomial object
	  with respect to Z.
	*/
	
	public Polynomial getDz() {
		if (this.dz == null)
			this.calculateDz();
		
		return this.dz;
	}
	
	/**
	  Calculates the terms of the partial derivative of the polynomial function represented by this Polynomial object
	  with respect to X and stores the terms for later evaluatation.
	*/
	
	public void calculateDx() {
		PolynomialTerm dxTerms[] = new PolynomialTerm[this.terms.length];
		
		for(int i = 0; i < this.terms.length; i++) {
			dxTerms[i] = this.terms[i].getDx();
		}
		
		this.dx = new Polynomial(dxTerms);
		this.dx.simplify();
	}
	
	/**
	  Calculates the terms of the partial derivative of the polynomial function represented by this Polynomial object
	  with respect to Y and stores the terms for later evaluatation.
	*/
	
	public void calculateDy() {
		PolynomialTerm dyTerms[] = new PolynomialTerm[this.terms.length];
		
		for(int i = 0; i < this.terms.length; i++) {
			dyTerms[i] = this.terms[i].getDy();
		}
		
		this.dy = new Polynomial(dyTerms);
		this.dy.simplify();
	}
	
	/**
	  Calculates the terms of the partial derivative of the polynomial function represented by this Polynomial object
	  with respect to Z and stores the terms for later evaluatation.
	*/
	
	public void calculateDz() {
		PolynomialTerm dzTerms[] = new PolynomialTerm[this.terms.length];
		
		for(int i = 0; i < this.terms.length; i++) {
			dzTerms[i] = this.terms[i].getDz();
		}
		
		this.dz = new Polynomial(dzTerms);
		this.dz.simplify();
	}
	
	/**
	  Returns a Vector object that represents the vector normal to this polynomial surface at the point represented by the specified Vector object.
	*/
	
	public Vector getNormalAt(Vector point) {
		return this.evaluateGradient(point.getX(), point.getY(), point.getZ());
	}
	
	/**
	  Returns true if the ray represented by the specified Ray object intersects the polynomial surface represented by this Polynomial object in real space.
	*/
	
	public boolean intersect(Ray ray) {
		if (this.intersectAt(ray).getIntersections().length > 0)
			return true;
		else
			return false;
	}
	
	/**
	  Returns an array of double values representing the distance along the ray represented by the specified Ray object that intersection
	  between the ray and the polynomial surface represented by this Polynomial object occurs.
	*/
	
	public Intersection intersectAt(Ray ray) {
		ray.transform(this.getTransform(true).getInverse());
		
		Vector o = ray.getOrigin();
		
		PolynomialTerm pxTerms[] = {new PolynomialTerm(ray.getDirection().getX(), 1, 0, 0),
						new PolynomialTerm(o.getX(), 0, 0, 0)};
		PolynomialTerm pyTerms[] = {new PolynomialTerm(ray.getDirection().getY(), 1, 0, 0),
						new PolynomialTerm(o.getY(), 0, 0, 0)};
		PolynomialTerm pzTerms[] = {new PolynomialTerm(ray.getDirection().getZ(), 1, 0, 0),
						new PolynomialTerm(o.getZ(), 0, 0, 0)};
		
		Polynomial px = new Polynomial(pxTerms);
		Polynomial py = new Polynomial(pyTerms);
		Polynomial pz = new Polynomial(pzTerms);
		
		px.simplify();
		py.simplify();
		pz.simplify();
		
		java.util.Vector pTopVector = new java.util.Vector();
		java.util.Vector pBottomVector = new java.util.Vector();
		
		i: for(int i = 0; i < this.getTerms().length; i++) {
			PolynomialTerm term = this.getTerms()[i];
			
			if (term.getCoefficient() == 0)
				continue i;
			
			Polynomial top = new Polynomial();
			top.addTerm(new PolynomialTerm(term.getCoefficient(), 0, 0, 0));
			
			Polynomial bottom = new Polynomial();
			bottom.addTerm(new PolynomialTerm(1.0, 0, 0, 0));
			
			if (term.getExpOfX() > 0) {
				Polynomial newP = px.expand(term.getExpOfX());
				newP.simplify();
				
				top = top.multiply(newP);
			} else if (term.getExpOfX() < 0) {
				Polynomial newP = px.expand(-term.getExpOfX());
				newP.simplify();
				
				bottom = bottom.multiply(newP);
			}
			
			if (term.getExpOfY() > 0) {
				Polynomial newP = py.expand(term.getExpOfY());
				newP.simplify();
				
				top = top.multiply(newP);
			} else if (term.getExpOfY() < 0) {
				Polynomial newP = py.expand(-term.getExpOfY());
				newP.simplify();
				
				bottom = bottom.multiply(newP);
			}
			
			if (term.getExpOfZ() > 0) {
				Polynomial newP = pz.expand(term.getExpOfZ());
				newP.simplify();
				
				top = top.multiply(newP);
			} else if (term.getExpOfZ() < 0) {
				Polynomial newP = pz.expand(-term.getExpOfZ());
				newP.simplify();
				
				bottom = bottom.multiply(newP);
			}
			
			pTopVector.addElement(top);
			pBottomVector.addElement(bottom);
		}
		
		Polynomial p = new Polynomial();
		
		for(int i = 0; i < pTopVector.size(); i++) {
			Polynomial newP = (Polynomial)pTopVector.elementAt(i);
			
			for(int j = 0; j < pBottomVector.size(); j++) {
				if (j != i)
					newP = newP.multiply((Polynomial)pBottomVector.elementAt(j));
			}
			
			p = p.add(newP);
		}
		
		p.simplify();
		
//		System.out.println("Polynomial = " + p.toString());
		
		double zeros[] = p.calculateZeros(0, Polynomial.maxIntersectionDistance, Polynomial.defaultZerosInterval, 0.0, 0.0, Polynomial.defaultZerosRecursions);
		
//		System.out.print("Zeros = {");
//		
//		for(int i = 0; i < zeros.length; i++) {
//			System.out.print(zeros[i]);
//			
//			if (i < zeros.length - 1)
//				System.out.print(", ");
//		}
//		
//		System.out.println("}");
		
		return new Intersection(ray, this, zeros);
	}
	
	/**
	  Returns a String representation of this Polynomial object.
	*/
	
	public String toString() {
		String output = null;
		
		for(int i = 0; i < this.getTerms().length; i++) {
			if (output == null)
				output = this.getTerms()[i].toString();
			else
				output = output + this.getTerms()[i].toString();
			
			if (i < this.getTerms().length - 1)
				output = output + " + ";
		}
		
		return output == null ? "" : output;
	}
}