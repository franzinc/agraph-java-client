package com.franz.agsail;

import org.openrdf.model.URI;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;


/**
 * This interface defines an instance of a literal node in AllegroGraph.
 * <p>
 * The AllegroGraph object defines two slots, id and label.  Both slots are copied 
 * to the Java instance.
 * <p>
 * The label member may be a lazy value in the Java instance.  If queryLabel returns
 * null, getLabel() must make a round-trip to the triple store for the actual value.
 * <p>
 * There is no public constructor.  Literal instances are created by calls
 * to AllegroGraph methods.
 */
public interface AGSailLiteral extends AGSailValue, org.openrdf.model.Literal {

	/**
	 * Return the string associated with the Literal instance.
	 * @return A string or null.
	 * <p>
	 * If the returned value is null, the string value is not in the local
	 * Java cache, and must be retrieved from the AllegroGraph server with
	 * a call to getLabel().
	 */
	public String queryLabel();

	/**
	 * Return the string associated with the Literal instance.
	 * @return A string.
	 * If the value is not in the Java cache, retrieve it from the triple store.
	 */
	public String getLabel();

	/**
	 * Retrieve the string label of the datatype of the Literal.
	 * @return null if the information is not in the local cache or
	 *    if the Literal does not have a datatype label.
	 * <p>
	 * If the returned value is null, getType() or getDatatype() must be called
	 * to get the actual value.
	 */
	public String queryType();

	/**
	 * Retrieve the datatype as a URI instance.
	 * @return 
	 * If the string label is not in the local Java cache, this method
	 * requires a round-trip to the AllegroGraph server.
	 */
	public URI getDatatype();

	/**
	 * Retrieve the string label for the datatype of the Literal.
	 * @return a string, or null if the Literal does not have a datatype field.
	 * This operation may require a round-trip to the AllegroGraph triple store.
	 */
	public String getType();

	/**
	 * Retrieve the language field of the Literal.
	 * @return null if the value is not in the local cache or
	 *     if the Literal does not have a language label.
	 * <p>
	 * If the returned value is null, getLanguage() must be called
	 * to get the actual value.
	 */
	public String queryLanguage();

	/**
	 * Retrieve the language qualifier of the Literal.
	 * 
	 * @return null if the Literal does not have a language qualifier.
	 */
	public String getLanguage();

	/**
	 * This method overrides the generic toString method.
	 * This method generates a more readable output string of the 
	 * form "&lt;Literal <i>id</i>: <i>label</i>[langortype]&gt;".
	 */
	public String toString();

	/**
	 * Implement equality for Literal instances.
	 * <p>
	 * Two Literal instances are equal if both are registered in the
	 * AllegroGraph triple store and  they have identical
	 * AllegroGraph part id numbers.
	 * <p>
	 * Otherwise, the string representations are compared.
	 */
	public boolean equals(Object other);

	/**
	 * Compute the hashcode of a Literal instance.
	 * <p>
	 * The hashcode of a Literal instance is the hashcode
	 * of its string representation.
	 */
	public int hashCode();

	/**
	 * Add this literal to the AllegroGraph triple store.
	 * If the literal already is in the triple store, do nothing.
	 * <p>
	 * A Literal instance is in the triple store if queryAGId() returns
	 * a non-null value.
	 * @throws AllegroGraphException 
	 *
	 */
	public void add() throws AllegroGraphException;

	/**
	 * Return the unique internal AllegroGraph identifier for this Literal.
	 * If the indentifier is no available in the local Java cache, go to
	 * the server to add the Literal and obtain the identifier. 
	 * @return The UPI that identifies this Literal.
	 * @throws AllegroGraphException
	 */
	public UPI getAGId() throws AllegroGraphException;

}