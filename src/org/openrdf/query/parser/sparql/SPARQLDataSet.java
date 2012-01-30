/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.parser.sparql;

import java.util.HashMap;
import java.util.Set;

public class SPARQLDataSet {

	private HashMap<String, String> namedGraphs = new HashMap<String, String>();

	private String defaultGraph;

	public SPARQLDataSet() {
	}

	public SPARQLDataSet(String defaultGraph) {
		this();
		setDefaultGraph(defaultGraph);
	}

	public void setDefaultGraph(String defaultGraph) {
		this.defaultGraph = defaultGraph;
	}

	public String getDefaultGraph() {
		return defaultGraph;
	}

	public void addNamedGraph(String graphName, String graphLocation) {
		namedGraphs.put(graphName, graphLocation);
	}

	public boolean hasNamedGraphs() {
		return (!namedGraphs.isEmpty());
	}

	public Set<String> getGraphNames() {
		return namedGraphs.keySet();
	}

	public String getGraphLocation(String graphName) {
		return namedGraphs.get(graphName);
	}
}
