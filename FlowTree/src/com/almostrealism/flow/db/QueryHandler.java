/*
 * Copyright (C) 2006  Mike Murray
 *
 *  All rights reserved.
 *  This document may not be reused without
 *  express written permission from Mike Murray.
 */
package com.almostrealism.flow.db;

import java.util.Hashtable;

public interface QueryHandler {
	public Hashtable executeQuery(Query q);
}