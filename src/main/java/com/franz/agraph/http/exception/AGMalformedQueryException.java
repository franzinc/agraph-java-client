/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;


public class AGMalformedQueryException extends AGHttpException {

    private static final long serialVersionUID = -554755730398050682L;

    public AGMalformedQueryException(String message) {
        super(message);
    }

    public AGMalformedQueryException(Exception e) {
        super(e);
    }

    public AGMalformedQueryException(String message, Throwable cause) {
        super(message, cause);
    }

}
