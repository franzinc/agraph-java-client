/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.actions;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.topbraid.core.TB;
import org.topbraid.core.change.AbstractChange;
import org.topbraid.core.change.IChange;
import org.topbraid.core.rdf.RDFModel;
import org.topbraid.core.session.ISession;
import org.topbraid.core.util.ModelUtil;
import org.topbraid.eclipsex.log.Log;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.system.ARQFactory;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.core.TBC;
import org.topbraidcomposer.core.models.Imports;

import com.franz.agraph.query.SPARQLPlugin;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Resource;

public class SaveQueryAction extends AbstractSPARQLViewAction {
	
	@Override
	protected void run() {
		
		// Check that this is a valid SPARQL query
		try {
			String text = getSparqlView().getQueryString();
			final RDFModel rdfModel = TB.getSession().getModel();
			final Query arq = ARQFactory.get().createQuery(rdfModel, text);
		
			// Get matching SP class
			Resource type = SP.Select;
			if(arq.isAskType()) {
				type = SP.Ask;
			}
			else if(arq.isConstructType()) {
				type = SP.Construct;
			}
			type = (Resource) type.inModel(rdfModel);
	
			Shell shell = getViewPart().getSite().getShell();
			// Make sure that SPIN is imported
			if(!spinExists()) {
				String msg = 
					"This model does not import the SPIN namespace yet," +
					" which is required to store SPARQL queries.  Would" +
					" you like to import SPIN now?";
				if(CorePlugin.confirmed(shell, msg)) {
					try {
						Resource oldSel = TBC.getSession().getSelectedResource();
						Imports.addImport(URI.create(SPIN.BASE_URI), null);
						TBC.getSession().setSelectedResource(oldSel);
					}
					catch(Throwable t) {
						Log.logError(SPARQLPlugin.PLUGIN_ID, "Could not import SPIN namespace.", t);
						return;
					}
				}
				else {
					return;
				}
			}
			
			final Resource resource = TBC.getSession().getSelectedResource();
			
			// Create the change
			IChange change = new AbstractChange(getAction().getText()) {
				public void execute(ISession session, IProgressMonitor monitor)
						throws InterruptedException {
					ARQ2SPIN a2s = new ARQ2SPIN(rdfModel);
					org.topbraid.spin.model.Query query = a2s.createQuery(arq, null);
					resource.addProperty(SPIN.query, query);
				}
			};
			
			TB.getSession().getChangeEngine().execute(change);
		}
		catch(Throwable t) {
			CorePlugin.showUserError("Cannot parse current SPARQL query", t);
			return;
		}
	}
	
	
	private boolean spinExists() {
		RDFModel rdfModel = TB.getSession().getModel();
		for(Graph graph : ModelUtil.getRegularSubGraphs(rdfModel)) {
			URI baseURI = TB.getGraphRegistry().getBaseURI(graph);
			if(baseURI != null && baseURI.toString().equals(SPIN.BASE_URI)) {
				return true;
			}
		}
		return false;
	}
}
