/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.openrdf.query.QueryLanguage;
import org.topbraid.core.TB;
import org.topbraid.core.change.AbstractChange;
import org.topbraid.core.change.IChange;
import org.topbraid.core.images.ImageMetadata;
import org.topbraid.core.model.DeletableStatement;
import org.topbraid.core.rdf.RDFModel;
import org.topbraid.core.session.ISession;
import org.topbraid.eclipsex.log.Log;
import org.topbraid.jenax.model.JenaUtil;
import org.topbraid.sparql.SPARQLFactory;
import org.topbraid.sparql.drivers.ISPARQLDriver;
import org.topbraid.sparql.drivers.SPARQLDrivers;
import org.topbraid.sparql.update.UpdateChange;
import org.topbraid.spin.editor.SPINCommandRowEditorDriver;
import org.topbraid.spin.system.ARQFactory;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.core.TBC;
import org.topbraidcomposer.core.session.IModelSelectionListener;
import org.topbraidcomposer.core.session.IResourceSelectionListener;
import org.topbraidcomposer.core.util.ThreadUtil;
import org.topbraidcomposer.ui.IDs;
import org.topbraidcomposer.ui.IRefreshableWorkbenchPart;
import org.topbraidcomposer.ui.TBCImages;
import org.topbraidcomposer.ui.UIPlugin;
import org.topbraidcomposer.ui.actions.AbstractGreyedImageAction;
import org.topbraidcomposer.ui.actions.AbstractToolTipAction;
import org.topbraidcomposer.ui.text.StyledTextEditOperation;
import org.topbraidcomposer.ui.views.AbstractView;

import com.franz.agraph.graphstore.AllegroGraphStore;
import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.jena.AGReasoner;
import com.franz.agraph.query.SPARQLPlugin;
import com.franz.agraph.query.resultsview.IResultsHandler;
import com.franz.agraph.query.view.editor.QueryEditor;
import com.franz.agraph.query.view.libtable.LibraryViewer;
import com.franz.agraph.query.view.resultstable.ConstructedGraphTableViewer;
import com.franz.agraph.query.view.resultstable.ResultsTableViewer;
import com.franz.agraph.repository.AGQueryLanguage;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;


/**
 * The AllegroGraph View in TBC.
 *
 */
public class SPARQLView extends AbstractView implements IModelSelectionListener, IRefreshableWorkbenchPart, IResourceSelectionListener, ISPARQLView, IStatusUpdater {
	
	private QueryEditor editor;
	
	private String errorMessage;
	
	public final static int QUERY_TEXT_PROPERTY_ID = 777;
	
	private IAction runQueryAction = new AbstractGreyedImageAction("Execute Query", TBCImages.RUN_QUERY) {
		public void run() {
			executeQuery();
		}
	};
	
	private IAction runWithPrologSelectAction;

	private IAction runWithInferencesAction;
	
	// Results are displayed either in a ResultsTableViewer or in a ConstructedGraphTableViewer
	
	private ConstructedGraphTableViewer constructedGraphTableViewer;
	
	private LibraryViewer libraryViewer;
	
	private ResultsTableViewer resultsTableViewer;
	
	private SashForm sashForm;
	
	private TabFolder tabFolder;
	
	
	public void assertConstruct(String title) {
		RDFModel rdfModel = TB.getSession().getModel();
		if(constructedGraphTableViewer != null && rdfModel != null) {
			final Model baseModel = TB.getSession().getSelectedBaseModel();
			addConstructedTriples(title, baseModel);
		}
	}
	

