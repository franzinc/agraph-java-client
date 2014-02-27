/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.openrdf;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

import test.AGAbstractTest;
import test.TestSuites;

public class AGSparqlUpdateTest extends SPARQLUpdateTest {

	@Test
    @Category(TestSuites.Prepush.class)
    public void batchUpdate() throws Exception {
    	URI s = f.createURI("http://example/book1");
    	URI p = f.createURI("http://purl.org/dc/elements/1.1/title");
    	Literal o_wrong = f.createLiteral("Fundamentals of Compiler Desing");
    	Literal o_right = f.createLiteral("Fundamentals of Compiler Design");
    	URI g = f.createURI("http://example/bookStore");
    	con.add(s,p,o_wrong,g);
    	
    	// Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
        	+ "\n"
        	+ "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        Update u = con.prepareUpdate(QueryLanguage.SPARQL, queryString);
        u.execute();
        Assert.assertTrue("Title should be correct", con.hasStatement(s,p,o_right,false,g));
        Assert.assertFalse("Incorrect title should be gone", con.hasStatement(s,p,o_wrong,false,g));
   }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void updateViaBooleanQuery() throws Exception {
    	URI s = f.createURI("http://example/book1");
    	URI p = f.createURI("http://purl.org/dc/elements/1.1/title");
    	Literal o_wrong = f.createLiteral("Fundamentals of Compiler Desing");
    	Literal o_right = f.createLiteral("Fundamentals of Compiler Design");
    	URI g = f.createURI("http://example/bookStore");
    	con.add(s,p,o_wrong,g);
    	
    	// Perform a sequence of SPARQL UPDATE queries in one request to correct the title
    	String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
    		+ "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
    		+ "\n"
    		+ "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
    		+ "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";
    	
    	// SPARQL Update queries can also be executed using a BooleanQuery (for side effect)
    	// Useful for older versions of Sesame that don't have a prepareUpdate method. 
    	con.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate();
    	Assert.assertTrue("Title should be correct", con.hasStatement(s,p,o_right,false,g));
    	Assert.assertFalse("Incorrect title should be gone", con.hasStatement(s,p,o_wrong,false,g));
    }

	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

	/* protected methods */

	protected void loadDataset(String datasetFile)
		throws RDFParseException, RepositoryException, IOException
	{
		logger.debug("loading dataset...");
		InputStream dataset = org.openrdf.query.parser.sparql.SPARQLUpdateTest.class.getResourceAsStream(datasetFile);
		try {
			RDFParser parser = Rio.createParser(RDFFormat.TRIG, f);
			parser.setPreserveBNodeIDs(true);
			StatementCollector collector = new StatementCollector();
			parser.setRDFHandler(collector);
			parser.parse(dataset, "");
			con.add(collector.getStatements());
		} catch (RDFParseException e) {
			throw new RuntimeException(e);
		} catch (RDFHandlerException e) {
			throw new RuntimeException(e);
		}
		finally {
			dataset.close();
		}
	}

}
