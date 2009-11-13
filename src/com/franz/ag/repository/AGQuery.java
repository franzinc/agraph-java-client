package com.franz.ag.repository;

import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.Query;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.impl.MapBindingSet;

import com.franz.agbase.SPARQLQuery;
import com.franz.agsail.util.AGSInternal;

public class AGQuery implements Query {

	AGSInternal ags = null;
	SPARQLQuery sq = null;
	
	protected MapBindingSet bindings = new MapBindingSet();
	protected int maxQueryTime = 0;

	public AGQuery(AGSInternal ags, SPARQLQuery sq) {
		this.ags = ags;
		this.sq = sq;
		// TODO: only do this if engine isn't already set, when AG2 :with-variables is fixed
		sq.setEngine(SPARQLQuery.ENGINE.ALGEBRA);
	}

	public SPARQLQuery getSPARQLQuery() {
		return sq;
	}

	// Query API below
	
	public BindingSet getBindings() {
		return bindings;
	}

	public Dataset getDataset() {
		DatasetImpl dataset = new DatasetImpl();
		String[] defaultGraphURIs = sq.getFrom();
		for (String uristr : defaultGraphURIs) {
			dataset.addDefaultGraph(new URIImpl(uristr));			
		}
		String[] namedGraphURIs = sq.getFromNamed();
		for (String uristr : namedGraphURIs) {
			dataset.addNamedGraph(new URIImpl(uristr));			
		}
		return dataset;
	}

	public boolean getIncludeInferred() {
		return sq.isIncludeInferred();
	}

	public int getMaxQueryTime() {
		return maxQueryTime;
	}

	public void removeBinding(String name) {
		bindings.removeBinding(name);
	}

	public void setBinding(String name, Value value) {
		bindings.addBinding(name, value);
		Iterator<Binding> it = bindings.iterator();
		Object[] varvals = new Object[2*bindings.size()];
		for (int i=0; it.hasNext(); i+=2) {
			Binding b = it.next();
			varvals[i] = b.getName();
			varvals[i+1] = ags.coerceToAGPart(b.getValue());
		}
		// FIXME: needs bug18180 fixed
		sq.setWithVariables(ags.getDirectInstance(), varvals);
	}

	public void setDataset(Dataset dataset) {
		// specify default graphs 
		Set<URI> defaultGraphs = dataset.getDefaultGraphs();
		String[] defaultURIStrings = new String[defaultGraphs.size()];
		int i = 0;
		for (Iterator<URI> iterator = defaultGraphs.iterator(); iterator.hasNext();i++) {
			URI uri = iterator.next();
			defaultURIStrings[i] = uri.stringValue();
		}
		// FIXME: needs bug18181 fixed
		sq.setFrom(defaultURIStrings);
		
		// specify named graphs
		Set<URI> namedGraphs = dataset.getNamedGraphs();
		String[] namedURIStrings = new String[namedGraphs.size()];
		i = 0;
		for (Iterator<URI> iterator = namedGraphs.iterator(); iterator.hasNext();i++) {
			URI uri = iterator.next();
			namedURIStrings[i] = uri.stringValue();
		}
		sq.setFromNamed(namedURIStrings);
	}

	public void setIncludeInferred(boolean includeInferred) {
		sq.setIncludeInferred(includeInferred);
	}

	public void setMaxQueryTime(int maxQueryTime) {
		// TODO: can we support this in SPARQLQuery, rfe8384?
		this.maxQueryTime = maxQueryTime;
	}
	
}
