/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
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
		super(message,cause);
	}

}
