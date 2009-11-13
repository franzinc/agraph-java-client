package com.franz.agbase;


import java.util.Iterator;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.impl.TripleImpl;

/**
 * Iterate over a collection of Triple instances.
 * <p>
 * Many triple store search operations may generate an indeterminate number of
 * results. These operations return a TriplesIterator instance which may be used to
 * iterate through the available results.
 * <p>
 * Instances are created by search operations.
 */
public interface TriplesIterator extends Iterator<Triple> {

	/**
	 * Query  the look-ahead value for this cursor instance.
	 * @return an integer
	 * @see #setLookAhead(int)
	 */
	public int getLookAhead();

	/**
	 * Set the look-ahead value for this cursor instance.
	 * The look-ahead value determines how many results are cached in the
	 *  cursor instance.  The cached values may be retrieved without a
	 *  round-trip to the AllegroGraph server.
	 *  The setting takes effect when next() requires more data from the server.
	 *  The initial setting is determined by the value of setLookAhead in the 
	 *  AllegroGraph instance where the query was run.
	 *  <p>
	 *  A large value optimizes access in the Java application but may
	 *  incur a large delay when the cursor is created.
	 * @param lh An integer. Any value less than 1 specifies the defaultLookAhead
	 *  value.
	 */
	public void setLookAhead(int lh);

	/**
	 * Retrieve the id number of the current triple in the Cursor instance.
	 * 
	 * @return integer id or -1 if the cursor is not positioned on a triple.
	 */
	public long get_id();

	/**
	 * Retrieve the subject node UPI of the current triple in the Cursor
	 * instance.
	 * 
	 * @return UPI or null if the cursor is not positioned on a triple.
	 */
	public UPI getS();

	/**
	 * Retrieve the string label for the subject of the current triple in the
	 * Cursor instance.
	 * 
	 * @return null if the Cursor is not positioned at a triple, or if the
	 *         subject label is not in the Java cache.
	 */
	public String querySubject();

	/**
	 * Retrieve the string label for the object of the current triple in the
	 * Cursor instance.
	 * 
	 * @return null if the Cursor is not positioned at a triple, or if the
	 *         object label is not in the Java cache.
	 */
	public String queryObject();

	/**
	 * Retrieve the string label for the predicate of the current triple in the
	 * Cursor instance.
	 * 
	 * @return null if the Cursor is not positioned at a triple, or if the
	 *         predicate label is not in the Java cache.
	 */
	public String queryPredicate();

	/**
	 * Retrieve the string label for the context of the current triple in the
	 * Cursor instance. 
	 * @return null if the Cursor is not positioned at a
	 * triple, or if the context label is not in the Java cache.
	 */
	public String queryContext(); // quad-store

	/**
	 * Retrieve the literal string for the subject of the current triple in the
	 * Cursor instance.
	 * Note that the label does not provide all the information describing the
	 * part of the triple. When the part is a literal with a language or datatype
	 * modifier, the modifiers are missing.  A literal label may also appear to be a 
	 * URL or a blank node id.
	 * 
	 * @return a string, or null if the cursor is not positioned on a triple.
	 * @throws AllegroGraphException
	 *             if there is a problem during the operation.
	 *             <p>
	 *             This method almost almost always requires a round-trip to the
	 *             AllegroGraph server.
	 */
	public String getSubjectLabel() throws AllegroGraphException;

	/**
	 * Retrieve the Value instance that identifies the subject of the current
	 * triple in the Cursor.
	 * 
	 * @return null if the Cursor is not positioned at a triple.
	 * @throws AllegroGraphException
	 */
	public ValueNode getSubject() throws AllegroGraphException;

	/**
	 * Retrieve the Value instance that identifies the context of the current
	 * triple in the Cursor.
	 * 
	 * @return null if the Cursor is not positioned at a triple.
	 * @throws AllegroGraphException
	 */
	public ValueNode getContext() throws AllegroGraphException;

	/**
	 * Retrieve the predicate node UPI of the current triple in the Cursor
	 * instance.
	 * 
	 * @return a UPI, or null if the cursor is not positioned on a triple.
	 */
	public UPI getP();

	/**
	 * Retrieve the literal string for the predicate of the current triple in
	 * the Cursor instance.
	 * Note that the label does not provide all the information describing the
	 * part of the triple. When the part is a literal with a language or datatype
	 * modifier, the modifiers are missing.  A literal label may alo appear to be a 
	 * URL or a blank node id. 
	 * 
	 * @return a string or null if the cursor is not positioned on a triple.
	 * @throws AllegroGraphException
	 *             if there is a problem during the operation.
	 *             <p>
	 *             This method almost always requires a round-trip to the
	 *             AllegroGraph server.
	 */
	public String getPredicateLabel() throws AllegroGraphException;

	/**
	 * Retrieve the Value instance that identifies the predicate of the current
	 * triple in the Cursor.
	 * 
	 * @return null if the Cursor is not positioned at a triple.
	 * @throws AllegroGraphException
	 */
	public ValueNode getPredicate() throws AllegroGraphException;

