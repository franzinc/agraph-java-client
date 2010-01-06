/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

public class AGHttpException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2608901334300829491L;
	
	private final AGErrorInfo errorInfo;
	
	AGHttpException(AGErrorInfo errorInfo) {
	    super(errorInfo.getErrorMessage());
		this.errorInfo = errorInfo;
	}
	
	public AGErrorInfo getErrorInfo() {
		return errorInfo;
	}
	
}
