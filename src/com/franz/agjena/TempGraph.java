package com.franz.agjena;

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
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.AddDeniedException;
import com.hp.hpl.jena.shared.DeleteDeniedException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class TempGraph implements InfGraph {

	public ExtendedIterator find(Node arg0, Node arg1, Node arg2, Graph arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	public Graph getDeductionsGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	public Iterator getDerivation(Triple arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Node getGlobalProperty(Node arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Graph getRawGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	public Reasoner getReasoner() {
		// TODO Auto-generated method stub
		return null;
	}

	public void prepare() {
		// TODO Auto-generated method stub

	}

	public void rebind() {
		// TODO Auto-generated method stub

	}

	public void rebind(Graph arg0) {
		// TODO Auto-generated method stub

	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public void setDerivationLogging(boolean arg0) {
		// TODO Auto-generated method stub

	}

	public boolean testGlobalProperty(Node arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public ValidityReport validate() {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public boolean contains(Triple arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(Node arg0, Node arg1, Node arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	public void delete(Triple arg0) throws DeleteDeniedException {
		// TODO Auto-generated method stub

	}

	public boolean dependsOn(Graph arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public ExtendedIterator find(TripleMatch arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public ExtendedIterator find(Node arg0, Node arg1, Node arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	public BulkUpdateHandler getBulkUpdateHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	public Capabilities getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	public GraphEventManager getEventManager() {
		// TODO Auto-generated method stub
		return null;
	}

	public PrefixMapping getPrefixMapping() {
		// TODO Auto-generated method stub
		return null;
	}

	public Reifier getReifier() {
		// TODO Auto-generated method stub
		return null;
	}

	public GraphStatisticsHandler getStatisticsHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	public TransactionHandler getTransactionHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isIsomorphicWith(Graph arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public QueryHandler queryHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void add(Triple arg0) throws AddDeniedException {
		// TODO Auto-generated method stub

	}

}
