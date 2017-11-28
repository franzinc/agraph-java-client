/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public class AGJSONArrayHandler extends AGResponseHandler {

    private JSONArray result;

    public AGJSONArrayHandler() {
        super("application/json");
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        /* TODO: server sometimes responds with text/plain, not application/json
        String mimeType = getResponseMIMEType(method);
        if (!mimeType.equals(getRequestMIMEType())) {
            throw new AGHttpException("unexpected response MIME type: " + mimeType);
        }*/
        try {
            InputStream response = getInputStream(method);
            String resp = streamToString(response);
            result = new JSONArray(resp);
        } catch (JSONException e) {
            throw new AGHttpException(e);
        }
    }

    public JSONArray getResult() {
        return result;
    }

}
