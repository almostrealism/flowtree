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
* Copyright (C) 2004  Mike Murray
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


import javax.swing.*;

import com.almostrealism.util.*;

/**
  An EditPanel object can be used to allow a user to set the properties
  of an Editable object.
*/

public class EditPanel extends JTable {
  private Editable editing;
  private EditablePropertiesTableModel tableModel;

	/**
	  Constructs a new EditPanel object that can be used to modify
	  the specified Editable object.
	*/
	
	public EditPanel(Editable editing) {
		this.tableModel = new EditablePropertiesTableModel(null);
		super.setModel(this.tableModel);
		super.setDefaultEditor(Object.class, new ExtendedCellEditor());
		super.setDefaultRenderer(Object.class, new ExtendedCellRenderer());
		
		this.setEditing(editing);
	}
	
	/**
	  Sets the Editable object that this panel modifies and updates
	  the table to reflect the change.
	*/
	
	public void setEditing(Editable editing) {
		this.editing = editing;
		
		this.tableModel.setEditing(this.editing);
		this.updateTable();
	}
	
	/**
	  Returns the Editable object that this panel modifies.
	*/
	
	public Editable getEditing() {
		return this.editing;
	}
	
	/**
	  Updates the table displayed by this EditPanel object.
	*/
	
	public void updateTable() {
		for (int i = 0; i < super.getModel().getRowCount(); i++)
			super.setRowHeight(i, 45);
		
		super.doLayout();
	}
}
