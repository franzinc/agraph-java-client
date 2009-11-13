package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.impl.NamedAttributeList;
import com.franz.agbase.impl.TriplesIteratorImpl;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGC;
import com.franz.agbase.util.AGInternals;


/**
 * This class collects the parameters and results of a query that retrieves triples
 *  by patterns of triple parts.
 * <p>
 * A triple is located by a pattern of four components, the subject, predicate, object,
 * and graph.  Each of these may be specified as a string in NTriples notation,
 * a Value instance, a UPI instance.  A null value is a wild card that matches anything.
 * The graph position can also be the empty string to denote the null context
 * or default graph of the triple store.
 * <p>
 * The initial value of the subject predicate and object positions is null.
 * The initial value of the graph position is the empty string.
 * <p>
 * Each of the triple components can also match a range of values if the 
 * corresponding end parameter is set.  In order to specify a range, the start 
 * and end must be specified as an EncodedLiteral instance or a UPI instance
 * that denotes an encoded UPI.  The strings "min" and "max" can also be used
 * to denote the minimum and maximum values of the corresponding UPI type.
 * <p>
 * The search can be managed in more detail by setting various attributes that
 * control what triples are examined and what triples are returned. 
 * <p>
 * An instance can be used many times to repeat the same query with identical
 * or different parameters.  The result fields are reset every time a parameter 
 * is modified.
 * 
 * 
 * @author mm
 *
 */
public class TriplesQuery {
	
	private NamedAttributeList queryAttributes;
	
	/**
	 * @return the context component of the query.
	 */
	public Object getContext() {
		return context;
	}

	/**
	 * Set the context component of the query.
	 * @param context the context to set.
	 *   A null value is a wild card that matches anything.
	 *   The empty string denote the default context, or null graph.
	 * @throws AllegroGraphException 
	 */
	public void setContext(Object context) throws AllegroGraphException {
		Object v = AllegroGraph.anyContextRef(context, 5);
		freshState();
		cref = v;
		this.context = context;
	}

	/**
	 * @return the contextEnd component of the query.
	 */
	public Object getContextEnd() {
		return contextEnd;
	}

	/**
	 * @param contextEnd the contextEnd to set
	 * @throws AllegroGraphException 
	 */
	public void setContextEnd(Object contextEnd) throws AllegroGraphException {
		Object v = AllegroGraph.anyContextRef(contextEnd, 6);
		freshState();
		ceref = v;
		this.contextEnd = contextEnd;
	}

	/**
	 * @return the object component of the query.
	 */
	public Object getObject() {
		return object;
	}
	
	private Object setPart ( Object ref ) {
		Object v = AllegroGraph.validRangeRef(ref);
		freshState();
		return v;
	}

	/**
	 * @param object the object to set
	 */
	public void setObject(Object object) {
		oref = setPart(object);
		this.object = object;
	}

	/**
	 * @return the objectEnd component of the query.
	 */
	public Object getObjectEnd() {
		return objectEnd;
	}

	/**
	 * @param objectEnd the objectEnd to set
	 */
	public void setObjectEnd(Object objectEnd) {
		oeref = setPart(objectEnd);
		this.objectEnd = objectEnd;
	}

	/**
	 * @return the predicate component of the query.
	 */
	public Object getPredicate() {
		return predicate;
	}

	/**
	 * @param predicate the predicate to set
	 */
	public void setPredicate(Object predicate) {
		pref = setPart(predicate);
		this.predicate = predicate;
	}

	/**
	 * @return the predicateEnd component of the query.
	 */
	public Object getPredicateEnd() {
		return predicateEnd;
	}

	/**
	 * @param predicateEnd the predicateEnd to set
	 */
	public void setPredicateEnd(Object predicateEnd) {
		peref = setPart(predicateEnd);
		this.predicateEnd = predicateEnd;
	}

	/**
	 * @return the subject component of the query.
	 */
	public Object getSubject() {
		return subject;
	}

	/**
	 * @param subject the subject to set
	 */
	public void setSubject(Object subject) {
		sref = setPart(subject);
		this.subject = subject;
	}

	/**
	 * @return the subjectEnd component of the query.
	 */
	public Object getSubjectEnd() {
		return subjectEnd;
	}

