/*
 * Copyright (C) 2006  Mike Murray
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 */

package com.almostrealism.photonfield;

import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JPanel;

import com.almostrealism.io.FileEncoder;
import com.almostrealism.photonfield.geometry.Plane;
import com.almostrealism.photonfield.util.Fast;
import com.almostrealism.photonfield.util.PhysicalConstants;
import com.almostrealism.photonfield.util.VectorMath;

import net.sf.j3d.util.graphics.GraphicsConverter;
import net.sf.j3d.util.graphics.RGB;

/**
 * An AbsorptionPlane object represents a plane in space that absorbs photons
 * that are within a certain (somewhat small) distance from the surface of the
 * plane. This object can be used as a sort of simulated camera film, as it keeps
 * track of where it photon is absorbed and can be used to construct an image.
 * 
 * @author Mike Murray
 */
public class AbsorptionPlane extends Plane implements Absorber, Fast {
	public static double displayCoords = Math.pow(10.0, 4.0);
	public static double verbose = 0.0 * Math.pow(10.0, -7.0);
	
	private Clock clock;
	
	private int w, h;
	private double max = Math.pow(10.0, 0.0);
	private double pixel;
	private double energy[][];
	private RGB image[][];
	
	private boolean noDisplay;
	private JPanel display;
	
	/**
	 * @param p  The pixel size of the absorption plane (usually measured in micrometers).
	 */
	public void setPixelSize(double p) {
		this.pixel = p;
		super.setWidth(this.w * this.pixel);
		super.setHeight(this.h * this.pixel);
	}
	
	/**
	 * @return  The pixel size of the absorption plane (usually measured in micrometers).
	 */
	public double getPixelSize() { return this.pixel; }
	
	/**
	 * @param w  The width of the absorption plane measured as a number of cells. The size
	 *           of a cell is given by the getPixelSize method.
	 */
	public void setWidth(double w) { this.w = (int) w; super.setWidth(this.w * this.pixel); }
	
	/**
	 * @return  The width of the absorption plane measured as a number of cells. The size
	 *          of a cell is given by the getPixelSize method.
	 */
	public double getWidth() { return this.w; }
	
	/**
	 * @param h  The height of the absorption plane measured as a number of cells. The size
	 *           of a cell is given by the getPixelSize method.
	 */
	public void setHeight(double h) { this.h = (int) h; super.setHeight(this.h * this.pixel); }
	
	/**
	 * @return  The height of the absorption plane measured as a number of cells. The size
	 *          of a cell is given by the getPixelSize method.
	 */
	public double getHeight() { return this.h; }
	
	/**
	 * @param p  {x, y, z} - The vector normal to the absorption plane.
	 */
	public void setSurfaceNormal(double p[]) { this.normal = p;	this.across = null; }
	
	/**
	 * @return  {x, y, z} - The vector normal to the absorption plane.
	 */
	public double[] getSurfaceNormal() { return this.normal; }
	
	/**
	 * @param p  {x, y, z} - The vector pointing upwards across the surface of this
	 *           absorption plane. This vector must be orthagonal to the surface normal.
	 */
	public void setOrientation(double p[]) { this.up = p; this.across = null; }
	
	/**
	 * @return  {x, y, z} - The vector pointing upwards across the surface of this
	 *           absorption plane.
	 */
	public double[] getOrientation() { return this.up; }
	
	public void setAbsorbDelay(double t) { }
	
	public void setOrigPosition(double x[]) { }
	
	public boolean absorb(double x[], double p[], double energy) {
		double d = Math.abs(VectorMath.dot(x, this.normal));
		double r = 1.0;
		if (AbsorptionPlane.verbose > 0.0) r = Math.random();
		
		if (r < AbsorptionPlane.verbose)
			System.out.println("AbsorptionPlane: " + d);
		
		if (d > this.thick) return false;
		
		if (this.energy == null)
			this.energy = new double[this.w][this.h];
		
		if (this.across == null)
			this.across = VectorMath.cross(this.up, this.normal);
		
		if (this.image == null) {
			this.image = new RGB[this.w][this.h];
			
			if (!noDisplay)
				for (int i = 0; i < this.w; i++)
				for (int j = 0; j < this.h; j++)
					this.image[i][j] = new RGB(0.0, 0.0, 0.0);
		}
		
		double a = VectorMath.dot(x, this.across) / this.pixel;
		double b = VectorMath.dot(x, this.up) / this.pixel;
		a = (this.h / 2.0) - a;
		b = (this.w / 2.0) + b;
		
		if (r < AbsorptionPlane.displayCoords * AbsorptionPlane.verbose)
			System.out.println("AbsorptionPlane: " + a + ", " + b);
		
		if (a > 0.0 && b > 0.0 && a < this.w && b < this.h) {
			int i = (int) a;
			int j = (int) b;
			this.energy[i][j] += energy;
			
			double n = 1000 * PhysicalConstants.HC / energy;
			
			if (r < AbsorptionPlane.verbose)
				System.out.println("AbsorptionPlane: " + n + " nanometers.");
			
			if (this.image[i][j] == null)
				this.image[i][j] = new RGB(n);
			else
				this.image[i][j].addTo(new RGB(n));
		} else {
			return false;
		}
		
		if (!noDisplay) {
			if (this.display != null && this.display.getGraphics() != null) {
				Graphics g = this.display.getGraphics();
				this.drawImage(g);
			}
		}
		
		return true;
	}

	public double[] emit() { return null; }
	public double getEmitEnergy() { return 0; }
	public double[] getEmitPosition() { return null; }
	public double getNextEmit() { return Double.MAX_VALUE; }
	
	public void setClock(Clock c) { this.clock = c; }
	public Clock getClock() { return this.clock; }
	
	public void drawImage(Graphics g) {
		g.drawImage(GraphicsConverter.convertToAWTImage(
				this.getImage()), 0, 0, this.display);
	}
	
	public void writeImage(OutputStream out) throws IOException {
		if (this.energy == null) return;
		FileEncoder.writeImage(this.getImage(), out, FileEncoder.PPMEncoding);
	}
	
	public void saveImage(String file) throws IOException {
		if (this.energy == null) return;
		
		if (file.endsWith("ppm"))
			FileEncoder.encodeImageFile(this.getImage(), new File(file),
										FileEncoder.PPMEncoding);
		else
			FileEncoder.encodeImageFile(this.getImage(), new File(file),
										FileEncoder.JPEGEncoding);
	}
	
	public void enableDisplay() { this.noDisplay = false; }
	public void disableDisplay() { this.noDisplay = true; }
	
	public boolean imageAvailable() { return this.image != null; }
	
	public RGB[][] getImage() {
		if (this.image == null) return new RGB[1][0];
		return this.image;
	}
	
	public RGB[][] getEnergyMap() {
		if (this.energy == null) return new RGB[1][0];
		
		RGB image[][] = new RGB[this.energy.length][this.energy[0].length];
		
		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				double value = this.energy[i][j] / this.max;
				image[i][j] = new RGB(value, value, value);
			}
		}
		
		return image;
	}
	
	public JPanel getDisplay() {
		if (this.display != null) return this.display;
		
		this.display = new JPanel() {
			public void paint(Graphics g) {
				AbsorptionPlane.this.drawImage(g);
			}
		};
		
		return this.display;
	}
}
