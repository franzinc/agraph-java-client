/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;


public class AGMalformedDataException extends AGHttpException {

    private static final long serialVersionUID = -8973332554840496701L;

    public AGMalformedDataException(String message) {
        super(message);
    }

    public AGMalformedDataException(Exception e) {
        super(e);
    }

    public AGMalformedDataException(String message, Throwable cause) {
        super(message, cause);
    }

}