	/**
	 * @param subjectEnd the subjectEnd to set
	 */
	public void setSubjectEnd(Object subjectEnd) {
		seref = setPart(subjectEnd);
		this.subjectEnd = subjectEnd;
	}

	/**
	 * Create a new empty query with default arguments.
	 *
	 */
	public TriplesQuery () {
		super();
		queryAttributes = new NamedAttributeList(queryOptions);
		}
	
	// These are the values set by the user, to be returned if queried.
	private Object subject = null;
	private Object predicate = null;
	private Object object = null;
	private Object context = "";
	private Object subjectEnd = null;
	private Object predicateEnd = null;
	private Object objectEnd = null;
	private Object contextEnd = null;
	
	// These are the verified and encoded values ready to be passed 
	// to the server.
	private Object sref = null;
	private Object pref = null;
	private Object oref = null;
	private Object cref = AGC.AGU_NULL_CONTEXT;
	private Object seref = null;
	private Object peref = null;
	private Object oeref = null;
	private Object ceref = null;
	
	
	
	// These values are kept in local variables and passed directly to server functions
	private boolean includeInferred = false;
	private boolean withParts = true;
	private int lookahead = -1;   // -1 means unset, use the default
	
	
	
	
	// These values are passed in a trailing pseudo-keyword argument list
	private static final String INCDELS = "include-deleted";
	private static final String DROPENC = "omit-encoded";
	private static final String DROPNON = "omit-non-encoded";
	private static final String FILTER = "filter";
	private static final String INDEXED = "indexed-only";
	private static final String WITHPARTS = "with-parts";
	private static final String INFER = "infer";
	private static final String HASVALUE = "has-value";
	private static final Object[] queryOptions = new Object[] {
		INCDELS, Boolean.class,      
		DROPENC, Boolean.class,     
		DROPNON, Boolean.class,
		FILTER, String.class,    
		INDEXED, Boolean.class,
		WITHPARTS, Boolean.class,
		INFER, Boolean.class,
		HASVALUE, Boolean.class
	};
	
	
	private boolean haveResultCursor = false;
	private TriplesIterator resultCursor = null;
	private long resultCount = -1;
	private boolean haveBooleanResult = false;
	private boolean booleanResult = false;
	private AllegroGraph ag = null;
	
	private void freshState() {
		haveResultCursor = false;
		resultCursor = null;
		resultCount = -1;
		haveBooleanResult = false;
		booleanResult = false;
	}
	
	/**
	 * Query the result of a query that had a boolean result.
	 * @return the boolean result.
	 * @throws IllegalStateException if a boolean result is not available.
	 */
	public boolean getBooleanResult () { 
		if ( haveBooleanResult ) return booleanResult;
		throw new IllegalStateException("BooleanResult is not set.");
	}
	
	/**
	 * Query the count value of a query that returns a numeric result.
	 * @return the number of results returned by a count() call.
	 * @throws IllegalStateException if the count is not available.
	 */
	public long getResultCount () { 
		if ( -1<resultCount ) return resultCount;
		throw new IllegalStateException("ResultCount is not set.");
	}
	
	
	/**
	 * Query the includeInferred option.
	 * @return the includeInferred
	 */
	public boolean isIncludeInferred() {
		return includeInferred;
	}
	
	/**
	 * Modify the includeInferred option.
	 * @param includeInferred the desired value.
	 */
	public void setIncludeInferred(boolean includeInferred) {
		freshState();
		this.includeInferred = includeInferred;
	}
	
	
	/**
	 * Query the includeDeleted option.
	 * When true, deleted triples are included in the result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getIncludeDeleted() {
		return boolAttr(INCDELS);
	}
	
	private int boolAttr ( String attr ) {
		Object v = queryAttributes.getAttribute(attr);
		if ( v==null ) return -1;
		if ( ((Boolean)v).booleanValue() ) return 1;
		return 0;
	}
	
	/**
	 * Set the includeDeleted option.
	 * When true, deleted triples are included in the result.
	 * @param true or false
	 */
	public void setIncludeDeleted(boolean v) {
		freshState();
		queryAttributes.setAttribute(INCDELS, v);
	}
	
