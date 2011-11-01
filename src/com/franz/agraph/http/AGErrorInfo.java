/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

public class AGErrorInfo extends Exception {

	private static final long serialVersionUID = -7109531872359055983L;

	private AGErrorType errorType;

	public AGErrorInfo(String errMsg, Throwable cause) {
		super(errMsg, cause);
		assert errMsg != null : "errMsg must not be null";
	}

	public AGErrorInfo(String errMsg) {
		this(errMsg, null);
	}

	public AGErrorInfo(AGErrorType errorType, String errMsg) {
		this(errMsg, null);
		this.errorType = errorType;
	}

	public AGErrorType getErrorType() {
		return errorType;
	}

	public String getErrorMessage() {
		return getMessage();
	}

	@Override
	public String toString() {
		if (errorType != null) {
			StringBuilder sb = new StringBuilder(64);
			sb.append(getClass().getName());
			sb.append(": ");
			sb.append(errorType);
			sb.append(": ");
			sb.append(getMessage());
			return sb.toString();
		}
		else {
			return getMessage();
		}
	}

	/**
	 * Parses the string output that is produced by {@link #toString()}.
	 */
	public static AGErrorInfo parse(String errInfoString) {
		String message = errInfoString;
		AGErrorType errorType = null;

		int colonIdx = errInfoString.indexOf(':');
		if (colonIdx >= 0) {
			String label = errInfoString.substring(0, colonIdx).trim();
			errorType = AGErrorType.forLabel(label);

			if (errorType != null) {
				message = errInfoString.substring(colonIdx + 1);
			}
		}

		return new AGErrorInfo(errorType, message.trim());
	}
}
