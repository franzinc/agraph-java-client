
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.ag.exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime exception that may embed other exceptions within it.
 */
public class NiceException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String message = "";
	private List<Exception> embeddedExceptions = new ArrayList<Exception>();
	
	public NiceException (String message, Exception embeddedException) {
		this.message = message;
		if (embeddedException instanceof NiceException) {
			this.embeddedExceptions = ((NiceException)embeddedException).embeddedExceptions;
		}
		this.embeddedExceptions.add(embeddedException);
		this.setStackTrace(this.embeddedExceptions.get(0).getStackTrace());
	}
	
	public NiceException (Exception embeddedException) {
		this.embeddedExceptions.add(embeddedException);
	}
	
	public NiceException(String message) {
		this.message = message;
	}

	public String getMessage () {
		String msg = this.message + "\n";
		for (Exception e : this.embeddedExceptions) {
			String m = e.getMessage();
			if ((m == null) || m.length() == 0) m = e.toString();
			msg += "\n" + m;
		}
		return msg.substring(0, msg.length() - 1);
	}

// HOPEFULLY, CALLING 'setStackTrace' MAKES THESE SUPERFLUOUS:
//	public Throwable getCause() {
//		if (!this.embeddedExceptions.isEmpty())
//			return this.embeddedExceptions.get(0).getCause();
//		else
//			return super.getCause();
//	}
//	
//
//	public StackTraceElement[] getStackTrace() {
//		if (!this.embeddedExceptions.isEmpty())
//			return this.embeddedExceptions.get(0).getStackTrace();
//		else
//			return super.getStackTrace();
//	}
//	
//	public void printStackTrace () {
//		if (!this.embeddedExceptions.isEmpty())
//			this.embeddedExceptions.get(0).printStackTrace();
//		else
//			super.printStackTrace();
//	}
	
	public String toString () {return this.getMessage();}
	
}
