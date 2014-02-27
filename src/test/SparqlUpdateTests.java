/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.FileInputStream;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

public class SparqlUpdateTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSesameUpdate() throws Exception {
    	URI s = vf.createURI("http://example/book1");
    	URI p = vf.createURI("http://purl.org/dc/elements/1.1/title");
    	Literal o_wrong = vf.createLiteral("Fundamentals of Compiler Desing");
    	Literal o_right = vf.createLiteral("Fundamentals of Compiler Design");
    	URI g = vf.createURI("http://example/bookStore");
    	conn.add(s,p,o_wrong,g);
    	
    	// Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
        	+ "\n"
        	+ "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        Update u = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
        u.execute();
        Assert.assertTrue("Title should be correct", conn.hasStatement(s,p,o_right,false,g));
        Assert.assertFalse("Incorrect title should be gone", conn.hasStatement(s,p,o_wrong,false,g));
   }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void testSesameUpdateViaBooleanQuery() throws Exception {
    	URI s = vf.createURI("http://example/book1");
    	URI p = vf.createURI("http://purl.org/dc/elements/1.1/title");
    	Literal o_wrong = vf.createLiteral("Fundamentals of Compiler Desing");
    	Literal o_right = vf.createLiteral("Fundamentals of Compiler Design");
    	URI g = vf.createURI("http://example/bookStore");
    	conn.add(s,p,o_wrong,g);
    	
    	// Perform a sequence of SPARQL UPDATE queries in one request to correct the title
    	String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
    		+ "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
    		+ "\n"
    		+ "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
    		+ "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";
    	
    	// SPARQL Update queries can also be executed using a BooleanQuery (for side effect)
    	// Useful for older versions of Sesame that don't have a prepareUpdate method. 
    	conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate();
    	Assert.assertTrue("Title should be correct", conn.hasStatement(s,p,o_right,false,g));
    	Assert.assertFalse("Incorrect title should be gone", conn.hasStatement(s,p,o_wrong,false,g));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testJenaUpdate() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.getUnionOfAllGraphs() );
    	AGModel model = closeLater( new AGModel(graph) );
    	model.read(new FileInputStream("src/test/example.nq"), null, "NQUADS");
    	Assert.assertEquals("expected size 10", 10, model.size());
    	Assert.assertTrue("Bob should be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Bob")));
    	Assert.assertFalse("Robert should not be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Robert")));
    	
    	// Perform a sequence of SPARQL UPDATE queries in one request to correct the title
    	String queryString = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
    			+ "DELETE DATA { GRAPH <http://example.org/bob/foaf.rdf> { <http://example.org/bob/foaf.rdf#me>  foaf:name  \"Bob\" } } ; \n"
    			+ "\n"
    			+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
    			+ "INSERT DATA { GRAPH <http://example.org/bob/foaf.rdf> { <http://example.org/bob/foaf.rdf#me>  foaf:name  \"Robert\" } }";
    	
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
		try {
			qe.execUpdate();
		} finally {
			qe.close();
		}
        Assert.assertTrue("Robert should be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Robert")));
        Assert.assertFalse("Bob should not be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Bob")));
   }
    
}
