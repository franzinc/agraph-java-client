package com.franz.agsail;

import com.franz.agbase.AllegroGraphException;


/**
 * This class collects the parameters and results of a query that retrieves triples
 *  by patterns of triple parts.
 * <p>
 * A triple is located by a pattern of four components, the subject, predicate, object,
 * and graph.  Each of these may be specified as a string in NTriples notation,
 * a Value instance, a UPI instance.  A null value is a wildcard that matches anything.
 * The graph position can also be the empty string to denote the null context
 * or default graph of the triple store.
 * <p>
 * The inital value of the subject predicate and object positions is null.
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
public class AGSailTriplesQuery {
	
	com.franz.agbase.TriplesQuery directInstance = null;
	
	
	/**
	 * @return the context component of the query.
	 */
	public Object getContext() {
		return context;
	}

	/**
	 * Set the context component of the query.
	 * @param context the context to set.
	 *   A null valueis a wild card that matches anything.
	 *   The empty string denote the default context, or null graph.
	 * @throws AllegroGraphException 
	 */
	public void setContext(Object context) throws AllegroGraphException {
		this.context = context;
		directInstance.setContext(setPart(context));
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
		this.contextEnd = contextEnd;
		directInstance.setContextEnd(setPart(contextEnd));
	}

	/**
	 * @return the object component of the query.
	 */
	public Object getObject() {
		return object;
	}
	
	private Object setPart ( Object ref ) {
		Object v = ag.coerceToAGPart(ref);
		freshState();
		return v;
	}

	/**
	 * @param object the object to set
	 */
	public void setObject(Object object) {
		this.object = object;
		directInstance.setObject(setPart(object));
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
		directInstance.setObjectEnd(setPart(objectEnd));
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
		directInstance.setPredicate(setPart(predicate));
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
		directInstance.setPredicateEnd(setPart(predicateEnd));
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
		directInstance.setSubject(setPart(subject));
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
		directInstance.setSubjectEnd(setPart(subjectEnd));
		this.subjectEnd = subjectEnd;
	}

	/**
	 * Create a new empty query with default arguments.
	 *
	 */
	public AGSailTriplesQuery ( AGForSail home) {
		super();
		ag = home;
		directInstance = new com.franz.agbase.TriplesQuery();
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
	
	
	
	
	private AGForSail ag = null;
	
	private void freshState() {
	}
	
	/**
	 * Query the result of a query that had a boolean result.
	 * @return the boolean result.
	 * @throws IllegalStateException if a boolean result is not available.
	 */
	public boolean getBooleanResult () { 
		return directInstance.getBooleanResult();
	}
	
	/**
	 * Query the count value of a query that returns a numeric result.
	 * @return the number of results returned by a count() call.
	 * @throws IllegalStateException if the count is not available.
	 */
	public long getResultCount () { 
		return directInstance.getResultCount();
	}
	
	
	/**
	 * Query the includeInferred option.
	 * @return the includeInferred
	 */
	public boolean isIncludeInferred() {
		return directInstance.isIncludeInferred();
	}
	
	/**
	 * Modify the includeInferred option.
	 * @param includeInferred the desired value.
	 */
	public void setIncludeInferred(boolean includeInferred) {
		freshState();
		directInstance.setIncludeInferred(includeInferred);
	}
	
	
	/**
	 * Query the includeDeleted option.
	 * When true, deleted triples are included in the result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getIncludeDeleted() {
		return directInstance.getIncludeDeleted();
	}
	
	
	/**
	 * Set the includeDeleted option.
	 * When true, deleted triples are included in the result.
	 * @param true or false
	 */
	public void setIncludeDeleted(boolean v) {
		directInstance.setIncludeDeleted(v);
	}
	
	/**
	 * Set the omitEncoded option.
	 * When true, encoded triples are not included in the search result.
	 * @param true or false
	 */
	public void setOmitEncoded ( boolean v ) {
		freshState();
		directInstance.setOmitEncoded(v);
	}
	
	/**
	 * Query the omitEncoded option.
	 * When true, encoded triples are not included in the search result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getOmitEncoded () {
		return directInstance.getOmitEncoded();
	}
	
	/**
	 * Set the lookahead parameter for this query.
	 * The lookahead parameter specifies how many results are cached 
	 * in the client.  A large value allows quick access to results but may incur 
	 * a delay when the results are transmitted immediately after the query.
	 * A small value implies more round-trips to the server.
	 * <p>
	 * A zero value specifies that all the search results remain on the server.
	 * This option creates a Cursor that can be passed to a serializer in the server.
	 * @param lh the desired lookahead value.  The initial setting is 1000.
	 */
	public void setLookahead ( int lh ) {
		directInstance.setLookAhead(lh);
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
	 * The initial setting is 1000.
	 */
	public int getLookahead () { return directInstance.getLookAhead(); }
	
	/**
	 * Set the omitNonEncoded option.
	 * When true, non-encoded triples are not included in the search result.
	 * @param true or false
	 */
	public void setOmitNonEncoded ( boolean v ) {
		freshState();
		directInstance.setOmitNonEncoded(v);
	}
	
	/**
	 * Query the omitEncoded option.
	 * When true, encoded triples are not included in the search result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getOmitNonEncoded () {
		return directInstance.getOmitNonEncoded();
	}
	
	/**
	 * Set the indexedOnly option.
	 * When true, only indexed triples are included in the search result.
	 * @param true or false
	 */
	public void setIndexedOnly ( boolean v ) {
		freshState();
		directInstance.setIndexedOnly(v);
	}
	
	/**
	 * Query the indexedOnly option.
	 * When true, only indexed triples are included included in the search result.
	 * @return 1 if true, 0 if false, -1 if unspecified (ie server default)
	 */
	public int getIndexedOnly () {
		return directInstance.getIndexedOnly();
	}
	
	
	
	
	
	/**
	 * Set the name of a filter function option.
	 * @param name
	 */
	public void setFilterFunction ( String name ) {
		directInstance.setFilterFunction(name);
	}
	
	/**
	 * Query the name of the filter function.
	 * @return the name of the load-function, or null if none is set.
	 */
	public String getLoadFunction () {
		return directInstance.getLoadFunction();
	}
	
	
	/**
	 * Query the result of a triples query that returns a collection of triples.
	 * This result is available only after a call to one of the describe()
	 * or construct()
	 * methods.
	 * @return the resultCursor
	 */
	public AGSailCursor getResultCursor() {
		return ag.coerceToSailCursor(directInstance.getResultCursor());
	}
	
	/**
	 * Specify the triple store agaist which this query will run.
	 * Setting the store clears out any previous results.
	 * @param ag
	 */
	public void setTripleStore ( AGForSail ag ) {
		freshState();
		this.ag = ag;
	}
	
	/**
	 * Query the triple store agaist which this query has or will run.
	 * @return the AllegroGraoh instance or null.
	 */
	public AGForSail getTripleStore () { return ag; }
	
	
	/**
	 * Count the number of triples located by a pattern of parts.
	 * 
	 * @return the number of results found.  The actual results are discarded.
	 * @throws AllegroGraphException
	 */
	public long count () throws AllegroGraphException {
		return directInstance.count();
	}
	
	/**
	 * Count the number of triples located by a pattern of parts.
	 * @param ag The triple store where the query should run.
	 * @return the number of results found.  The actual results are discarded.
	 * @throws AllegroGraphException
	 */
	public long count ( AGForSail ag ) throws AllegroGraphException {
		this.ag = ag;
		return directInstance.count(ag.getDirectInstance());
	}
	
	
	/**
	 * Estimate the number of triples located by a pattern of parts
	 * using only the information in indices.
	 * Unindexed triples are not included in the estimate.
	 * @param roughly When true, the estimate can be off by as much as twice 
	 *     the triple store's metaindex-skip-size for each index chunk involved.
	 *     When false, return a more acurate (but slower) estimate.
	 * @return the number of results estimated.  The actual results are never located.
	 * @throws AllegroGraphException
	 */
	public long estimate( boolean roughly ) throws AllegroGraphException {
		return directInstance.estimate(roughly);
	}
	
	
	/**
	 * Determine if a triple matching a pattern exists..
	 * 
	 * @return true if a triple exists.  The triple is not returned.
	 * @throws AllegroGraphException
	 */
	public boolean ask () throws AllegroGraphException {
		return directInstance.ask();
	}
	public boolean ask ( AGForSail ag ) throws AllegroGraphException {
		this.ag = ag;
		return directInstance.ask(ag.getDirectInstance());
	}
	
	/**
	 * Find one triple that matches a pattern.
	 * Any additional triples are ignored.
	 * @return the Triple instance or null.
	 * @throws AllegroGraphException
	 */
	public AGSailTriple find () throws AllegroGraphException {
		return ag.coerceToSailTriple(directInstance.find());
	}
	
	
	
	/**
	 * Find all the triples that match the pattern.
	 * 
	 * @return A Cursor instance that can iterate over the results.
	 * @throws AllegroGraphException if a problem was encountered during the search.
	 * @throws IllegalArgumentException if this instance is not properly initialized.
	 */
	public AGSailCursor run () throws AllegroGraphException {
		return ag.coerceToSailCursor(directInstance.run());
	}
	public AGSailCursor run ( AGForSail ag ) throws AllegroGraphException {
		this.ag = ag;
		return ag.coerceToSailCursor(directInstance.run(ag.getDirectInstance()));
	}
	
	
	
	
}
