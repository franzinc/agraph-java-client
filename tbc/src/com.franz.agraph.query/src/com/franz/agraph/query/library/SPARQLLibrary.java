/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.topbraid.core.TB;
import org.topbraid.core.change.AbstractChangeEngineListener;
import org.topbraid.core.change.ChangeOperation;
import org.topbraid.core.change.IChangeEngineListener;
import org.topbraid.core.change.TripleChangeRecord;
import org.topbraid.core.model.Properties;
import org.topbraid.core.rdf.RDFModel;
import org.topbraid.eclipsex.log.Log;
import org.topbraid.jenax.model.RDFProperties;
import org.topbraid.sparql.SPARQLFactory;
import org.topbraid.spin.Activator;
import org.topbraid.spin.model.Query;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.util.SPINExpressions;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraidcomposer.core.TBC;
import org.topbraidcomposer.core.session.IModelSelectionListener;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
 * A singleton that can manage SPARQLLibraryEntries for the currently
 * active RDFModel.
 * 
 * @author Holger Knublauch
 */
public class SPARQLLibrary {
	
	/**
	 * Triple -> StatementEntry
	 */
	private Map<Triple,SPARQLLibraryEntry> entries;
	
	private Set<ISPARQLLibraryListener> listeners = new HashSet<ISPARQLLibraryListener>();
	
	private IChangeEngineListener changeEngineListener = new AbstractChangeEngineListener() {

		public void changeOperationPerformed(ChangeOperation operation, boolean undone) {
			RDFModel rdfModel = TB.getSession().getModel();
			if(entries != null && SP.exists(rdfModel)) {
				Set<Resource> properties = getQueryProperties(rdfModel);
				Iterator<TripleChangeRecord> it = operation.listRecords();
				while(it.hasNext()) {
					TripleChangeRecord r = it.next();
					Triple t = r.getTriple();
					Property p = ResourceFactory.createProperty(t.getPredicate().getURI());
					if(properties.contains(p)) {
						if(r.isAdded() == !undone) {
							addEntry(t);
						}
						else {
							removeEntry(t);
						}
					}
				}
			}
		}
	};
	
	private IModelSelectionListener modelSelectionListener = new IModelSelectionListener() {
		public void modelSelectionChanged() {
			entries = null;
		}
	};
	
	private static SPARQLLibrary singleton = new SPARQLLibrary();
	
	
	public SPARQLLibrary() {
		TBC.getSession().addModelSelectionListener(modelSelectionListener);
		TB.getSession().getChangeEngine().addChangeEngineListener(changeEngineListener);
	}
	
	
	private void addEntry(Triple t) {
		Node object = t.getObject();
		RDFModel model = TB.getSession().getModel();
		if(object.isLiteral()) { 
			String toString = object.getLiteralLexicalForm();
			if(SPARQLFactory.isQuery(model, toString)) {
				addEntry(t, model);
			}
		}
		else {
			Resource q = (Resource) model.asRDFNode(object);
			Query query = SPINFactory.asQuery(q);
			if(query != null) {
				addEntry(t, model);
			}
		}
	}


	private void addEntry(Triple t, OntModel model) {
		Statement s = model.asStatement(t);
		SPARQLLibraryEntry entry = new SPARQLLibraryEntry(s);
		entries.put(t, entry);
		Iterator<ISPARQLLibraryListener> it = new ArrayList<ISPARQLLibraryListener>(listeners).iterator();
		while(it.hasNext()) {
			it.next().sparqlLibraryEntryAdded(entry);
		}
	}
	
	
	public void addListener(ISPARQLLibraryListener listener) {
		listeners.add(listener);
	}
	
	
	private void ensureInitialized() {
		if(entries == null) {
			RDFModel rdfModel = TB.getSession().getModel();
			if(rdfModel != null) {
				entries = getAll(rdfModel);
			}
		}
	}
	
	
	public static SPARQLLibrary get() {
		return singleton;
	}
	
	
	private Map<Triple,SPARQLLibraryEntry> getAll(OntModel ontModel) {
		Map<Triple,SPARQLLibraryEntry> entries = new HashMap<Triple,SPARQLLibraryEntry>();
		for(Resource predicate : getQueryProperties(ontModel)) {
			StmtIterator it = ontModel.listStatements(null, RDFProperties.asProperty(predicate), (RDFNode)null);
			while(it.hasNext()) {
				Statement s = it.nextStatement();
				if(s.getObject().isLiteral() || (SPINFactory.asTemplateCall(s.getResource()) == null && !SPINExpressions.isExpression(s.getResource()))) {
					try {
						SPARQLLibraryEntry entry = new SPARQLLibraryEntry(s);
						entries.put(s.asTriple(), entry);
					}
					catch(Throwable t) {
						// Ignore
						Log.logWarning(Activator.PLUGIN_ID, "Invalid SPARQL library entry: " + s, t);
					}
				}
			}
		}
		return entries;
	}
	
	
	public Set<SPARQLLibraryEntry> getEntries() {
		ensureInitialized();
		if(entries == null) {
			return new HashSet<SPARQLLibraryEntry>();
		}
		else {
			return new HashSet<SPARQLLibraryEntry>(entries.values());
		}
	}
	
	
	public Set<SPARQLLibraryEntry> getEntriesWithSubject(Node subject) {
		Set<SPARQLLibraryEntry> results = new HashSet<SPARQLLibraryEntry>();
		for(Triple t : entries.keySet()) {
			if(t.getSubject().equals(subject)) {
				SPARQLLibraryEntry entry = entries.get(t);
				results.add(entry);
			}
		}
		return results;
	}

	
	private Set<Resource> getQueryProperties(OntModel ontModel) {
		Property queryProperty = ontModel.getProperty(SPIN.query.getURI());
		Set<Resource> properties = Properties.getAllSubProperties(queryProperty);
		properties.add(queryProperty);
		removeProperties(ontModel, SPIN.body, properties);
		removeProperties(ontModel, SPIN.constraint, properties);
		removeProperties(ontModel, SPIN.constructor, properties);
		removeProperties(ontModel, SPIN.rule, properties);
		return properties;
	}
	
	
	public void refresh() {
		entries = null;
	}
	
	
	private void removeEntry(Triple t) {
		SPARQLLibraryEntry entry = entries.get(t);
		if(entry != null) {
			entries.remove(t);
			Iterator<ISPARQLLibraryListener> it = new ArrayList<ISPARQLLibraryListener>(listeners).iterator();
			while(it.hasNext()) {
				it.next().sparqlLibraryEntryRemoved(entry);
			}
		}
	}
	
	
	public void removeListener(ISPARQLLibraryListener listener) {
		listeners.remove(listener);
	}
	
	
	private void removeProperties(Model model, Property property, Set<Resource> properties) {
		properties.remove(property);
		Property p = model.getProperty(property.getURI());
		properties.removeAll(Properties.getAllSubProperties(p));
	}
}
