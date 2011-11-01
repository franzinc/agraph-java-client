/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.handler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.exception.AGMalformedDataException;
import com.franz.agraph.http.exception.AGMalformedQueryException;
import com.franz.agraph.http.exception.AGPreconditionFailedException;
import com.franz.agraph.http.exception.AGUnsupportedFileFormatException;
import com.franz.agraph.http.exception.AGUnsupportedQueryLanguageException;

public class AGErrorHandler extends AGResponseHandler {

	// Standard Sesame error types
	public static final String MALFORMED_QUERY = "MALFORMED QUERY";
	public static final String MALFORMED_DATA = "MALFORMED DATA";
	public static final String UNSUPPORTED_QUERY_LANGUAGE = "UNSUPPORTED QUERY LANGUAGE";
	public static final String UNSUPPORTED_FILE_FORMAT = "UNSUPPORTED FILE FORMAT";

	// Extended error types
	public static final String PRECONDITION_FAILED = "PRECONDITION FAILED";
	public static final String IO_EXCEPTION = "IO EXCEPTION";

	private AGHttpException result = null;

	public AGErrorHandler() {
		super("text/plain");
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		InputStream response = getInputStream(method);
		String errorString = streamToString(response);
		result = newException(errorString);
	}
	
	protected static AGHttpException newException(String errorString) {
		int colonIdx = errorString.indexOf(':');
		if (colonIdx >= 0) {
			String type = errorString.substring(0, colonIdx).trim();
			String message = errorString.substring(colonIdx + 1);
			if (MALFORMED_DATA.equals(type)) {
				return new AGMalformedDataException(message);
			} else if (MALFORMED_QUERY.equals(type)) {
				return new AGMalformedQueryException(message);
			} else if (PRECONDITION_FAILED.equals(type)) {
				return new AGPreconditionFailedException(message);
			} else if (UNSUPPORTED_FILE_FORMAT.equals(type)) {
				return new AGUnsupportedFileFormatException(message);
			} else if (UNSUPPORTED_QUERY_LANGUAGE.equals(type)) {
				return new AGUnsupportedQueryLanguageException(message);
			}
		}
		// unrecognized error type, use the whole errorString
		return new AGHttpException(errorString);
	}
	
	
	public AGHttpException getResult() {
		return result;
	}
	
}
