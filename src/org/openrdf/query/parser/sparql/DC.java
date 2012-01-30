/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.parser.sparql;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Constants for Dublin Core primitives and for the Dublin Core namespace.
 */
public final class DC {

	/** http://purl.org/dc/elements/1.1/ */
	public final static String NAMESPACE = "http://purl.org/dc/elements/1.1/";

	/** http://purl.org/dc/elements/1.1/contributor */
	public final static URI CONTRIBUTOR;

	/** http://purl.org/dc/elements/1.1/coverage */
	public final static URI COVERAGE;

	/** http://purl.org/dc/elements/1.1/creator */
	public final static URI CREATOR;

	/** http://purl.org/dc/elements/1.1/date */
	public final static URI DATE;

	/** http://purl.org/dc/elements/1.1/description */
	public final static URI DESCRIPTION;

	/** http://purl.org/dc/elements/1.1/format */
	public final static URI FORMAT;

	/** http://purl.org/dc/elements/1.1/identifier */
	public final static URI IDENTIFIER;

	/** http://purl.org/dc/elements/1.1/language */
	public final static URI LANGUAGE;

	/** http://purl.org/dc/elements/1.1/publisher */
	public final static URI PUBLISHER;

	/** http://purl.org/dc/elements/1.1/relation */
	public final static URI RELATION;

	/** http://purl.org/dc/elements/1.1/rights */
	public final static URI RIGHTS;

	/** http://purl.org/dc/elements/1.1/source */
	public final static URI SOURCE;

	/** http://purl.org/dc/elements/1.1/subject */
	public final static URI SUBJECT;

	/** http://purl.org/dc/elements/1.1/title */
	public final static URI TITLE;

	/** http://purl.org/dc/elements/1.1/type */
	public final static URI TYPE;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		CONTRIBUTOR = factory.createURI(NAMESPACE, "contributor");
		COVERAGE = factory.createURI(NAMESPACE, "coverage");
		CREATOR = factory.createURI(NAMESPACE, "creator");
		DATE = factory.createURI(NAMESPACE, "date");
		DESCRIPTION = factory.createURI(NAMESPACE, "description");
		FORMAT = factory.createURI(NAMESPACE, "format");
		IDENTIFIER = factory.createURI(NAMESPACE, "identifier");
		LANGUAGE = factory.createURI(NAMESPACE, "language");
		PUBLISHER = factory.createURI(NAMESPACE, "publisher");
		RELATION = factory.createURI(NAMESPACE, "relation");
		RIGHTS = factory.createURI(NAMESPACE, "rights");
		SOURCE = factory.createURI(NAMESPACE, "source");
		SUBJECT = factory.createURI(NAMESPACE, "subject");
		TITLE = factory.createURI(NAMESPACE, "title");
		TYPE = factory.createURI(NAMESPACE, "type");
	}
}