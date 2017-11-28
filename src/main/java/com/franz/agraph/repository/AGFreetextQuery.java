/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGRDFHandler;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public class AGFreetextQuery {

    private final AGRepositoryConnection conn;

    protected String pattern;
    protected String expression;
    protected String index;
    protected boolean sorted = false;
    protected int limit = 0;
    protected int offset = 0;

    public AGFreetextQuery(AGRepositoryConnection conn) {
        this.conn = conn;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public RepositoryResult<Statement> evaluate() throws QueryEvaluationException {
        try {
            // TODO: make this efficient for large result sets
            StatementCollector collector = new StatementCollector();
            evaluate(collector);
            return conn.createRepositoryResult(collector.getStatements());
        } catch (RDFHandlerException e) {
            // Found a bug in StatementCollector?
            throw new RuntimeException(e);
        }
    }

    /**
     * Evaluates the query and uses handler to process the result.
     *
     * @param handler used to process the query results
     * @throws QueryEvaluationException if errors occur while evaluating the query
     * @throws RDFHandlerException      if errors occur while processing results
     */
    public void evaluate(RDFHandler handler) throws QueryEvaluationException,
            RDFHandlerException {
        try {
            conn.prepareHttpRepoClient().evalFreetextQuery(pattern, expression, index, sorted, limit, offset,
                    new AGRDFHandler(conn.prepareHttpRepoClient().getPreferredRDFFormat(), handler, conn.getValueFactory(), conn.prepareHttpRepoClient().getAllowExternalBlankNodeIds()));
        } catch (AGHttpException e) {
            // TODO: distinguish RDFHandlerException
            throw new QueryEvaluationException(e);
        }
    }

}
