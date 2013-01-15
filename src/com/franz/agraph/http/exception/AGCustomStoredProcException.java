/******************************************************************************
 ** Copyright (c) 2008-2013 Franz Inc.
 ** All rights reserved. This program and the accompanying materials
 ** are made available under the terms of the Eclipse Public License v1.0
 ** which accompanies this distribution, and is available at
 ** http://www.eclipse.org/legal/epl-v10.html
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
