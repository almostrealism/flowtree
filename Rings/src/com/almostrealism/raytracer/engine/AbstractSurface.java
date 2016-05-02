/*
 * Copyright (C) 2004-16  Mike Murray
 * All rights reserved.
 */

package com.almostrealism.raytracer.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.almostrealism.geometry.BasicGeometry;
import com.almostrealism.raytracer.primitives.Mesh;
import com.almostrealism.raytracer.shaders.DiffuseShader;
import com.almostrealism.raytracer.shaders.Shader;
import com.almostrealism.raytracer.shaders.ShaderParameters;
import com.almostrealism.raytracer.shaders.ShaderSet;

import net.sf.j3d.util.Vector;
import net.sf.j3d.util.graphics.RGB;

/**
 * {@link AbstractSurface} is an abstract implementation of {@link Surface} that takes
 * care of all of the standard methods of {@link Surface} that are shared by most
 * {@link Surface} implementations in the same way. By default the location is at the
 * origin, the size is 1.0, and the color is black. Also, an {@link AbstractSurface}
 * uses a {@link DiffuseShader} by default.
 * 
 * @author  Mike Murray
 */
public abstract class AbstractSurface extends BasicGeometry implements Surface {
	private boolean shadeFront, shadeBack;

	private RGB color;

	private double rindex = 1.0, reflectP = 1.0, refractP = 0.0;

	private Texture textures[];
	private ShaderSet shaders;

	private AbstractSurface parent;
	
	/**
	 * Sets all values of this AbstractSurface to the defaults specified above.
	 */
	public AbstractSurface() {
		this.setShadeFront(true);
		this.setShadeBack(false);
		
		this.setTextures(new Texture[0]);
		this.setShaders(new Shader[] {DiffuseShader.defaultDiffuseShader});
		
		this.setColor(new RGB(0.0, 0.0, 0.0));
	}
	
	/**
	 * Sets the location and size of this AbstractSurface to those specifed, and uses the defaults for the other values.
	 */
	public AbstractSurface(Vector location, double size) {
		this();
		
		this.setLocation(location);
		this.setSize(size);
	}
	
	/**
	 * Sets the location, size, and color of this AbstractSurface to those specified.
	 */
	public AbstractSurface(Vector location, double size, RGB color) {
		this(location, size);
		this.setColor(color);
	}
	
	/**
	 * Sets the location, size, and color of this AbstractSurface to those specified.
	 */
	public AbstractSurface(Vector location, double size, RGB color, boolean addDefaultDiffuseShader) {
		this(location, size, color);
		
		if (!addDefaultDiffuseShader)
			this.setShaders(new Shader[0]);
	}
	
	/**
	 * Sets the parent surface group of this AbstractSurface to the specified SurfaceGroup object.
	 */
	public void setParent(SurfaceGroup parent) { this.parent = parent; }
	
	/**
	 * Returns the parent of this {@link AbstractSurface} as a {@link SurfaceGroup} object.
	 */
	public SurfaceGroup getParent() { return (SurfaceGroup) parent; }
	
	/**
	 * Sets the flag indicating that the front side of this AbstractSurface should be shaded
	 * to the specified boolean value.
	 */
	public void setShadeFront(boolean shade) { this.shadeFront = shade; }
	
	/**
	 * Sets the flag indicating that the back side of this AbstractSurface should be shaded
	 * to the specified boolean value.
	 */
	public void setShadeBack(boolean shade) { this.shadeBack = shade; }
	
	/**
	 * Returns true if the front side of this AbstractSurface should be shaded.
	 * The "front side" is the side that the Vector object returned by the getNormalAt()
	 * method for this AbstractSurface points outward from.
	 */
	public boolean getShadeFront() {
	    if (this.parent != null && this.parent.getShadeFront())
	        return true;
	    else
	        return this.shadeFront;
	}
	
	/**
	 * Returns true if the back side of this AbstractSurface should be shaded.
	 * The "back side" is the side that the vector opposite the Vector object
	 * returned by the getNormalAt() method for this AbstractSurface points outward from.
	 */
	public boolean getShadeBack() {
	    if (this.parent != null && this.parent.getShadeBack())
	        return true;
	    else
	        return this.shadeBack;
	}
	
	/**
	 * @return  A Mesh object with location, size, color, scale coefficients,
	 *          rotation coefficients, and transformations as this AbstractSurface.
	 */
	public Mesh triangulate() {
		Mesh m = super.triangulate();
		m.setColor(this.getColor());
		return m;
	}
	
	public void setIndexOfRefraction(double n) { this.rindex = n; }
	public double getIndexOfRefraction() { return this.rindex; }
	public double getIndexOfRefraction(Vector p) { return this.rindex; }
	
	public void setReflectedPercentage(double p) { this.reflectP = p; }
	public void setRefractedPercentage(double p) { this.refractP = p; }
	
