/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.exception;

import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A general exception for the AllegroGraph client's HTTP layer.
 * <p>
 * For now, this extends RepositoryException, allowing it to be
 * thrown as a RepositoryException in com.franz.agraph.repository;
 * one drawback is that the HTTP layer is not independent of the
 * higher level repository package.
 */
public class AGHttpException extends RepositoryException {

    private static final long serialVersionUID = 4352985824130756505L;

    public AGHttpException(String message) {
        super(message);
    }

    public AGHttpException(Exception e) {
        super(e);
    }

    public AGHttpException(String message, Throwable cause) {
        super(message, cause);
    }

}
