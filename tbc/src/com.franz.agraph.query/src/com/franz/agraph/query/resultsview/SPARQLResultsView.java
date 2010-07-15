/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.resultsview;

import java.util.List;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.topbraid.eclipsex.log.Log;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.ui.views.AbstractStructuredViewerView;
import org.topbraidcomposer.widgets.Activator;

import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.query.view.ISPARQLView;
import com.franz.agraph.query.view.resultstable.ResultsTableViewer;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;


public class SPARQLResultsView extends AbstractStructuredViewerView implements ISPARQLView {
	
	private ResultsTableViewer tableViewer;


	@Override
	protected StructuredViewer createStructuredViewer(Composite parent) {
		tableViewer = new ResultsTableViewer(parent);
		return tableViewer;
	}
	
	
	public AGQuery getCurrentQuery() {
		return tableViewer.getCurrentQuery();
	}


	public List<QuerySolution> getSelectedQuerySolutions() {
		return tableViewer.getSelectedQuerySolutions();
	}


	public void setResultSet(AGQuery query, ResultSet rs) {
		if(!tableViewer.setResultSet(query, rs, null)) {
			setContentDescription(CorePlugin.getDefault().getScalabilityCapMessage());
		}
		else {
			setContentDescription("");
		}
	}

	
	public static void show(AGQuery query, ResultSet rs, String title) {
		try {
			String id = null;
			if(title != null) {
				id = title.replace(':', '-');
			}
			SPARQLResultsView view = (SPARQLResultsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.topbraidcomposer.sparql.resultsview", id, IWorkbenchPage.VIEW_ACTIVATE);
			if(title != null) {
				view.setPartName(title);
			}
			view.setResultSet(query, rs);
		}
		catch(Throwable t) {
			Log.logError(Activator.PLUGIN_ID, "Failed to open SPARQL results view with title " + title, t);
		}
	}
}
