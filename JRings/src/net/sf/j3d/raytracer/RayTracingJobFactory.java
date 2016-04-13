/*
 * Copyright (C) 2005-06  Mike Murray
 * 
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 *
 */

package net.sf.j3d.raytracer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sf.j3d.network.Job;
import net.sf.j3d.network.JobFactory;
import net.sf.j3d.network.Server;
import net.sf.j3d.network.db.Client;
import net.sf.j3d.network.db.Query;

/**
 * @author Mike Murray
 */
public class RayTracingJobFactory implements JobFactory {
  private String uri, sLoader;
  private double pri = 1.0;
  private int dx = 1, dy = 1;
  private int i;
  private int width, height, ssWidth, ssHeight, jobSize;
  private double pw = -1.0, ph = -1.0;
  private double fl = -1.0;
  private double clx, cly, clz;
  private double cdx, cdy, cdz;
  private long taskId;
  private int totalJobs, nullCount;
  
  private List jobs;

	/**
	 * Constructs a new RayTracingJobFactory object.
	 */
	public RayTracingJobFactory() { this.jobs = new ArrayList(); }
	
	/**
	 * Constructs a new RayTracingJobFactory object using the specified parameters.
	 * 
	 * @param uri  URI of scene to be rendered.
	 * @param width  Image width.
	 * @param height  Image height.
	 * @param ssWidth  Super sample width.
	 * @param ssHeight  Super sample height.
	 * @param jobSize  Job size.
	 */
	public RayTracingJobFactory(String uri, int width, int height,
								int ssWidth, int ssHeight, int jobSize,
								long taskId) {
		this.uri = uri;
		this.width = width;
		this.height = height;
		this.ssWidth = ssWidth;
		this.ssHeight = ssHeight;
		this.jobSize = jobSize;
		this.taskId = taskId;
		
		int l = (int)Math.ceil(Math.sqrt(jobSize));
		
		this.dx = l;
		this.dy = l;
		
		this.totalJobs = ((this.width * this.height) / (this.dx * this.dy));
		
		this.jobs = new ArrayList();
	}
	
	/**
	 * Constructs a new RayTracingJobFactory object using the specified parameters.
	 * 
	 * @param uri  URI of scene to be rendered.
	 * @param loader  Class name of scene loader.
	 * @param width  Image width.
	 * @param height  Image height.
	 * @param ssWidth  Super sample width.
	 * @param ssHeight  Super sample height.
	 * @param jobSize  Job size.
	 */
	public RayTracingJobFactory(String uri, String loader, int width, int height, int ssWidth, int ssHeight, int jobSize, long taskId) {
		this.uri = uri;
		this.width = width;
		this.height = height;
		this.ssWidth = ssWidth;
		this.ssHeight = ssHeight;
		this.jobSize = jobSize;
		this.taskId = taskId;
		
		int l = (int)Math.ceil(Math.sqrt(jobSize));
		
		this.dx = l;
		this.dy = l;
		
		this.totalJobs = ((this.width * this.height) / (this.dx * this.dy));
		
		this.jobs = new ArrayList();
	}
	
	public long getTaskId() { return this.taskId; }
	
	public void setSceneLoader(String loader) { this.sLoader = loader; }
	
	public void setProjectionWidth(double pw) { this.pw = pw; }
	public void setProjectionHeight(double ph) { this.ph = ph; }
	public void setFocalLength(double fl) { this.fl = fl; }
	
	public void setCameraLocation(double x, double y, double z) {
		this.clx = x;
		this.cly = y;
		this.clz = z;
	}
	
	public void setCameraDirection(double x, double y, double z) {
		this.cdx = x;
		this.cdy = y;
		this.cdz = z;
	}
	
