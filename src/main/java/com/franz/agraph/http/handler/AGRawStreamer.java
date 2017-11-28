/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;

/**
 * A handler that allows access to raw response stream.
 */
public class AGRawStreamer extends AGResponseHandler {
    private HttpMethod method;

    /**
     * Creates a streaming handler.
     *
     * @param mimeType MIME type to be requested from the server.
     */
    public AGRawStreamer(final String mimeType) {
        super(mimeType);
    }

    /**
     * Creates a streaming handler that does not specify the MIME type.
     * <p>
     * The server is free to return any content type.
     */
    public AGRawStreamer() {
        this("*/*");
    }

    /**
     * Creates a streaming handler that requests results in specified RDF format.
     *
     * @param format Format to return the data in.
     */
    public AGRawStreamer(final RDFFormat format) {
        this(format.getDefaultMIMEType());
    }

    /**
     * Creates a streaming handler that requests results in specified tuple format.
     *
     * @param format Format to return the data in.
     */
    public AGRawStreamer(final TupleQueryResultFormat format) {
        this(format.getDefaultMIMEType());
    }

    /**
     * Creates a streaming handler that requests results in specified boolean format.
     *
     * @param format Format to return the data in.
     */
    public AGRawStreamer(final BooleanQueryResultFormat format) {
        this(format.getDefaultMIMEType());
    }

    /**
     * Gets the data stream contaning the server's response.
     *
     * @return An input stream. It must be closed by the caller.
     * @throws AGHttpException if an error happens when decoding
     *                         the response (e.g. if the server sent an
     *                         invalid GZIP stream).
     */
    public InputStream getStream() throws AGHttpException {
        // Return the stream, but make sure that:
        //   1. It releases the connection once closed
        //   2. It is closed once exhausted
        try {
            return new AutoCloseInputStream(getInputStream(method)) {
                private boolean closed = false;

                @Override
                public void close() throws IOException {
                    if (!closed) {
                        super.close();
                        closed = true;
                        method.releaseConnection();
                    }
                }
            };
        } catch (final IOException e) {
            // Convert to AGHttpException to make the interface neater.
            throw new AGHttpException(e);
        }
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        // Just save the method (it includes the input stream).
        this.method = method;
    }

    // Do not release request resources after handleResponse.
    // Resources will only be released after the stream
    // returned by getStream() is closed.
    @Override
    public boolean releaseConnection() {
        return false;
    }
}
