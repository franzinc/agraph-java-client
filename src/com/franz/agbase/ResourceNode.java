package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;




/**
 * This interface includes URI, BlankNode and the DefaultGraph marker.
 */
public interface ResourceNode extends ValueNode {

	public void addProperty(URINode property, ValueNode value) throws AllegroGraphException;

	public TriplesIterator getSubjectStatements() throws AllegroGraphException;

}