	protected void createMainControls(final Composite parent) {
		
		sashForm = new SashForm(parent, SWT.HORIZONTAL);
		
		tabFolder = new TabFolder(sashForm, SWT.TOP);
		
		editor = new QueryEditor(tabFolder);
		StyledTextEditOperation.initUndo(editor.getStyledText(), getViewSite());

		editor.getControl().addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CR && e.stateMask != 0) {
					executeQuery();
				}
			}

			public void keyReleased(KeyEvent e) {
			}
		});
		TabItem editorTab = new TabItem(tabFolder, 0);
		editorTab.setControl(editor.getControl());
		editorTab.setText("Query Editor");
		
		libraryViewer = new LibraryViewer(tabFolder, this);
		libraryViewer.getTableViewer().getTable().addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if(e.detail == SWT.CHECK) {
					updateStatus();
				}
			}
		});
		
		TabItem libraryTab = new TabItem(tabFolder, 0);
		libraryTab.setControl(libraryViewer);
		libraryTab.setText("Query Library");
		libraryTab.setToolTipText(
				"Manages SPARQL queries stored in values of the spin:query property.\n" +
				"In order to use this feature, your model should import\n" +
				SPIN.BASE_URI + "\n" +
				"A copy of this can be found in the TBC folder when you have\n" +
				"imported Composer's ontology library.");
		
		resultsTableViewer = new ResultsTableViewer(sashForm);
		TBC.getSession().addModelSelectionListener(this);
		TBC.getSession().addResourceSelectionListener(this);
		
		editor.getStyledText().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});
		
		runWithPrologSelectAction = new AbstractToolTipAction("Use Prolog Select",
				TBC.getImageDescriptor(new ImageMetadata(SPARQLPlugin.PLUGIN_ID, "QueryBaseModel")), Action.AS_CHECK_BOX) {
			public void run() {
			}
		};
		runWithPrologSelectAction.setDisabledImageDescriptor(TBC.getImageDescriptor(new ImageMetadata(SPARQLPlugin.PLUGIN_ID, "QueryBaseModel.greyed")));
		
		String imageName = "RunOnInferenceGraph";
		runWithInferencesAction = new AbstractToolTipAction("Use RDFS++ Inference",
				TBC.getImageDescriptor(new ImageMetadata(SPARQLPlugin.PLUGIN_ID, imageName)), Action.AS_CHECK_BOX) {
			public void run() {
				//TODO: runWithPrologSelectAction.setEnabled(!runWithInferencesAction.isChecked());
			}
		};
		runWithInferencesAction.setDisabledImageDescriptor(TBC.getImageDescriptor(new ImageMetadata(SPARQLPlugin.PLUGIN_ID, imageName + ".greyed")));
	
		getViewSite().getActionBars().getToolBarManager().add(runQueryAction);
		if(TB.hasNature(TB.NATURE_TBC_SE)) {
			getViewSite().getActionBars().getToolBarManager().add(runWithInferencesAction);
		}
		getViewSite().getActionBars().getToolBarManager().add(runWithPrologSelectAction);
		
		tabFolder.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				updateStatus();
			}
		});
		
		updateStatus();
	}

	
	public Query createQuery(String queryString, final Model model, boolean checkingOnly) {
		if(runWithPrologSelectAction.isChecked()) {
			for(ISPARQLDriver driver : SPARQLDrivers.getDrivers()) {
				Query query = driver.createQuery(queryString, model, checkingOnly);
				if(query != null) {
					return query;
				}
			}
		}
		return ARQFactory.get().createQuery(model, queryString);
	}


	public QueryExecution createQueryExecution(Query query, Model queryModel) {
		if(runWithPrologSelectAction.isChecked()) {
			for(ISPARQLDriver driver : SPARQLDrivers.getDrivers()) {
				QueryExecution qexec = driver.createQueryExecution(query, queryModel);
				if(qexec != null) {
					return qexec;
				}
			}
		}
		return SPARQLFactory.createQueryExecution(query, queryModel);
	}
	
	
	public void displayResultSet(AGQuery query, ResultSet rs) {
		ensureResultsTableViewer();
        resultsTableViewer.setResultSet(query, rs, null);
	}
	
	
	public void displayStatements(Collection<Statement> ss) {
		ensureStatementTableViewer();
		constructedGraphTableViewer.setInput(ss.toArray());
	}
	
	
	public void dispose() {
		super.dispose();
		libraryViewer.dispose();
		TBC.getSession().removeModelSelectionListener(this);
		TBC.getSession().removeResourceSelectionListener(this);
	}
	
	
	private void ensureStatementTableViewer() {
		if(constructedGraphTableViewer == null) {
			resultsTableViewer.getControl().dispose();
			resultsTableViewer = null;
			constructedGraphTableViewer = new ConstructedGraphTableViewer(sashForm);
			sashForm.layout();
		}
	}
	
	
	private void ensureResultsTableViewer() {
		if(resultsTableViewer == null) {
			constructedGraphTableViewer.getControl().dispose();
			constructedGraphTableViewer = null;
			resultsTableViewer = new ResultsTableViewer(sashForm);
		}
	}
	
	
	public void executeQuery() {
		executeQuery((Runnable)null);
	}	
	
	
	public void executeQuery(final Runnable andThen) {
		executeQuery(new IResultsHandler() {
			
			private List<QuerySolution> nextResults;
			
			public void handleAsk(final List<Integer> fails) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						boolean value = fails.isEmpty();
						String str = "ASK result is " + value;
						if(fails.size() > 0 && !isEditorVisible()) {
							str += " at the selected queries";
							libraryViewer.selectQueries(fails);
						}
						getViewSite().getActionBars().getStatusLineManager().setMessage(str);
					}
				});
			}
			
			
			public void handleConstructedModel(Model model) {
				ensureStatementTableViewer();
				List<Statement> statements = new ArrayList<Statement>();
				RDFModel rdfModel = TB.getSession().getModel();
				StmtIterator it = model.listStatements();
				while(it.hasNext()) {
					Statement s = it.nextStatement();
					s = rdfModel.asStatement(s.asTriple());
					s = new DeletableStatement(s.getSubject(), s.getPredicate(), s.getObject(), (ModelCom) rdfModel);
					statements.add(s);
				}
				constructedGraphTableViewer.setInput(statements.toArray());
				if(andThen != null) {
					andThen.run();
				}
			}

			public void handleResultSet(AGQuery query, QueryExecution qexec, ResultSet rs) {
				ensureResultsTableViewer();
		        resultsTableViewer.setResultSet(query, rs, nextResults);
				int max = CorePlugin.getDefault().getScalabilityCap();
				if(nextResults != null && nextResults.size() >= max) {
					setContentDescription(CorePlugin.getDefault().getScalabilityCapMessage());
				}
				else {
					setContentDescription("");
				}
				if(andThen != null) {
					andThen.run();
				}
			}
			
			public void handleResultSet(QueryExecution qexec, ResultSet rs, IProgressMonitor monitor) {
				int max = CorePlugin.getDefault().getScalabilityCap();
				nextResults = new ArrayList<QuerySolution>();
				monitor.setTaskName("Retrieving query results...");
				try {
					while(rs.hasNext() && nextResults.size() < max && (monitor == null || !monitor.isCanceled())) {
						nextResults.add(rs.next());
						if((nextResults.size() % 10) == 0) {
							monitor.subTask("Result " + nextResults.size());
						}
					}
				}
				catch(Throwable t) {
					if(!(t instanceof ThreadDeath)) {
						CorePlugin.showUserError("Failed to retrieve all query results", t);
					}
				}
				qexec.close();
			}


			public void handleUpdate(UpdateChange change) {
				UIPlugin.getDefault().showView(IDs.OPERATIONS_VIEW, getSite().getPage());
			}
		});
	}
	
	
	public void executeQuery(final IResultsHandler handler) {
		final String queryString = getQueryString();
		try {
			String str = SPARQLFactory.createPrefixDeclarations() + queryString;
			UpdateRequest updateRequest = UpdateFactory.create(str);
			if(updateRequest != null) {
				final UpdateChange change = new UpdateChange(updateRequest, queryString);
				TBC.getSession().getChangeEngine().execute(change, new Runnable() {
					public void run() {
						handler.handleUpdate(change);
					}
				});
				return;
			}
		}
		catch(Throwable t) {
			// Ignore
		}
		
		String[] split = SPARQLView.splitQueries(queryString);
		final boolean construct = SPARQLFactory.isConstruct(split[0]);
		
		try {
			final List<Object> results = new LinkedList<Object>();
			
	        final IRunnableWithProgress r = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						if(construct) {
							runConstruct(queryString, results, monitor);
						}
						else if(isAsk(queryString)) {
							runAsk(handler, queryString, monitor);
						}
						else {
							runSelectOrDescribe(handler, queryString, results, monitor);
						}
					}
					catch(Throwable t) {
						throw new InvocationTargetException(t);
					}
				}

				private void runSelectOrDescribe(final IResultsHandler handler,
						final String queryString,
 						final List<Object> results, final IProgressMonitor monitor)
						throws InterruptedException {
					monitor.setTaskName("Preparing query model...");
					AGModel queryModel = getQueryModel(monitor, false);
					if(queryModel != null) {
						QueryLanguage ql = QueryLanguage.SPARQL;
						if (runWithPrologSelectAction.isChecked()) {
							ql = AGQueryLanguage.PROLOG;
						}
						final AGQuery query = AGQueryFactory.create(ql,queryString);
						final QueryExecution qexec = AGQueryExecutionFactory.create(query, queryModel);
						//setInitialMapping(qexec); TODO?
						monitor.setTaskName("Executing query...");
						if(queryString.contains("describe")) {
							// TODO fix the above hackery
							Model model = qexec.execDescribe();
							if(!monitor.isCanceled()) {
								results.add(model);
							}
						} else {
							ResultSet rs = qexec.execSelect();
							if(!monitor.isCanceled()) {
								results.add(rs);
								results.add(query);
								handler.handleResultSet(qexec, rs, monitor);
							}
						}
					}
				}

				private void runAsk(final IResultsHandler handler,
					final String queryString,
					final IProgressMonitor monitor)
					throws InterruptedException {
					final List<Integer> fails = new ArrayList<Integer>();
					monitor.setTaskName("Preparing query model...");
					final AGModel queryModel = getQueryModel(monitor, false);
					if(queryModel != null) {
						String[] queries = splitQueries(queryString);
						for(int i = 0; i < queries.length; i++) {
							final AGQuery query = AGQueryFactory.create(queryString);
							final QueryExecution qexec = AGQueryExecutionFactory.create(query, queryModel);
							setInitialMapping(qexec);
							monitor.setTaskName("Executing query...");
							boolean ask = qexec.execAsk();
							if(!ask) {
								fails.add(i);
							}
						}
					}
					if(!monitor.isCanceled()) {
						monitor.done();
						handler.handleAsk(fails);
					}
				}

				private void runConstruct(final String queryString,
						final List<Object> results,
						final IProgressMonitor monitor)
						throws InterruptedException {
					Model model = null;
					final AGModel[] queryModels = new AGModel[1];
					String[] queries = SPARQLView.splitQueries(queryString);
					for(int i = 0; i < queries.length && !monitor.isCanceled(); i++) {
					    final Model[] ms = new Model[1];
						if(queryModels[0] == null) {
			        		monitor.setTaskName("Preparing query model...");
			        		queryModels[0] = getQueryModel(monitor, false);
						}
						AGModel queryModel = queryModels[0];
						final AGQuery query = AGQueryFactory.create(queryString);
						final QueryExecution qexec = AGQueryExecutionFactory.create(query, queryModel);
				        setInitialMapping(qexec);
			    		monitor.setTaskName("Executing query...");
				        ms[0] = qexec.execConstruct();

					    Model m = ms[0];
					    if(m == null) {
					    	m = JenaUtil.createDefaultModel();
					    }
					    if(model == null) {
					    	if(queries.length == 1) {
					    		model = m;
					    	}
					    	else {
					    		model = JenaUtil.createDefaultModel();
					    		model.add(m);
					    	}
					    }
					    else {
					    	model.add(m);
					    }
					}
					results.add(model);
				}
			};
			
			if(ThreadUtil.runCancelable("Could not execute query", r)) {
				if(!results.isEmpty()) {
					Object result = results.get(0);
					if(result instanceof Model) {
						handler.handleConstructedModel((Model)result);
					}
					else if(result instanceof ResultSet) {
						AGQuery query = (AGQuery) results.get(1);
						handler.handleResultSet(query, null, (ResultSet)result);
					}
				}
			}
		}
		catch(Throwable t) {
			setError(t.getMessage(), construct);
			Log.logWarning(SPARQLPlugin.PLUGIN_ID, "Failed to execute query", t);
		}
	}
	
	
	public void executeQuery(String query) {
		setQuery(query);
		executeQuery();
	}
	
	
	public static SPARQLView get() {
		try {
			return (SPARQLView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.topbraidcomposer.sparql.view");
		}
		catch(PartInitException ex) {
		}
		return null;
	}
	
	
	public AGQuery getCurrentQuery() {
		if(resultsTableViewer != null) {
			return resultsTableViewer.getCurrentQuery();
		}
		else {
			return null;
		}
	}


	public String getErrorMessage() {
		return errorMessage; 
	}
	
	
	private AGModel getQueryModel(IProgressMonitor monitor, boolean justChecking) {
		ISession s = TB.getSession();
		URI uri = s.getSelectedBaseURI();
		AGGraph graph = AllegroGraphStore.getAGGraph(uri);
		AGModel model = new AGModel((AGGraph)graph);
		if(runWithInferencesAction.isChecked() && !justChecking) {
			AGReasoner reasoner = new AGReasoner();
			AGInfModel infmodel = new AGInfModel(reasoner, model);
			return infmodel;
		} else {
			return model;
		}
	}

	
	public String getQueryString() {
		return getRawQueryString();
	}


	public List<QuerySolution> getSelectedQuerySolutions() {
		if(resultsTableViewer != null) {
			return resultsTableViewer.getSelectedQuerySolutions();
		}
		else {
			return new ArrayList<QuerySolution>();
		}
	}


	public String getSPARQLExpression() {
		final String[] ss = new String[1];
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				ss[0] = libraryViewer.getQueryText();
			}
		});
		while(ss[0] == null) {
			try {
				Thread.sleep(100);
			}
			catch(Throwable t) {
			}
		}
		return ss[0];
	}


	private String getRawQueryString() {
		if(isEditorVisible()) { 
			return editor.getQueryText();
		}
		else {
			return libraryViewer.getQueryText();
		}
	}
	
	
	public ResultsTableViewer getResultsTableViewer() {
		return resultsTableViewer;
	}
	
	
	public void inferConstruct(String title) {
		RDFModel rdfModel = TB.getSession().getModel();
		if(constructedGraphTableViewer != null && rdfModel != null) {
			final Model infModel = rdfModel.getInferenceModel();
			addConstructedTriples(title, infModel);
		}
	}


	private void addConstructedTriples(String title, final Model infModel) {
		Statement[] ss = constructedGraphTableViewer.getStatements();
		final List<Statement> toAdd = new LinkedList<Statement>();
		for(int i = 0; i < ss.length; i++) {
			if(!infModel.contains(ss[i])) {
				toAdd.add(ss[i]);
			}
		}
		if(!toAdd.isEmpty()) {
			IChange change = new AbstractChange(title) {
				public void execute(ISession session, IProgressMonitor monitor) throws InterruptedException {
					Iterator<Statement> it = toAdd.iterator();
					while(it.hasNext()) {
						Statement s = it.next();
						infModel.add(s);
					}
				}
			};
			TB.getSession().getChangeEngine().execute(change);
		}
	}


	/**
	 * Checks whether a given query represents an ASK query.
	 * @param queryString  the string to check
	 * @return true  if it's an ASK query
	 */
	private static boolean isAsk(final String queryString) {
		try {
			String[] queryStrings = SPARQLView.splitQueries(queryString);
			Query query = ARQFactory.get().createQuery(TB.getSession().getModel(), queryStrings[0]);
			if(query != null) {
				return query.isAskType();
			}
			else {
				return false;
			}
		}
		catch(Throwable t) {
			// Ignore
			return false;
		}
	}


	private boolean isEditorVisible() {
		return tabFolder.getSelectionIndex() == 0;
	}


	public Iterator<Statement> listConstructedStatements() {
		if(constructedGraphTableViewer != null) {
			return constructedGraphTableViewer.listAllStatements();
		}
		else {
			return null;
		}
	}


	public void modelSelectionChanged() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if(!tabFolder.isDisposed()) {
					updateStatus();
				}
			}
		});
	}
	
	
	public void refreshWorkbenchPart(boolean structural) {
		libraryViewer.getTableViewer().refresh();
	}


	public void resourceSelected(Resource resource) {
		if(resource != null) {
			asyncExec(new Runnable() {
				public void run() {
					try {
						String q = getRawQueryString();
						if(q.contains("?" + SPIN.THIS_VAR_NAME) || q.contains("$" + SPIN.THIS_VAR_NAME)) {
							final String queryString = getQueryString();
							Model model = TB.getSession().getModel();
							ARQFactory.get().createQuery(model, queryString);
							executeQuery();
						}
					}
					catch(Throwable t) {
						// Ignore parsing error above - this may be normal if
						// the current query refers to namespaces that are not
						// defined in the newly switched-to model
					}
				}
			});
		}
	}


	private void setError(String message, boolean construct) {
		if(message != null) {
			message = SPINCommandRowEditorDriver.getErrorMessage(message);
		}
		this.errorMessage = message;
		getViewSite().getActionBars().getStatusLineManager().setErrorMessage(message);
		runQueryAction.setEnabled(errorMessage == null);
		// runWithInferencesAction.setEnabled(errorMessage == null);
		// constructInferAction.setEnabled(errorMessage == null && construct);
	}


	public void setFocus() {
		if(isEditorVisible()) {
			editor.getStyledText().setFocus();
		}
		else {
			libraryViewer.forceFocus();
		}
	}


	/**
	 * Sets the initial mapping of the variable ?this to the
	 * currently selected resource.
	 * @param qexec  the QueryExecution to operate on
	 */
	private void setInitialMapping(QueryExecution qexec) {
		Resource sel = TBC.getSession().getSelectedResource();
		SPARQLFactory.setInitialMapping(qexec, sel);
	}


	public void setQuery(String query) {
		editor.getStyledText().setText(query);
	}

	
	private static String[] splitQueries(String str) {
		List<String> queries = new LinkedList<String>();
		StringReader r = new StringReader(str);
		BufferedReader br = new BufferedReader(r);
		try {
			StringBuffer q = new StringBuffer();
			for(;;) {
				String line = br.readLine();
				if(line == null) {
					break;
				}
				if(line.trim().length() == 0) {
					if(q.length() > 0) {
						queries.add(q.toString());
					}
					q.setLength(0);
				}
				else {
					q.append(line);
					q.append("\n");
				}
			}
			if(q.length() > 0) {
				queries.add(q.toString());
			}
		}
		catch(IOException ex) {
			// Won't happen
		}
		return (String[]) queries.toArray(new String[0]);
	}
	
	
	public void toggleLayout() {
		int newOrientation = sashForm.getOrientation() == SWT.HORIZONTAL ? SWT.VERTICAL : SWT.HORIZONTAL;
		sashForm.setOrientation(newOrientation);
	}
	
	
	public void updateStatus() {
		// TODO determine when this is necessary for AG
		/*
		RDFModel rdfModel = TB.getSession().getModel();
		if(rdfModel == null) {
			return;
		}
		String queryString = getQueryString();
		String newError = null;

		boolean isUpdate = false; 
		try {
			String str = SPARQLFactory.createPrefixDeclarations() + queryString;
			UpdateFactory.create(str);
			isUpdate = true;
		}
		catch(Throwable t) {
			// TODO: Better error handling for these guys
		}
		
		boolean construct = false;
		String[] split = SPARQLView.splitQueries(queryString);
		if(split.length > 0) {
			construct = SPARQLFactory.isConstruct(split[0]);
		}
		if(!isUpdate) {
			String[] queries;
			if(queryString.trim().toUpperCase().startsWith("SELECT")) {
				queries = new String[] {
					queryString	
				};
			}
			else {
				queries = SPARQLView.splitQueries(queryString);
			}
			if(queryString.length() == 0) {
				newError = "No query specified";
			}
			else {
				int previousType = -1;
				for(int i = 0; i < queries.length; i++) {
					try {
						String str = queries[i];
						Model queryModel = getQueryModel(null, true);
						Query query = createQuery(str, queryModel, true);
						int type = query.getQueryType();
						if(i > 0 && type != previousType) {
				        	newError = "Cannot mix ASK, CONSTRUCT and SELECT queries";
						}
						else {
							previousType = type;
							QueryExecutionFactory.create(query, rdfModel);
							if(!query.isSelectType() && !query.isConstructType() && !query.isAskType() && !query.isDescribeType()) {
								newError = "Only ASK, CONSTRUCT, DESCRIBE and SELECT queries are supported";
							}
					        else if(queries.length > 1 && query.isSelectType()) {
					        	newError = "Only one SELECT query can be processed";
					        }
				        }
					}	
					catch(Throwable ex) {
						newError = SPINCommandRowEditorDriver.extractErrorMessage(i, ex);
						break;
					}
				}
			}
		}
        setError(newError, construct);
		firePropertyChange(QUERY_TEXT_PROPERTY_ID);*/
	}
}
