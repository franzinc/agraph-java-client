/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.impl.TransactionHandlerBase;
import org.apache.jena.shared.Command;
import org.apache.jena.shared.JenaException;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Implements the Jena TransactionHandler interface for AllegroGraph.
 */
public class AGTransactionHandler extends TransactionHandlerBase {

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