	/**
	 * Set the omitEncoded option.
	 * When true, encoded triples are not included in the search result.
	 * @param true or false
	 */
	public void setOmitEncoded ( boolean v ) {
		freshState();
		queryAttributes.setAttribute(DROPENC, v);
	}
	
	/**
	 * Query the omitEncoded option.
	 * When true, encoded triples are not included in the search result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getOmitEncoded () {
		return boolAttr(DROPENC);
	}
	
	
	/**
	 * Query the hasValue option.
	 * When true, hasValue reasoning is enabled.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getHasValue () { 
		Object v = queryAttributes.getAttribute(HASVALUE);
		if ( v==null ) return -1;
		if ( ((Boolean)v).booleanValue() ) return 1;
		return 0;
		}
	
	/**
	 * Enable or disable hasValue reasoning.
	 * This option is in effect only if reasoning is ebabled.
	 * @param v true or false
	 */
	public void setHasValue ( boolean v ) {
		freshState();
		queryAttributes.setAttribute(HASVALUE, new Boolean(v));
	}
	
	/**
	 * Set the initial lookahead parameter for this query.
	 * The lookahead parameter specifies how many results are cached 
	 * in the client.  A large value allows quick access to results but may incur 
	 * a delay when the results are transmitted immediately after the query.
	 * A small value implies more round-trips to the server.
	 * <p>
	 * A zero value specifies that all the search results remain on the server.
	 * This option creates a TriplesIterator that can be passed to a serializer in the server.
	 * <p>
	 * A negative value reverts the setting to the default specified in the AllegroGraph class.
	 * 
	 * @param lh the desired lookahead value.  The initial setting is 1000.
	 */
	public void setLookAhead ( int lh ) {
		if ( lh<0 ) 
			throw new IllegalArgumentException( "Lookahead must be positive" );
		lookahead = lh;
	}
	
	/**
	 * Query the lookahead parameter for the query.
	 * @return The lookahead parameter specifies how many results are cached 
	 * in the client.  A large value allows quick access to results but may incur 
	 * a delay when the results are transmitted immediately after the query.
	 * A small value implies more round-trips to the server.
	 * <p>
	 * A zero value specifies that all the search results remain on the server.
	 * This option creates a Cursor that can be passed to a serializer in the server.
	 * <p>
	 * A negative value means that the current system default will be used.
	 * 
	 */
	public int getLookAhead () {
		if ( lookahead>-1 ) return lookahead;
		if ( ag==null ) return -1;
		if ( 0==ag.defaultLookAhead )
			return TriplesIteratorImpl.getDefaultLookAhead();
		return ag.defaultLookAhead;
	}
	
	/**
	 * Set the omitNonEncoded option.
	 * When true, non-encoded triples are not included in the search result.
	 * @param true or false
	 */
	public void setOmitNonEncoded ( boolean v ) {
		freshState();
		queryAttributes.setAttribute(DROPNON, v);
	}
	
	/**
	 * Query the omitEncoded option.
	 * When true, encoded triples are not included in the search result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getOmitNonEncoded () {
		return boolAttr(DROPNON);
	}
	
	/**
	 * Set the indexedOnly option.
	 * When true, only indexed triples are included in the search result.
	 * @param true or false
	 */
	public void setIndexedOnly ( boolean v ) {
		freshState();
		queryAttributes.setAttribute(INDEXED, v);
	}
	
	/**
	 * Query the indexedOnly option.
	 * When true, only indexed triples are included included in the search result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getIndexedOnly () {
		return boolAttr(INDEXED);
	}
	
	
	
	
	
	/**
	 * Set the name of a filter function option.
	 * @param name
	 */
	public void setFilterFunction ( String name ) {
		freshState();
		queryAttributes.setAttribute(FILTER, name);
	}
	
	/**
	 * Query the name of the filter function.
	 * @return the name of the load-function, or null if none is set.
	 */
	public String getLoadFunction () {
		return (String) queryAttributes.getAttribute(FILTER);
	}
	
	
	/**
	 * Query the result of a triples query that returns a collection of triples.
	 * This result is available only after a call to one of the describe()
	 * or construct()
	 * methods.
	 * @return the resultCursor
	 */
	public TriplesIterator getResultCursor() {
		if ( haveResultCursor ) return resultCursor;
		throw new IllegalStateException("ResultCursor is not set.");
	}

