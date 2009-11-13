
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

/*
 * Created on Jun 30, 2006
 *
 */
package com.franz.ag;


/**
 * AllegroGraph exceptions.
 * 
 * @author mm
 *
 */
public class AllegroGraphException extends Exception {
	
	//FIXME
	// THIS CLASS CAN BE ELIMINATED by merging down to the subclass
	// in com.franz.agbase when com.franz.ag package is discarded.

	/**
	 * 
	 */
	private static final long serialVersionUID = -1311774930268277268L;

	/**
	 * 
	 */
	public AllegroGraphException() {
		super();
	}

	/**
	 * @param arg0
	 */
	public AllegroGraphException(String arg0) {
		super(arg0);

	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public AllegroGraphException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 */
	public AllegroGraphException(Throwable arg0) {
		super(arg0);
	}

}
