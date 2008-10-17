/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * Default implementation of the ValueFactory interface that uses the RDF model
 * classes from this package.
 * 
 * @author Arjohn Kampman
 */
public class ValueFactoryImpl extends ValueFactoryBase {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final ValueFactoryImpl sharedInstance = new ValueFactoryImpl();

	public static ValueFactoryImpl getInstance() {
		return sharedInstance;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public URI createURI(String uri) {
		return new URIImpl(uri);
	}

	public URI createURI(String namespace, String localName) {
		return createURI(namespace + localName);
	}

	public BNode createBNode(String nodeID) {
		return new BNodeImpl(nodeID);
	}

	public Literal createLiteral(String value) {
		return new LiteralImpl(value);
	}

	public Literal createLiteral(String value, String language) {
		return new LiteralImpl(value, language);
	}

	public Literal createLiteral(String value, URI datatype) {
		return new LiteralImpl(value, datatype);
	}

	public Statement createStatement(Resource subject, URI predicate, Value object) {
		return new StatementImpl(subject, predicate, object);
	}

	public Statement createStatement(Resource subject, URI predicate, Value object, Resource context) {
		return new ContextStatementImpl(subject, predicate, object, context);
	}
}
