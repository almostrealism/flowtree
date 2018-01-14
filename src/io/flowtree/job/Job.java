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
 * Copyright (C) 2004-06  Mike Murray
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 *
 */

package io.flowtree.job;

/**
 * Implementations of the Job interface represent jobs that can be processed
 * by nodes and encoded to transmit to remote nodes.
 * 
 * @author Mike Murray
 */
public interface Job extends Runnable {
	/**
	 * @return  A network wide unique id for the task (JobFactory) this Job is asociated with.
	 *          This value should be included in the string produced by the encode method
	 *          and the set method should set the id.
	 */
	public long getTaskId();
	
	/**
	 * @return  A string describing the task (JobFactory) this Job is asociated with.
	 */
	public String getTaskString();
	
    /**
     * This method should return a string of the form:
     * "classname:key0=value0:key1=value1:key2=value2..."
     * Where classname is the name of the class that is implmenting Job, and the
     * key=value pairs are pairs of keys and values that will be passed to the set method
     * of the class to initialize the state of the object after it has been transmitted
     * from one node to another.
     * 
     * @return  A String representation of this Job.
     */
    public String encode();
    
    /**
     * Sets a property of this Job object. Any Job object that is to be
     * transmitted between network nodes will have this method called when it arrives at
     * a new host to initialize its variables based on the string returned by the encode
     * method.
     * 
     * @param key  Property name.
     * @param value  Property value.
     */
    public void set(String key, String value);
}
