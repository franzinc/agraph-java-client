package com.franz.agbase;

import com.franz.ag.NamespaceRegistry;
import com.franz.agbase.impl.NamedAttributeList;
import com.franz.agbase.impl.ValueSetIteratorImpl;
import com.franz.agbase.util.AGBase;
import com.franz.agbase.util.SPARQLQueryInternals;


/**
 * This class collects the parameters and results of a SPARQL query.
 * <p>
 * The typical sequence of steps is to
 *  <ul>
 *   <li>Create an instance.
 *   <li>Set or modify some of the optional parameters.
 *   <li>Call one of the action methods
 *        <ul>
 *          <li>{@link #ask()}
 *          <li>{@link #construct()}
 *          <li>{@link #count()}
 *          <li>{@link #describe()}
 *          <li>{@link #run()}
 *          <li>{@link #select()}
 *         </ul>
 *    <li>Use the value returned by the action method, or extract the result from the
 *        instance.
 *   </ul>
 *  The choice of action method is determined by the kind of query and by the desired
 *  datatype of the result.
 * <p>
 * This document is not intended to be a SPARQL tutorial or reference.
 * Please refer to other documents for the definition of SPARQL syntax and semantics
 * and for the precise behavior of the SPARQL engine.
 * 
 * @author mm
 *
 */
public class SPARQLQuery extends SPARQLQueryInternals implements SPARQLQueryConstants {
	
	/**
	 * Create a new empty SPARQL query with default arguments.
	 *
	 */
	public SPARQLQuery () {
		super();
		queryAttributes = new NamedAttributeList(queryOptions);
		}
	
	
	/**
	 * Get the query string.
	 * @return the query string or null if the instance is not initialized/
	 */
	public String getQuery () { return super.getQuery(); }
	
	/**
	 * Set the query string.
	 * Setting the query string clears out any previous results.
	 * @param newQuery a string containing a complete well-formed SPARQL query.
	 */
	public void setQuery ( String newQuery ) {
		freshState();
		super.setQuery(newQuery); 
		}
	
	
	private TriplesIterator resultCursor = null;
	private ValueSetIterator resultSets = null;
	
	private void freshState() {
		freshStateVars();
		resultCursor = null;
		resultSets = null;
	}
	
	/**
	 * Query the result of a query that has a boolean result.
	 * @return the boolean result.
	 * @throws IllegalStateException if a boolean result is not available.
	 */
	public boolean getBooleanResult () { return super.getBooleanResult(); }
	
	/**
	 * Query the count value of a query that returns a numeric result.
	 * @return the number of results returned by a count() call.
	 * @throws IllegalStateException if the count is not available.
	 */
	public long getResultCount () { return super.getResultCount(); }
	
	/**
	 * Query the names and order of the variables in a SELECT query result.
	 * @return An array of strings.
	 * @throws IllegalStateException if the names are not available.
	 */
	public String[] getResultNames () { return super.getResultNames(); }
	
	
	/**
	 * Query the includeInferred option.
	 * @return the includeInferred
	 */
	public boolean isIncludeInferred() { return super.isIncludeInferred(); }
	
	/**
	 * Modify the includeInferred option.
	 * @param includeInferred the desired value.
	 */
	public void setIncludeInferred(boolean includeInferred) {
		freshState();
		super.setIncludeInferred(includeInferred);
	}
	
	/**
	 * Query the hasValue option.
	 * When true, hasValue reasoning is enabled.
	 * @return true or false, null if unspecified (ie server default is false)
	 */
	public Boolean getHasValue () { return super.getHasValue(); }
	
	/**
	 * Enable or disable hasValue reasoning.
	 * This option is in effect only if reasoning is enabled.
	 * @param v true or false
	 */
	public void setHasValue ( boolean v ) {
		freshState();
		queryAttributes.setAttribute(HASVALUE, new Boolean(v));
	}
	
	/**
	 * Query the limit option.
	 *
	 * The limit option overrides a LIMIT clause in the query.
	 * A value less than 1 specifies that the LIMIT clause in the query applies.
	 * @return the limit
	 */
	public long getLimit() { return limit; }
	
