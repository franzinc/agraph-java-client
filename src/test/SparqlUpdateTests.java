/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;

public class SparqlUpdateTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void batchSparqlUpdates() throws Exception {
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

        // SPARQL Update queries can be executed using a BooleanQuery (for side effect) 
        conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate();
        Assert.assertTrue("Title should be correct", conn.hasStatement(s,p,o_right,false,g));
        Assert.assertFalse("Incorrect title should be gone", conn.hasStatement(s,p,o_wrong,false,g));
   }

}
