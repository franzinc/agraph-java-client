/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.topbraid.core.TB;
import org.topbraid.eclipsex.log.Log;
import org.topbraid.sparql.TOPS;
import org.topbraid.sparql.TopBraidDataset;
import org.topbraidcomposer.core.TBC;
import org.topbraidcomposer.core.io.TBCIO;
import org.topbraidcomposer.editors.ResourceEditor;

import com.franz.agraph.query.functions.BasketFunction;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;

public class SPARQLPlugin extends AbstractUIPlugin {
	
	public final static String PLUGIN_ID = TBC.BASE_ID + ".sparql";

	private static SPARQLPlugin plugin;

	
	public SPARQLPlugin() {
		plugin = this;
	}

	
	public static SPARQLPlugin getDefault() {
		return plugin;
	}
	
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		PropertyFunctionRegistry.get().put(TOPS.NS + "basket", BasketFunction.class);

		TopBraidDataset.setDefault(new TopBraidDataset() {

			@Override
			protected Graph load(URI uri, final IFile file) throws Exception {
				TBCIO.get().loadModel(uri, file, null);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						try {
							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
							IEditorPart activateEditor = page.getActiveEditor();
							page.openEditor(new FileEditorInput(file), ResourceEditor.ID);
							page.openEditor(activateEditor.getEditorInput(), ResourceEditor.ID);
						}
						catch(Exception ex) {
							Log.logError(PLUGIN_ID, "Failed to load named graph " + file, ex);
						}
					}
				});
				return TB.getGraphRegistry().getBaseGraph(uri);
			}
		});
	}


	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}
}