	/**
	 * @see net.sf.j3d.network.JobFactory#nextJob()
	 */
	public Job nextJob() {
		if (i >= this.totalJobs) {
			if (this.jobs.size() > 0) return (Job)this.jobs.remove(0);
			
			if (this.nullCount > 300 && this.nullCount % 10 == 0) {
				Client c = Client.getCurrentClient();
				if (c == null) return null;
				
				Hashtable h = c.sendQuery(new Query("image-" + this.taskId, "", "", this.width + "x" + this.height));
				
				if (h != null) {
					System.out.println("RayTracingJobFactory: Query returned " + h.size() + " entries.");
					
					Iterator itr = h.values().iterator();
					
					while (itr.hasNext()) {
						String s = (String)itr.next();
						int index = s.indexOf(":");
						
						int x = Integer.parseInt(s.substring(0, index));
						int y = Integer.parseInt(s.substring(index + 1));
						
						RayTracingJob j = new RayTracingJob(this.uri, x, y, 1, 1,
															this.width, this.height,
															this.ssWidth, this.ssHeight,
															this.taskId);
						
						this.jobs.add(j);
					}
				}
				
				if (this.jobs.size() > 0) {
					return (Job)this.jobs.remove(0);
				} else {
					this.nullCount++;
					return null;
				}
			} else {
				this.nullCount++;
				return null;
			}
		}
		
		int l = this.i * this.dx;
		int y = (l / this.width) * this.dx;
		int x = l % this.width;
		
		int dx = this.dx;
		int dy = this.dy;
		
		if (this.width - x < dx) dx = this.width - x;
		if (this.height - y < dy) dy = this.height - y;
		
		RayTracingJob j = new RayTracingJob(this.uri, x, y, dx, dy,
												this.width, this.height,
												this.ssWidth, this.ssHeight,
												this.taskId);
		
		if (this.sLoader != null) j.setSceneLoader(this.sLoader);
		
		j.set("pw", String.valueOf(this.pw));
		j.set("ph", String.valueOf(this.ph));
		j.set("fl", String.valueOf(this.fl));
		j.set("clx", String.valueOf(this.clx));
		j.set("cly", String.valueOf(this.cly));
		j.set("clz", String.valueOf(this.clz));
		j.set("cdx", String.valueOf(this.cdx));
		j.set("cdy", String.valueOf(this.cdy));
		j.set("cdz", String.valueOf(this.cdz));
		
		this.i++;
		
		return j;
	}

	/**
	 * @see net.sf.j3d.network.JobFactory#createJob(java.lang.String)
	 */
	public Job createJob(String data) {
		Client c = Client.getCurrentClient();
		
		if (c != null && c.getServer() != null)
			return c.getServer().createJob(data);
		else
			return Server.instantiateJobClass(data);
	}
	
	/**
	 * @return  A String encoding of this RayTracingJobFactory object.
	 */
	public String encode() {
		StringBuffer buf = new StringBuffer();
		
		buf.append(this.getClass().getName());
		buf.append(":uri=");
		buf.append(this.uri);
		
		if (this.sLoader != null) {
			buf.append(":sl=");
			buf.append(this.sLoader);
		}
		
		buf.append(":w=");
		buf.append(this.width);
		buf.append(":h=");
		buf.append(this.height);
		buf.append(":sw=");
		buf.append(this.ssWidth);
		buf.append(":sh=");
		buf.append(this.ssHeight);
		buf.append(":js=");
		buf.append(this.jobSize);
		buf.append(":id=");
		buf.append(this.taskId);
		
		if (this.pw != -1) {
			buf.append(":pw=");
			buf.append(this.pw);
		}
		
		if (this.ph != -1) {
			buf.append(":ph=");
			buf.append(this.ph);
		}
		
		if (this.fl != -1) {
			buf.append(":fl=");
			buf.append(this.fl);
		}
		
		if (this.clx != 0) {
			buf.append(":clx=");
			buf.append(this.clx);
		}
		
		if (this.cly != 0) {
			buf.append(":cly=");
			buf.append(this.cly);
		}
		
		if (this.clz != 0) {
			buf.append(":clz=");
			buf.append(this.clz);
		}
		
		if (this.cdx != 0) {
			buf.append(":cdx=");
			buf.append(this.cdx);
		}
		
		if (this.cdy != 0) {
			buf.append(":cdy=");
			buf.append(this.cdy);
		}
		
		if (this.cdz != 0) {
			buf.append(":cdz=");
			buf.append(this.cdz);
		}
		
		return buf.toString();
	}
	
