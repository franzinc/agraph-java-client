/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
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
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.StatementCollector;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Reifier;
import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.graph.impl.ReifierFragmentHandler;
import com.hp.hpl.jena.graph.impl.ReifierFragmentsMap;
import com.hp.hpl.jena.graph.impl.ReifierTripleMap;
import com.hp.hpl.jena.graph.impl.SimpleReifier;
import com.hp.hpl.jena.shared.AddDeniedException;
import com.hp.hpl.jena.shared.DeleteDeniedException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

/**
 * Implements the Jena Graph interface for AllegroGraph.
 * 
 */
public class AGGraph extends GraphBase implements Graph, Closeable {

	protected final AGGraphMaker maker;
	protected final Node graphNode;

	protected AGRepositoryConnection conn;
	protected AGValueFactory vf;
	protected final Resource context;
	protected final Resource[] contexts;

	protected String entailmentRegime = "false";
	
	AGGraph(AGGraphMaker maker, Node graphNode) {
		super(maker.getReificationStyle());
		this.maker = maker;
		this.graphNode = graphNode;
		conn = maker.getRepositoryConnection();
		vf = conn.getValueFactory();
		context = vf.asResource(graphNode);
		contexts = new Resource[]{context};
	}

	AGGraph(AGGraphMaker maker, Resource context, Resource... contexts) {
		super(maker.getReificationStyle());
		this.maker = maker;
		this.graphNode = null;
		this.conn = maker.getRepositoryConnection();
		this.vf = conn.getValueFactory();
		this.context = context;
		this.contexts = contexts;
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

	Resource[] getGraphContexts() {
		return contexts;
	}
	
	AGRepositoryConnection getConnection() {
		return conn;
	}

	String getEntailmentRegime() {
		return entailmentRegime;
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
	 * Answer a human-consumable representation of <code>that</code>. The string
	 * <code>prefix</code> will appear near the beginning of the string. Nodes
	 * may be prefix-compressed using <code>that</code>'s prefix-mapping. This
	 * default implementation will display all the triples exposed by the graph
	 * (ie including reification triples if it is Standard).
	 */
	public static String toString(String prefix, Graph that) {
		// PrefixMapping pm = that.getPrefixMapping();
		StringBuffer b = new StringBuffer(prefix + " {");
		String gap = "";
		ClosableIterator<Triple> it = GraphUtil.findAll(that);
		int count = 0;
		while (it.hasNext() && count < 20) {
			b.append(gap);
			gap = "; ";
			b.append(it.next().toString());
		}
		b.append("}");
		return b.toString();
	}

	Dataset getDataset() {
		DatasetImpl dataset = new DatasetImpl();
		for (Resource c : contexts) {
			if (c == null) {
				dataset.addDefaultGraph(null);
			} else if (c instanceof URI) {
				dataset.addDefaultGraph((URI) c);
				dataset.addNamedGraph((URI) c);
			}
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
				StatementCollector collector = new StatementCollector();
				conn.getHttpRepoClient().getStatements(vf.asResource(s), vf.asURI(p), vf.asValue(m
						.getMatchObject()), entailmentRegime, collector, contexts);
				result = conn.createRepositoryResult(collector.getStatements());
			}
		} catch (AGHttpException e) {
			throw new RuntimeException(e);
		} catch (RDFHandlerException e) {
			throw new RuntimeException(e);
		}
		return new AGTripleIterator(this, result);
	}

	@Override
	public void performAdd(Triple t) {
		try {
			AGRepositoryConnection conn = maker.getRepositoryConnection();
			AGValueFactory vf = conn.getValueFactory();
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
					.getPredicate()), vf.asValue(t.getObject()), contexts);
		} catch (UnauthorizedException e) {
			throw new DeleteDeniedException(e.getMessage());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected int graphBaseSize() {
		// TODO deal with graphs bigger than int's.
		int size;
		try {
			size = (int) conn.size(contexts);
		} catch (RepositoryException e) {
			// TODO: proper exception to throw?
			throw new RuntimeException(e);
		}
		return size;
	}
	
    /**
     * Answer true iff this graph contains no triples.  
     * 
     * Implemented using a SPARQL ASK for any triple in the graph's
     * dataset; on large databases this is faster than determining
     * whether the graph's size is zero.
     */
	@Override
	public boolean isEmpty() {
		String queryString = "ask {?s ?p ?o}";
		AGBooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
		bq.setDataset(getDataset());
		try {
			return !bq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Reifier constructReifier() {
		if (!style.intercepts() && !style.conceals()) {
			return new SimpleReifier( this, new EmptyReifierTripleMap(), new EmptyReifierFragmentsMap(), style );
		} else if (style == ReificationStyle.Standard){
			return new AGReifier( this );
		} else {
			return new SimpleReifier( this, style );
		}
	}
	
	class EmptyReifierTripleMap implements ReifierTripleMap {

		@Override
		public Triple getTriple(Node tag) {
			return null;
		}

		@Override
		public boolean hasTriple(Triple t) {
			return false;
		}

		@Override
		public Triple putTriple(Node key, Triple value) {
			return null;
		}

		@Override
		public void removeTriple(Node key) {
		}

		@Override
		public void removeTriple(Node key, Triple value) {
		}

		@Override
		public void removeTriple(Triple triple) {
		}

		@Override
		public ExtendedIterator<Triple> find(TripleMatch m) {
			return NullIterator.instance();
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public ExtendedIterator<Node> tagIterator() {
			return NullIterator.instance();
		}

		@Override
		public ExtendedIterator<Node> tagIterator(Triple t) {
			return NullIterator.instance();
		}

		@Override
		public void clear() {
		}
		
	}
	
	class EmptyReifierFragmentsMap implements ReifierFragmentsMap {

		@Override
		public ExtendedIterator<Triple> find(TripleMatch m) {
			return NullIterator.instance();
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public ReifierFragmentHandler getFragmentHandler(Triple fragment) {
			return null;
		}

		@Override
		public boolean hasFragments(Node tag) {
			return false;
		}

		@Override
		public void clear() {
		}
		
	}
}
