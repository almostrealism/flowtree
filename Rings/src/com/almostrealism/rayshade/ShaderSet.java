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

package com.almostrealism.rayshade;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.almostrealism.texture.RGB;

/**
 * @author Mike Murray
 */
public class ShaderSet extends HashSet implements Shader {
    /**
     * Adds the specified Object to this set and returns true.
     * 
     * @throws IllegalArgumentException  If the specified Object is not an instance of Shader.
     */
    public boolean add(Object o) {
        if (o instanceof Shader == false)
            throw new IllegalArgumentException("Illegal argument: " + o.toString());
        
        return super.add(o);
    }
    
    /**
     * Adds all of the elements stored by the specified Collection object to this set.
     * Returns true if the set changed as a result.
     * 
     * @throws IllegalArgumentException  If an element in the specified Collection object is not
     * 									an instance of Shader. Note: Elements that have not yet been added
     * 									to the set at the time this error occurs will not be added.
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
     * @return  The sum of the values given by the shade method for each Shader object stored by this ShaderSet object.
     */
    public RGB shade(ShaderParameters p) {
        RGB color = new RGB(0.0, 0.0, 0.0);
        
        Iterator itr = super.iterator();
        while (itr.hasNext()) color.addTo(((Shader)itr.next()).shade(p));
        
        return color;
    }
    
	/**
	 * @throws IllegalArgumentException  If args[0] is not a ShaderParameters object.
	 * @return  this.shade(args[0]).
	 */
	public RGB evaluate(Object args[]) {
	    if (args[0] instanceof ShaderParameters) {
	        return this.shade((ShaderParameters)args[0]);
	    } else {
	        throw new IllegalArgumentException("Illegal argument: " + args[0]);
	    }
	}
	
	/**
	 * @return  False.
	 */
	public boolean equals(Object o) { return false; }
	
	/**
	 * @return  "ShaderSet".
	 */
	public String toString() { return "ShaderSet"; }
}
