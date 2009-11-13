package com.franz.agbase;


import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.impl.ValueNodeImpl;

/**
 * This interface includes all the types that may appear as a component of a Triple.
 */
public interface ValueNode extends ValueObject {

	/**
	 * Return the unique identifier of the object.
	 * 
	 * @return A UPI instance. If the value is null, then the object has not
	 *         been stored in the triple store and is simply a place holder for a
	 *         label.
	 */
	public UPI queryAGId();

	public TriplesIterator getObjectStatements() throws AllegroGraphException;

	public int compareTo(ValueNodeImpl to);

}