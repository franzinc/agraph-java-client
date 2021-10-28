/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public abstract class AGResponseHandler {

    private final String requestMIMEType;

    public AGResponseHandler(String mimeType) {
        requestMIMEType = mimeType;
    }

    protected static InputStream getInputStream(HttpResponse httpResponse) throws IOException {
        if (httpResponse.getEntity() != null) {
            InputStream is = httpResponse.getEntity().getContent();
            Header h = httpResponse.getFirstHeader("Content-Encoding");
            if (h != null && h.getValue().equals("gzip")) {
                is = new GZIPInputStream(is);
            }
            return is;
        } else {
            return null;
        }
    }

    protected static String streamToString(InputStream in) throws IOException {
        // TODO: protect against buffering very large streams
        return IOUtils.toString(in, StandardCharsets.UTF_8);
    }

    public String getRequestMIMEType() {
        return requestMIMEType;
    }

    public abstract void handleResponse(HttpResponse httpResponse, HttpUriRequest httpUriRequest) throws IOException, AGHttpException;

    /**
     * For most responses, AGHTTPClient releases resources after
     * calling {@link #handleResponse(HttpResponse, HttpUriRequest)}; this can be
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
     * <code>Content-Type: application/xml;charset=UTF-8</code>, this method will
     * return <code>application/xml</code> as the MIME type.
     *
     * @param httpResponse the method to get the reponse MIME type from
     * @return the response MIME type, or <code>null</code> if not available
     */
    protected String getResponseMIMEType(HttpResponse httpResponse) throws IOException {
        Header[] headers = httpResponse.getHeaders("Content-Type");

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
