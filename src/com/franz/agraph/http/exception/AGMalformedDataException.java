/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
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
		super(message,cause);
	}

}
