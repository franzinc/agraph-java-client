/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.ArrayList;

import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.Dataset;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.shared.AddDeniedException;
import com.hp.hpl.jena.shared.DeleteDeniedException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Implements the Jena Graph interface for AllegroGraph.
 * 
 */
public class AGGraph extends GraphBase implements Graph, Closeable {

	private final AGGraphMaker maker;
	private final Node graphNode;

	private final AGRepositoryConnection conn;
	private final AGValueFactory vf;
	private final Resource context;

	protected boolean inferred = false;
	
	AGGraph(AGGraphMaker maker, Node graphNode) {
		this.maker = maker;
		this.graphNode = graphNode;
		conn = maker.getRepositoryConnection();
		vf = conn.getValueFactory();
		context = vf.asResource(graphNode);
	}

	AGGraphMaker getGraphMaker() {
		return maker;
	}

	Node getGraphNode() {
		return graphNode;
	}

	public String getName() {
		if (graphNode == null)
			return "default-graph";
		return graphNode.toString();
	}
	
	Resource getGraphContext() {
		return context;
	}

	AGRepositoryConnection getConnection() {
		return conn;
	}

	//@Override
	//public void close() {
	//}

	@Override
	public BulkUpdateHandler getBulkUpdateHandler() {
		return new AGBulkUpdateHandler(this);
	}

    @Override
    public Capabilities getCapabilities()
    { 
    	if (capabilities == null) capabilities = new AGCapabilities();
    	return capabilities;
    }
    
	@Override
	public PrefixMapping getPrefixMapping() {
		return new AGPrefixMapping(this);
	}

	@Override
	public TransactionHandler getTransactionHandler() {
		return new AGTransactionHandler(this);
	}

	/*@Override
	public String toString() {
		if (graphNode == null)
			return "default-graph";
		return graphNode.toString();
	}*/
	@Override
    public String toString() 
        { return toString(getName()+(closed ? " (closed) " : " (size: " + graphBaseSize() + ")."),this); }

    /**
    Answer a human-consumable representation of <code>that</code>. The 
    string <code>prefix</code> will appear near the beginning of the string. Nodes
    may be prefix-compressed using <code>that</code>'s prefix-mapping. This
    default implementation will display all the triples exposed by the graph (ie
    including reification triples if it is Standard).
*/
public static String toString( String prefix, Graph that )
   {
   //PrefixMapping pm = that.getPrefixMapping();
	StringBuffer b = new StringBuffer( prefix + " {" );
	String gap = "";
	ClosableIterator<Triple> it = GraphUtil.findAll( that );
	int count = 0;
	while (it.hasNext() && count < 20) 
       {
		b.append( gap );
		gap = "; ";
		b.append( it.next().toString() );
	    } 
	b.append( "}" );
	return b.toString();
  }

	public Dataset getDataset() {
		DatasetImpl dataset = new DatasetImpl();
		if (context == null) {
			dataset.addDefaultGraph(null);
		} else if (context instanceof URI) {
			dataset.addDefaultGraph((URI) context);
			dataset.addNamedGraph((URI) context);
		}
		return dataset;
	}

	/*================
	 * 
	 * GraphBase methods that should be implemented or overridden
	 * 
	 *================*/

	@Override
	protected ExtendedIterator<Triple> graphBaseFind(TripleMatch m) {
		RepositoryResult<Statement> result;
		try {
			// TODO: allow arbitrary values in subject and predicate positions? 
			Node s = m.getMatchSubject();
			Node p = m.getMatchPredicate();
			// quickly return no results if RDF constraints for subject and predicate
			// are violated, as occurs in the Jena test suite for Graph. 
			if ((s!=null && s.isLiteral()) || 
					(p!=null && (p.isLiteral() || p.isBlank()))) {
				result = conn.createRepositoryResult(new ArrayList<Statement>());
			} else {
				result = conn.getStatements(vf.asResource(s), vf.asURI(p), vf.asValue(m
					.getMatchObject()), inferred, context);
			}
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return new AGTripleIterator(this, result);
	}

	@Override
	public void performAdd(Triple t) {
		try {
			conn.add(vf.asResource(t.getSubject()), vf.asURI(t.getPredicate()),
					vf.asValue(t.getObject()), context);
		} catch (UnauthorizedException e) {
			throw new AddDeniedException(e.getMessage());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void performDelete(Triple t) {
		try {
			conn.remove(vf.asResource(t.getSubject()), vf.asURI(t
					.getPredicate()), vf.asValue(t.getObject()), context);
		} catch (UnauthorizedException e) {
			throw new DeleteDeniedException(e.getMessage());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	protected int graphBaseSize() {
		// TODO deal with graphs bigger than int's.
		int size;
		try {
			size = (int) conn.size(context);
		} catch (RepositoryException e) {
			// TODO: proper exception to throw?
			throw new RuntimeException(e);
		}
		return size;
	}

}
