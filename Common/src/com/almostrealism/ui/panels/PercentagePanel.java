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

package com.almostrealism.ui.panels;

import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.text.ParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.almostrealism.raytracer.Settings;


/**
 * A PercentagePanel object can be used to display a decimal value
 * (0.0 - 1.0) as a percentage and provide a slider for setting the value.
 * 
 * @author Mike Murray
 */
public class PercentagePanel extends JPanel {
  private double value;
  
  private JFormattedTextField valueField;
  private JSlider slider;

	/**
	 * Constructs a new PercentagePanel object using a grid layout.
	 */
	public PercentagePanel() { this(new GridLayout(0, 1)); }
	
	/**
	 * Constructs a new PercentagePanel object using the specified layout.
	 */
	public PercentagePanel(LayoutManager layout) {
		super(layout);
		
		this.valueField = new JFormattedTextField(Settings.decimalFormat);
		this.valueField.setColumns(6);
		this.slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		
		this.valueField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent source) {
				JFormattedTextField field = (JFormattedTextField)source;
				AbstractFormatter formatter = field.getFormatter();
				
				double v = 0.0;
				
				if (formatter != null) {
					String text = field.getText();
					
					try {
						v = ((Number)formatter.stringToValue(text)).doubleValue();
						field.commitEdit();
					} catch (ParseException pe) {
						return false;
					}
				}
				
				PercentagePanel.this.setValue(v);
				
				return true;
			}
		});
		
		this.slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				PercentagePanel.this.setValue(PercentagePanel.this.slider.getValue() / 100.0);
			}
		});
		
		this.setValue(0.0);
		
		super.add(this.slider);
		super.add(this.valueField);
	}
	
	/**
	 * Adds a change listener to the slider on this PercentagePanel.
	 * 
	 * @param l  ChangeListener to add.
	 */
	public void addChangeListener(ChangeListener l) {
		this.slider.addChangeListener(l);
	}
	
	public void setSliderName(String name) {
		this.slider.setName(name);
	}
	
	/**
	 * Sets the value (0.0 - 1.0) displayed by this PercentagePanel object.
	 * 
	 * @param value  The value to use.
	 */
	public void setValue(double value) {
		this.value = value;
		
		this.valueField.setValue(new Double(this.value));
		this.slider.setValue((int)(this.value * 100));
	}
	
	/**
	 * @return  The value displayed by this PercentagePanel object.
	 */
	public double getValue() { return this.value; }
}
