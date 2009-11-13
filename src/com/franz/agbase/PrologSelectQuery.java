package com.franz.agbase;

import java.util.ArrayList;

import com.franz.agbase.impl.NamedAttributeList;
import com.franz.agbase.impl.ValueSetIteratorImpl;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGBase;
import com.franz.agbase.util.QueryBase;

/**
 * This class collects the parameters and results of a query posed in
 * a restricted version of Allegro Prolog.
 * 
 * A query is composed of two main components, the variables for which bindings 
 * are requested, and the body of the query which consists of Allegro Prolog clauses.
 * The variables are used to form a flat list template (see the documentation for select
 * and friends in the AllegroGraph Lisp documentation). 
 *  
 * @author mm
 *
 */
public class PrologSelectQuery extends QueryBase {
	
	/**
	 * Create a fresh Proloq query holder.
	 * Before it can be used, the variables, query and triple store must be set.
	 */
	public PrologSelectQuery () {
		super();
		queryAttributes = new NamedAttributeList(queryOptions);
	}

	private boolean distinct = false;
	private ArrayList<String> invars = null;
	private ArrayList<Object> invals = null;
	
	private String listToString ( ArrayList<String> a ) {
		if ( a==null ) return null; 
		String r = "";
		for (String s : a) {
			if ( 0!=s.indexOf("?") ) s = "?" + s;
			r = r + " " + s;
		}
		return r;
	}
	
	private ValueSetIteratorImpl result = null;
	
	private void freshState ( boolean keepPlan ) {
		result = null;  
		if ( !keepPlan ) planToken = null;
	}
	
	/**
	 * Add a pre-set binding to the query.
	 * This binding turns a variable in the query into a constant when the query is
	 * presented to the triple store.
	 * @param var The name of a variable (with or without the leading ?).
	 *   A null value removes all the variable bindings.
	 * @param val The value for this variable may be a ValueNode instance, a UPI,
	 *    or a string in Ntriples notation.
	 *    A null value removes the variable from the list.
	 */
	public void bind ( String var, Object val ) {
		if ( var==null ) {
			if ( val!=null ) throw new IllegalArgumentException
			   ("When var name is null, value must also be null.");
			invars = null;
			invals = null;
			return;
		}
		
		
		if  ( invars==null ) {
			if ( val==null ) return;
			invars = new ArrayList<String>();
			invals = new ArrayList<Object>();
		}
		int i = invars.indexOf(var);
		if ( i<0 )
		{
			if ( val==null ) return;
			freshState(false);
			invars.add(var); invals.add(val);
		}
		else
		{
			freshState(true);   // Keep the plan if list of vars is not chamged.
			if ( val==null )
			{
				invars.remove(i);
				invals.remove(i);
				if ( 0==invars.size() )
				{
					invars = null; invals = null; return;
				}
			}
			invars.set(i, var); invals.set(i, val); 
		}
	}
	
	@Override
	public boolean isIncludeInferred() { return super.isIncludeInferred(); }
	
	@Override
	public void setIncludeInferred(boolean includeInferred) {
		freshState(false);
		super.setIncludeInferred(includeInferred); 
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
		freshState(false);
		queryAttributes.setAttribute(HASVALUE, new Boolean(v));
	}
	
	

	@Override
	public AGBase getTripleStore() { return super.getTripleStore(); }

	
	@Override
	public String getQuery() { return super.getQuery(); }
	
	@Override
	public void setQuery(String newQuery) { 
		freshState(false);
		super.setQuery(newQuery);
		}
	
	/**
	 * Set the result template and clauses of the query with one call.
	 * @param vars the variable names to appear in the result template
	 * @param query the clauses in the body of the query
	 */
	public void setQuery ( String[] vars, String query ) {
		setVariables(vars);
		setQuery(query);
	}

	@Override
	public void setTripleStore(AllegroGraph ag) { 
		freshState(false);
		super.setTripleStore(ag);
		}

	/**
	 * Query the Distinct option on the query.
	 * @return when true, the query will return only the unique sets of results.
	 *    When false, the results may include some duplicates.
	 */
	public boolean isDistinct () {
		return distinct;
	}
	