	/** Set the limit option.
	 * The limit option overrides a LIMIT clause in the query.
	 * @param limit the limit to set.  
	 * A value less than 1 specifies that the LIMIT clause in the query applies.
	 */
	public void setLimit( int limit ) {
		freshState();
		this.limit = limit;
	}
	
	/**
	 * Query the offset option.
	 * The offset option overrides an OFFSET clause in the query.
	 * A value less than 1 specifies that the LIMIT clause in the query applies.
	 * @return the offset.
	 */
	public long getOffset() { return offset; }
	
	/**
	 * Set the offset option.
	 * The offset option overrides an OFFSET clause in the query.
	 * @param offset the offset to set. 
	 * A value less than 1 specifies that the LIMIT clause in the query applies.
	 */
	public void setOffset( int offset ) {
		freshState();
		this.offset = offset;
	}
	
	/**
	 * Query the RDFFormat option.
	 * This option applies to queries that return an RDF XML serialization string.
	 * @return the RDFFormat
	 */
	public String getRDFFormat() { return RDFFormat; }
	
	/**
	 * Set the RDFFormat option.
	 * This option applies to queries that return an RDF XML serialization string.
	 * @param format the RDFFormat to set
	 */
	public void setRDFFormat(String format) {
		freshState();
		RDFFormat = format;
	}
	
	/**
	 * Query the resultsFormat option.
	 * This option applies to queries that return a value set.
	 * @return the resultsFormat
	 */
	public String getResultsFormat() { return resultsFormat; }
	
	/**
	 * Set the resultsFormat option.
	 * This option applies to queries that return a value set.
	 * @param resultsFormat the resultsFormat to set
	 */
	public void setResultsFormat(String resultsFormat) {
		freshState();
		this.resultsFormat = resultsFormat;
	}
	/**
	 * Query the vars option.
	 * This option determines the content of the results array.
	 * @return the vars. A null value specifies the default result set which consists
	 * of the variables listed in the query, in the order listed in the query.
	 */
	public String getVars() { return vars; }
	
	/**
	 * Set the vars option.
	 * This option determines the content of the results array.
	 * @param vars null or a string containing variable names separated by spaces
	 */
	public void setVars(String vars) {
		freshState();
		this.vars = vars;
	}
	
	
	
	/**
	 * Query the extended option.
	 * When true, extended SPARQL verbs are available.
	 * See the description of <code> *use-extended-sparql-verbs-p*</code> in the server 
	 * documentation for more details.
	 * @return true or false, null if unspecified (ie server default)
	 */
	public Boolean getExtended() { return super.getExtended(); }
	
	/**
	 * Set the extended option.
	 * When true, extended SPARQL verbs are available.
	 * @param extended the extended to set
	 */
	public void setExtended(boolean extended) {
		freshState();
		queryAttributes.setAttribute(EXT, new Boolean(extended));
	}
	
	/**
	 * Query the memoize option.
	 * When true, query results are memoized for potential time savings.
	 * See the description of <code>*build-filter-memoizes-p*</code> in the server 
	 * documentation for more details.
	 * @return true or false, null if unspecified (ie server default)
	 */
	public Boolean getMemoized() { return super.getMemoized(); }
	
	/**
	 * Set the memoize option.
	 * When true, query results are memoized for potential time savings.
	 * @param memoized the memoized to set
	 */
	public void setMemoized(boolean memoized) {
		freshState();
		queryAttributes.setAttribute(MEM, new Boolean(memoized));
	}
	
	/**
	 * Set the name of a memo table.
	 * The scope of the name is one AllegroGraphConnection instance.
	 * @param name
	 */
	public void setMemoTable ( String name ) {
		freshState();
		queryAttributes.setAttribute(MEMO, name);
	}
	
	/**
	 * Query the name of the memo table.
	 * @return the name of the memo table, or null if none is set.
	 */
	public String getMemoTable () {
		return (String) queryAttributes.getAttribute(MEMO);
	}
	
