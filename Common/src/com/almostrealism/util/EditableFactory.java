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

package com.almostrealism.util;

/**
  EditableFactory is the parent class for classes that can be used to construct Editable objects
  of some type.
*/

public abstract class EditableFactory {
	/**
	  Returns an array of String objects containing names for each type of Editable object
	  this EditableFactory implementation can construct. The names must be in the array
	  in the same order as the object indices they represent. This method must be implemented
	  by classes that extend EditableFactory.
	*/
	
	public abstract String[] getTypeNames();
	
	/**
	  Constructs an Editable object of the type specified by the integer index.
	  This method must be implemented by classes that extend EditableFactory.
	*/
	
	public abstract Editable constructObject(int index);
}