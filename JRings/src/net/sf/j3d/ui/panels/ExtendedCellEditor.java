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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import com.almostrealism.texture.ImageTexture;
import com.almostrealism.util.Editable;
import com.almostrealism.util.Vector;
import com.almostrealism.util.graphics.GraphicsConverter;
import com.almostrealism.util.graphics.RGB;

import net.sf.j3d.ui.dialogs.EditRGBDialog;
import net.sf.j3d.ui.dialogs.EditVectorDialog;


// TODO  Add support for File objects.

/**
 * An ExtendedCellEditor object can be used to allow a user to edit
 * values in a table including instances of RGB and Vector.
 */
public class ExtendedCellEditor extends DefaultCellEditor implements TableCellEditor, DynamicDisplay {
  private Class currentValueType;
  private Object currentValue;
  
  private JTextField field;
  private JComboBox combo;
  private PercentagePanel percentPanel;

	/**
	 * Constructs a new ExtendedCellEditor object.
	 */
	public ExtendedCellEditor() { super(new JTextField()); }
	
	/**
	 * @return  The value stored by this ExtendedCellEditor object.
	 */
	public Object getCellEditorValue() {
		if (this.currentValue instanceof Vector || this.currentValue instanceof RGB) {
			return this.currentValue;
		} else if (this.currentValue instanceof URL) {
		    try {
		        return new URL(this.field.getText());
		    } catch (MalformedURLException murl) {
		        return (URL)this.currentValue;
		    }
		} else if (this.currentValue instanceof Editable.Selection) {
			((Editable.Selection)this.currentValue).setSelected(this.combo.getSelectedIndex());
			return this.currentValue;
		} else if (this.currentValue instanceof ImageTexture) {
			try {
				((ImageTexture)this.currentValue).setPropertyValue(new URL(this.field.getText()), 0);
			} catch (Exception e) {
				// Revert settings.
			}
			
			return this.currentValue;
		} else if (this.currentValue instanceof Double &&
					((Double)this.currentValue).doubleValue() >= 0.0 &&
					((Double)this.currentValue).doubleValue() < 1.0) {
			return new Double(this.percentPanel.getValue());
		} else {
			Object value = super.getCellEditorValue();
			
			if (this.currentValueType.isInstance(value)) {
				return value;
			} else if (value instanceof String) {
				if (this.currentValueType.equals(Double.class))
					return new Double((String)value);
				else if (this.currentValueType.equals(Boolean.class))
					return new Boolean((String)value);
				else
					return null;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * @return  A Component for editing the specifed value.
	 */
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
							int row, int column) {
		if (value == null) return null;
		
		this.currentValueType = value.getClass();
		this.currentValue = value;
		
		if (this.currentValue instanceof Vector) {
			final JButton button = new JButton();
			button.setBorderPainted(false);
			button.setBackground(table.getSelectionBackground());
			button.setForeground(table.getSelectionForeground());
			
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					button.setText(currentValue.toString());
					
					EditVectorDialog editDialog = new EditVectorDialog((Vector)currentValue, ExtendedCellEditor.this);
					editDialog.setVisible(true);
				}
			});
			
			return button;
		} else if (this.currentValue instanceof RGB) {
			final JButton button = new JButton();
			button.setBorderPainted(false);
			
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					button.setBackground(GraphicsConverter.convertToAWTColor((RGB)currentValue));
					
					EditRGBDialog editDialog = new EditRGBDialog((RGB)currentValue, ExtendedCellEditor.this);
					editDialog.setVisible(true);
				}
			});
			
			return button;
		//} else if (this.currentValue instanceof Boolean) {
		//	DefaultCellEditor editor = new DefaultCellEditor(new JCheckBox());
		//	return editor.getTableCellEditorComponent(table, value, isSelected, row, column);
		} else if (this.currentValue instanceof URL) {
		    this.field = new JTextField(((URL)this.currentValue).toString());
		    return this.field;
		} else if (this.currentValue instanceof Editable.Selection) {
			this.combo = new JComboBox(((Editable.Selection)this.currentValue).getOptions());
			return this.combo;
		} else if (this.currentValue instanceof ImageTexture) {
			this.field = new JTextField(((ImageTexture)this.currentValue).getPropertyValues()[0].toString());
			return this.field;
		} else if (this.currentValue instanceof Double &&
				((Double)this.currentValue).doubleValue() >= 0.0 &&
				((Double)this.currentValue).doubleValue() < 1.0) {
			this.percentPanel = new PercentagePanel();
			this.percentPanel.setValue(((Double)this.currentValue).doubleValue());
			
			return this.percentPanel;
		} else {
			return super.getTableCellEditorComponent(table, value, isSelected, row, column);
		}
	}
	
	/**
	 * Signals the completion of the editing operation.
	 * 
	 * @see fireEditingStopped()
	 */
	public void updateDisplay() { fireEditingStopped(); }
}
