/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.repository.RepositoryException;

import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.shared.Command;
import com.hp.hpl.jena.shared.JenaException;

/**
 * Implements the Jena TransactionHandler interface for AllegroGraph.
 * 
 */
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
		try {
			begin();
			c.execute();
			commit();
		} catch (Throwable e) {
			throw new JenaException(e);
		}
		
		// TODO determine what object to return here, currently the 
		// command is executed for side effects rather than a result.
		return null;
	}

	@Override
	public boolean transactionsSupported() {
		return true;
	}

}