	/**
	 * Set the Distinct option on the query.
	 * @param onoff when true, the query will return only the unique sets of results.
	 *    When false, the results may include some duplicates.
	 */
	public void setDistinct ( boolean onoff ) {
		freshState(false);
		distinct = onoff;
	}
	
	public long getLimit () {
		Object v = queryAttributes.getAttribute(LIMIT);
		if ( v==null ) return 0;
		return (Long)v;
	}
	
	public void setLimit ( long limit ) {
		queryAttributes.setAttribute(LIMIT, (limit<1)?null:limit);
	}
	
	private String template = "()";
	private String[] vars = null;
	private Object planToken;
	
	/**
	 * Query the names of the variables returned in each result set.
	 * @return an array of variable names (as previously supplied)
	 */
	public String [] getVariables () { return vars.clone(); }
	
	/**
	 * Set the template of values desired in each result set.
	 * @param vars an array of variable names.
	 */
	public void setVariables ( String[] vars ) {
		freshState(false);
		this.vars = vars.clone();  
		template = "(";
		for (int i = 0; i < vars.length; i++) {
			int p = vars[i].indexOf("?");
			switch ( p ) {
			case -1: template = template + " ?" + vars[i]; break;
			case 0: template = template + " " + vars[i]; break;
			default: throw new IllegalArgumentException("Ill-formed variable " + vars[i]);
			}
		}
		template = template + ")";
	}
	
	/**
	 * Set the template of values desired in each result set.
	 * @param vars A string containing variable names separated by spaces.
	 */
	public void setVariables ( String vars ) {
		  // regex was " *" and split on every char [bug18500]
		setVariables(vars.split("\\s+", 0));   
	}
	
	
	/**
	 * Run the query and collect the results.
	 * @return a ValueSetIterator instance that will iterate through the results.
	 * @throws AllegroGraphException
	 */
	public ValueSetIterator run () throws AllegroGraphException {
		runBody(null);
		return result;
	}
	
	/**
	 * Retrieve the result of the last query.
	 * @return Return the ValueSetIterator instance or null if the query result is not
	 * available.
	 */
	public ValueSetIterator getResult () {
		return result;
	}
	
	/**
	 * Run the query in the specified triple store.
	 * @param ag the AllegroGraph instance where the query should run.
	 *    This instance is remembered in the PrologSelectQuery instance.
	 * @return see {@link #run()}
	 * @throws AllegroGraphException
	 */
	public ValueSetIterator run ( AllegroGraph ag ) throws AllegroGraphException {
		validate(ag);
		return run();
	}
	
	/**
	 * Run the specified query.
	 * @param vars the variable names to appear in the result template
	 * @param newQuery the clauses in the body of the query
	 * @return see {@link #run()}
	 * @throws AllegroGraphException
	 */
	public ValueSetIterator run ( String[] vars, String newQuery ) throws AllegroGraphException {
		setVariables(vars);
		super.setQuery(newQuery);
		return run();
	}
	
	public ValueSetIterator run ( AllegroGraph ag, String[] vars, String newQuery ) throws AllegroGraphException {
		setVariables(vars);
		super.setQuery(newQuery);
		validate(ag);
		return run();
	}
	
	
	
//;; misc.
//    (defplan-option :verbose nil)
//    (defplan-option :plan-only nil)
//    (defplan-option :display-plan nil)
//    * (defplan-option :name nil)
//
//    ;; modify planning process
//    * (defplan-option :use-planner t)
//    (defplan-option :reorder t)
//    (defplan-option :use-maps t)
//    (defplan-option :use-transforms t)
//    (defplan-option :remove-redundant-type-filters t)
//
//    ;; modify output
//    * (defplan-option :limit nil)
//    * (defplan-option :count-only nil)
//    (defplan-option :distinct nil)
//    (defplan-option :translate-upis nil)
	private NamedAttributeList queryAttributes;
	
