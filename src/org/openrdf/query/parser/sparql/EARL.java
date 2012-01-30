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
 * Constants for EARL primitives and for the EARL namespace.
 */
public class EARL {

	public static final String NAMESPACE = "http://www.w3.org/ns/earl#";

	public final static URI ASSERTION;

	public final static URI ASSERTEDBY;
	
	public final static URI SUBJECT;
	
	public final static URI TEST;

	public final static URI RESULT;

	public final static URI MODE;

	public final static URI TESTRESULT;

	public final static URI OUTCOME;

	public final static URI SOFTWARE;

	// Outcome values
	
	public final static URI PASS;

	public final static URI FAIL;

	public final static URI CANNOTTELL;

	public final static URI NOTAPPLICABLE;

	public final static URI NOTTESTED;

	// Test modes

	public final static URI MANUAL;

	public final static URI AUTOMATIC;

	public final static URI SEMIAUTOMATIC;

	public final static URI NOTAVAILABLE;

	public final static URI HEURISTIC;
	
	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		ASSERTION = factory.createURI(EARL.NAMESPACE, "Assertion");
		ASSERTEDBY = factory.createURI(EARL.NAMESPACE, "assertedBy");
		SUBJECT = factory.createURI(EARL.NAMESPACE, "subject");
		TEST = factory.createURI(EARL.NAMESPACE, "test");
		RESULT = factory.createURI(EARL.NAMESPACE, "result");
		MODE = factory.createURI(EARL.NAMESPACE, "mode");
		TESTRESULT = factory.createURI(EARL.NAMESPACE, "TestResult");
		OUTCOME = factory.createURI(EARL.NAMESPACE, "outcome");
		SOFTWARE = factory.createURI(EARL.NAMESPACE, "Software");

		// Outcome values
		
		PASS = factory.createURI(EARL.NAMESPACE, "pass");
		FAIL = factory.createURI(EARL.NAMESPACE, "fail");
		CANNOTTELL = factory.createURI(EARL.NAMESPACE, "cannotTell");
		NOTAPPLICABLE = factory.createURI(EARL.NAMESPACE, "notApplicable");
		NOTTESTED = factory.createURI(EARL.NAMESPACE, "notTested");
		
		// Test modes
		MANUAL = factory.createURI(EARL.NAMESPACE, "manual");
		AUTOMATIC = factory.createURI(EARL.NAMESPACE, "automatic");
		SEMIAUTOMATIC = factory.createURI(EARL.NAMESPACE, "semiAutomatic");
		NOTAVAILABLE = factory.createURI(EARL.NAMESPACE, "notAvailable");
		HEURISTIC = factory.createURI(EARL.NAMESPACE, "heuristic");
	}
}
