/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;


/**
 * Error message returned by custom stored procedure.
 *
 * @since v4.2
 * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
 */
public class AGCustomStoredProcException extends AGHttpException {

    private static final long serialVersionUID = -2612222758468197539L;

    public AGCustomStoredProcException(String message) {
        super(message);
    }

    public AGCustomStoredProcException(String message, Throwable cause) {
        super(message, cause);
    }

}
