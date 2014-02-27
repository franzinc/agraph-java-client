/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static test.Stmt.statementSet;
import static test.Stmt.stmts;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.QueryLanguage;
import org.openrdf.rio.RDFFormat;

import com.franz.agraph.repository.AGSpinFunction;
import com.franz.agraph.repository.AGSpinMagicProperty;
import com.franz.agraph.repository.AGTupleQuery;

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
    	conn.getHttpRepoClient().deleteHardSpinFunction(ageFn);
    }
    
    @After
    public void cleanup2() throws Exception {
    	conn.getHttpRepoClient().deleteHardSpinMagicProperty(parentsMP);
    }

    /**
     * rfe10988
     */
    @Test
    @Category(TestSuites.Prepush.class)
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
    	conn.getHttpRepoClient().deleteHardSpinMagicProperty(ageFn);
    	
    	String ageFnSparql = "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "prefix xs: <http://www.w3.org/2001/XMLSchema#>\n"
    	+ "select ( (2011 - xs:int(?birthYear)) as ?age ) { ?who kennedy:birth-year ?birthYear . }";
    	conn.putSpinFunction(new AGSpinFunction(ageFn, new String[] {"who"}, ageFnSparql));
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

    	AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    	query.setEngine("set-based");
        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral("39", XMLSchema.INTEGER)),
        		new Stmt(null, null, vf.createLiteral("43", XMLSchema.INTEGER))}),
        		statementSet(query.evaluate(), null, null, "age"));
        
        List<AGSpinFunction> list = conn.listSpinFunctions();
        Assert.assertEquals(list.toString(), 1, list.size());
        AGSpinFunction fn = list.get(0);
        Assert.assertEquals(list.toString(), ageFnSparql, fn.getQuery());
        Assert.assertEquals(list.toString(), 1, fn.getArguments().length);
        Assert.assertEquals(list.toString(), "who", fn.getArguments()[0]);
    }
    
    /**
     * rfe11117
     */
    @Test
    @Category(TestSuites.Prepush.class)
    public void listSpinFunctions_0args() throws Exception {
    	String ageFnSparql = "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "prefix xs: <http://www.w3.org/2001/XMLSchema#>\n"
    	+ "select ( (2011 - xs:int(?birthYear)) as ?age ) { ?who kennedy:birth-year ?birthYear . }";
    	conn.putSpinFunction(new AGSpinFunction(ageFn, null, ageFnSparql));
    	Assert.assertEquals(ageFnSparql, conn.getSpinFunction(ageFn));
    	
        List<AGSpinFunction> list = conn.listSpinFunctions();
        Assert.assertEquals(list.toString(), 1, list.size());
        AGSpinFunction fn = list.get(0);
        Assert.assertEquals(list.toString(), ageFnSparql, fn.getQuery());
        Assert.assertEquals(list.toString(), 0, fn.getArguments().length);
    }

    /**
     * rfe11117
     */
    @Test
    @Category(TestSuites.Prepush.class)
    public void listSpinFunctions_2args() throws Exception {
    	String ageFnSparql = "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "prefix xs: <http://www.w3.org/2001/XMLSchema#>\n"
    	+ "select ( (2011 - xs:int(?birthYear)) as ?age ) { ?who kennedy:birth-year ?birthYear . }";
    	conn.putSpinFunction(new AGSpinFunction(ageFn, new String[] {"who", "extra"}, ageFnSparql));
    	Assert.assertEquals(ageFnSparql, conn.getSpinFunction(ageFn));
    	
        List<AGSpinFunction> list = conn.listSpinFunctions();
        Assert.assertEquals(list.toString(), 1, list.size());
        AGSpinFunction fn = list.get(0);
        Assert.assertEquals(list.toString(), ageFnSparql, fn.getQuery());
        Assert.assertEquals(list.toString(), 2, fn.getArguments().length);
        Assert.assertEquals(list.toString(), "who", fn.getArguments()[0]);
        Assert.assertEquals(list.toString(), "extra", fn.getArguments()[1]);
    }

    /**
     * rfe10988
     */
    @Test
    @Category(TestSuites.Prepush.class)
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
    	conn.putSpinMagicProperty(new AGSpinMagicProperty(parentsMP, new String[] {"?child"}, parentsFnSparql));
    	Assert.assertEquals(parentsFnSparql, conn.getSpinMagicProperty(parentsMP));

    	String prefixes = "prefix ex: <" + baseURI + ">\n"
    	+ "prefix kennedy: <" + kennedyNamespace + ">\n";
    	String queryString = "select ?person ?parentFirst {\n"
    	+ "?person kennedy:first-name 'Joseph' .\n"
//    	+ "?person kennedy:birth-year '1915'^^<" + XMLSchema.INT + "> .\n"
    	+ "?person kennedy:birth-year '1915' .\n"
    	+ "?person ex:parents ?parent .\n"
//    	+ "?parent kennedy:has-child ?person .\n"
    	+ "?parent kennedy:first-name ?parentFirst .\n"
    	+ "}";
    	
    	AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, prefixes + queryString);
    	query.setEngine("set-based");
        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral("Joseph")),
        		new Stmt(null, null, vf.createLiteral("Rose"))}),
                statementSet(query.evaluate(),
                		null, null, "parentFirst"));

        List<AGSpinMagicProperty> list = conn.listSpinMagicProperties();
        Assert.assertEquals(list.toString(), 1, list.size());
        AGSpinMagicProperty fn = list.get(0);
        Assert.assertEquals(list.toString(), parentsFnSparql, fn.getQuery());
        Assert.assertEquals(list.toString(), 1, fn.getArguments().length);
        Assert.assertEquals(list.toString(), "?child", fn.getArguments()[0]);
    }

}