	// These values are passed in a trailing pseudo-keyword argument list
	private static final String REORDER = "reorder";
	private static final String USE_MAPS = "use-maps";
	private static final String USE_TRANSFORMS = "use-transforms";
	private static final String REMOVE_RTF = "remove-redundant-type-filters";
	//private static final String VERBOSE = "verbose";
	//private static final String USE_PLANNER = "use-planner";
	private static final String LIMIT = "limit";
	private static final String COUNT_ONLY = "count-only";
	//private static final String DISTICT = "distinct";
	private static final String PLAN_TOKEN = "plan-token";
	private static final String PLAN_ACTION = "plan-action";
	private static final String PLAN_ACTION_SAVE = "save";
	private static final String PLAN_ACTION_PLAN = "plan";
	private static final String PLAN_ACTION_SHOW = "show";
	private static final String PLAN_ACTION_RUN = "run";
	private static final String TEMPLATE = "template";
	private static final String HASVALUE = "has-value";
	
	protected static final Object[] queryOptions = new Object[] {
		REORDER, Boolean.class,      
		USE_MAPS, Boolean.class,    
		USE_TRANSFORMS, Boolean.class,
		REMOVE_RTF, Boolean.class,
		PLAN_TOKEN, String.class,
		PLAN_ACTION, String.class,
		TEMPLATE, String.class,
		COUNT_ONLY, Boolean.class,
		LIMIT, Long.class,
		HASVALUE, Boolean.class
	};
	
	private void setRunOptions ( String action ) {
		queryAttributes.setAttribute(PLAN_TOKEN, null);
		queryAttributes.setAttribute(PLAN_ACTION, action);
	}

	/**
	 * Query the state of the <code>:reorder</code> 
	 * planner option on the query.
	 * @return true or false.
	 * Return null if the option has not been set and the server default cannot
	 * be determined.
	 * @throws AllegroGraphException 
	 */
	public Boolean isReorder () throws AllegroGraphException {
		return isPlannerOption(REORDER);
	}
	
	/**
	 * Set the state of the <code>:reorder</code> 
	 * planner option on the query.
	 * @param useMaps True or false. A null value reverts to the built-in default in the server.
	 */
	public void setReorder ( Boolean reorder ) {
		freshState(false);
		queryAttributes.setAttribute(REORDER, reorder);
	}
	
	/**
	 * Query the state of the <code>:use-maps</code> 
	 * planner option on the query.
	 * @return true or false.
	 * Return null if the option has not been set and the server default cannot
	 * be determined.
	 * @throws AllegroGraphException 
	 */
	public Boolean isUseMaps () throws AllegroGraphException {
		return isPlannerOption(USE_MAPS);
	}
	
	/**
	 * Set the state of the <code>:use-maps</code> 
	 * planner option on the query.
	 * @param useMaps True or false. A null value reverts to the built-in default in the server.
	 */
	public void setUseMaps ( Boolean useMaps ) {
		freshState(false);
		queryAttributes.setAttribute(USE_MAPS, useMaps);
	}
	
	/**
	 * Query the state of the <code>:use-transforms</code> 
	 * planner option on the query.
	 * Return null if the option has not been set and the server default cannot
	 * be determined.
	 * @return true or false.
	 * @throws AllegroGraphException 
	 */
	public Boolean isUseTransforms () throws AllegroGraphException {
		return isPlannerOption(USE_TRANSFORMS);
	}
	
	/**
	 * Set the state of the <code>:use-transforms</code> 
	 * planner option on the query.
	 * @param useMaps True or false. A null value reverts to the built-in default in the server.
	 */
	public void setUseTransforms ( Boolean useTransforms ) {
		freshState(false);
		queryAttributes.setAttribute(USE_TRANSFORMS, useTransforms);
	}
	
	/**
	 * Query the state of the <code>:remove-redundant-type-filters</code> 
	 * planner option on the query.
	 * @return true or false.  
	 * Return null if the option has not been set and the server default cannot
	 * be determined.
	 * @throws AllegroGraphException 
	 */
	public Boolean isRemoveRTF () throws AllegroGraphException {
		return isPlannerOption(REMOVE_RTF);
	}
	
