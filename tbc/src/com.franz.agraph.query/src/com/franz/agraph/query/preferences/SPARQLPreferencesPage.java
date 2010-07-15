/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.topbraid.sparql.Activator;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.ui.preferences.AbstractWorkbenchPreferencePage;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.util.Symbol;

public class SPARQLPreferencesPage extends AbstractWorkbenchPreferencePage {
	
	
	public SPARQLPreferencesPage() {
		
		super(CorePlugin.getDefault().getTBPreferenceStore(), 
				"Can be used to fine tune the performance of the Jena ARQ SPARQL engine");
		
		IPreferenceStore preferenceStore = CorePlugin.getDefault().getTBPreferenceStore();
		preferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String str = event.getProperty();
				Object newValue = event.getNewValue();
				ARQ.getContext().set(Symbol.create(str), Boolean.TRUE.equals(newValue));
			}
		});
	}

	
	@Override
	protected void createFieldEditors(Composite parent) {
		for(Symbol symbol : Activator.getPreferenceSymbols()) {
			String str = symbol.getSymbol();
			int hash = str.lastIndexOf('#');
			String label = hash > 0 ? str.substring(hash + 1) : str;
			addField(new BooleanFieldEditor(str, label, parent));
		}
	}
}
