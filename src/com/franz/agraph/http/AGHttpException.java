package com.franz.agraph.http;

public class AGHttpException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2608901334300829491L;
	
	private final AGErrorInfo errorInfo;
	
	AGHttpException(AGErrorInfo errorInfo) {
		this.errorInfo = errorInfo;
	}
	
	public AGErrorInfo getErrorInfo() {
		return errorInfo;
	}
	
}
