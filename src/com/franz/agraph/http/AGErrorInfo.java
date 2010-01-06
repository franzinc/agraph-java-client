/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

public class AGErrorInfo {

	private AGErrorType errorType;

	private String errMSg;

	public AGErrorInfo(String errMsg) {
		assert errMsg != null : "errMsg must not be null";
		this.errMSg = errMsg;
	}

	public AGErrorInfo(AGErrorType errorType, String errMsg) {
		this(errMsg);
		this.errorType = errorType;
	}

	public AGErrorType getErrorType() {
		return errorType;
	}

	public String getErrorMessage() {
		return errMSg;
	}

	@Override
	public String toString() {
		if (errorType != null) {
			StringBuilder sb = new StringBuilder(64);
			sb.append(errorType);
			sb.append(": ");
			sb.append(errMSg);
			return sb.toString();
		}
		else {
			return errMSg;
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
