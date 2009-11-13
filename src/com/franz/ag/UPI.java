package com.franz.ag;

/**
 * This interface defines instances of Universal Part Identifiers.
 * UPIs are used to denote nodes and literals in AllegroGraph.
 * They are returned as values of queries or accessors, and may be used
 * as arguments to queries and accessors.  In general, UPI references 
 * are more efficient than string references.
 * 
 * <p>
 * UPIs are discussed in more detail in the AllegroGraph Introduction.
 * 
 * @author mm
 *
 */
public interface UPI {
	
	//FIXME
	//TODO - when ag package is discarded, this interface can be moved to
	// the agbase package after the sub-interface is deleted.
	// Any code outside these internal modules can already mention 
	// agbase.UPI and will work without change after the move.
	//
	// Any instance can be cast to agbase.UPI because the only constructor is in agbase.

	public boolean equals(Object x);

	public int hashCode();

	public String toString();

}