	Boolean isPlannerOption ( String option ) throws AllegroGraphException {
		Object v = queryAttributes.getAttribute(option);
		if ( v!=null ) return (Boolean) v;
		if ( ag==null ) return null;
		Object[] d = ((AllegroGraph)ag).getConnection().selectPlannerOptions;
		if ( d==null )
		{
			d = ((AllegroGraph)ag).verifyEnabled().clientOption(ag, "select-planner-options");
			if ( d==null ) return null;
			((AllegroGraph)ag).getConnection().selectPlannerOptions = d;
		}
		v = this;  // something that cannot be found in d
		for (int i = 0; i < d.length; i=i+2) {
			if ( v==this ) 
			{
				String opt = (String) d[i];
				if ( option.equalsIgnoreCase(opt) ) 
					v = d[i+1];
			}
		}
		if ( v==this )	return null;  //unknown default
		if ( v==null ) return false;
		return true;
	}
	
	/**
	 * Set the state of the <code>:remove-redundant-type-filters</code> 
	 * planner option on the query.
	 * @param useMaps True or false. A null value reverts to the built-in default in the server.
	 */
	public void setRemoveRTF ( Boolean removeRTF ) {
		freshState(false);
		queryAttributes.setAttribute(REMOVE_RTF, removeRTF);
	}
	
//	ValueSetIterator runAndSavePlan()
//	   Run the query and save the plan if possible.
	/**
	 * Run the query and save the plan for later re-use.
	 * @return true if the plan was made and saved.  False if the plan failed for some
	 * reason.
	 * <p>
	 * The results of the query are obtained by calling getResult().
	 */
	public boolean runAndSavePlan () throws AllegroGraphException {
		runBody(PLAN_ACTION_SAVE);
		return isSavedPlan();
	}
	
	private void runBody ( String action ) throws AllegroGraphException {
		if ( vars==null )
			throw new IllegalStateException("Cannot run without query variables.");
		validate(ag);
		boolean level12 = ((AllegroGraph)ag).ags.serverLevel(12);
		if ( level12 ) queryAttributes.setAttribute(TEMPLATE, template);
		setRunOptions(action);
		Object[] more = queryAttributes.getList();
		setRunOptions(null);
		queryAttributes.setAttribute(TEMPLATE, null);
		if ( (more!=null) && (0<more.length) && !level12 )
			throw new UnsupportedOperationException
			("This version of the AllegroGraph server does not support planner options.");
		Object[] rv = ag.verifyEnabled().selectValues(ag,
				level12?query:(template + query), 
				(invals==null)?null:invals.toArray(),
				listToString(invars),
				includeInferred, distinct, more);
		result =  new ValueSetIteratorImpl(ag, rv);
		result.savedExtra = vars;
		if ( (rv!=null) && 9<rv.length )
			planToken = rv[9];
	}
	
	/**
	 * Run the query and count the results.
	 * The actual results are never collected.
	 * @return the number of result sets found.
	 * @throws AllegroGraphException
	 */
	public long count () throws AllegroGraphException {
		validate(ag);
		boolean level16 = ((AllegroGraph)ag).ags.serverLevel(16);
		if ( !level16 ) 
			throw new UnsupportedOperationException
			("This version of the AllegroGraph server does not support the count option.");
		queryAttributes.setAttribute(TEMPLATE, template);
		setRunOptions(null);
		queryAttributes.setAttribute(COUNT_ONLY, true);
		Object[] more = queryAttributes.getList();
		setRunOptions(null);
		queryAttributes.setAttribute(TEMPLATE, null);
		queryAttributes.setAttribute(COUNT_ONLY, null);
		
		Object[] rv = ag.verifyEnabled().selectValues(ag,
				query, 
				(invals==null)?null:invals.toArray(),
				listToString(invars),
				includeInferred, distinct, more);
		return AGConnector.longValue(rv[0]);
	}
	
	/**
	 * Run the query in the specified triple store.
	 * The actual results are never collected.
	 * @param ag the AllegroGraph instance where the query should run.
	 *    This instance is remembered in the PrologSelectQuery instance.
	 * @return the number of result sets found.
	 * @throws AllegroGraphException
	 */
	public long count ( AllegroGraph ag ) throws AllegroGraphException {
		validate(ag);
		return count();
	}
	
	/**
	 * Run the specified query.
	 * The actual results are never collected.
	 * @param vars the variable names to appear in the result template.
	 *     Since the results are not actually collected, this argument may be null.
	 * @param newQuery the clauses in the body of the query
	 * @return the number of result sets found.
	 * @throws AllegroGraphException
	 */
	public long count ( String[] vars, String newQuery ) throws AllegroGraphException {
		freshState(false);
		if ( null!=vars ) setVariables(vars);
		super.setQuery(newQuery);
		return count();
	}
	
