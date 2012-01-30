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
 * Constants for DOAP primitives and for the DOAP namespace.
 */
public class DOAP {

	public static final String NAMESPACE = "http://usefulinc.com/ns/doap#";

	public final static URI PROJECT;

	public final static URI NAME;

	public final static URI RELEASE;

	public final static URI VERSION;

	public final static URI CREATED;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		PROJECT = factory.createURI(DOAP.NAMESPACE, "Project");
		NAME = factory.createURI(DOAP.NAMESPACE, "name");
		RELEASE = factory.createURI(DOAP.NAMESPACE, "release");
		VERSION = factory.createURI(DOAP.NAMESPACE, "Version");
		CREATED = factory.createURI(DOAP.NAMESPACE, "created");
	}
}