	/**
	 * Retrieve the object node UPI of the current triple in the Cursor
	 * instance.
	 * 
	 * @return a UPI, or null if the cursor is not positioned on a triple.
	 */
	public UPI getO();

	/**
	 * Retrieve the context node UPI of the current triple in the Cursor
	 * instance.
	 * 
	 * @return a UPI, or null if the cursor is not positioned on a triple.
	 */
	public UPI getC();

	/**
	 * Retrieve the literal string for the object of the current triple in the
	 * Cursor instance.
	 * Note that the label does not provide all the information describing the
	 * part of the triple. When the part is a literal with a language or datatype
	 * modifier, the modifiers are missing.  A literal label may alo appear to be a 
	 * URL or a blank node id.
	 * 
	 * @return a string or null if the cursor is not positioned on a triple.
	 * @throws AllegroGraphException
	 *             if there is a problem during the operation.
	 *             <p>
	 *             This method almost always requires a round-trip to the
	 *             AllegroGraph server.
	 */
	public String getObjectLabel() throws AllegroGraphException;

	/**
	 * Retrieve the string label of the context of the current triple in the
	 * Cursor instance.
	 * Note that the label does not provide all the information describing the
	 * part of the triple. When the part is a literal with a language or datatype
	 * modifier, the modifiers are missing.  A literal label may also appear to be a 
	 * URI or a blank node id.
	 * 
	 * @return a string or null if the cursor is not positioned on a triple.
	 * @throws AllegroGraphException
	 *             if there is a problem during the operation.
	 *             <p>
	 *             This method almost always requires a round-trip to the
	 *             AllegroGraph server.
	 */
	public String getContextLabel() throws AllegroGraphException;

	/**
	 * Retrieve the Value instance that identifies the object of the current
	 * triple in the Cursor.
	 * 
	 * @return null if the Cursor is not positioned at a triple.
	 * @throws AllegroGraphException
	 */
	public ValueNode getObject() throws AllegroGraphException;

	/**
	 * Create a Triple instance from the current triple in the Cursor.
	 * Build the triple from whatever data is immediately available in the Cursor.
	 * 
	 * @return A Triple instance, or null if the Cursor is not positioned at a
	 *         triple.
	 */
	public TripleImpl queryTriple();

	/**
	 * Create a Triple instance from the current triple in the Cursor.
	 * This operation may require a round-trip to the server if some of the labels
	 * are not yet in the client.
	 * 
	 * @return A Triple instance, or null if the Cursor is not positioned at a
	 *         triple.
	 * @throws AllegroGraphException 
	 */
	public Triple getTriple() throws AllegroGraphException;

	/**
	 * Advance the Cursor and return a Triple instance. 
	 * If the Cursor is
	 * exhausted, return null.
	 * Build the Triple from whatever parts are available immediately.
	 */
	public Triple next();

	/**
	 * Advance the Cursor and return a Triple instance.
	 *  If the Cursor is
	 * exhausted, return null.
	 * This operation may require a trip to the server to fetch any needed
	 * label strings.
	 */
	public Triple getNext();

	/**
	 * Query the state of a Cursor instance.
	 * 
	 * @return true if the Cursor is positioned on a triple.
	 */
	public boolean atTriple();

	/**
	 * Query the state of a Cursor instance.
	 * 
	 * @return true if the Cursor can be advanced to a new triple.
	 */
	public boolean hasNext();

	/**
	 * Query the state of a Cursor instance.
	 * 
	 * @return true if the Cursor does not have any more entries because an
	 *         arbitrary limit was reached but more values were available in the
	 *         triple store.
	 */
	public boolean limitReached();

	/**
	 * Discard the remote reference to the AllegroGraph cursor instance. This method
	 * releases the reference and allows the server storage to be reclaimed.
	 * <p>
	 * An attempt to call next() will case an exception to be thrown.
	 * 
	 * @throws IllegalStateException
	 */
	public void close();

	/**
	 * Advance the Cursor instance to the next triple.
	 * 
	 * @return true if a new triple was retrieved.
	 */
	public boolean step() throws AllegroGraphException;

	/**
	 * This method returns an array of Triple instances.
	 * 
	 * @param n A positive integer number of instances desired.
	 * @return An array of Triple instances. The length of the array may be
	 *      less than or equal to n.  
	 * 
	 */
	public Triple[] step(int n) throws AllegroGraphException;

	/**
	 * Override the default method to give a more informatve representation.
	 * 
	 * @return a string of the form "&lt;Cursor <i>id</i>: <i>s p o</i>&gt;".
	 */
	public String toString();

	/**
	 * Discard the current triple in the Cursor.
	 * <p>
	 * The program must advance the Cursor in order to access a triple again.
	 * 
	 * @see java.util.Iterator#remove()
	 */
	public void remove();
	

}