	public long count ( AllegroGraph ag, String[] vars, String newQuery ) throws AllegroGraphException {
		freshState(false);
		if ( null!=vars ) setVariables(vars);
		super.setQuery(newQuery);
		validate(ag);
		return count();
	}
	
	
	
//
//	boolean isSavedPlan()
//	   Return true if a plan was saved.
	/**
	 * Query the state of a saved plan for this query.
	 * @return true if there is a plan that can be used.
	 */
	public boolean isSavedPlan () { return null!=planToken; }
	

//	String showPlan()
//	   Return a string containing the Lisp representation of the plan.
//	   Return null if there is no plan to show.
	
	/**
	 * Return a string containing a description of the query plan.
	 *
	 */
	public String showPlan () throws AllegroGraphException {
		boolean level12 = ((AllegroGraph)ag).ags.serverLevel(12);
		if ( !level12 )
			throw new UnsupportedOperationException
			("This version of the AllegroGraph server does not support planner options.");
		if ( !isSavedPlan() )
			throw new IllegalStateException("Cannot run without a plan.");
		setRunOptions(PLAN_ACTION_SHOW);
		queryAttributes.setAttribute(PLAN_TOKEN, planToken);
		Object[] more = queryAttributes.getList();
		setRunOptions(null);
		queryAttributes.setAttribute(TEMPLATE, null);
		Object rv = ag.verifyEnabled().selectValues(ag,
				null, 
				(invals==null)?null:invals.toArray(),
				listToString(invars),
				includeInferred, distinct, more);
		if ( (rv!=null) && 9<((Object[])rv).length )
			return (String) ((Object[])rv)[9];
		return "";
	}
	
//
//	boolean preparePlan()
//	   Return true if a plan was created and saved.
	/**
	 * Prepare a plan for this query.
	 * @return true if the plan was prepared.  Return false if the plan 
	 * preparation failed.
	 */
	public boolean preparePlan () throws AllegroGraphException {
		boolean level12 = ((AllegroGraph)ag).ags.serverLevel(12);
		if ( !level12 )
			throw new UnsupportedOperationException
			("This version of the AllegroGraph server does not support planner options.");
		if ( vars==null )
			throw new IllegalStateException("Cannot run without query variables.");
		validate(ag);
		setRunOptions(PLAN_ACTION_PLAN);
		queryAttributes.setAttribute(TEMPLATE, template);
		Object[] more = queryAttributes.getList();
		setRunOptions(null);
		queryAttributes.setAttribute(TEMPLATE, null);
		Object rv = ag.verifyEnabled().selectValues(ag,
				level12?query:(template + query), 
				(invals==null)?null:invals.toArray(),
				listToString(invars),
				includeInferred, distinct, more);
		if ( (rv!=null) && 9<((Object[])rv).length )
			planToken = ((Object[])rv)[9];
		return isSavedPlan();
	}
	
//
//	ValueSetIterator runPlan()
//	   Run a saved plan.
//	   Throws IllegalStateException if no plan is available.
	
	/**
	 * Run a previously saved plan.
	 * @return the results
	 */
	public ValueSetIterator runPlan () throws AllegroGraphException {
		boolean level12 = ((AllegroGraph)ag).ags.serverLevel(12);
		if ( !level12 )
			throw new UnsupportedOperationException
			("This version of the AllegroGraph server does not support planner options.");
		if ( !isSavedPlan() )
			throw new IllegalStateException("Cannot run without a plan.");
		setRunOptions(PLAN_ACTION_RUN);
		queryAttributes.setAttribute(PLAN_TOKEN, planToken);
		Object[] more = queryAttributes.getList();
		setRunOptions(null);
		queryAttributes.setAttribute(TEMPLATE, null);
		Object rv = ag.verifyEnabled().selectValues(ag,
				null, 
				(invals==null)?null:invals.toArray(),
				listToString(invars),
				includeInferred, distinct, more);
		result =  new ValueSetIteratorImpl(ag, rv);
		result.savedExtra = vars;
		return result;
	}

	   
}
