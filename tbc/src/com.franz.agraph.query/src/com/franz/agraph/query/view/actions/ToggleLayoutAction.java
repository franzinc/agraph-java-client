/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.actions;

import com.franz.agraph.query.view.resultstable.ResultsTableViewer;



public class ToggleLayoutAction extends AbstractSPARQLViewAction {

	protected void run() {
		getSparqlView().toggleLayout();
		ResultsTableViewer rtv = getSparqlView().getResultsTableViewer();
		if(rtv != null) {
			rtv.refreshTableColumnsBounds();
		}
	}
}
