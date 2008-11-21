package org.openrdf.repository.sail;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.Dataset;

/**
 * Represents a dataset against which queries can be evaluated. A dataset
 * consists of a default graph, which is the <a
 * href="http://www.w3.org/TR/rdf-mt/#defmerge">RDF merge</a> of one or more
 * graphs, and a set of named graphs. See <a
 * href="http://www.w3.org/TR/rdf-sparql-query/#rdfDataset">SPARQL Query
 * Language for RDF</a> for more info.
 */
public class AllegroDataset implements Dataset {
	private Set<URI> defaultGraphs = new HashSet<URI>();
	private Set<URI> namedGraphs = new HashSet<URI>();

	/**
	 * Gets the default graph URIs of this dataset.
	 * The Sesame 'Dataset' is prohibiting BNodes here; I don't know why. 
	 */
	public Set<URI> getDefaultGraphs() {
		return this.defaultGraphs;
	}

	/**
	 * Gets the named graph URIs of this dataset. An empty set indicates that
	 * there are no named graphs in this dataset.
	 */
	public Set<URI> getNamedGraphs() {
		return this.namedGraphs;
	}
	
	/**
	 * Include 'context' in the set of the named graph URIs for this dataset. 
	 */
	public void addNamedGraph(URI context) {
		this.namedGraphs.add(context);
	}
	
	/**
	 * Include 'context' in the set of the default graph Resources for this dataset. 
	 */
	public void addDefaultGraph(URI context) {
		this.defaultGraphs.add(context);
	}


}
