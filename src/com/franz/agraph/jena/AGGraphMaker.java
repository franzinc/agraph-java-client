/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.GraphMaker;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.shared.AlreadyExistsException;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * Implements the Jena GraphMaker interface for AllegroGraph.
 * 
 */
public class AGGraphMaker implements GraphMaker, Closeable {

	private AGRepositoryConnection conn;
	private ReificationStyle style;
	private AGGraph defaultGraph;
	
	// TODO make this persistent?
    protected Map<String, AGGraph> created = CollectionFactory.createHashedMap();

    public AGGraphMaker(AGRepositoryConnection conn) {
    	this(conn,ReificationStyle.Minimal);
	}

    public AGGraphMaker(AGRepositoryConnection conn, ReificationStyle style) {
    	this.conn = conn;
    	// It's common enough for Jena applications to use ResourceFactory to 
    	// create new blank nodes, so experimentally enable this by default
    	conn.getHttpRepoClient().setAllowExternalBlankNodeIds(true);
    	this.style = style;
    }
    
	public AGRepositoryConnection getRepositoryConnection() {
		return conn;
	}

	@Override
	public void close() {
	}

	@Override
	public AGGraph getGraph() {
		if (defaultGraph==null) {
			defaultGraph = new AGGraph(this, (Node)null);
		}
		return defaultGraph;
	}

	@Override
	public AGGraph createGraph() {
		// Get an unused blank node id from the server.  Note that using
		// createAnon() would generate an illegal id based on a uuid. 
		String id = conn.getValueFactory().createBNode().getID();
		Node anon = Node.createAnon(AnonId.create(id));
		return new AGGraph(this,anon);
	}

	@Override
	public AGGraph createGraph(String uri) {
		return createGraph(uri, false);
	}

	@Override
	public AGGraph createGraph(String uri, boolean strict) {
        AGGraph g = created.get( uri );
        if (g == null) {
        	Node node = Node.createURI(absUriFromString(uri));
        	g = new AGGraph(this, node);
        	created.put(uri, g);
        } else if (strict) {
        	throw new AlreadyExistsException( uri );
        }
		return g;
	}

	private String absUriFromString(String name) {
		String uri = name;
		if (name.indexOf(':') < 0) {
			// TODO: absolute uri's must contain a ':'
			// GraphMaker tests don't supply absolute URI's
			uri = "urn:x-franz:"+name;
		}
		return uri;
	}

	@Override
	public ReificationStyle getReificationStyle() {
		return style;
	}

	@Override
	public boolean hasGraph(String uri) {
		return null!=created.get(uri);
	}

	@Override
	public ExtendedIterator<String> listGraphs() {
		return new NiceIterator<String>().andThen(created.keySet().iterator());
	}

	@Override
	public AGGraph openGraph() {
		return getGraph();
	}

	@Override
	public AGGraph openGraph(String name) {
		return openGraph(name, false);
	}

	@Override
	public AGGraph openGraph(String uri, boolean strict) {
        AGGraph g = created.get( uri );
        if (g == null) {
        	if (strict) {
        		throw new DoesNotExistException( uri );
        	} else {
        		Node node = Node.createURI(absUriFromString(uri));
        		g = new AGGraph(this, node);
        		created.put(uri, g);
        	}
        }
		return g;
	}

	@Override
	public void removeGraph(String uri) {
        AGGraph g = created.get( uri );
        if (g == null) {
    		throw new DoesNotExistException( uri );
    	} else {
    		try {
				g.getConnection().clear(g.getGraphContext());
				created.remove(uri);
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
    	}
	}

	/**
	 * Returns the union of all graphs (includes default graph).
	 * 
	 * Add operations on this graph are housed in the
	 * default graph.
	 *  
	 * @return the union of all graphs.
	 */
	public AGGraph getUnionOfAllGraphs() {
		return createUnion();
	}

	/**
	 * Returns a graph that is the union of specified graphs.
	 * By convention, the first graph mentioned will be used
	 * for add operations on the union.  All other operations
	 * will apply to all graphs in the union.  If no graphs
	 * are supplied, the union of all graphs is assumed, and
	 * add operations apply to the default graph.
	 * 
	 * @param graphs the graphs in the union
	 * @return the union of the specified graphs
	 */
	public AGGraphUnion createUnion(AGGraph... graphs) {
		Set<Resource> contexts = new HashSet<Resource>();
		for (AGGraph g: graphs) {
			contexts.addAll(Arrays.asList(g.getGraphContexts()));
		}
		Resource context = null;
		if (graphs.length>0) {
			context = graphs[0].getGraphContext();
		}
		return new AGGraphUnion(this,context,contexts.toArray(new Resource[contexts.size()]));
	}

}
