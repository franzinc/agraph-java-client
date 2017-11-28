/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGBQRHandler;
import com.franz.agraph.http.handler.AGDownloadHandler;
import com.franz.agraph.http.handler.AGRawStreamer;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;

import java.io.File;
import java.io.InputStream;

/**
 * Implements the Sesame BooleanQuery interface for AllegroGraph.
 */
public class AGBooleanQuery extends AGQuery implements BooleanQuery {

    /**
     * Creates an AGBooleanQuery instance for a given connection.
     *
     * @param con         the connection
     * @param ql          the query language
     * @param queryString the query
     * @param baseURI     the base URI for the query
     */
    public AGBooleanQuery(AGRepositoryConnection con, QueryLanguage ql,
                          String queryString, String baseURI) {
        super(con, ql, queryString, baseURI);
    }

    /**
     * Evaluates the query and returns a boolean result.
     */
    public boolean evaluate() throws QueryEvaluationException {
        AGBQRHandler handler = new AGBQRHandler();
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
    public void download(final File file, final BooleanQueryResultFormat format)
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
    public void download(final String file, final BooleanQueryResultFormat format)
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
    public InputStream stream(final BooleanQueryResultFormat format)
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
