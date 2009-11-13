package com.franz.agbase;

import java.util.Iterator;

/**
 * Iterate over sets of results returned by a query.
 * 
 * Each result set is an array of ValueObject instances.
 * Each position in the array normally contains the binding of a named query variable.
 * Some queries may return null values for unbound positions in a result set.
 * @author mm
 *
 */
public interface ValueSetIterator extends Iterator<ValueObject[]>  {
	
	/**
	 * Step to the next element in the iteration and return the i-th sub-element.
	 * @param i
	 * @return
	 */
	public ValueObject next ( int i );
	
	/**
	 * Get the number of results in each result set.
	 * @return -1 if the width cannot be determined.
	 */
	public int width ();
	
	/**
	 * Get the current element in the iteration.
	 * @return
	 */
	public ValueObject[] get();
	
	/**
	 * Get the i-th sub-element from the current element in the iteration.
	 * @param i
	 * @return
	 */
	public ValueObject get ( int i );
	
	/**
	 * Get the index of a given variable in the result array.
	 * @param var A variable name
	 * @return the index or -1 if the the position is unknown.
	 *    If getNames() returned an array of names, 
	 *    then a -1 indicates that the specified variable is not mentioned in the result set.
	 *    If getNames() returned null, then the variable names in the result set are not known. 
	 */
	public int getIndex ( String var );
	
	/**
	 * Get the binding of the named variable in the current result set.
	 * @param name A variable name
	 * @return the binding, or null if not there.
	 */
	public ValueObject get ( String name );
	
	/**
	 * Get a count or estimate on the number of result sets available from this iterator.
	 * @return a positive integer if the result is an exact count.
	 *     A negative integer if the result is a lower bound.
	 *     
	 *      If a result set is currently available (i.e. get() returns a non-null value)
	 *      then the count includes the current result.
	 */
	public long getCount ();
	
	/**
	 * Get the names of the results in each result set.
	 * @return an array of names when the names are available, null otherwise.
	 * 
	 * The values in each result set are named in the same order as the names
	 * appear in this array.
	 */
	public String[] getNames ();

}