	/**
	 * Set the name of a load-function option.
	 * @param name
	 */
	public void setLoadFunction ( String name ) {
		freshState();
		queryAttributes.setAttribute(LOADFN, name);
	}
	
	/**
	 * Query the name of the load-function.
	 * @return the name of the load-function, or null if none is set.
	 */
	public String getLoadFunction () {
		return (String) queryAttributes.getAttribute(LOADFN);
	}
	
	/**
	 * Set the default-prefixes option.
	 * @param prefixes an array of alternating prefix and URI strings.
	 */
	public void setDefaultPrefixes ( String[] prefixes ) {
		freshState();
		queryAttributes.setAttribute(PREFIXES, prefixes);
	}
	
	/**
	 * Set the default-prefixes option.
	 * @param prefixes a NamespaceRegistry instance that defines the desired prefixes.
	 */
	public void setDefaultPrefixes ( NamespaceRegistry prefixes ) {
		freshState();
		queryAttributes.setAttribute(PREFIXES, prefixes.toArray());
	}
	
	/**
	 * Query the default-prefixes option.
	 * A user supplied value overrides a PREFIX clause in the query string.
	 * See the description of <code>default-prefixes</code> in the server 
	 * documentation for more details.
	 * @return an array of strings. The content of the array is a list 
	 *    of alternating prefix and URI values.
	 */
	public String[] getDefaultPrefixes () {
		return (String[]) queryAttributes.getAttribute(PREFIXES);
	}
	
	/**
	 * Set the default-base option for this SPARQL query.
	 * @param base a string containing the default base URI.
	 */
	public void setDefaultBase ( String base ) {
		freshState();
		queryAttributes.setAttribute(BASE, base);
	}
	
	/**
	 * Query the default base value of the query.
	 * A user supplied value overrides a BASE clause in the query string.
	 * See the description of <code>default-base</code> in the server 
	 * documentation for more details.
	 * @return null if the sever default applies, or the string value set by the user.
	 */
	public String getDefaultBase () {
		return (String) queryAttributes.getAttribute(BASE);
	}
	
	/**
	 * Set the from-named option,
	 * @param uriLabels an array of URI strings
	 */
	public void setFromNamed ( String[] uriLabels ) {
		freshState();
		queryAttributes.setAttribute(NAMED, uriLabels);
	}
	
	/**
	 * Query the from-named option.
	 * @return an array of strings containing the from-named URIs.
	 */
	public String[] getFromNamed () {
		return (String[]) queryAttributes.getAttribute(NAMED);
	}
	
	/**
	 * Set the from option,
	 * @param uriLabels an array of URI strings
	 */
	public void setFrom ( String[] uriLabels ) {
		freshState();
		queryAttributes.setAttribute(FROM, uriLabels);
	}
	
	/**
	 * Query the from option.
	 * @return an array of strings containing the from URIs.
	 */
	public String[] getFrom () {
		return (String[]) queryAttributes.getAttribute(FROM);
	}
	
	/**
	 * Set the default-data-set-behavior option to "all".
	 *
	 */
	public void setDefaultDatasetBehaviorAll () {
		freshState();
		queryAttributes.setAttribute(BEHAVIOR, "all");
	}
	
	/**
	 * Set the default-data-set-behavior option to "default".
	 *
	 */
	public void setDefaultDatasetBehaviorDefault () {
		freshState();
		queryAttributes.setAttribute(BEHAVIOR, "default");
	}
	
	/**
	 * Query the default dataset behavior for this SPARQL query.
	 * This option  controls how the query engine builds the 
	 * dataset environment if FROM or FROM NAMED are not provided.
	 * See the description of <code>*sparql-default-graph-behavior*</code> in the server 
	 * documentation for more details.
	 * @return null if the server default applies, or the string "all" or "default".
	 */
	public String getDefaultDatasetBehavior () {
		return (String) queryAttributes.getAttribute(BEHAVIOR);
	}
	