	private void validate ( AllegroGraph ag ) {
		freshState();
		if ( ag!=null ) this.ag = ag;
		if ( this.ag==null )
			throw new IllegalStateException("Cannot run without a triple store.");
	}
	
	/**
	 * Specify the triple store against which this query will run.
	 * Setting the store clears out any previous results.
	 * @param ag
	 */
	public void setTripleStore ( AllegroGraph ag ) {
		freshState();
		this.ag = ag;
	}
	
	/**
	 * Query the triple store agaist which this query has or will run.
	 * @return the AllegroGraoh instance or null.
	 */
	public AGInternals getTripleStore () { return ag; }
	
	private Object[] getOpts () {
		if ( withParts )
			queryAttributes.setAttribute(WITHPARTS, true);
		return queryAttributes.getList();
	}
	
	/**
	 * Count the number of triples located by a pattern of parts.
	 * 
	 * @return the number of results found.  The actual results are discarded.
	 * @throws AllegroGraphException
	 */
	public long count () throws AllegroGraphException {
		validate(null);
		Object v = ag.verifyEnabled().getTriples(ag, sref, pref, oref, cref,
										seref, peref, oeref, ceref, -5, getOpts());
		return AGConnector.longValue(v);
	}
	
	/**
	 * Count the number of triples located by a pattern of parts.
	 * @param ag The triple store where the query should run.
	 * @return the number of results found.  The actual results are discarded.
	 * @throws AllegroGraphException
	 */
	public long count ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return count();
	}
	
	
	/**
	 * Estimate the number of triples located by a pattern of parts
	 * using only the information in indices.
	 * Unindexed triples are not included in the estimate.
	 * @param roughly When true, the estimate can be off by as much as twice 
	 *     the triple store's metaindex-skip-size for each index chunk involved.
	 *     When false, return a more accurate (but slower) estimate.
	 * @return the number of results estimated.  The actual results are never located.
	 * @throws AllegroGraphException
	 */
	public long estimate( boolean roughly ) throws AllegroGraphException {
		validate(null);
		Object v = ag.verifyEnabled().getTriples(ag, sref, pref, oref, cref,
				seref, peref, oeref, ceref,
				(roughly?-3:-4), getOpts());
		resultCount = AGConnector.longValue(v);
		return resultCount;
	}
	
	public long estimate ( boolean roughly, AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return estimate(roughly);
	}
	
	
	/**
	 * Determine if a triple matching a pattern exists..
	 * 
	 * @return true if a triple exists.  The triple is not returned.
	 * @throws AllegroGraphException
	 */
	public boolean ask () throws AllegroGraphException {
		validate(null);
		Object v = ag.verifyEnabled().getTriples(ag, sref, pref, oref, cref,
				seref, peref, oeref, ceref, -1, getOpts());
		booleanResult =  (Boolean)v;
		haveBooleanResult = true;
		return booleanResult;
	}
	public boolean ask ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return ask();
	}
	
	/**
	 * Find one triple that matches a pattern.
	 * Any additional triples are ignored.
	 * @return the Triple instance or null.
	 * @throws AllegroGraphException
	 */
	public Triple find () throws AllegroGraphException {
		freshState();
		validate(null);
		Object v = ag.verifyEnabled().getTriples(ag, sref, pref, oref, cref,
				seref, peref, oeref, ceref,
				-2, getOpts());
		resultCursor = (TriplesIterator) v;
		if ( !resultCursor.hasNext() ) return null;
		return resultCursor.next();
		
	}
	
	public Triple find ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return find();
	}
	
	
	
	/**
	 * Find all the triples that match the pattern.
	 * 
	 * @return A Cursor instance that can iterate over the results.
	 * @throws AllegroGraphException if a problem was encountered during the search.
	 * @throws IllegalArgumentException if this instance is not properly initialized.
	 */
	public TriplesIterator run () throws AllegroGraphException {
		validate(null);
		Object v = ag.verifyEnabled().getTriples(ag, sref, pref, oref, cref,
				seref, peref, oeref, ceref,
				getLookAhead(), getOpts());
		resultCursor = (TriplesIterator) v;
		return resultCursor;
	}
	public TriplesIterator run ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return run();
	}
	
	
	
	
}
