/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;


public class AGQueryTimeoutException extends AGHttpException {

    private static final long serialVersionUID = -2493579811305767901L;

    public AGQueryTimeoutException(String message) {
        super(message);
    }

    public AGQueryTimeoutException(Exception e) {
        super(e);
    }

    public AGQueryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