	public double getReflectedPercentage() { return this.reflectP; }
	public double getReflectedPercentage(Vector p) { return this.reflectP; }
	public double getRefractedPercentage() { return this.refractP; }
	public double getRefractedPercentage(Vector p) { return this.refractP; }
	
	/**
	 * Sets the Texture object (used to color this AbstractSurface) at the specified index
	 * to the specified Texture object.
	 */
	public void setTexture(int index, Texture texture) {
		this.textures[index] = texture;
	}
	
	/**
	 * Sets the Texture objects (used to color this AbstractSurface) to those specified.
	 */
	public void setTextures(Texture textures[]) {
		this.textures = textures;
	}
	
	/**
	 * Appends the specified Texture object to the list of Texture objects used to color this AbstractSurface.
	 */
	public void addTexture(Texture texture) {
		Texture newTextures[] = new Texture[this.textures.length + 1];
		
		for (int i = 0; i < this.textures.length; i++) { newTextures[i] = this.textures[i]; }
		newTextures[newTextures.length - 1] = texture;
		
		this.textures = newTextures;
	}
	
	/**
	 * Removes the Texture object at the specified index from the list of Texture objects used
	 * to color this AbstractSurface.
	 */
	public void removeTexture(int index) {
		Texture newTextures[] = new Texture[this.textures.length - 1];
		
		for (int i = 0; i < index; i++) { newTextures[i] = this.textures[i]; }
		for (int i = index + 1; i < newTextures.length; i++) { newTextures[i] = this.textures[i]; }
		
		this.textures = newTextures;
        }
	
	/**
	 * Returns a Set object that maintains the Texture objects stored by this AbstractSurface.
	 */
	public Set getTextureSet() {
		Set textureSet = new Set() {
			/**
			 * @return  The number of elements stored by this set.
			 */
			public int size() { return textures.length; }
			
			/**
			 * @return  True if this set contains no elements, false otherwise.
			 */
			public boolean isEmpty() {
				if (textures.length <= 0)
					return true;
				else
					return false;
			}
			
			/**
			 * @return  An Iterator object using the elements stored by this set.
			 */
			public Iterator iterator() {
				Iterator itr = new Iterator() {
					int index = 0;

					public boolean hasNext() {
						if (index < textures.length)
							return true;
						else
							return false;
					}

					public Object next() throws NoSuchElementException {
						if (this.index >= textures.length)
							throw new NoSuchElementException("No element at " + this.index);
						return textures[this.index++];
					}

					public void remove() {
						removeTexture(this.index);
					}
				};

				return itr;
			}

			/**
			 * @return  An array containing all of the elements stored by this set.
			 */
			public Object[] toArray() { return textures; }
			
			/**
			 * @return  An array containing all of the elements stored by this set.
			 */
			public Object[] toArray(Object o[]) { return this.toArray(); }
			
			/**
			 * Adds the specified Object to this set and returns true.
			 * 
			 * 
			 * @throws IllegalArgumentException  If the specified Object is not an instance of Texture.
			 */
			public boolean add(Object o) {
				if (o instanceof Texture == false)
					throw new IllegalArgumentException("Illegal argument: " + o.toString());
				
				addTexture((Texture)o);
				
				return true;
			}
			
			/**
			 * Adds all of the elements stored by the specified Collection object to this set.
			 * @return  True if the set changed as a result.
			 * 
			 * @throws IllegalArgumentException  If an element in the specified Collection object is not
			 *                                   an instance of Texture. Note: Elements that have not yet been added
			 *                                   to the set at the time this error occurs will not be added.
			 * @throws NullPointerException  If the specified Collection object is null.
			 */
			public boolean addAll(Collection c) {
				boolean added = false;
				
				Iterator itr = c.iterator();
				
				while (itr.hasNext()) {
					this.add(itr.next());
					added = true;
				}
				
				return added;
			}
			
			/**
			 * Removes all occurences specified element from this set and returns true
			 * if the set changed as a result.
			 */
			public boolean remove(Object o) {
				boolean removed = false;
				
				for (int i = 0; i < textures.length; i++) {
					if (o.equals(textures[i])) {
						removeTexture(i--);
						removed = true;
					}
				}
				
				return removed;
			}
			
			/**
			 * Removes all of the elements stored by the specified Collection object from this set.
			 * @return  True if the set changed as a result.
			 * 
			 * @throws NullPointerException  If the specified Collection object is null.
			 */
			public boolean removeAll(Collection c) {
				if (c == null)
					throw new NullPointerException();
				
				boolean removed = false;
				
				Iterator itr = c.iterator();
				
				while (itr.hasNext()) {
					if (this.remove(itr.next()))
						removed = true;
				}
				
				return removed;
			}
			
			/**
			 * Removes all elements stored by this set that are not contained in the specified Collection object.
			 * @return  True if the set changed as a result.
			 * 
			 * @throws NullPointerException  If the specified Collection object is null.
			 */
			public boolean retainAll(Collection c) {
				if (c == null)
					throw new NullPointerException();
				
				boolean removed = false;
				
				Iterator itr = this.iterator();
				
				while (itr.hasNext()) {
					if (c.contains(itr.next()) == false) {
						itr.remove();
						removed = true;
					}
				}
				
				return removed;
			}
			
			/**
			 * Removes all elements of this set.
			 */
			public void clear() { textures = new Texture[0]; }
			
			/**
			 * @return  True if this set contains the specified Object, false otherwise.
			 */
			public boolean contains(Object o) {
				if (o instanceof Texture != true)
					return false;
				
				for (int i = 0; i < textures.length; i++) {
					if (o == null ? textures[i] == null : o.equals(textures[i]))
						return true;
				}
				
				return false;
			}
			
			/**
			 * @return  True if this set contains all of the elements of the specified Collection object.
			 * 
			 * @throws NullPointerException  If the specified Collection object is null.
			 */
			public boolean containsAll(Collection c) {
				if (c == null)
					throw new NullPointerException();
				
				Iterator itr = c.iterator();
				
				while (itr.hasNext()) {
					if (this.contains(itr.next()) == false)
						return false;
				}
				
				return true;
			}
			
			/**
			 * @return  True if the specified object is also an instance of Set with elements that
			 *          are equal to those.
			 */
			public boolean equals(Object o) {
				if (o instanceof Set == false)
					return false;
				
				if (((Set)o).size() != this.size())
					return false;
				
				if (this.containsAll((Set)o))
					return true;
				else
					return false;
			}
			
			/**
			 * @return  An integer hash code for this set by adding the hash codes for all elements
			 *          it stores.
			 */
			public int hashCode() {
				int hash = 0;
				
				Iterator itr = this.iterator();
				
				while (itr.hasNext())
					hash += itr.next().hashCode();
				
				return hash;
			}
		};
		
		return textureSet;
	}
	
