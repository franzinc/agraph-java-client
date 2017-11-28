/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;

public class AGUnsupportedQueryLanguageException extends AGHttpException {

    private static final long serialVersionUID = 8094767019992317184L;

    public AGUnsupportedQueryLanguageException(String message) {
        super(message);
    }

    public AGUnsupportedQueryLanguageException(Exception e) {
        super(e);
    }

    public AGUnsupportedQueryLanguageException(String message, Throwable cause) {
        super(message, cause);
    }

}
