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
import java.util.List;

import org.apache.commons.httpclient.util.URIUtil;
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
    	conn.registerPredicateMapping(vf.createURI(kennedyNamespace, "birth-year"), XMLSchema.INT);
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
		// TODO MH: multiple args
        Assert.assertEquals(list.toString(), "who", fn.getArguments()[0]);
    }

    /**
     * rfe10988
     */
    @Test
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
    	conn.putSpinMagicProperty(new AGSpinMagicProperty(parentsMP, new String[] {"?child"}, parentsFnSparql));
    	Assert.assertEquals(parentsFnSparql, conn.getSpinMagicProperty(parentsMP));

    	String queryString = "prefix ex: <" + baseURI + ">\n"
    	+ "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "select ?person ?parentFirst {\n"
    	+ "?person kennedy:first-name 'Joseph' .\n"
//    	+ "?person kennedy:birth-year '1915'^^<" + XMLSchema.INT + "> .\n"
    	+ "?person kennedy:birth-year 1915 .\n"
    	+ "?person ex:parents ?parent .\n"
//    	+ "?parent kennedy:has-child ?person .\n"
    	+ "?parent kennedy:first-name ?parentFirst .\n"
    	+ "}";
    	
    	//             prefix+ex%3A+%3Chttp%3A%2F%2Fex.org%23%3E%0Aprefix+kennedy%3A+%3Chttp%3A%2F%2Fwww.franz.com%2Fsimple%23%3E%0A
    	//             select+%3Fperson+%3FparentFirst+%7B%0A%3Fperson+kennedy%3Afirst-name+%27Joseph%27+.%0A%3Fperson+kennedy%3Abirth-year+1915+.%0A%3Fperson+ex%3Aparents+%3Fparent+.%0A%3Fparent+kennedy%3Afirst-name+%3FparentFirst+.%0A%7D
//    	queryString = "select%20%3Fperson%20%3FparentFirst%20%7B%0A%20%20%3Fperson%20kennedy%3Afirst-name%20'Joseph'%20.%0A%20%20%3Fperson%20kennedy%3Abirth-year%201915%20.%0A%20%20%3Fperson%20ex%3Aparents%20%3Fparent%20.%0A%20%20%3Fparent%20kennedy%3Afirst-name%20%3FparentFirst%20.%0A%7D";
//    	queryString = "select+%3Fperson+%3FparentFirst+%7B%0A%3Fperson+kennedy%3Afirst-name+%27Joseph%27+.%0A++%3Fperson+  kennedy%3Abirth-year+1915+.%0A++%3Fperson+ex%3Aparents+%3Fparent+.%0A++%3Fparent+kennedy%3Afirst-name+%3FparentFirst+.%0A%7D";
//    	queryString = URIUtil.decode(queryString);
    	
    	AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    	query.setEngine("set-based");
        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral("Joseph")),
        		new Stmt(null, null, vf.createLiteral("Rose"))}),
                statementSet(query.evaluate(),
                		null, null, "parentFirst"));

        String list = conn.getHttpRepoClient().listSpinMagicProperties();
        log.info(list);
    	Assert.assertTrue(list, list.contains(parentsFnSparql));
    }

}
