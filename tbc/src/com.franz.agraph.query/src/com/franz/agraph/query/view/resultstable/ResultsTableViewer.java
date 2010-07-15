/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.resultstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.PluginTransferData;
import org.topbraid.core.TB;
import org.topbraid.core.rdf.RDFModel;
import org.topbraid.eclipsex.log.Log;
import org.topbraid.strings.Labels;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.ui.IDs;
import org.topbraidcomposer.ui.RDFNodeTransfer;
import org.topbraidcomposer.ui.UIPlugin;
import org.topbraidcomposer.ui.util.Keys;
import org.topbraidcomposer.ui.util.Tables;
import org.topbraidcomposer.ui.viewers.tables.SwitchableTableSorter;
import org.topbraidcomposer.ui.views.ResourceSelectorDragSourceAdapter;

import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.query.SPARQLPlugin;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;


public class ResultsTableViewer extends TableViewer {
	
	// Most recently executed Query
	private AGQuery query;
	
	private List<String> vars;
	

	public ResultsTableViewer(Composite parent) {
		super(parent, SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		setLabelProvider(new ResultsTableLabelProvider(this));
		setContentProvider(new ResultsSetContentProvider());
		
		final Table table = getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				selectResource(e.x);
			}
		});
		
		Keys.addKeySelectAllSupport(table);
		
		Transfer[] transfers;
		if(UIPlugin.hasMoveResourcesDropAction()) {
			transfers = new Transfer[] {
				TextTransfer.getInstance(), 
				PluginTransfer.getInstance() 
			};
		}
		else {
			transfers = new Transfer[] {
					TextTransfer.getInstance() 
				};
		}
		addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers, 
				new DragSourceAdapter() {

					public void dragSetData(DragSourceEvent event) {
						
						RDFNode[] nodes = RDFNodeTransfer.getInstance().getNodes();
						RDFNodeTransfer.getInstance().setNodes(nodes);
						RDFNodeTransfer.getInstance().setDragSource(ResultsTableViewer.this);

						StringBuffer sb = new StringBuffer();
						Labels.append(sb, nodes, " ");
						if (PluginTransfer.getInstance().isSupportedType(event.dataType)) {
							byte[] segmentData = sb.toString().getBytes();
							event.data = new PluginTransferData(IDs.MOVE_RESOURCES_DROP_ACTION, segmentData);
							ResourceSelectorDragSourceAdapter.nodes = nodes;
						}
						else {
							event.data = sb.toString();
							ResourceSelectorDragSourceAdapter.nodes = null; 
						}
					}

					public void dragStart(DragSourceEvent event) {
						Collection<Resource> instances = getSelectedResources();
						RDFModel rdfModel = TB.getSession().getModel();
						RDFNode[] nodes = new RDFNode[instances.size()];
						Iterator<Resource> it = instances.iterator();
						for(int i = 0; it.hasNext(); i++) {
							nodes[i] = ((Resource)it.next()).inModel(rdfModel);
						}
						if(nodes.length > 0) {
							RDFNodeTransfer.getInstance().setNodes(nodes);
							RDFNodeTransfer.getInstance().setDragSource(ResultsTableViewer.this);
						}
						else {
							event.doit = false;
						}
					}
			
				});
	}
	
	
	public AGQuery getCurrentQuery() {
		return query;
	}
	
	
	@SuppressWarnings("unchecked")
	public List<QuerySolution> getSelectedQuerySolutions() {
		List<QuerySolution> results = new ArrayList<QuerySolution>();
		IStructuredSelection sel = (IStructuredSelection) getSelection();
		Iterator it = sel.iterator();
		while(it.hasNext()) {
			QuerySolution solution = (QuerySolution) it.next();
			results.add(solution);
		}		
		return results;
	}
	
	
	@SuppressWarnings("unchecked")
	public Set<Resource> getSelectedResources() {
		Set<Resource> results = new HashSet<Resource>();
		IStructuredSelection sel = (IStructuredSelection) getSelection();
		Iterator it = sel.iterator();
		while(it.hasNext()) {
			QuerySolution solution = (QuerySolution) it.next();
			RDFNode node = solution.get(getVar(0));
			if(node != null && node.isURIResource()) {
				results.add((Resource)node);
			}
		} 
		return results;
	}
	
	
	public String getVar(int index) {
		if(index < vars.size()) {
			return (String) vars.get(index);
		}
		else {
			return "@! 02";
		}
	}
	
	
	private void selectResource(int x) {
		RDFModel rdfModel = TB.getSession().getModel();
		if(rdfModel != null) {
			Table table = getTable();
			TableItem[] items = table.getSelection();
			if(items.length > 0) {
				QuerySolution solution = (QuerySolution) items[0].getData();
				int col = Tables.getColumnAtX(table, x);
				RDFNode node = solution.get(getVar(col));
				if(node != null && node.isResource()) {
					Resource r = (Resource) node.inModel(rdfModel);
					UIPlugin.navigateToResource(r);
				}
			}
		}
	}
	

	//@SuppressWarnings("unchecked")
	public boolean setResultSet(AGQuery query, ResultSet resultSet, List<QuerySolution> results) {
		
		this.query = query;

		vars = resultSet.getResultVars();
		
		Table table = getTable();
		
		try {
			while(table.getColumnCount() > 0) {
				table.getColumn(0).dispose();
			}
		}
		catch(Throwable t) {
			Log.logWarning(SPARQLPlugin.PLUGIN_ID, "Weird error (suspected Eclipse bug with SWT.VIRTUAL) ignored:", t);
		}
		
		TableLayout layout = new TableLayout();
		int x = vars.isEmpty() ? 100 : 100 / vars.size();
		for(int i = 0; i < vars.size(); i++) {
			String var = (String) vars.get(i);
			TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
			nameColumn.setText(var);
			layout.addColumnData(new ColumnWeightData(x, true));
		}
		table.setLayout(layout);
		
		// [GS] I am commenting out the following, becauses 
		// it causes Bug 242 (resizing bug) for this viewer
		// Tables.addColumnWidthsUpdater(table);
		ViewerSorter old = getSorter();
		if(old instanceof SwitchableTableSorter) {
			((SwitchableTableSorter)old).dispose();
		}
		super.setInput(Collections.EMPTY_LIST);
		setSorter(null);
		/* TODO: confirm that this logic is not needed for AG
		if(query.getOrderBy() == null || query.getOrderBy().isEmpty()) {
			setSorter(new SwitchableTableSorter(this) {

				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					int col = getActiveColumn();
					ResultsTableLabelProvider lp = (ResultsTableLabelProvider) getLabelProvider();
					RDFNode node1 = lp.getColumnNode(e1, col);
					RDFNode node2 = lp.getColumnNode(e2, col);
					if(node1 != null && node1.isLiteral() && 
						node2 != null && node2.isLiteral()) {
						Literal literal1 = (Literal)node1;
						Literal literal2 = (Literal)node2;
						if(literal1.getDatatype() != null && literal2.getDatatype() != null) {
							Object o1 = literal1.getValue();
							Object o2 = literal2.getValue();
							if(o1.getClass() == o2.getClass() && o1 instanceof Comparable) {
								if(isInverse()) {
									return ((Comparable)o2).compareTo(o1);
								}
								else {
									return ((Comparable)o1).compareTo(o2);
								}
							}
						}
					}
					return super.compare(viewer, e1, e2);
				}
			});
		}
		else {
			setSorter(null);
		}*/

		boolean complete = true;
		if(results == null) {
			int max = CorePlugin.getDefault().getScalabilityCap();
			results = new ArrayList<QuerySolution>();
			while(resultSet.hasNext() && results.size() < max) {
				results.add(resultSet.nextSolution());
			}
			if(resultSet.hasNext()) {
				complete = false;
			}
		}
		
		super.setInput(results);
		refreshTableColumnsBounds();
		table.getParent().layout();
		
		return complete;
	}
	
	
	public void refreshTableColumnsBounds(){
		TableColumn[] columns = getTable().getColumns();
		if(columns.length > 0) {
			int width = getControl().getSize().x - 30;
			int columnWidth = width / columns.length;
			int columnCount = getTable().getColumnCount();
			for(int i = 0; i < columnCount; i++){
				TableColumn col = columns[i];
				col.setWidth(columnWidth);
			}
		}
	}
}
