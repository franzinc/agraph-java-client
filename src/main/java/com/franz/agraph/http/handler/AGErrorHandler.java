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
            if (MALFORMED_DATA.equals(type)) {
                return new AGMalformedDataException(message);
            } else if (MALFORMED_QUERY.equals(type)) {
                return new AGMalformedQueryException(message);
            } else if (PRECONDITION_FAILED.equals(type)) {
                return new AGPreconditionFailedException(message);
            } else if (UNSUPPORTED_FILE_FORMAT.equals(type)) {
                return new AGUnsupportedFileFormatException(message);
            } else if (UNSUPPORTED_QUERY_LANGUAGE.equals(type)) {
                return new AGUnsupportedQueryLanguageException(message);
            } else if (QUERY_TIMEOUT.equals(type)) {
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
        result = newException(errorString);
    }

    public AGHttpException getResult() {
        return result;
    }

}
