
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

package com.franz.agjena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.franz.agjena.exceptions.NiceException;

public class Utils {

	public static boolean isNullString (String s) {
		return s == null || s.length() == 0;
	}
	
	public static String uriToLocalName (String uri) {
		int sharpPos = uri.lastIndexOf("#");
		int slashPos = uri.lastIndexOf("/");
		int colonPos = uri.lastIndexOf(":");
		int pos = Math.max(sharpPos, Math.max(slashPos, colonPos));
		if (pos >= 0) return uri.substring(pos + 1);
		throw new NiceException("Caught illegal URI '" + uri + "'.");
	}
	
	/**
	 * If 'something' is a collection or an iterator, iterate over it and
	 * return a list of its elements.  Otherwise, return a singleton
	 * list containing 'something'.
	 * 
	 * Possible ToDo: Make it understand maps.
	 */
	public static List toList (Object something) {
		if (something instanceof List) return (List)something;
		List<Object> answer = new ArrayList<Object>();
		if (something instanceof Collection) {
			for (Object item : ((Collection)something)) answer.add(item);
		} else if (something instanceof Iterator) {
			Iterator it = (Iterator)something;
			while(it.hasNext()) { answer.add(it.next()); }			
		} else {
			answer.add(something);
		}
		return answer;
	}
	
	/**
	 * Currently semi-phony URI test.  Replace with something else.
	 */
	public static boolean isLegalURI (String uri) {
		if (uri == null) return false;
		return uri.startsWith("http://");
	}
	
	/**
	 * Need to upgrade this to insure uniqueness.
	 */
	public static String uriFromBNodeId (String id) {
		String uri = "http://www.franz.com/temporary#" + id;
		System.out.println("CREATING QUESTIONABLE URI: " + uri);
		return uri;
	}
}
