/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public abstract class AGResponseHandler {

    private final String requestMIMEType;

    public AGResponseHandler(String mimeType) {
        requestMIMEType = mimeType;
    }

    protected static InputStream getInputStream(HttpMethod method) throws IOException {
        InputStream is = method.getResponseBodyAsStream();
        Header h = method.getResponseHeader("Content-Encoding");
        if (h != null && h.getValue().equals("gzip")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    protected static String streamToString(InputStream in) throws IOException {
        // TODO: protect against buffering very large streams
        return IOUtils.toString(in, StandardCharsets.UTF_8);
    }

    public String getRequestMIMEType() {
        return requestMIMEType;
    }

    public abstract void handleResponse(HttpMethod method) throws IOException, AGHttpException;

    /**
     * For most responses, AGHTTPClient releases resources after
     * calling {@link #handleResponse(HttpMethod)}; this can be
     * overridden in subclasses that stream results.
     *
     * @return Boolean  always returns true
     */
    public boolean releaseConnection() {
        return true;
    }

    /**
     * Gets the MIME type specified in the response headers of the supplied
     * method, if any. For example, if the response headers contain
     * <tt>Content-Type: application/xml;charset=UTF-8</tt>, this method will
     * return <tt>application/xml</tt> as the MIME type.
     *
     * @param method the method to get the reponse MIME type from
     * @return the response MIME type, or <tt>null</tt> if not available
     */
    protected String getResponseMIMEType(HttpMethod method) throws IOException {
        Header[] headers = method.getResponseHeaders("Content-Type");

        for (Header header : headers) {
            HeaderElement[] headerElements = header.getElements();

            for (HeaderElement headerEl : headerElements) {
                String mimeType = headerEl.getName();
                if (mimeType != null) {
                    // TODO: logger.debug("response MIME type is {}", mimeType);
                    return mimeType;
                }
            }
        }

        return null;
    }

}
