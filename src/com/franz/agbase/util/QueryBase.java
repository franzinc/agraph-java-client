package com.franz.agbase.util;

import com.franz.agbase.AllegroGraph;



public abstract class QueryBase  {
	
	
	protected String query = null;
	
	/**
	 * Get the query string.
	 * @return the query string or null if the instance is not initialized/
	 */
	public String getQuery () { return query; }
	
	/**
	 * Set the query string.
	 * Setting the query string clears out any previous results.
	 * @param newQuery a string containing a complete well-formed query.
	 */
	public void setQuery ( String newQuery ) {
		freshState();
		query = newQuery; 
		}
	
	protected AGBase ag = null;
	
	private void freshState() {
	}
	

	protected boolean includeInferred = false;
	
	/**
	 * Query the includeInferred option.
	 * @return the includeInferred
	 */
	public boolean isIncludeInferred() { return includeInferred; }
	
	/**
	 * Modify the includeInferred option.
	 * @param includeInferred the desired value.
	 */
	public void setIncludeInferred(boolean includeInferred) {
		freshState();
		this.includeInferred = includeInferred;
	}
	


	public void validate ( AGBase ag ) {
		freshState();
		if ( ag!=null ) this.ag = ag;
		if ( query==null )
			throw new IllegalStateException("Cannot run without a query.");
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
	 * Query the triple store against which this query has or will run.
	 * @return the AllegroGraoh instance or null.
	 */
	public AGBase getTripleStore () { return ag; }
	
	
	
}
