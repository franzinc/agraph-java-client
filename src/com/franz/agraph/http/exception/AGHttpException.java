/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.exception;

import org.openrdf.repository.RepositoryException;

/**
 * A general exception for the AllegroGraph client's HTTP layer.
 * 
 * For now, this extends RepositoryException, allowing it to be
 * thrown as a RepositoryException in com.franz.agraph.repository; 
 * one drawback is that the HTTP layer is not independent of the 
 * higher level repository package.
 * 
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
