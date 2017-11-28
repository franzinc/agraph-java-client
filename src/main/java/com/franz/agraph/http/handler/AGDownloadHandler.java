/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.File;
import java.io.IOException;

/**
 * A response handler that writes the returned body to a file.
 */
public class AGDownloadHandler extends AGResponseHandler {
    // Output file.
    private final File file;

    /**
     * Creates a download handler.
     *
     * @param file     Output path.
     * @param mimeType MIME type to be requested from the server.
     */
    public AGDownloadHandler(final File file, final String mimeType) {
        super(mimeType);
        this.file = file;
    }

    /**
     * Creates a download handler.
     *
     * @param file     Output path.
     * @param mimeType MIME type to be requested from the server.
     */
    public AGDownloadHandler(final String file, final String mimeType) {
        this(new File(file), mimeType);
    }

    /**
     * Creates a download handler that does not specify the MIME type.
     * <p>
     * The server is free to return any content type.
     *
     * @param file Output path.
     */
    public AGDownloadHandler(final File file) {
        this(file, "*/*");
    }

    /**
     * Creates a download handler that does not specify the MIME type.
     * <p>
     * The server is free to return any content type.
     *
     * @param file Output path.
     */
    public AGDownloadHandler(final String file) {
        this(new File(file));
    }

    /**
     * Creates a download handler that requests results in specified RDF format.
     *
     * @param file   Output path.
     * @param format Format to return the data in.
     */
    public AGDownloadHandler(final File file, final RDFFormat format) {
        this(file, format.getDefaultMIMEType());
    }

    /**
     * Creates a download handler that requests results in specified RDF format.
     *
     * @param file   Output path.
     * @param format Format to return the data in.
     */
    public AGDownloadHandler(final String file, final RDFFormat format) {
        this(new File(file), format);
    }

    /**
     * Creates a download handler that requests results in specified tuple format.
     *
     * @param file   Output path.
     * @param format Format to return the data in.
     */
    public AGDownloadHandler(final File file, final TupleQueryResultFormat format) {
        this(file, format.getDefaultMIMEType());
    }

    /**
     * Creates a download handler that requests results in specified tuple format.
     *
     * @param file   Output path.
     * @param format Format to return the data in.
     */
    public AGDownloadHandler(final String file, final TupleQueryResultFormat format) {
        this(new File(file), format);
    }

    /**
     * Creates a download handler that requests results in specified boolean format.
     *
     * @param file   Output path.
     * @param format Format to return the data in.
     */
    public AGDownloadHandler(final File file, final BooleanQueryResultFormat format) {
        this(file, format.getDefaultMIMEType());
    }

    /**
     * Creates a download handler that requests results in specified boolean format.
     *
     * @param file   Output path.
     * @param format Format to return the data in.
     */
    public AGDownloadHandler(final String file, final BooleanQueryResultFormat format) {
        this(new File(file), format);
    }

    @Override
    public void handleResponse(final HttpMethod method) throws IOException, AGHttpException {
        FileUtils.copyInputStreamToFile(getInputStream(method), file);
    }
}