	/**
	 * Query the engine that was or will be used for the SPARQL query.
	 * See the description of <code>*default-sparql-query-engine*</code> in the server 
	 * documentation for more details.
	 * @return The setting; a null value indicates that the default server behavior is
	 * in effect.
	 */
	public ENGINE getEngine () { 		return engine; }
	
	/**
	 * Set the engine that will be used for the SPARQL query.
	 * @param e
	 */
	public void setEngine ( ENGINE e ) {
		freshState();
		engine = e;
		queryAttributes.setAttribute(ENGINE.attrName, e.value());
	}
	
	/**
	 * Query the engine that was or will be used for the SPARQL query.
	 * A null value specifies the default engine in the server.
	 * See the description of <code>*sparql-query-planner*</code> in the server 
	 * documentation for more details.
	 * 
	 * @return 
	 */
	public PLANNER getPlanner () { return planner; }
	
	/**
	 * Set the engine that will be used for the SPARQL query.
	 * @param e
	 */
	public void setPlanner ( PLANNER e ) {
		freshState();
		planner = e;
		queryAttributes.setAttribute(PLANNER.attrName, e.value());
	}
	
	
	/**
	 * Query the with-variables option.
	 * @return a copy of the array that was used to set this option.
	 */
	public Object[] getWithVariables() {
		return  saveWithVars;
	}
	
	/**
	 * Set the with-variables option for the SPARQL query.
	 * @param ag the AllegroGraph instance where the variable values are resolved.
	 * @param withVariables an array of alternating variable names and values.
	 *      The variable names must be strings. 
	 *      The variable values may be any valid triple part specifier as in addStatement.
	 */
	public void setWithVariables( AllegroGraph ag, Object[] withVariables ) {
		String[] withVars = saveWithVariables(ag, withVariables);
		freshState();
		queryAttributes.setAttribute(WITHV, withVars);
	}
	protected String convertWithVariable( AGBase ag, Object var ) {
		return ((AllegroGraph) ag).refToString(var);
	}
	
	
	/**
	 * Query the results of a SPARQL query that returns sets of bindings.
	 * This result is available only after a call to one of the select()
	 * methods.
	 * @return the resultSet
	 * @throws IllegalStateException if the result is not available.
	 */
	public ValueSetIterator getResultSets() {
		if ( null!=resultSets ) return resultSets;
		throw new IllegalStateException("ResultSets is not set.");
	}
	
	
	/**
	 * Query the result of a SPARQL query that returns a collection of triples.
	 * This result is available only after a call to one of the describe()
	 * or construct()
	 * methods.
	 * @return the resultCursor
	 */
	public TriplesIterator getResultCursor() {
		if ( haveResultCursor ) return resultCursor;
		throw new IllegalStateException("ResultCursor is not set.");
	}
	
	/**
	 * Query the result of a SPARQL query that returns a string result.
	 * This result is available only after a call to one of the run()
	 * methods.
	 * @return the resultString
	 */
	public String getResultString() {
		if ( null!=resultString ) return resultString;
		throw new IllegalStateException("ResultString is not set.");
	}

	private void validate ( AllegroGraph ag ) {
		freshState();
		if ( ag!=null ) this.ag = ag;
		if ( query==null )
			throw new IllegalStateException("Cannot run without a query.");
		if ( this.ag==null )
			throw new IllegalStateException("Cannot run without a triple store.");
	}
	
	/**
	 * Specify the triple store against which this SPARQL query will run.
	 * Setting the store clears out any previous results.
	 * @param ag
	 */
	public void setTripleStore ( AllegroGraph ag ) {
		freshState();
		super.setTripleStore(ag);
	}
	
	/**
	 * Query the triple store against which this SPARQL query has or will run.
	 * @return the AllegroGraoh instance or null.
	 */
	public AllegroGraph getTripleStore () { return (AllegroGraph) super.getTripleStore(); }
	
