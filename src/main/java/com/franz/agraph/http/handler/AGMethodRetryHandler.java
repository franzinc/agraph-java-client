package com.franz.agraph.http.handler;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;

public class AGMethodRetryHandler extends DefaultHttpMethodRetryHandler {

    @Override
    public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
        // Retry the method if either:
        //  1) By default it would be retried (e.g. if server dropped our connection):
        if (super.retryMethod(method, exception, executionCount)) {
            return true;
        }
        //  2) This is an idempotent method that has only been tried once,
        //     and failed with a SocketException (in particular this catches "Connection reset"):
        //      - for now only consider GET and HEAD idempotent;
        //      - PUT and DELETE should be idempotent according to the HTTP philosophy,
        //        but might not always be for AG.
        if ((executionCount == 1) && (exception instanceof java.net.SocketException)) {
                switch (method.getName()) {
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
