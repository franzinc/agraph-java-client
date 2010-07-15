/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.libtable;

import java.util.Set;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.topbraidcomposer.core.TBC;
import org.topbraidcomposer.core.session.DisplayLabelsUpdater;
import org.topbraidcomposer.core.session.ILabelListener;
import org.topbraidcomposer.core.session.IModelSelectionListener;
import org.topbraidcomposer.ui.util.Keys;
import org.topbraidcomposer.ui.util.Tables;
import org.topbraidcomposer.ui.viewers.tables.SwitchableTableSorter;

import com.franz.agraph.query.library.ISPARQLLibraryListener;
import com.franz.agraph.query.library.SPARQLLibrary;
import com.franz.agraph.query.library.SPARQLLibraryEntry;
import com.franz.agraph.query.view.IStatusUpdater;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class LibraryTableViewer extends TableViewer implements ILabelListener {
	
	private ISPARQLLibraryListener libraryListener = new ISPARQLLibraryListener() {

		public void sparqlLibraryEntryAdded(final SPARQLLibraryEntry entry) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if(!getTable().isDisposed()) {
						add(entry);
						TableItem item = (TableItem) findItem(entry);
						if(item != null) {
							item.setChecked(true);
						}
						statusUpdater.updateStatus();
					}
				}
			});
		}

		public void sparqlLibraryEntryRemoved(final SPARQLLibraryEntry entry) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if(!getTable().isDisposed()) {
						remove(entry);
						statusUpdater.updateStatus();
					}
				}
			});
		}
	};
	
	private IModelSelectionListener modelSelectionListener = new IModelSelectionListener() {
		public void modelSelectionChanged() {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if(!getTable().isDisposed()) {
						setInput("");
						setAllItemsChecked(true);
					}
				}
			});
		}
	};
	
	private Shell shell;
	
	private IStatusUpdater statusUpdater;
	
	
	public LibraryTableViewer(Composite parent, IStatusUpdater statusUpdater) {
		
		super(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		
		shell = parent.getShell();
		this.statusUpdater = statusUpdater;
		
		setLabelProvider(new LibraryTableLabelProvider());
		setContentProvider(new LibraryTableContentProvider());
		Keys.addKeySelectAllSupport(getTable());

		Table table = getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableLayout tableLayout = new TableLayout();

		TableColumn queryColumn = new TableColumn(table, SWT.LEFT);
		queryColumn.setText("Query Expression");
		tableLayout.addColumnData(new ColumnWeightData(70, true));

		TableColumn subjectColumn = new TableColumn(table, SWT.LEFT);
		subjectColumn.setText("Associated Subject");
		tableLayout.addColumnData(new ColumnWeightData(30, true));
		
		table.setLayout(tableLayout);
		
		Tables.addColumnWidthsUpdater(table);
		setSorter(new SwitchableTableSorter(this));
		
		setInput("");
		setAllItemsChecked(true);
		
		SPARQLLibrary.get().addListener(libraryListener);
		TBC.getSession().addModelSelectionListener(modelSelectionListener);
		
		DisplayLabelsUpdater.get().addListener(this);
		
		getTable().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				handleDoubleClick();
			}
		});
	}
	
	
	public void dispose() {
		SPARQLLibrary.get().removeListener(libraryListener);
		TBC.getSession().removeModelSelectionListener(modelSelectionListener);
		DisplayLabelsUpdater.get().removeListener(this);
	}
	
	
	public String getQueryText() {
		StringBuffer sb = new StringBuffer();
		TableItem[] items = getTable().getItems();
		for(int i = 0; i < items.length; i++) {
			TableItem item = items[i];
			if(item.getChecked()) {
				SPARQLLibraryEntry entry = (SPARQLLibraryEntry) item.getData();
				sb.append(entry.getText());
				sb.append("\n\n");
			}
		}
		return sb.toString();
	}
	
	
	private void handleDoubleClick() {
		IStructuredSelection sel = (IStructuredSelection) getSelection();
		if(!sel.isEmpty()) {
			SPARQLLibraryEntry entry = (SPARQLLibraryEntry) sel.getFirstElement();
			Statement s = entry.getStatement();
			Resource subject = s.getSubject();
			if(subject.isURIResource()) {
				TBC.getSession().setSelectedResource(subject);
			}
		}
	}


	public void labelsChanged(Set<Resource> resources) {
		for(Resource resource : resources) {
			for(final SPARQLLibraryEntry entry : SPARQLLibrary.get().getEntriesWithSubject(resource.asNode())) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if(!getTable().isDisposed()) {
							update(entry, null);
							statusUpdater.updateStatus();
						}
					}
				});
			}
		}
	}


	public void setAllItemsChecked(boolean value) {
		TableItem[] items = getTable().getItems();
		for(int i = 0; i < items.length; i++) {
			TableItem item = items[i];
			item.setChecked(value);
		}
	}
}
