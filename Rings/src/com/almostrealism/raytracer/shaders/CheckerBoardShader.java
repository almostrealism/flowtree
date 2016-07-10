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
 * Copyright (C) 2005  Mike Murray
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

package com.almostrealism.raytracer.shaders;

import com.almostrealism.raytracer.engine.AbstractSurface;
import com.almostrealism.util.Vector;
import com.almostrealism.util.graphics.ColorProducer;
import com.almostrealism.util.graphics.RGB;

/**
 * @author Mike Murray
 */
public class CheckerBoardShader implements Shader {
  private ColorProducer hotColor, coldColor;

	/**
	 * @see com.almostrealism.raytracer.shaders.Shader#shade(com.almostrealism.raytracer.shaders.ShaderParameters)
	 */
	public RGB shade(ShaderParameters p) {
		double intensity = 0.0;
		
		AbstractSurface s = (AbstractSurface) p.getSurface();
		
		Vector v = s.getTransform(true).getInverse().transformAsLocation(p.getPoint());
		
		intensity += Math.sin(v.getX()) + 1;
		intensity += Math.sin(v.getZ()) + 1;
		intensity = intensity / 4.0;
		
		Object args[] = {p};
		
		RGB hot = this.hotColor.evaluate(args);
		hot.multiplyBy(intensity);
		
		RGB cold = this.coldColor.evaluate(args);
		cold.multiplyBy(1.0 - intensity);
		
		// System.out.println(intensity + " " + hot + " " + cold);
		
		hot.addTo(cold);
		
		return hot;
	}

	/**
	 * @see com.almostrealism.util.graphics.ColorProducer#evaluate(java.lang.Object[])
	 */
	public RGB evaluate(Object args[]) { return this.shade((ShaderParameters) args[0]); }
	
	/**
	 * @return Returns the coldColor.
	 */
	public ColorProducer getColdColor() { return coldColor; }
	
	/**
	 * @param coldColor The coldColor to set.
	 */
	public void setColdColor(ColorProducer coldColor) { this.coldColor = coldColor; }
	
	/**
	 * @return Returns the hotColor.
	 */
	public ColorProducer getHotColor() { return hotColor; }
	
	/**
	 * @param hotColor The hotColor to set.
	 */
	public void setHotColor(ColorProducer hotColor) { this.hotColor = hotColor; }
}
