package org.openrdf.repository.sail;

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

	/**
	 * Gets the default graph URIs of this dataset. An empty set indicates that
	 * the default graph is an empty graph.
	 */
	public Set<URI> getDefaultGraphs() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Gets the named graph URIs of this dataset. An empty set indicates that
	 * there are no named graphs in this dataset.
	 */
	public Set<URI> getNamedGraphs() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Include 'context' in the set of the named graph URIs for this dataset. 
	 */
	public void addNamedGraph(URI context) {
		
	}
	
	/**
	 * Include 'context' in the set of the default graph Resources for this dataset. 
	 */
	public void addDefaultGraph(Resource context) {
		
	}


}
