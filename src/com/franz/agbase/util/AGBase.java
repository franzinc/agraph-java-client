package com.franz.agbase.util;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.transport.AGConnector;

public abstract class AGBase {
	


public int selectLimit = 1000;
	
	public boolean sync = true;
	
	public int tsx = -1;
	
	public String storeName = null;

	public String storeDirectory = null;
	
	
	public abstract AGConnector verifyEnabled();
	
	
	
	
	public String getLangPart(UPI id) throws AllegroGraphException {
		if ( !UPIImpl.canReference(id) )
			throw new IllegalStateException("getLangPart " + id);
		return verifyEnabled().getLangPart(this, id);
	}
	
	public String getTextPart(UPI id) throws AllegroGraphException {
		if ( !UPIImpl.canReference(id) )
			throw new IllegalStateException("getTextPart " + id);
		return verifyEnabled().getTextPart(this, id);
	}


	public String getTypePart(UPI id) throws AllegroGraphException {
		if ( !UPIImpl.canReference(id) )
			throw new IllegalStateException("getTypePart " + id);
		return verifyEnabled().getTypePart(this, id);
	}


	public String getText(UPI id, String lit) {
		if (lit != null) return lit;
		if ( !UPIImpl.canReference(id) ) throw new IllegalStateException("getText " + id);
		try {
			return getTextPart(id);
		} catch (AllegroGraphException e) {
			throw new IllegalStateException("getText " + e);
		}
	}

	public static String refNtripleString ( String s ) {
		if ( s==null ) return s;
		if ( 0==s.length() ) return s;
		if ( s.startsWith("<" ) ) {
			if ( s.endsWith(">") )
				return s;
		}
		else if ( s.startsWith("\"") )
				return s;
		else if ( s.startsWith("!") )
			return s;
		throw new IllegalArgumentException
			("String does not seem to be in ntriples format: " + s);
	}
	
	protected static String refUPIToString(UPI n) {
		//prdb("valRef", ("" + n.asChars("%Z")));
		if ( UPIImpl.isNullContext(n) ) return "" + ((UPIImpl) n).getCode();
		if ( UPIImpl.canReference(n) ) return ("" + ((UPIImpl) n).asChars("%Z"));
		// must be wild or a triple id
		return "" + ((UPIImpl) n).getCode();
	}
	

	protected static void notValRef(Object node) {
		throw new IllegalArgumentException
	       ("Cannot convert to AG reference string " + node);
	}


	// MADE THIS PUBLIC - RMM
	public static String refNodeToString(String uri) { return "%N" + uri; }


	protected static String refAnonToString(String label) { return "%B" + label; }

	protected static String refLitInternal(String prefix, String part1, CharSequence part2) {
		// %L label            %G lang label
		// %T label type-uri   OBSOLETE: %R label type-id
		// %U label type-upi
		
		if ( part2==null ) return prefix+part1;
		
		// compute length of part1
		int len = part1.length();
		String dd = "";
		String digits = "0123456789abcdefghijklmnopqrstABCDEFGHIJKLMNOPQRST";
		while ( len>0 )
		{
			int digit = len%50;
			len = len/50;
			dd = digits.charAt(digit) + dd;
		}
		if ( 0==dd.length() ) dd = "0";
		return prefix + dd + "X" + part1 + part2;	
	}


	protected static String refLitToString(String value, UPI type) {
		return refLitInternal( "%U", value, ((UPIImpl) type).asChars());
	}
	

	// MADE THIS PUBLIC - RMM
	public static String refLitToString(String label, String lang, String type) {
		if ( type==null ) 
		{
			if ( lang==null ) return refLitInternal("%L", label, null);
			if ( 0==lang.length() ) return refLitInternal("%L", label, null);
			return refLitInternal("%G", lang, label); 
		}
		if ( lang==null ) ;
		else if ( 0==lang.length() ) ;
		else throw new IllegalArgumentException
		  ("Cannot specify language tag and type URI on one literal: " +
				  label + " " + lang + " " + type);
		if ( 0==type.length() )
			throw new IllegalArgumentException
			("Empty string is not a valid type URI: " + label);
		return refLitInternal("%T", label, type);
	}

	protected static UPI validUPI(UPI id) {
		if ( UPIImpl.canReference(id) ) return id;
		throw new IllegalArgumentException
					("Id number may not be negative " + id);
	}

	protected static Object minMaxRef(Object ref) {
		if ( ref==null ) return null;
		if ( ref instanceof String ) {
			String s = (String) ref;
			if ( (s.indexOf("m")==0) || (s.indexOf("M")==0) )
			{
				if ( ((s.indexOf("i")==1) || (s.indexOf("I")==1))
						&&
						((s.indexOf("n")==2) || (s.indexOf("N")==2)) )
					return "min";
				if ( ((s.indexOf("a")==1) || (s.indexOf("A")==1))
						&&
						((s.indexOf("x")==2) || (s.indexOf("X")==2)) )
					return "max";
			}
		}
		return null;
	}
	
	protected String[] validRefStrings(String[] nodes) {
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = refNtripleString(nodes[i]);
		}
		return nodes;
		}


	// MADE THIS PUBLIC - RMM
	public String typeToString(int type) {
		// types:  1     2     3        4             5
		//        anon  node  literal  literal/lang  typed-literal
		switch (type) {
		case 1: return "anon";
		case 2: return "node";
		case 3: return "literal";
		case 4: return "literal/lang";
		case 5: return "typed-literal";
		case 6: return "triple";
		case 7: return "default-graph";
		case 8: return "encoded-string";
		case 9: return "encoded-integer";
		case 10: return "encoded-float";
		case 11: return "encoded-triple-id";
		default: return "unknown";
		}
	}
	
	public void discardCursors(Object[] refs) throws AllegroGraphException {
		verifyEnabled().discardCursors(this, refs);
	}
	
	public long twinqlCount ( boolean includeInferred, String query, String vars , int limit, int offset, Object[] more)
	throws AllegroGraphException {
    	Object[] v = verifyEnabled().twinqlSelect(this, query, vars, limit, offset, -1, includeInferred, more);
    	return AGConnector.longValue(v[0]);
    }
	
	

}
