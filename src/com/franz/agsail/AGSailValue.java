package com.franz.agsail;

import org.openrdf.model.Value;

import com.franz.agbase.UPI;

/**
 * This is the superclass of all Value instances.
 */
public interface AGSailValue extends Value, AGSailValueObject {

	/**
	 * Return the unique identifier of the object.
	 * 
	 * @return A UPI instance. If the value is null, then the object has not
	 *         been stored in the triple store and is simply a place holder for a
	 *         label.
	 */
	public UPI queryAGId();

}