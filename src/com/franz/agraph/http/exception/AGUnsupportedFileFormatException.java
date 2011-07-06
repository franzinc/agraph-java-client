/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
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
		super(message,cause);
	}

}