	/**
	 * Run a SPARQL query that retrieves a set of variable bindings.
	 * The query must be a SPARQL SELECT query.
	 * @return an array of result sets.  Each result set is an array of values.
	 * @throws AllegroGraphException
	 */
	public ValueSetIterator select () throws AllegroGraphException {
		validate(null);
		Object[] v = ag.verifyEnabled().twinqlSelect(ag, query, vars, limit, offset, ag.selectLimit,
				includeInferred, queryAttributes.getList());
    	resultSets =  new ValueSetIteratorImpl(ag, v);
    	resultVars = resultSets.getNames();
		return resultSets;
	}
	
	
	/**
	 * Run a SPARQL query that retrieves a set of variable bindings.
	 * @param ag the AllogroGraph instance against which the query will run. 
	 * @return an array of result sets.  Each result set is an array of values.
	 * @throws AllegroGraphException
	 */
	public ValueSetIterator select ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return select();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of variable bindings.
	 * @param query the complete and well-formed SPARQL SELECT query string.
	 * @return an array of result sets.  Each result set is an array of values.
	 * @throws AllegroGraphException
	 */
	public ValueSetIterator select ( String query ) throws AllegroGraphException {
		this.query = query;
		return select();
	}
	
	public ValueSetIterator select ( AllegroGraph ag, String query ) throws AllegroGraphException {
		this.ag = ag;
		this.query = query;
		return select();
	}
	
	
	/**
	 * Run a SPARQL query that retrieves a set (or row) of variable bindings.
	 * The query must be a SPARQL SELECT query.
	 * @return the number of result sets found.  The actual result sets are discarded.
	 * <p>
	 * The result may also be obtained subsequently by calling {@link #getResultCount()}.
	 * @throws AllegroGraphException
	 */
	public long count () throws AllegroGraphException {
		validate(null);
		resultCount = ag.twinqlCount(includeInferred, query, vars, limit, offset,
				queryAttributes.getList());
		return resultCount;
	}
	
	/**
	 * Run a SPARQL query that retrieves a set (or row) of variable bindings.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @return the number of result sets found.
	 * <p>
	 * @see #count().
	 * @throws AllegroGraphException
	 */
	public long count ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return count();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set (or row) of variable bindings.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @param query a string containing a well-formed SPARQL SELECT query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return the number of result sets found.
	 * <p>
	 * @see #count().
	 * @throws AllegroGraphException
	 */
	public long count ( AllegroGraph ag, String query ) throws AllegroGraphException {
		this.ag = ag;
		this.query = query;
		return count();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set (or row) of variable bindings.
	 * @param query a string containing a well-formed SPARQL SELECT query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return the number of result sets found.
	 * <p>
	 * @see #count().
	 * @throws AllegroGraphException
	 */
	public long count ( String query ) throws AllegroGraphException {
		this.query = query;
		return count();
	}
	
	
	/**
	 * Run a SPARQL query that returns a boolean result.
	 * The query must be a SPARQL ASK query.
	 * @return true if the query succeeded.
	 * <p>
	 * The result may also be obtained subsequently by calling {@link #getBooleanResult()}.
	 * @throws AllegroGraphException
	 */
	public boolean ask () throws AllegroGraphException {
		validate(null);
		booleanResult = ag.verifyEnabled().twinqlAsk(ag, query, includeInferred,
				queryAttributes.getList());
		haveBooleanResult = true;
		return booleanResult;
	}
	
	/**
	 * Run a SPARQL query that returns a boolean result.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @return true if the query succeeded.
	 * 
	 * @see #ask().
	 * 
	 * @throws AllegroGraphException
	 */
	public boolean ask ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return ask();
	}
	
	/**
	 * Run a SPARQL query that returns a boolean result.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @param query a string containing a well-formed SPARQL ASK query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return true if the query succeeded.
	 * 
	 * @see #ask().
	 * 
	 * @throws AllegroGraphException
	 */
	public boolean ask ( AllegroGraph ag, String query ) throws AllegroGraphException {
		this.ag = ag;
		this.query = query;
		return ask();
	}
	
