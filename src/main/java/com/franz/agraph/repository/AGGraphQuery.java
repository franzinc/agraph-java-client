/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGDownloadHandler;
import com.franz.agraph.http.handler.AGLongHandler;
import com.franz.agraph.http.handler.AGRDFHandler;
import com.franz.agraph.http.handler.AGRawStreamer;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.IteratingGraphQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.File;
import java.io.InputStream;

/**
 * Implements the Sesame GraphQuery interface for AllegroGraph.
 */
public class AGGraphQuery extends AGQuery implements GraphQuery {

    /**
     * Creates an AGGraphQuery instance for the given connection.
     *
     * @param con         the connection
     * @param ql          the query language
     * @param queryString the query
     * @param baseURI     the base URI for the query
     */
    public AGGraphQuery(AGRepositoryConnection con, QueryLanguage ql,
                        String queryString, String baseURI) {
        super(con, ql, queryString, baseURI);
    }

    /**
     * Evaluates the query and returns a GraphQueryResult.
     */
    public GraphQueryResult evaluate() throws QueryEvaluationException {
        try {
            // TODO: make this efficient for large result sets
            StatementCollector collector = new StatementCollector();
            evaluate(collector);
            return new IteratingGraphQueryResult(collector.getNamespaces(),
                    collector.getStatements());
        } catch (RDFHandlerException e) {
            // Found a bug in StatementCollector?
            throw new RuntimeException(e);
        }
    }

    /**
     * Evaluates the query and uses handler to process the result.
     */
    public void evaluate(RDFHandler handler) throws QueryEvaluationException,
            RDFHandlerException {
        final RDFFormat format = httpCon.prepareHttpRepoClient().getPreferredRDFFormat();
        evaluate(new AGRDFHandler(format, handler, httpCon.getValueFactory(), httpCon.prepareHttpRepoClient().getAllowExternalBlankNodeIds()));
    }

    /**
     * Evaluates the query and returns only the number of results
     * to the client (counting is done on the server, the results
     * are not returned).
     *
     * @return long  the number of results
     * @throws QueryEvaluationException if an error occurs while evaluating the query
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
    public void download(final File file, final RDFFormat format)
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
    public void download(final String file, final RDFFormat format)
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
    public InputStream stream(final RDFFormat format)
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
