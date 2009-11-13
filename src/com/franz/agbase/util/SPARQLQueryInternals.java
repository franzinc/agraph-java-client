package com.franz.agbase.util;

import com.franz.agbase.SPARQLQueryConstants.ENGINE;
import com.franz.agbase.SPARQLQueryConstants.PLANNER;
import com.franz.agbase.impl.NamedAttributeList;

public abstract class SPARQLQueryInternals extends QueryBase {
	
	public NamedAttributeList queryAttributes;
	
	// These values are passed in a trailing pseudo-keyword argument list
	protected static final String EXT = "extendedp";
	protected static final String MEM = "memoizep";
	protected static final String MEMO = "memos";
	protected static final String WITHV = "with-variables";
	protected static final String LOADFN = "load-function";
	protected static final String PREFIXES = "default-prefixes";
	protected static final String BASE = "default-base";
	protected static final String NAMED = "from-named";
	protected static final String FROM = "from";
	protected static final String BEHAVIOR = "default-dataset-behavior";
	protected static final String HASVALUE = "has-value";
	protected static final Object[] queryOptions = new Object[] {
		FROM, String[].class,      // { uri-label ... }
		NAMED, String[].class,     // { uri-label ... }
		BASE, String.class,
		PREFIXES, String[].class,    // { prefix uri ... } ==> ((prefix uri) ... )
		WITHV, String[].class,     // { name, value. ... } ==>  ((name . part) ...)
		BEHAVIOR, String.class,
		HASVALUE, Boolean.class,
		EXT, Boolean.class,
		MEM, Boolean.class,
		MEMO, String.class,    //run-sparql: memos -- lookup hash-table in table
		LOADFN, String.class,  // must be name of Lisp function using read-from-string
		ENGINE.attrName, String.class,
		PLANNER.attrName, String.class
	};
	
	// These values are kept in local variables and passed directly to server functions
	protected int limit = 0;
	protected int offset = 0;
	protected String vars;
	protected String resultsFormat = null;
	protected String RDFFormat = null;
	protected String[] resultVars = null;
	
	protected boolean haveResultCursor = false;
	protected String resultString = null;
	protected long resultCount = -1;
	protected boolean haveBooleanResult = false;
	protected boolean booleanResult = false;
	
	protected ENGINE engine = null;
	protected PLANNER planner = null;
	
	protected void freshStateVars() {
		haveResultCursor = false;
		resultString = null;
		resultCount = -1;
		haveBooleanResult = false;
		booleanResult = false;
		resultVars = null;
	}
	
	protected boolean getBooleanResult () { 
		if ( haveBooleanResult ) return booleanResult;
		throw new IllegalStateException("BooleanResult is not set.");
	}
	
	protected long getResultCount () { 
		if ( -1<resultCount ) return resultCount;
		throw new IllegalStateException("ResultCount is not set.");
	}
	
	protected String[] getResultNames () {
		if ( null!=resultVars ) return resultVars;
		throw new IllegalStateException("Result variable names are not set.");
	}
	
	protected Boolean getHasValue () {
		return (Boolean) queryAttributes.getAttribute(HASVALUE);
	 }
	
	protected Boolean getExtended() {
		return (Boolean) queryAttributes.getAttribute(EXT);
	}
	
	protected Boolean getMemoized() {
		return (Boolean) queryAttributes.getAttribute(MEM);
	}
	
	protected Object[] saveWithVars = null;
	protected abstract String convertWithVariable( AGBase ag, Object var );
	/**
	 * Set the with-variables option for the SPARQL query.
	 * @param ag the AllegroGraph instance where the variable values are resolved.
	 * @param withVariables an array of alternating variable names and values.
	 *      The variable names must be strings. 
	 *      The variable values may be any valid triple part specifier as in addStatement.
	 */
	protected String[] saveWithVariables( AGBase ag, Object[] withVariables ) {
		String[] withVars = new String[withVariables.length];
		boolean even = true;
		for (int i = 0; i < withVars.length; i++) {
			if ( even )
			{
				Object v = withVariables[i];
				if ( v instanceof String )
					withVars[i] = (String)v;
				else
					throw new IllegalArgumentException("Variable name must be String " + v);
				even = false;
			}
			else
			{
				withVars[i] = convertWithVariable(ag, withVariables[i]);
				even = true;
			}
		}
		saveWithVars = withVariables.clone();
		return withVars;
	}

}
