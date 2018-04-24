/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.exception.AGMalformedDataException;
import com.franz.agraph.http.exception.AGMalformedQueryException;
import com.franz.agraph.http.exception.AGPreconditionFailedException;
import com.franz.agraph.http.exception.AGQueryTimeoutException;
import com.franz.agraph.http.exception.AGUnsupportedFileFormatException;
import com.franz.agraph.http.exception.AGUnsupportedQueryLanguageException;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.io.InputStream;

public class AGErrorHandler extends AGResponseHandler {

    // Standard Sesame error types
    public static final String MALFORMED_QUERY = "MALFORMED QUERY";
    public static final String MALFORMED_DATA = "MALFORMED DATA";
    public static final String UNSUPPORTED_QUERY_LANGUAGE = "UNSUPPORTED QUERY LANGUAGE";
    public static final String UNSUPPORTED_FILE_FORMAT = "UNSUPPORTED FILE FORMAT";

    // Extended error types
    public static final String PRECONDITION_FAILED = "PRECONDITION FAILED";
    public static final String IO_EXCEPTION = "IO EXCEPTION";
    public static final String QUERY_TIMEOUT = "QUERY TIMEOUT";

    private AGHttpException result = null;

    public AGErrorHandler() {
        super("text/plain");
    }

    protected static AGHttpException newException(String errorString) {
        int colonIdx = errorString.indexOf(':');
        if (colonIdx >= 0) {
            String type = errorString.substring(0, colonIdx).trim();
            String message = errorString.substring(colonIdx + 1);
            switch (type) {
                case MALFORMED_DATA:
                    return new AGMalformedDataException(message);
                case MALFORMED_QUERY:
                    return new AGMalformedQueryException(message);
                case PRECONDITION_FAILED:
                    return new AGPreconditionFailedException(message);
                case UNSUPPORTED_FILE_FORMAT:
                    return new AGUnsupportedFileFormatException(message);
                case UNSUPPORTED_QUERY_LANGUAGE:
                    return new AGUnsupportedQueryLanguageException(message);
                case QUERY_TIMEOUT:
                    return new AGQueryTimeoutException(message);
            }
        }
        // unrecognized error type, use the whole errorString
        return new AGHttpException(errorString);
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        InputStream response = getInputStream(method);
        String errorString = streamToString(response);
        if (!errorString.isEmpty()) {
            result = newException(errorString);
        } else {
            // Could be e.g. "HTTP 408 Request Timeout"
            result = new AGHttpException("" + method.getStatusCode() + " " + method.getStatusText());
        }
    }

    public AGHttpException getResult() {
        return result;
    }

}
