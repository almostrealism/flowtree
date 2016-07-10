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

package com.almostrealism.ui.event;

import com.almostrealism.raytracer.engine.*;

/**
  A SurfaceAddEvent object represents the event of adding a new surface to the current scene.
*/

public class SurfaceAddEvent extends SceneEditEvent implements SurfaceEvent {
  private Surface target;

	/**
	  Constructs a new SurfaceAddEvent object with the specified target.
	*/
	
	public SurfaceAddEvent(Surface target) {
		this.target = target;
	}
	
	/**
	  Returns the target of this SurfaceAddEvent object.
	*/
	
	public Surface getTarget() {
		return this.target;
	}
	
	/**
	  Returns "SurfaceAddEvent";
	*/
	
	public String toString() {
		return "SurfaceAddEvent";
	}
}
