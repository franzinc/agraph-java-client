/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.handler.AGTQRStreamer;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;

import javax.xml.stream.XMLStreamReader;

/**
 * Wraps an AGTupleQuery to provide streaming results.
 * The default, TupleQueryResultParser and TupleQueryResultBuilder use
 * SAX to parse and an ArrayList to collect results,
 * so {@link AGTupleQuery#evaluate()} does not return until the
 * entire stream is parsed.
 * <p>AGStreamTupleQuery uses {@link XMLStreamReader}, so the result is
 * pulled from the http response stream as methods such as
 * {@link TupleQueryResult}.{@link TupleQueryResult#hasNext() hasNext()}
 * are called.
 * </p>
 * <p>Usage:</p>
 * <pre>{@code
 * AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ...");
 * query = new AGStreamTupleQuery(query);
 * TupleQueryResult results = query.evaluate();
 * ...
 * }</pre>
 *
 * @see AGRepositoryConnection#prepareTupleQuery(org.eclipse.rdf4j.query.QueryLanguage, String)
 * @since v4.3
 */
public class AGStreamTupleQuery extends AGTupleQuery implements TupleQuery {

    /**
     * Wraps a query with this object that will stream the response.
     *
     * @param query to wrap
     */
    public AGStreamTupleQuery(AGTupleQuery query) {
        super(query.httpCon, query.queryLanguage, query.queryString, query.baseURI);
    }

    /**
     * Returns a result object that will read from the http response as
     * results are requested, by
     * {@link TupleQueryResult}.{@link TupleQueryResult#hasNext() hasNext()}.
     * (Note that {@link TupleQueryResult}.{@link TupleQueryResult#next() next()}
     * does not actually do the work if hasNext() is called first.)
     */
    @Override
    public TupleQueryResult evaluate() throws QueryEvaluationException {
        AGTQRStreamer handler = AGTQRStreamer.createStreamer(httpCon.prepareHttpRepoClient().getPreferredTQRFormat(), httpCon.getRepository().getValueFactory());
        try {
            httpCon.prepareHttpRepoClient().query(this, false, handler);
        } catch (Exception e) {
            throw new QueryEvaluationException(e);
        }
        return handler.getResult();
    }

    /**
     *
     */
    @Override
    public void evaluate(TupleQueryResultHandler handler) throws QueryEvaluationException, TupleQueryResultHandlerException {
        TupleQueryResult result = evaluate();
        handler.startQueryResult(result.getBindingNames());
        while (result.hasNext()) {
            handler.handleSolution(result.next());
        }
        handler.endQueryResult();
    }

}
