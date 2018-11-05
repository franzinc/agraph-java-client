/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;

public class AGBooleanHandler extends AGResponseHandler {

    private boolean result = false;

    public AGBooleanHandler() {
        super("text/plain");
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        String mimeType = getResponseMIMEType(method);
        if (!mimeType.equals(getRequestMIMEType())) {
            throw new AGHttpException("unexpected response MIME type: " + mimeType);
        }
        result = Boolean.parseBoolean(streamToString(getInputStream(method)));
    }

    public boolean getResult() {
        return result;
    }

}
