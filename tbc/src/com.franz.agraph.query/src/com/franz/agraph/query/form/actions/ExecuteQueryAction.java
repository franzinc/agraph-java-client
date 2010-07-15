/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.form.actions;

import org.topbraid.actions.statement.AbstractStatementAction;
import org.topbraid.core.TB;
import org.topbraid.core.images.ImageMetadata;
import org.topbraid.core.rdf.RDFModel;
import org.topbraid.sparql.SPARQLFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.util.SPINUtil;

import com.franz.agraph.query.SPARQLPlugin;
import com.franz.agraph.query.view.SPARQLView;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;


public class ExecuteQueryAction extends AbstractStatementAction {

	public ExecuteQueryAction() {
		super("Execute as SPARQL Query", new ImageMetadata(SPARQLPlugin.PLUGIN_ID, "RunQuery"));
	}

	
	public boolean isEnabledFor(Statement statement) {
		try {
			String queryString = SPINUtil.getQueryString(statement.getObject(), true);
			RDFModel rdfModel = TB.getSession().getModel();
			return SPARQLFactory.isQuery(rdfModel, queryString);
		}
		catch(Throwable t) {
			return false;
		}
	}

	
	public boolean isVisibleFor(Statement statement) {
		RDFNode object = statement.getObject();
		if(object != null) {
			return SPINFactory.isQueryProperty(statement.getPredicate());
		}
		else {
			return false;
		}
	}
	

	public void run(Statement statement) {
		String queryString = SPINUtil.getQueryString(statement.getObject(), true);
		SPARQLView view = SPARQLView.get();
		if(view != null) {
			view.executeQuery(queryString);
		}
	}
}
