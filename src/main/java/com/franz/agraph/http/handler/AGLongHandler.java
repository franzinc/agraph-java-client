/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;

public class AGLongHandler extends AGResponseHandler {

    private long result;

    public AGLongHandler() {
        super("text/integer");
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        /* TODO: server responds with text/plain here, not text/integer
        String mimeType = getResponseMIMEType(method);
        if (!mimeType.equals(getRequestMIMEType())) {
            throw new AGHttpException("unexpected response MIME type: " + mimeType);
        }*/
        InputStream response = getInputStream(method);
        try {
            String str = streamToString(response);
            result = Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new AGHttpException(
                    "Server responded with invalid long value: " + e.getLocalizedMessage(), e);
        }
    }

    public long getResult() {
        return result;
    }

}
