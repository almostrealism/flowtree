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
 * Copyright (C) 2004-05  Mike Murray
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

package com.almostrealism.texture;

import com.almostrealism.util.Vector;
import com.almostrealism.util.graphics.ColorProducer;
import com.almostrealism.util.graphics.RGB;

/**
 * The Texture interface is implemented by classes that can be used to texture a surface.
 * 
 * @author Mike Murray
 */
public interface Texture extends ColorProducer {
	/**
	 * Returns the color of the texture represented by this Texture object at the specified point as an RGB object
	 * using the arguments stored by this Texture object.
	 */
	public RGB getColorAt(Vector point);
	
	/**
	 * Returns the color of the texture represented by this Texture object at the specified point as an RGB object
	 * using the specified arguments.
	 */
	public RGB getColorAt(Vector point, Object args[]);
}
