/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.resultstable;

import org.eclipse.swt.widgets.Composite;
import org.topbraid.core.change.TripleChangeRecord;
import org.topbraidcomposer.ui.viewers.tables.StatementTableViewer;
import org.topbraidcomposer.ui.viewers.tables.SwitchableTableSorter;
import org.topbraidcomposer.ui.views.IResourceSelector;


public class ConstructedGraphTableViewer extends StatementTableViewer implements IResourceSelector {
	
	public ConstructedGraphTableViewer(Composite parent) {
		super(parent);
		setupDoubleClickSelection();
		setSorter(new SwitchableTableSorter(this));
	}
	
	protected void handleTripleRemoved(TripleChangeRecord r) {
	}
}
