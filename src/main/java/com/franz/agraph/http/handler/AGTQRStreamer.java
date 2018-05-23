package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AGTQRStreamer extends AGResponseHandler {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected AGValueFactory vf;
    protected HttpMethod method;

    public AGTQRStreamer(String mimeType) {
        super(mimeType);
    }

    @Override
    public abstract String getRequestMIMEType();

    /**
     * False because the Result will release the HTTP resources.
     * For most responses, AGHTTPClient releases resources after
     * calling {@link #handleResponse(HttpMethod)},
     * but here the results are pulled when needed from the Result class,
     * which is an inner class of child classes.
     */
    @Override
    public boolean releaseConnection() {
        return false;
    }

    @Override
    public abstract void handleResponse(HttpMethod method) throws IOException, AGHttpException;

    public abstract TupleQueryResult getResult();

    public static AGTQRStreamer createStreamer(TupleQueryResultFormat format, AGValueFactory vf) {
        if (format.equals(TupleQueryResultFormat.TSV)) {
            return new AGTQRTSVStreamer(vf);
        } else if (format.equals(TupleQueryResultFormat.SPARQL)) {
            return new AGTQRXMLStreamer(vf);
        } else if (format.equals(TupleQueryResultFormat.JSON)) {
            return new AGTQRJSONStreamer(vf);
        } else {
            throw new IllegalArgumentException("Unable to find AGTQRStreamer for format " + format);
        }
    }
}
