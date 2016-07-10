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
 *  
 */



package com.almostrealism.photonfield.ui;

import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;

import com.almostrealism.photonfield.distribution.OverlayBRDF;


/**
 *  @author Samuel Tepper
 */

public class VOBRDFPanel extends JPanel{

	private JLabel nameLabel;
	private static String nameString;
	private OverlayBRDF currentBRDF;
	
	public VOBRDFPanel(){
		super(new FlowLayout());
		nameString = currentBRDF.getName();
		nameLabel = new JLabel(nameString);
		add(nameLabel);
		}
	
	
}
