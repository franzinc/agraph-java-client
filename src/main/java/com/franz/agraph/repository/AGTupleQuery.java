/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGDownloadHandler;
import com.franz.agraph.http.handler.AGLongHandler;
import com.franz.agraph.http.handler.AGRawStreamer;
import com.franz.agraph.http.handler.AGTQRHandler;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

import java.io.File;
import java.io.InputStream;

/**
 * Implements the Sesame TupleQuery interface for AllegroGraph.
 */
public class AGTupleQuery extends AGQuery implements TupleQuery {

    public AGTupleQuery(AGRepositoryConnection con, QueryLanguage ql,
                        String queryString, String baseURI) {
        super(con, ql, queryString, baseURI);
    }

    public TupleQueryResult evaluate() throws QueryEvaluationException {
        try {
            // TODO: make this efficient for large result sets
            TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
            evaluate(builder);
            return builder.getQueryResult();
        } catch (TupleQueryResultHandlerException e) {
            // Found a bug in TupleQueryResultBuilder?
            throw new RuntimeException(e);
        }
    }

    public void evaluate(TupleQueryResultHandler handler)
            throws QueryEvaluationException, TupleQueryResultHandlerException {
        evaluate(new AGTQRHandler(httpCon.prepareHttpRepoClient().getPreferredTQRFormat(), handler, httpCon.getValueFactory(), httpCon.prepareHttpRepoClient().getAllowExternalBlankNodeIds()));
    }

    /**
     * Evaluates the query and returns only the number of results
     * to the client (counting is done on the server, the results
     * are not returned).
     *
     * @return the number of results
     * @throws QueryEvaluationException if there is an error with this request
     */
    public long count() throws QueryEvaluationException {
        AGLongHandler handler = new AGLongHandler();
        evaluate(handler);
        return handler.getResult();
    }

    /**
     * Evaluates the query and saves the results to a file.
     *
     * @param file   Output path.
     * @param format Output format.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public void download(final File file, final TupleQueryResultFormat format)
            throws QueryEvaluationException {
        evaluate(new AGDownloadHandler(file, format));
    }

    /**
     * Evaluates the query and saves the results to a file.
     *
     * @param file   Output path.
     * @param format Output format.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public void download(final String file, final TupleQueryResultFormat format)
            throws QueryEvaluationException {
        evaluate(new AGDownloadHandler(file, format));
    }

    /**
     * Evaluates the query and returns the result as an input stream.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param format Output format.
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public InputStream stream(final TupleQueryResultFormat format)
            throws QueryEvaluationException {
        final AGRawStreamer handler = new AGRawStreamer(format);
        evaluate(handler);
        try {
            return handler.getStream();
        } catch (final AGHttpException e) {
            throw new QueryEvaluationException(e);
        }
    }
}
