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
package com.almostrealism.lighting;

import org.almostrealism.space.Vector;
import org.almostrealism.texture.RGB;

import com.almostrealism.raytracer.Scene;
import com.almostrealism.raytracer.primitives.Plane;

/**
 * @author  Michael Murray
 */
public class StandardLightingRigs {
	private static final boolean enableRectangularLight = false;
	
	public static double defaultBrightness = 1.0;
	
	public static void addDefaultLights(Scene<?, ?> scene) {
		RectangularLight rl = new RectangularLight(2.0, 2.0);
		rl.setColor(new RGB(1.0, 1.0, 1.0));
		rl.getLocation().setY(6.0);
		rl.setType(Plane.XZ);
		rl.setIntensity(0.7);
		rl.setSampleCount(6);
		
		PointLight pl1 = new PointLight(new Vector(4.0, 4.0, 10.0), defaultBrightness, new RGB(1.0, 1.0, 1.0));
//		PointLight pl2 = new PointLight(new Vector(-4.0, 4.0, 3.0), defaultBrightness, new RGB(1.0, 1.0, 1.0));
		
//		PointLight pl3 = new PointLight(new Vector(0.0, 5.0, 4.0), 0.7, new RGB(0.0, 0.0, 1.0)) {
//			public RGB getColorAt(Vector p) {
//				RGB c = super.getColorAt(p);
//				c.multiplyBy(Math.sin(p.subtract(super.getLocation()).length()));
//				return c;
//			}
//		};
		
		if (enableRectangularLight) scene.addLight(rl);
		scene.addLight(pl1);
//		scene.addLight(pl2);
//		scene.addLight(pl3);
	}
}
