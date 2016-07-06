/*
 * Copyright (C) 2007  Mike Murray
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 *
 */

package com.almostrealism.flow.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class ConcatenatedResource extends DistributedResource {
	private static String header = "<ConcatenatedResource>";
	private static String footer = "";
	
	public static class ConcatenatedResourceHeaderParser implements ResourceHeaderParser {
		public boolean doesHeaderMatch(byte[] head) {
			String s = new String(head);
			return s.startsWith(header);
		}

		public Class getResourceClass() { return ConcatenatedResource.class; }
	}
	
	public static class ConcatenatedInputStream extends InputStream {
		private String files[];
		private byte sep[] = "\n".getBytes();
		private InputStream current;
		private int index, sepIndex = 0;
		private int bytes;
		
		public ConcatenatedInputStream(String files[]) {
			this.files = files;
			this.index = 0;
		}
		
		public int read() throws IOException {			
			if (this.index == 0 && this.current == null) this.next();
			
			int read = -1;
			if (this.sepIndex <= 0) read = this.current.read();
			
			if (read < 0) {
				if (this.sepIndex < this.sep.length) {
					return this.sep[this.sepIndex++];
				} else if (this.sepIndex == this.sep.length) {
					while (read < 0) {
						if (this.index >= this.files.length) {
							return -1;
						} else {
							this.next();
							read = this.current.read();
						}
					}
					
					this.sepIndex = 0;
				}
			}
			
			if (read >= 0) this.bytes++;
			return read;
		}
		
		private void next() throws IOException {
			if (this.current != null) {
				System.out.println("ConcatenatedResource: Read " + this.bytes +
									" from " + this.current);
				this.current.close();
			}
			
			this.bytes = 0;
			this.current = ResourceDistributionTask.getCurrentTask().
							getResource(this.files[this.index++]).getInputStream();
			if (index == this.files.length)
				this.sep = (new String(this.sep) + footer).getBytes();
		}
	}
	
	public InputStream getInputStream() {
		ResourceDistributionTask t = ResourceDistributionTask.getCurrentTask();
		BufferedReader buf = new BufferedReader(new InputStreamReader(super.getInputStream()));
		
		try {
			String l = buf.readLine();
			buf.close();
			l = l.substring(header.length());
			String c[] = t.getChildren(l);
			return new ConcatenatedInputStream(c);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long getTotalBytes() {
		ResourceDistributionTask t = ResourceDistributionTask.getCurrentTask();
		BufferedReader buf = new BufferedReader(new InputStreamReader(super.getInputStream()));
		
		try {
			String l = buf.readLine();
			buf.close();
			l = l.substring(header.length());
			String c[] = t.getChildren(l);
			long tot = c.length * 2;
			
			for (int i = 0; i < c.length; i++) {
				DistributedResource r = t.getResource(c[i]);
				tot += r.getTotalBytes();
			}
			
			tot += footer.getBytes().length;
			return tot;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Creates a ConcatenatedResource at the specified URI on the distributed database.
	 * This ConcatenatedResource will concatenate the contents of the specified directory.
	 * 
	 * @param uri  URI pointing to where the resource shall reside.
	 * @param dir  Directory containing files to use for the resource.
	 */
	public static void createConcatenatedResource(String uri, String dir) throws IOException {
		PrintStream out = new PrintStream(ResourceDistributionTask.
									getCurrentTask().getOutputStream(uri));
		out.println(header + dir);
		out.flush();
		out.close();
	}
}
