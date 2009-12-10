package com.franz.agraph.jena;

import java.util.Iterator;

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
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.AddDeniedException;
import com.hp.hpl.jena.shared.DeleteDeniedException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class AGInfGraph implements InfGraph {

	@Override
	public ExtendedIterator<Triple> find(Node subject, Node property,
			Node object, Graph param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Graph getDeductionsGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Derivation> getDerivation(Triple triple) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getGlobalProperty(Node property) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Graph getRawGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reasoner getReasoner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void prepare() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rebind() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rebind(Graph data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean testGlobalProperty(Node property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ValidityReport validate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	@Override
	public boolean dependsOn(Graph other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ExtendedIterator<Triple> find(TripleMatch m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BulkUpdateHandler getBulkUpdateHandler() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void add(Triple t) throws AddDeniedException {
		// TODO Auto-generated method stub

	}

}
