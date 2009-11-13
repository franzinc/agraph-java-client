 
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

package com.franz.agsail;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.ResourceNode;
import com.franz.agbase.URINode;
import com.franz.agbase.ValueNode;
import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.util.AGSInternal;




public class AGForSail extends AGSInternal
	implements ValueFactory {
	
	/**
	 * The name of this class identifies the version of the AllegroGraph
	 * Java implementation.
	 * This name is also visible in the list of members in a jar file
	 * when it is inspected with Emacs or WinZip.
	 */
	public static class V3_2Jan27 {}
	
	

	/**
	 * Query the current AGForSail version.
	 * @return a version string.
	 */
	@SuppressWarnings("unchecked")
	public static String version () { 
		Class thisClass = AGForSail.class;
		Class[] mems = thisClass.getDeclaredClasses();
		String home = thisClass.getName();
		String s = "";
		home = home + "$V";
		for (int i = 0; i < mems.length; i++) {
			String sub = mems[i].getName();
			if ( sub.startsWith(home) )
				s = sub;
		}
		return s; 
		}
	
	/**
	 * Print AllegroGraph version information.
	 *
	 */
	public static void main ( String[] args ) {
			System.out.println(version());
	}
	
	
	

	public AGForSail ( AllegroGraph d) {
		super(d);
	}

	public BNode createBNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public BNode createBNode(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(byte arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(short arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(long arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(float arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(double arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(XMLGregorianCalendar arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Literal createLiteral(String arg0, String arg1) {
		return (Literal) coerceToSailValue(getDirectInstance().createLiteral(arg0, arg1));
	}

	public Literal createLiteral(String arg0, URI arg1) {
		return (Literal) coerceToSailValue(getDirectInstance().createTypedLiteral(arg0, arg1.toString()));
	}

	public Statement createStatement(Resource arg0, URI arg1, Value arg2) {
		return createStatement(arg0, arg1, arg2, null);
	}

	public Statement createStatement(Resource arg0, URI arg1, Value arg2, Resource arg3) {
		return coerceToSailTriple(getDirectInstance().createStatement(
				(ResourceNode)coerceToAGPart(arg0), 
				(URINode)coerceToAGPart(arg1), 
				(ValueNode)coerceToAGPart(arg2),
				(ResourceNode)coerceToAGPart(arg3)));
	}

	public URI createURI(String arg0) {
		return (URI)coerceToSailValue(getDirectInstance().createURI(arg0));
	}

	public URI createURI(String arg0, String arg1) {
		return (URI)coerceToSailValue(getDirectInstance().createURI(arg0, arg1));
	} 
	
	
	public int getLookAhead () { return getDirectInstance().getLookAhead();  }
	public void setLookAhead ( int n ) { getDirectInstance().setLookAhead(n); }
	public int getSelectLimit () { return getDirectInstance().getSelectLimit(); }
	public void setSelectLimit ( int v ) { getDirectInstance().setSelectLimit(v); }
	public void setSyncEveryTime ( boolean s ) { getDirectInstance().setSyncEveryTime(s); }
	public void clear () throws AllegroGraphException { getDirectInstance().clear(); }
	public void indexNewTriples (boolean wait) throws AllegroGraphException { getDirectInstance().indexNewTriples(wait); }
	public void indexAllTriples (boolean wait) throws AllegroGraphException { getDirectInstance().indexAllTriples(wait); }
	public long numberOfTriples () throws AllegroGraphException { return getDirectInstance().numberOfTriples(); }
	
	public void addStatement ( Resource s, URI p, Value o ) throws AllegroGraphException {
		getDirectInstance().addStatement(coerceToAGPart(s), coerceToAGPart(p), coerceToAGPart(o), null);
	}
	public void addStatement ( Resource s, URI p, Value o, Resource c ) throws AllegroGraphException {
		getDirectInstance().addStatement(coerceToAGPart(s), coerceToAGPart(p), coerceToAGPart(o), coerceToAGPart(c));
	}

	public AGSailCursor getFreetextStatements ( String pattern ) throws AllegroGraphException {
		return coerceToSailCursor(getDirectInstance().getFreetextStatements(pattern));
	}

	public ValueSetIterator getFreetextUniqueSubjects(String pattern) throws AllegroGraphException {
		return getDirectInstance().getFreetextUniqueSubjects(pattern);
	}

	public AGSailCursor getStatements(boolean includeInferred, Resource subject, URI predicate, Value object, Resource context) throws AllegroGraphException {
		return coerceToSailCursor(getDirectInstance().getStatements(includeInferred,
				coerceToAGPart(subject), coerceToAGPart(predicate), coerceToAGPart(object),
				coerceToAGPart(context)));
	}

	public AGSailCursor getStatements(boolean includeInferred, Resource subject, URI predicate, Value object) throws AllegroGraphException {
		return coerceToSailCursor(getDirectInstance().getStatements(includeInferred,
				coerceToAGPart(subject), coerceToAGPart(predicate), coerceToAGPart(object)));
	}


	public void registerFreetextPredicate(URI predicate) throws AllegroGraphException {
		getDirectInstance().registerFreetextPredicate(coerceToAGPart(predicate));
		
	}

	public void removeStatements(Resource subject, URI predicate, Value object, Resource... context) throws AllegroGraphException {
		if ( context==null )
			throw new IllegalArgumentException("Null context array.");
		if ( 0==context.length )
			// No contexts specified --> apply to all contexts.
			getDirectInstance().removeStatements(coerceToAGPart(subject), coerceToAGPart(predicate), coerceToAGPart(object),
					null);
		else
			for ( Resource c : context ) {
				Object agc;
				if ( c==null )
					agc = "";  // A null in Sesame maps to "" in AG
				else
					agc = coerceToAGPart(c);
				getDirectInstance().removeStatements(coerceToAGPart(subject), coerceToAGPart(predicate), coerceToAGPart(object),
						agc);
			}
		
	}
	
	
}

