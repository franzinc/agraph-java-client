/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;


public class AGUnsupportedFileFormatException extends AGHttpException {

    private static final long serialVersionUID = -2912726941057964054L;

    public AGUnsupportedFileFormatException(String message) {
        super(message);
    }

    public AGUnsupportedFileFormatException(Exception e) {
        super(e);
    }

    public AGUnsupportedFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

}