	/**
	 * Sets the Shader objects (used to shade this AbstractSurface) to those specified.
	 */
	public void setShaders(Shader shaders[]) {
	    if (this.shaders == null) this.shaders = new ShaderSet();
	    
		this.shaders.clear();
		
		for (int i = 0; i < shaders.length; i++) this.shaders.add(shaders[i]);
	}
	
	/**
	 * @param set  New ShaderSet object to use for shading.
	 */
	public void setShaders(ShaderSet set) { this.shaders = set; }
	
	/**
	 * Appends the specified Shader object to the list of Shader objects used to shade this AbstractSurface.
	 */
	public boolean addShader(Shader shader) {
		if (this.shaders == null) this.shaders = new ShaderSet();
		return this.shaders.add(shader);
	}
	
	/**
	 * Returns a Set object that maintains the Shader objects stored by this AbstractSurface.
	 */
	public ShaderSet getShaderSet() { return this.shaders; }
	
	/**
	 * Calculates a color value for this AbstractSurface using the sum of the values
	 * calculated by the Shader objects stored by this AbstractSurface and the parent
	 * of this AbstractSurface and returns this value as an RGB object.
	 */
	public RGB shade(ShaderParameters p) {
		p.setSurface(this);
		
		RGB color = null;
		
		if (this.shaders == null)
			color = new RGB(0.0, 0.0, 0.0);
		else
			color = this.shaders.shade(p);
		
		if (this.getParent() != null)
			color.addTo(this.getParent().shade(p));
		
		return color;
	}
	
	/**
	 * Sets the color of this AbstractSurface to the color represented by the specified RGB object.
	 */
	public void setColor(RGB color) { this.color = color; }
	
	/**
	 * Returns the Texture object at the specified index in the list of Texture objects used
	 * to color this AbstractSurface.
	 */
	public Texture getTexture(int index) { return this.textures[index]; }
	
	/**
	 * Returns the list of Texture objects used to color this AbstractSurface as an array of Texture objects.
	 */
	public Texture[] getTextures() { return this.textures; }
	
	/**
	 * Returns the color of this AbstractSurface as an RGB object.
	 */
	public RGB getColor() { return this.color; }
	
	/**
	 * @return  The color of this AbstractSurface at the specified point as an RGB object.
	 */
	public RGB getColorAt(Vector p) { return this.getColorAt(p, true); }
	
	/**
	 * @return  The color of this AbstractSurface at the specified point as an RGB object.
	 */
	public RGB getColorAt(Vector point, boolean transform) {
	    if (transform) point = getTransform(true).getInverse().transformAsLocation(point);
	    
	    RGB colorAt = new RGB(0.0, 0.0, 0.0);
	    
	    if (textures.length > 0) {
	        for (int i = 0; i < this.textures.length; i++) {
	            colorAt.addTo(this.textures[i].getColorAt(point));
	        }
	        
	        colorAt.multiplyBy(this.color);
	    } else {
	        colorAt = (RGB)this.color.clone();
	    }
	    
	    if (this.parent != null)
	        colorAt.multiplyBy(this.parent.getColorAt(point));
		
		return colorAt;
	}
}