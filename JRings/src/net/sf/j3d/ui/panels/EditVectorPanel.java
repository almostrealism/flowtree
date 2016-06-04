/*
* Copyright (C) 2004-05  Mike Murray
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

package net.sf.j3d.ui.panels;

import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.almostrealism.util.Vector;

import net.sf.j3d.run.Settings;


/**
 * An EditVectorPanel object can be used to specify a 3D vector.
 */
public class EditVectorPanel extends JPanel {
  private JLabel xLabel, yLabel, zLabel;
  private JFormattedTextField xField, yField, zField;
  private JComboBox coordTypeBox;

	/**
	 * Constructs a new EditVectorPanel object with the initial values set to 0.0.
	 */
	public EditVectorPanel() {
		this(new Vector(0.0, 0.0, 0.0));
	}
	
	/**
	 * Constructs a new EditVectorPanel object with the initial values set to those
	 * of the specified Vector object.
	 */
	public EditVectorPanel(Vector value) {
		super(new GridLayout(0, 2));
		
		this.xField = new JFormattedTextField(Settings.decimalFormat);
		this.yField = new JFormattedTextField(Settings.decimalFormat);
		this.zField = new JFormattedTextField(Settings.decimalFormat);
		
		this.xField.setColumns(6);
		this.yField.setColumns(6);
		this.zField.setColumns(6);
		
		this.coordTypeBox = new JComboBox(new String[] {"Cartesian", "Spherical"});
		
		this.coordTypeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				if (EditVectorPanel.this.coordTypeBox.getSelectedIndex() == 0) {
					EditVectorPanel.this.xLabel.setText("X = ");
					EditVectorPanel.this.yLabel.setText("Y = ");
					EditVectorPanel.this.zLabel.setText("Z = ");
				} else {
					EditVectorPanel.this.xLabel.setText("r = ");
					EditVectorPanel.this.yLabel.setText('\u03b8' + " = ");
					EditVectorPanel.this.zLabel.setText('\u03d5' + " = ");
				}
			}
		});
		
		FocusListener listener = new FocusAdapter() {
			public void focusGained(FocusEvent event) {
				JTextField field = (JTextField)event.getSource();
				field.setSelectionStart(0);
				field.setSelectionEnd(field.getText().length());
			}
		};
		
		this.xField.addFocusListener(listener);
		this.yField.addFocusListener(listener);
		this.zField.addFocusListener(listener);
		
		this.setSelectedVector(value);
		
		this.xLabel = new JLabel("X = ");
		this.yLabel = new JLabel("Y = ");
		this.zLabel = new JLabel("Z = ");
		
		this.add(this.xLabel);
		this.add(this.xField);
		this.add(this.yLabel);
		this.add(this.yField);
		this.add(this.zLabel);
		this.add(this.zField);
		this.add(new JLabel("System: "));
		this.add(this.coordTypeBox);
	}
	
	/**
	 * Updates the fields of this EditVectorPanel object to display the values for
	 * the vector represented by the specified Vector object.
	 */
	public void setSelectedVector(Vector value) {
		this.xField.setValue(new Double(value.getX()));
		this.yField.setValue(new Double(value.getY()));
		this.zField.setValue(new Double(value.getZ()));
		this.coordTypeBox.setSelectedIndex(0);
	}
	
	/**
	 * @return  The vector selected by this EditVectorPanel object as a Vector object.
	 */
	public Vector getSelectedVector() {
		double x = ((Number)this.xField.getValue()).doubleValue();
		double y = ((Number)this.yField.getValue()).doubleValue();
		double z = ((Number)this.zField.getValue()).doubleValue();
		
		if (this.coordTypeBox.getSelectedIndex() > 0)
			return new Vector(x, y, z, Vector.SPHERICAL_COORDINATES);
		else
			return new Vector(x, y, z);
	}
	
	/**
	 * Adds the specified KeyListener to each field of this EditVectorPanel object.
	 */
	public void addKeyListener(KeyListener l) {
		this.xField.addKeyListener(l);
		this.yField.addKeyListener(l);
		this.zField.addKeyListener(l);
	}
}
