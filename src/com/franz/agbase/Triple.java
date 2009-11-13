package com.franz.agbase;


import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.impl.TripleImpl;

/**
 * This interface defines a representation of an AllegroGraph triple.
 * <p>
 * The AllegroGraph instance has 5 components, id, subject, predicate, object
 *  and context.
 * All 5 are copied to the Java instance.
 * <p>
 * The components are copied when a Triple instance is created.
 * The associated URI or literal strings may or may not be filled at
 * creation time.
 * <p>
 * Triple instances are not unique.
 * There may be multiple Triple instances associated with one triple in
 * the triple store.
 */
public interface Triple extends  ValueObject {

	/**
	 * Return the unique integer identifier of the object.
	 * @return An integer object id.  If the value is negative, then the 
	 *    object has not been stored in the triple store and is simply a
	 *    place holder for a label.  If the value is zero, this is an inferred
	 *    triple that does not exist in the persisitnet triple store.
	 */
	public long queryAGId();

	/**
	 * Return the unique integer identifier of the object.
	 * @return An integer object id.  If the value is zero, this is an inferred
	 *    triple that does not exist in the persistent triple store.
	 * @throws AllegroGraphException If the triple is not yet stored.
	 */
	public long getAGId() throws AllegroGraphException;

	/**
	 * Add a triple to the triple store.
	 * The Triple instance must have been created 
	 * with {@link AllegroGraph#createStatement(ResourceNode, URINode, ValueNode)}
	 * or with {@link AllegroGraph#createStatement(ResourceNode, URINode, ValueNode, ResourceNode)}.
	 * @return true if the triple was added, false if the triple was already added
	 *    and therefore nothing was done.
	 * @throws AllegroGraphException
	 */
	public boolean add() throws AllegroGraphException;

	/**
	 * Retrieve the subject slot of the Triple instance.
	 * @return A UPI instance that identifies the subject node.  A null value means that
	 *    the subject is not in the local cache, and must be obtained from the
	 *    server with a call to getS();
	 */
	public UPI queryS();

	/**
	 * Retrieve the subject slot of the Triple instance.
	 * If the value is not in the local cache, this call will require a round-trip
	 * to the AllegroGraph server.
	 * @return An integer subject node number.  
	 * 
	 */
	public UPI getS();

	/**
	 * Retrieve the subject slot of the Triple instance.
	 * @return A string containing the URI of the subject node.
	 * If the returned value is null, the method getSubjectLabel() must be used
	 * to get the true value.
	 */
	public String querySubject();

	/**
	 * Retrieve the URI string associated with the 
	 * subject component of this Triple.
	 * @return A string containing the URI of the subject node.
	 */
	public String getSubjectLabel() throws AllegroGraphException;

	/**
	 * Create a Resource instance that describes the subject component
	 * of the triple.
	 * 
	 *   This method must return a Resource instance as specified in the openrdf
	 *   model specification.  Since AllegroGraph is more general, the subject
	 *   slot can be a more general object.  In those cases, this method will
	 *   throw a CLassCastException.
	 *   
	 *   Use the more general method 
	 *   {@link #getSubjectInstance()}
	 *   to retrieve any type.
	 */
	public ResourceNode getSubject();

	public ValueNode getSubjectInstance();

	/**
	 * Retrieve the predicate slot of the Triple instance.
	 * @return A UPI instance that identifies the predicate node.  A null value means that
	 *    the predicate is not in the local cache, and must be obtained from the
	 *    server with a call to getP();
	 */
	public UPI queryP();

	/**
	 * Retrieve the predicate slot of the Triple instance.
	 * If the value is not in the local cache, this call will require a round-trip
	 * to the AllegroGraph server.
	 * @return A UPI instance that identifies the predicate node.  
	 * 
	 */
	public UPI getP();

	/**
	 * Retrieve the predicate slot of the Triple instance.
	 * @return The string form of the predicate node or null if the string
	 * form is not available.  Use getPredicateLabel() to always return a string.
	 */
	public String queryPredicate();

	/**
	 * Retrieve the URI string associated with the 
	 * predicate component of this Triple.
	 * @return A string containing the URI of the predicate node.
	 * @throws AllegroGraphException 
	 */
	public String getPredicateLabel() throws AllegroGraphException;

	/**
	 * Create a URI instance that describes the predicate component
	 * of the triple.  
	 * 
	 * This method must return a Resource instance as specified in the openrdf
	 *   model specification.  Since AllegroGraph is more general, the subject
	 *   slot can be a more general object.  In those cases, this method will
	 *   throw a CLassCastException.
	 *   
	 *   Use the more general method 
	 *   {@link #getPredicateInstance()}
	 *   to retrieve any type.
	 */
	public URINode getPredicate();

	public ValueNode getPredicateInstance();

	/**
	 * Retrieve the object slot of the Triple instance.
	 * @return A UPI instance that identifies the object node.  A null value means that
	 *    the subject is not in the local cache, and must be obtained from the
	 *    server with a call to getS();
	 */
	public UPI queryO();

	/**
	 * Retrieve the object slot of the Triple instance.
	 * If the value is not in the local cache, this call will require a round-trip
	 * to the AllegroGraph server.
	 * @return An integer object node number.  
	 * 
	 */
	public UPI getO();

	/**
	 * Retrieve the object slot of the Triple instance.
	 * @return A string or null. Use getObjectLabel() to always return a string.
	 */
	public String queryObject();

	/**
	 * Retrieve the URI string associated with the 
	 * object component of this Triple.
	 * @return A string containing the URI of the object node.
	 * @throws AllegroGraphException 
	 */
	public String getObjectLabel() throws AllegroGraphException;

	/**
	 * Create a Value instance that describes the object component
	 * of the triple.  
	 */
	public ValueNode getObject();

	/*
	 * Retrieve the context slot of the Triple instance.
	 * @return An integer context node number.
	 */
	public UPI getC();

	/*
	 * Retrieve the context slot of the Triple instance.
	 * @return A string containg the URI of the context node.
	 * If the returned value is null, the method getContextLabel() must be used
	 * to get the true value.
	 */
	public String queryContext();

	/**
	 * Retrieve the URI string associated with the 
	 * context component of this Triple.
	 * @return A string containing the URI of the context node.
	 */
	public String getContextLabel();

	/**
	 * Create a Resource instance that describes the context component
	 * of the triple.  
	 */
	public ValueNode getContext();

	/**
	 * This method overrides the generic toString method.
	 * This method generates a more readable output string.
	 */
	public String toString();

	public boolean equals(Object other);

	public int hashCode();

	public int compareTo(TripleImpl to);

}