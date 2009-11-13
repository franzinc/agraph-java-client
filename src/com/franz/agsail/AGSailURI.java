package com.franz.agsail;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;


/**
 * This interface defines an instance of a labelled resource node in AllegroGraph.
 * <p>
 * The AllegroGraph object defines two slots, id and uri.  Both slots are copied 
 * to the Java instance.
 * <p>
 * The URI member may be a lazy value in the Java instance.  If queryURI returns
 * null, getURI() will need a round-trip to the triple store to fetch the
 * actual value.
 * <p>
 * Node instances are created by calls to AllegroGraph methods.
 */
public interface AGSailURI extends AGSailResource, org.openrdf.model.URI {

	/**
	 * Retrieve the AllegroGraph ID number of the Node.
	 * @return the ID number
	 * <p>
	 * If the Node is already registered in the AG triple store, return the locally
	 * cached value of the ID number.  Otherwise, register the Node in the
	 * AG triple store and return the new ID number.
	 * @throws AllegroGraphException
	 */
	public UPI getAGId() throws AllegroGraphException;

	/**
	 * Retrieve the URI string associated with the node instance.
	 * @return A string or null.
	 * If the returned value is null, the actual value must be obtained
	 * by calling getURI().
	 */
	public String queryURI();

	/**
	 * Retrieve the local name component of the URI string associated 
	 * with the node instance.
	 * @return A string.
	 * If the value is not in the Java cache, retrieve it from the triple store.
	 */
	public String getLocalName();

	/**
	 * Retrieve the namespace component of the URI string associated 
	 * with the node instance.
	 * @return A string or null if the URI does not have a namespace component.
	 * If the value is not in the Java cache, retrieve it from the triple store.
	 */
	public String getNamespace();

	/**
	 * Retrieve the URI string associated with the node instance.
	 * @return A string.
	 * If the value is not in the Java cache, retrieve it from the triple store.
	 * <p>
	 * Defined in interface org.openrdf.model.URI
	 */
	public String toString();

	/**
	 * Implement equality for Node instances.
	 * <p>
	 * Two Node instances are equal if both are registered in the
	 * AllegroGraph triple store and  they have identical
	 * AllegroGraph part id numbers.
	 * <p>
	 * Otherwise, the string representations are compared.
	 */
	public boolean equals(Object other);

	/**
	 * Compute the hashcode of a Node instance.
	 * <p>
	 * The hashcode of a Node instance is the hashcode
	 * of its string representation.
	 */
	public int hashCode();

	/**
	 * Add this node to the AllegroGraph triple store.
	 * If the node already is in the triple store, do nothing.
	 * <p>
	 * A Node instance is in the triple store if queryAGId() returns
	 * a non-null value.
	 * @throws AllegroGraphException 
	 *
	 */
	public void add() throws AllegroGraphException;

}