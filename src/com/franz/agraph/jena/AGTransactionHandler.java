package com.franz.agraph.jena;

import org.openrdf.repository.RepositoryException;

import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.shared.Command;

public class AGTransactionHandler implements TransactionHandler {

	private final AGGraph graph;
	
	AGTransactionHandler(AGGraph graph) {
		this.graph = graph;
	}
	
	@Override
	public void abort() {
		try {
			graph.getConnection().rollback();
			// end the transaction, return to autocommit mode
			graph.getConnection().setAutoCommit(true);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void begin() {
		// TODO check for nested transactions
		// TODO address multiple transactions on a graph
		try {
			graph.getConnection().setAutoCommit(false);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void commit() {
		try {
			graph.getConnection().commit();
			// end the transaction, return to autocommit mode
			graph.getConnection().setAutoCommit(true);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object executeInTransaction(Command c) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public boolean transactionsSupported() {
		return true;
	}

}
