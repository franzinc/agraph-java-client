/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.topbraid.core.TB;
import org.topbraid.core.io.IO;
import org.topbraid.sparql.update.UpdateChange;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.ui.util.PlatformUtil;
import org.topbraidcomposer.ui.viewers.tables.TableViewerUtil;
import org.topbraidcomposer.ui.views.AbstractViewAction;

import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.query.resultsview.IResultsHandler;
import com.franz.agraph.query.view.SPARQLView;
import com.franz.agraph.query.view.resultstable.ResultsTableViewer;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;


public class ExportResultSetAction extends AbstractViewAction implements IResultsHandler {

	protected void run() {
		SPARQLView view = (SPARQLView) getViewPart();
		view.executeQuery(this);
	}
	
	
	public void handleAsk(List<Integer> fails) {
	}


	public void handleConstructedModel(final Model model) {
		asyncExec(new Runnable() {
			public void run() {
				handleConstructedModelReally(model);
			}
		});
	}
	
	
	private void handleConstructedModelReally(Model model) {
		SPARQLView view = (SPARQLView) getViewPart();
		Shell shell = view.getViewSite().getShell();
		FileDialog dialog = new FileDialog(shell, SWT.SAVE | SWT.SINGLE);
		
		if( PlatformUtil.isWIN32() ){
			dialog.setFilterExtensions(new String[]{
					"*.rdf",
					"*.rdfs", 
					"*.owl"
			});
			dialog.setFilterNames(new String[]{
					"RDF Files",
					"RDFS Files",
					"OWL Files"
			});
			dialog.setText("Export constructed graph to...");
		}
		else{ // Mac cannot support file extensions now, so manually do something
			List<String> strList = new ArrayList<String>();
			strList.add(".rdf");
			strList.add(".rdfs");
			strList.add(".owl");
			
			String str = "Export constructed graph to...";
			String initEnd = ".rdf";
			
			PlatformUtil.generateFileDialogStr(dialog, strList, str, initEnd);
		}

		String firstFile = dialog.open();
		if(firstFile != null) {
			File file = new File(firstFile);
			String baseURI = TB.getSession().getSelectedBaseURI().toString();
			try {
				IO.write(file, model, FileUtils.langXMLAbbrev, baseURI);
				CorePlugin.showSuccess("Export successful", 
						"The result graph has been exported to " + firstFile + ".");
			}
			catch(Exception ex) {
				CorePlugin.showUserError("Could not write to " + firstFile, ex);
			}
		}
	}
	
	
	public void handleResultSet(final AGQuery query, final QueryExecution qexec, final ResultSet rs) {
		asyncExec(new Runnable() {
			public void run() {
				handleResultSetReally(query, qexec, rs);
			}
		});
	}
	
	
	public void handleResultSet(QueryExecution qexec, ResultSet rs, IProgressMonitor monitor) {
	}


	private void handleResultSetReally(AGQuery query, QueryExecution qexec, ResultSet rs) {
		SPARQLView view = (SPARQLView) getViewPart();
		Shell shell = view.getViewSite().getShell();
		FileDialog dialog = new FileDialog(shell, SWT.SAVE | SWT.SINGLE);
		
		
		if( PlatformUtil.isWIN32() ){
			dialog.setFilterExtensions(new String[]{
					"*.json",
					"*.rdf",
					"*.txt",
					"*.xml"
			});
			dialog.setFilterNames(new String[]{
					"JSON Files",
					"RDF Files",
					"Tab-separated Spreadsheet",
					"XML Files"
			});
			dialog.setText("Export result set to...");
		}
		else{ // Mac cannot support file extensions now, so manually do something
			List<String> strList = new ArrayList<String>();
			strList.add(".json");
			strList.add(".rdf");
			strList.add(".txt");
			strList.add(".xml");
			
			String str = "Export result set to...";
			String initEnd = ".rdf";
			
			PlatformUtil.generateFileDialogStr(dialog, strList, str, initEnd);
		}
		

		String firstFile = dialog.open();
		if(firstFile != null) {
			File file = new File(firstFile);
			try {
				OutputStream os = new FileOutputStream(file);
				if(firstFile.toLowerCase().endsWith("json")) {
					ResultSetFormatter.outputAsJSON(os, rs);
				}
				else if(firstFile.toLowerCase().endsWith("rdf")) {
					ResultSetFormatter.outputAsRDF(os, FileUtils.langXMLAbbrev, rs);
				}
				else if(firstFile.toLowerCase().endsWith("xml")) {
					ResultSetFormatter.outputAsXML(os, rs);
				}
				else {
					ResultsTableViewer resultsTableViewer = view.getResultsTableViewer();
					resultsTableViewer.setResultSet(query, rs, null);
					TableViewerUtil.exportTableViewer(file, resultsTableViewer);
				}
				os.close();
				
				CorePlugin.showSuccess("Export successful", 
						"The result set has been exported to " + firstFile + ".");
			}
			catch(Exception ex) {
				CorePlugin.showUserError("Could not write to " + firstFile, ex);
			}
			if( qexec != null ){
				qexec.close();
			}
		}
	}


	public void handleUpdate(UpdateChange change) {
		CorePlugin.showUserError("SPARQL Update calls cannot be saved.");
	}
}
