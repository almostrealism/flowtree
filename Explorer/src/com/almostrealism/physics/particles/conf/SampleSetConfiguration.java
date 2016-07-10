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
 * Copyright (C) 2007  Almost Realism Software Group
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 */

package com.almostrealism.physics.particles.conf;

import com.almostrealism.physics.particles.InputFunction;

public class SampleSetConfiguration {
	public float samples[], mult[];
	public InputFunction input[];
	public int add[];
	
	private SampleSetConfiguration() {}
	
	public static SampleSetConfiguration getConfiguration(float samples[],
					int add[], float multiply[], InputFunction input[]) {
		SampleSetConfiguration c = new SampleSetConfiguration();
		c.samples = samples;
		c.add = add;
		c.mult = multiply;
		c.input = input;
		return c;
	}
}
