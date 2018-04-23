/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.jena.sparql.resultset.JSONInput;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

import java.io.IOException;

/**
 * Similar to {@link org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser}
 * but uses {@link JSONInput} instead of SAXParser so the results
 * streaming in the http response can be processed in a
 * {@link TupleQueryResult} pulling from the JSON stream.
 *
 * @since v6.4.2
 */
public class AGTQRJSONStreamer extends AGTQRStreamer {
    public AGTQRJSONStreamer(AGValueFactory vf) {
        super(TupleQueryResultFormat.JSON.getDefaultMIMEType());
        this.vf = vf;
    }

    @Override
    public String getRequestMIMEType() { return TupleQueryResultFormat.JSON.getDefaultMIMEType(); }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        this.method = method;
        resultSet = JSONInput.fromJSON(AGResponseHandler.getInputStream(method));
    }
}
