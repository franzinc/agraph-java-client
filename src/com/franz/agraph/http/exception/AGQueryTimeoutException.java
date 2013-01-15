/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
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
		super(message,cause);
	}

}
