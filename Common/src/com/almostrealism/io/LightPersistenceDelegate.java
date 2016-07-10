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

package com.almostrealism.io;


import java.beans.*;

import com.almostrealism.raytracer.lighting.*;

/**
  A LightPersistenceDelegate object adjusts the way the a Light object is encoded into XML
  when using an XMLEncoder.
*/

public class LightPersistenceDelegate extends DefaultPersistenceDelegate {
	/**
	  Properly encodes a Light object.
	*/
	
	public void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		
		if (oldInstance instanceof PointLight) {
			double a[] = ((PointLight)oldInstance).getAttenuationCoefficients();
			
			out.writeStatement(new Statement(oldInstance, "setAttenuationCoefficients",
						new Object[] {new Double(a[0]), new Double(a[1]), new Double(a[2])}));
		}
	}
}
