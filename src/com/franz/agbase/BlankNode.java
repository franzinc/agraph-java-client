package com.franz.agbase;


/**
 * This interface represents an instance of a blank (anonymous) node in AllegroGraph.
 * <p>
 * BlankNode instances are created by a call to 
 * the createBNode() methods in the AllegroGraph class.
 */
public interface BlankNode extends ResourceNode {

	/**
	 * Retrieve the identifying string of the BlankNode instance.
	 * <p>
	 * This identifying string exists only in the Java application.
	 * AllegroGraph does not implement persistent labels in the triple
	 * store.  The persistent label of the BlankNode instance in the
	 * triple store is determined by the AllegroGraph implementation.
	 */
	public String getID();

	/**
	 * This method overrides the generic toString method.
	 * This method generates an output string of 
	 * the form "&lt;_:blank<i>nnn</i>&gt;.
	 */
	public String toString();

	/**
	 * Implement equality for BlankNode instances.
	 * <p>
	 * Two BlankNode instances are equal if they have identical
	 * AllegroGraph part identifiers.
	 * <p>
	 * Otherwise, the string representations are compared.
	 */
	public boolean equals(Object other);

	/**
	 * Compute the hashcode of a BlankNode instance.
	 * <p>
	 * The hashcode of a BlankNode instance is the hashcode
	 * of its string representation.
	 */
	public int hashCode();

}