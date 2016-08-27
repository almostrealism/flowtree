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

package com.almostrealism.replicator.geometry;

import org.almostrealism.space.Surface;

import com.almostrealism.raytracer.Scene;
import com.almostrealism.raytracer.engine.ShadableSurface;
import com.almostrealism.raytracer.engine.SurfaceGroup;

/**
 * {@link ReplicantScene} stores for rendering all the {@link Replicant}s in a
 * {@link SurfaceGroup}
 * 
 * @author  Michael Murray
 */
public class ReplicantScene extends Scene<Replicant> {
	public ReplicantScene(SurfaceGroup<ShadableSurface> g) {
		for (Surface s : g) if (s instanceof Replicant) add((Replicant) s);
	}
}