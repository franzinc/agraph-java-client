/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

import java.util.HashMap;
import java.util.Map;

public class AGErrorType { // can't extend/reuse ErrorType

	private static final Map<String, AGErrorType> registry = new HashMap<String, AGErrorType>();

	// Standard Sesame error types
	
	public static final AGErrorType MALFORMED_QUERY = register("MALFORMED QUERY");
	public static final AGErrorType MALFORMED_DATA = register("MALFORMED DATA");
	public static final AGErrorType UNSUPPORTED_QUERY_LANGUAGE = register("UNSUPPORTED QUERY LANGUAGE");
	public static final AGErrorType UNSUPPORTED_FILE_FORMAT = register("UNSUPPORTED FILE FORMAT");

	// Extended error types

	public static final AGErrorType PRECONDITION_FAILED = register("PRECONDITION FAILED");
	public static final AGErrorType IO_EXCEPTION = register("IO EXCEPTION");

	protected static AGErrorType register(String label) {
		synchronized (registry) {
			AGErrorType errorType = registry.get(label);

			if (errorType == null) {
				errorType = new AGErrorType(label);
				registry.put(label, errorType);
			}

			return errorType;
		}
	}

	public static AGErrorType forLabel(String label) {
		synchronized (registry) {
			return registry.get(label);
		}
	}

	/**
	 * The error type's label.
	 */
	private String label;

	protected AGErrorType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AGErrorType) {
			return ((AGErrorType)other).getLabel().equals(this.getLabel());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getLabel().hashCode();
	}

	@Override
	public String toString() {
		return label;
	}
}