	/**
	 * @see net.sf.j3d.network.JobFactory#set(java.lang.String, java.lang.String)
	 */
	public void set(String key, String value) {
		if (key.equals("uri")) {
			this.uri = value;
		} else if (key.equals("sl")) {
			this.sLoader = value;
		} else if (key.equals("w")) {
			this.width = Integer.parseInt(value);
			this.totalJobs = ((this.width * this.height) / (this.dx * this.dy));
		} else if (key.equals("h")) {
			this.height = Integer.parseInt(value);
			this.totalJobs = ((this.width * this.height) / (this.dx * this.dy));
		} else if (key.equals("sw")) {
			this.ssWidth = Integer.parseInt(value);
		} else if (key.equals("sh")) {
			this.ssHeight = Integer.parseInt(value);
		} else if (key.equals("pw")) {
			this.pw = Double.parseDouble(value);
		} else if (key.equals("ph")) {
			this.ph = Double.parseDouble(value);
		} else if (key.equals("fl")) {
			this.fl = Double.parseDouble(value);
		} else if (key.equals("clx")) {
			this.clx = Double.parseDouble(value);
		} else if (key.equals("cly")) {
			this.cly = Double.parseDouble(value);
		} else if (key.equals("clz")) {
			this.clz = Double.parseDouble(value);
		} else if (key.equals("cdx")) {
			this.cdx = Double.parseDouble(value);
		} else if (key.equals("cdy")) {
			this.cdy = Double.parseDouble(value);
		} else if (key.equals("cdz")) {
			this.cdz = Double.parseDouble(value);
		} else if (key.equals("js")) {
			this.jobSize = Integer.parseInt(value);
			
			int l = (int)Math.ceil(Math.sqrt(jobSize));
			
			this.dx = l;
			this.dy = l;
			
			this.totalJobs = ((this.width * this.height) / (this.dx * this.dy));
		} else if (key.equals("id")) {
			this.taskId = Long.parseLong(value);
		}
	}
	
	public String getName() {
		StringBuffer b = new StringBuffer();
		
		double c = this.getCompleteness();
		
		for (int i = 0; i < 10; i++) {
			if (c > i * 0.1)
				b.append("#");
			else
				b.append("_");
		}
		
		b.append(":: Ray Tracing Task <a href=\"images/NetworkRender-");
		b.append(this.taskId);
		b.append(".jpg");
		b.append("\">");
		b.append(this.taskId);
		b.append("</a>");
		b.append("  ");
		b.append(this.width);
		b.append("x");
		b.append(this.height);
		
		return b.toString();
	}
	
	public double getCompleteness() { return ((double)this.i) / ((double)this.totalJobs); }
	
	public boolean isComplete() {
		if (this.nullCount > 360) {
			System.out.println("RayTracingJobFactory (" + this.taskId + "): Declaring completeness.");
			return true;
		} else {
			return false;
		}
	}
	
	public void setPriority(double p) { this.pri = p; }
	
	public double getPriority() {
		double c = this.getCompleteness();
		
		if (c > 0.75) {
			return this.pri * (1.0 + c);
		} else {
			return this.pri;
		}
	}
	
	public String toString() {
		return "RayTracingJobFactory: " + this.taskId + " " + this.getCompleteness() +
				"  " + this.uri + " " + this.width + "x" + this.height + " " +
				this.ssWidth + "x" + this.ssHeight + " " + this.pw + "x" + this.ph +
				" " + this.fl + " " + this.clx + "," + this.cly + ", " + this.clz +
				" " + this.cdx + "," + this.cdy + "," + this.cdz;
	}
}
