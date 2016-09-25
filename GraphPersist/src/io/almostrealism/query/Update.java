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

package io.almostrealism.query;

import java.lang.reflect.InvocationTargetException;

/**
 * An {@link Update} writes an object to structured data in any database.
 * 
 * @author  Michael Murray
 */
public interface Update<D, K, V> {
	/**
	 * This method may be called by multiple threads simultaneously.
	 * 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException
	 */
	void execute(D database, K key, V value) throws IllegalAccessException, InvocationTargetException;
}
