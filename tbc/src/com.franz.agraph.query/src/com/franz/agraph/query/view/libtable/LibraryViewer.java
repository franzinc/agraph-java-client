/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.libtable;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.topbraidcomposer.ui.UIImages;
import org.topbraidcomposer.ui.actions.AbstractToolTipAction;

import com.franz.agraph.query.view.IStatusUpdater;


public class LibraryViewer extends Composite {
	
	private IAction selectAllAction = new AbstractToolTipAction("Select all", UIImages.SELECT_ALL) {
		public void run() {
			setAllChecked(true);
		}
	};

	private LibraryTableViewer tableViewer;
	
	private IAction unselectAllAction = new AbstractToolTipAction("Unselect all", UIImages.UNSELECT_ALL) {
		public void run() {
			setAllChecked(false);
		}
	};
	
	private IStatusUpdater statusUpdater;

	
	public LibraryViewer(Composite parent, IStatusUpdater statusUpdater) {
		
		super(parent, 0);
		
		this.statusUpdater = statusUpdater;
		
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginLeft = 0;
		gridLayout.marginRight = 0;
		gridLayout.marginTop = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		setLayout(gridLayout);
		
		tableViewer = new LibraryTableViewer(this, statusUpdater);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		ToolBarManager tbm = new ToolBarManager();
		tbm.add(selectAllAction);
		tbm.add(unselectAllAction);
		tbm.createControl(this);
	}
	
	
	public void dispose() {
		tableViewer.dispose();
	}
	
	
	public String getQueryText() {
		return tableViewer.getQueryText();
	}
	
	
	public LibraryTableViewer getTableViewer() {
		return tableViewer;
	}
	
	
	public void selectQueries(List<Integer> indices) {
		TableItem[] items = tableViewer.getTable().getItems();
		int[] map = new int[items.length];
		int index = 0;
		for(int i = 0; i < items.length; i++) {
			TableItem item = items[i];
			if(item.getChecked()) {
				map[index++] = i;
			}
		}
		
		List<Object> elements = new ArrayList<Object>();
		for(Integer i : indices) {
			Object element = tableViewer.getElementAt(map[i]);
			elements.add(element);
		}
		tableViewer.setSelection(new StructuredSelection(elements));
	}
	
	
	private void setAllChecked(boolean value) {
		tableViewer.setAllItemsChecked(value);
		statusUpdater.updateStatus();
	}
}
