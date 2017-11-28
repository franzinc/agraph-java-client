/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;


public class AGPreconditionFailedException extends AGHttpException {

    private static final long serialVersionUID = -5446862828628318163L;

    public AGPreconditionFailedException(String message) {
        super(message);
    }

    public AGPreconditionFailedException(Exception e) {
        super(e);
    }

    public AGPreconditionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
