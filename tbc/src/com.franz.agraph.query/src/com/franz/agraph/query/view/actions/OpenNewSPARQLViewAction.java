/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.actions;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.topbraid.eclipsex.log.Log;
import org.topbraidcomposer.ui.IDs;
import org.topbraidcomposer.ui.views.AbstractViewAction;

import com.franz.agraph.query.SPARQLPlugin;
import com.franz.agraph.query.view.SPARQLView;


public class OpenNewSPARQLViewAction extends AbstractViewAction {

	public void run() {
		SPARQLView view = (SPARQLView) getViewPart();
		String oldQuery = view.getQueryString();
		try {
			IWorkbenchPage page = view.getViewSite().getPage();
			SPARQLView newView = (SPARQLView) page.showView(IDs.SPARQL_VIEW, 
					"SPARQL" + System.currentTimeMillis(), IWorkbenchPage.VIEW_CREATE);
			newView.setQuery(oldQuery);
			page.activate(newView);
		}
		catch(PartInitException ex) {
			Log.logError(SPARQLPlugin.PLUGIN_ID, "Failed to open new SPARQL view", ex);
		}
	}
}
