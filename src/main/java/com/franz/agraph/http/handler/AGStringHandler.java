/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;

public class AGStringHandler extends AGResponseHandler {

    private String result = null;

    public AGStringHandler() {
        super("text/plain");
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        String mimeType = getResponseMIMEType(method);
        if (!mimeType.equals(getRequestMIMEType())) {
            throw new AGHttpException("unexpected response MIME type: " + mimeType);
        }
        InputStream response = getInputStream(method);
        result = streamToString(response);
    }

    public String getResult() {
        return result;
    }

}