	/**
	 * Run a SPARQL query that returns a boolean result.
	 * @param query a string containing a well-formed SPARQL ASK query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return true if the query succeeded.
	 * 
	 * @see #ask().
	 * 
	 * @throws AllegroGraphException
	 */
	public boolean ask ( String query ) throws AllegroGraphException {
		this.query = query;
		return ask();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * The query must be a SPARQL DESCRIBE query.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * The result may also be obtained subsequently by calling {@link #getResultCursor()}.
	 * @throws AllegroGraphException
	 */
	public TriplesIterator describe () throws AllegroGraphException {
		validate(null);
		resultCursor = ag.verifyEnabled().twinqlDescribe((AllegroGraph) ag, query, limit, offset, ag.selectLimit, 
				includeInferred, queryAttributes.getList());
		haveResultCursor = true;
		return resultCursor;
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * @see #describe().
	 * @throws AllegroGraphException
	 */
	public TriplesIterator describe ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return describe();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @param query a string containing a well-formed SPARQL DESCRIBE query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * @see #describe().
	 * @throws AllegroGraphException
	 */
	public TriplesIterator describe ( AllegroGraph ag, String query ) throws AllegroGraphException {
		this.ag = ag;
		this.query = query;
		return describe();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * @param query a string containing a well-formed SPARQL DESCRIBE query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * @see #describe().
	 * @throws AllegroGraphException
	 */
	public TriplesIterator describe ( String query ) throws AllegroGraphException {
		this.query = query;
		return describe();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * The query must be a SPARQL CONSTRUCT query.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * The result may also be obtained subsequently by calling {@link #getResultCursor()}.
	 * @throws AllegroGraphException
	 */
	public TriplesIterator construct () throws AllegroGraphException {
		validate(null);
		resultCursor = ag.verifyEnabled().twinqlConstruct((AllegroGraph) ag, query, limit, offset, ag.selectLimit, 
				includeInferred, queryAttributes.getList());
		haveResultCursor = true;
		return resultCursor;
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * @see #construct().
	 * @throws AllegroGraphException
	 */
	public TriplesIterator construct ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return construct();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * @param ag the AllegroGraph instance where the query will run.
	 *     This instance is remembered as if set with {@link #setTripleStore(AllegroGraph)}.
	 * @param query a string containing a well-formed SPARQL CONSTRUCT query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * @see #construct().
	 * @throws AllegroGraphException
	 */
	public TriplesIterator construct ( AllegroGraph ag, String query ) throws AllegroGraphException {
		this.ag = ag;
		this.query = query;
		return construct();
	}
	
	/**
	 * Run a SPARQL query that retrieves a set of statements.
	 * @param query a string containing a well-formed SPARQL CONSTRUCT query.
	 *     The query is remembered as if set with {@link #setQuery(String)}.
	 * @return a Cursor instance that will iterate over the resulting statements.
	 * <p>
	 * @see #construct().
	 * @throws AllegroGraphException
	 */
	public TriplesIterator construct ( String query ) throws AllegroGraphException {
		this.query = query;
		return construct();
	}
	
	/**
	 * Run a SPARQL query that returns a serialized string result.
	 * @return the string containing the serialized result.
	 * @throws AllegroGraphException if a problem was encountered during the search.
	 * @throws IllegalArgumentException if this instance is not properly initialized.
	 */
	public String run () throws AllegroGraphException {
		validate(null);
		if ( (resultsFormat==null) && RDFFormat==null ) 
			throw new IllegalStateException("Cannot run without a result format.");
		resultString = ag.verifyEnabled().twinqlQuery(
				ag,
				query, 
				(resultsFormat==null)?RDFFormat:resultsFormat,
						limit, offset, includeInferred, queryAttributes.getList() );
		return resultString;
	}
	public String run ( AllegroGraph ag ) throws AllegroGraphException {
		this.ag = ag;
		return run();
	}
	public String run ( AllegroGraph ag, String query ) throws AllegroGraphException {
		this.ag = ag;
		this.query = query;
		return run();
	}
	public String run ( String query ) throws AllegroGraphException {
		this.query = query;
		return run();
	}
	
	
	
}
