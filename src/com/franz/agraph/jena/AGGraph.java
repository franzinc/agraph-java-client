package com.franz.agraph.jena;

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
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphEventManager;
import com.hp.hpl.jena.graph.GraphStatisticsHandler;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Reifier;
import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.shared.AddDeniedException;
import com.hp.hpl.jena.shared.DeleteDeniedException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class AGGraph implements Graph {

	private final AGGraphMaker maker;
	private final Node graphNode;

	private final AGRepositoryConnection conn;
	private final AGValueFactory vf;
	private final Resource context; 
	
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
	
	Resource getGraphContext() {
		return context;
	}
	
	AGRepositoryConnection getConnection() {
		return conn;
	}
	
	@Override
	public void close() {
	}

	@Override
	public boolean contains(Triple t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Node s, Node p, Node o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void delete(Triple t) throws DeleteDeniedException {
		try {
			conn.remove(vf.asResource(t.getSubject()), vf.asURI(t.getPredicate()), vf.asValue(t.getObject()), context);
		} catch (UnauthorizedException e) {
			throw new DeleteDeniedException(e.getMessage());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean dependsOn(Graph other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ExtendedIterator<Triple> find(TripleMatch m) {
		return find(m.getMatchSubject(),m.getMatchPredicate(),m.getMatchObject());
	}

	@Override
	public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
		RepositoryResult<Statement> result;
		try {
			result = conn.getStatements(vf.asResource(s), vf.asURI(p), vf.asValue(o), false, context);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return new AGTripleIterator(result);
	}

	@Override
	public BulkUpdateHandler getBulkUpdateHandler() {
		return new AGBulkUpdateHandler(this);
	}

	@Override
	public Capabilities getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphEventManager getEventManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping getPrefixMapping() {
		return new AGPrefixMapping(this);
	}

	@Override
	public Reifier getReifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphStatisticsHandler getStatisticsHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionHandler getTransactionHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIsomorphicWith(Graph g) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public QueryHandler queryHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO deal with graphs bigger than int's.
		int size;
		try {
			size = (int)conn.size(context);
		} catch (RepositoryException e) {
			// TODO: proper exception to throw?
			throw new RuntimeException(e);
		}
		return size;
	}

	@Override
	public void add(Triple t) throws AddDeniedException {
		try {
			conn.add(vf.asResource(t.getSubject()), vf.asURI(t.getPredicate()), vf.asValue(t.getObject()), context);
		} catch (UnauthorizedException e) {
			throw new AddDeniedException(e.getMessage());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		if (graphNode==null)
			return "default-graph";
		return graphNode.toString();
	}

	public Dataset getDataset() {
		DatasetImpl dataset = new DatasetImpl();
		if (context==null) {
			dataset.addDefaultGraph(null);
		} else if (context instanceof URI) {
			dataset.addDefaultGraph((URI)context);
			dataset.addNamedGraph((URI)context);
		}
		return dataset;
	}
}
