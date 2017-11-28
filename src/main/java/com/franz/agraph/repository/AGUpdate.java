/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.handler.AGBQRHandler;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;

/**
 * Implements the Sesame Update interface for AllegroGraph.
 */
public class AGUpdate extends AGQuery implements Update {

    /**
     * Creates an AGUpdate instance for a given connection.
     *
     * @param con         the connection
     * @param ql          the query language
     * @param queryString the query
     * @param baseURI     the base URI for the query
     */
    public AGUpdate(AGRepositoryConnection con, QueryLanguage ql,
                    String queryString, String baseURI) {
        super(con, ql, queryString, baseURI);
    }

    /**
     * Execute the update.
     */
    @Override
    public void execute() throws UpdateExecutionException {
        AGBQRHandler handler = new AGBQRHandler();
        try {
            evaluate(handler);
        } catch (QueryEvaluationException e) {
            throw new UpdateExecutionException(e);
        }
    }

}
