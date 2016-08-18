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

package com.almostrealism.projection;

import java.util.Hashtable;

import org.almostrealism.graph.PathElement;
import org.almostrealism.space.Intersection;
import org.almostrealism.texture.ImageCoordinates;

import com.almostrealism.raytracer.Scene;

/**
 * {@link PathCache} relates {@link ImageCoordinates} to {@link PathElement}s
 * so that path tracing can be done once and saved for a {@link Scene} and used
 * repeatedly for varying material and shading settings.
 * 
 * @author  Michael Murray
 */
public class PathCache<V extends Intersection> extends Hashtable<ImageCoordinates, PathElement<V>> {
	public PathCache() {
		
	}
}
