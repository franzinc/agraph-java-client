/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.functions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.topbraidcomposer.ui.IDs;
import org.topbraidcomposer.ui.views.basket.BasketView;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterConcat;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionBase;
import com.hp.hpl.jena.sparql.util.IterLib;


public class BasketFunction extends PropertyFunctionBase {

	@Override
	public QueryIterator exec(Binding arg0, PropFuncArg argSubject, Node predicate,
				PropFuncArg argObject, ExecutionContext execCxt) {
		final Set<Resource> resources = getBasketResources();
		QueryIterConcat concat = new QueryIterConcat(execCxt);
		if(argSubject.getArg().isVariable()) {
			for(Resource resource : resources) {
				BindingMap map = new BindingMap();
				map.add((Var)argSubject.getArg(), resource.asNode());
				QueryIterator nested = IterLib.result(map, execCxt);
				concat.add(nested);
			}
		}
		return concat;
	}

	
	private Set<Resource> getBasketResources() {
		final Set<Resource> resources = new HashSet<Resource>();
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IViewReference[] refs = page.getViewReferences();
					for(int i = 0; i < refs.length; i++) {
						if(IDs.BASKET_VIEW.equals(refs[i].getId())) {
							BasketView basketView = (BasketView) refs[i].getView(true);
							resources.addAll(basketView.getResources());
						}
					}
				}
				catch(Throwable t) {
				}
			}
		});
		return resources;
	}
}
