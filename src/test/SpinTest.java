/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static test.Stmt.statementSet;
import static test.Stmt.stmts;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.QueryLanguage;
import org.openrdf.rio.RDFFormat;

public class SpinTest extends AGAbstractTest {

    private final String baseURI = "http://ex.org#";
    
    private final String kennedyNamespace = "http://www.franz.com/simple#";

	private final String ageFn = baseURI + "age";
	private final String parentsMP = baseURI + "parents";

	private URI context;

    @Before
    public void installScripts() throws Exception {
    	conn.setAutoCommit(true);
    	context = vf.createURI(kennedyNamespace);
    	conn.setNamespace("ex", baseURI);
    	conn.setNamespace("kennedy", kennedyNamespace);
//    	conn.registerPredicateMapping(vf.createURI(kennedyNamespace, "birth-year"), XMLSchema.INT);
    	conn.add(new File("src/tutorial/java-kennedy.ntriples"), kennedyNamespace, RDFFormat.NTRIPLES, context);
    }
    
    @After
    public void cleanup0() throws Exception {
//    	conn.remove((Resource)null, null, null);
    }
    
    @After
    public void cleanup1() throws Exception {
// TODO broken:    	conn.getHttpRepoClient().deleteHardSpinFunction(ageFn);
    }
    
    @After
    public void cleanup2() throws Exception {
    	conn.getHttpRepoClient().deleteHardSpinMagicProperty(parentsMP);
    }

    /**
     * rfe10988
     */
    @Test
    @Ignore
    @Category(TestSuites.Temp.class)
    public void spinFunction() throws Exception {
    	try {
    		Assert.fail( conn.getSpinFunction(ageFn) );
    	} catch (Exception e) {
    		if (e.getMessage().contains(ageFn + " is not a registered SPIN function")) {
        		// expected
    		} else {
    			throw e;
    		}
    	}
    	String ageFnSparql = "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "prefix xs: <http://www.w3.org/2001/XMLSchema#>\n"
    	+ "select ( (2011 - xs:int(?birthYear)) as ?age ) { ?who kennedy:birth-year ?birthYear . }";
    	conn.putSpinFunction(ageFn, ageFnSparql, new String[] {"?who"});
    	Assert.assertEquals(ageFnSparql, conn.getSpinFunction(ageFn));

    	String queryString = "prefix ex: <" + baseURI + ">\n"
    	+ "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "prefix kennedy: <http://www.franz.com/simple#>\n"
    	+ "prefix ex: <http://ex.org#>\n"
    	+ "select ?first ?last ?age ?birthYear {\n"
    	+ "?person kennedy:first-name ?first .\n"
    	+ "?person kennedy:last-name ?last .\n"
    	+ "?person kennedy:birth-year ?birthYear .\n"
    	+ "bind( ex:age( ?person ) as ?age ) .\n"
    	+ "} order by ?age limit 2";

        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral("39", XMLSchema.INTEGER)),
        		new Stmt(null, null, vf.createLiteral("43", XMLSchema.INTEGER))}),
        		statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate(), null, null, "age"));

    	// TODO MH: Assert.assertEquals("TODO", conn.getHttpRepoClient().listSpinFunctions());
}

    /**
     * rfe10988
     */
    @Test
    @Ignore
    @Category(TestSuites.Temp.class)
    public void spinMagicProperty() throws Exception {
    	try {
    		Assert.fail( conn.getSpinMagicProperty(parentsMP) );
    	} catch (Exception e) {
    		if (e.getMessage().contains(parentsMP + " is not a registered SPIN magic property")) {
        		// expected
    		} else {
    			throw e;
    		}
    	}
    	conn.getHttpRepoClient().deleteHardSpinMagicProperty(parentsMP);
    	
    	String parentsFnSparql = "prefix kennedy: <" + kennedyNamespace+ ">\n"
    	+ "select ?parent { ?parent kennedy:has-child ?child . }";
    	conn.putSpinMagicProperty(parentsMP, parentsFnSparql, "?child");
    	Assert.assertEquals(parentsFnSparql, conn.getSpinMagicProperty(parentsMP));

    	String queryString = "prefix ex: <" + baseURI + ">\n"
    	+ "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "select ?person ?parentFirst {\n"
    	+ "?person kennedy:first-name 'Joseph' .\n"
//    	+ "?person kennedy:birth-year '1915'^^<" + XMLSchema.INT + "> .\n"
    	+ "?person kennedy:birth-year '1915' .\n"
    	+ "?person ex:parents ?parent .\n"
//    	+ "?parent kennedy:has-child ?person .\n"
    	+ "?parent kennedy:first-name ?parentFirst .\n"
    	+ "}";
    	
        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral("Joseph")),
        		new Stmt(null, null, vf.createLiteral("Rose"))}),
                statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate(),
                		null, null, "parentFirst"));

    	// TODO MH: Assert.assertEquals("TODO", conn.getHttpRepoClient().listSpinMagicProperties());
    }

}
