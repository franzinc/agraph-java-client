package com.franz.agraph.http.handler;

import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class AGMethodRetryHandler extends DefaultHttpRequestRetryHandler {

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
    // Retry the method if either:
        //  1) By default it would be retried (e.g. if server dropped our connection):
        if (super.retryRequest(exception, executionCount, context)) {
            return true;
        }
        //  2) This is an idempotent method that has only been tried once,
        //     and failed with a SocketException (in particular this catches "Connection reset"):
        //      - for now only consider GET and HEAD idempotent;
        //      - PUT and DELETE should be idempotent according to the HTTP philosophy,
        //        but might not always be for AG.
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final HttpRequest request = clientContext.getRequest();
        if ((executionCount == 1) && (exception instanceof java.net.SocketException)) {
                switch (request.getRequestLine().getMethod()) {
                case "GET":
                case "HEAD":
                        return true;
                default:
                        break;
                }
        }

        return false;
    }
}
