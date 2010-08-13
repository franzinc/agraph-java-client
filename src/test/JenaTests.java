/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.jena.AGReasoner;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public class JenaTests extends AGAbstractTest {

	private void addOne(AGModel model) throws RepositoryException {
		Assert.assertEquals(0, model.size());
		
		Resource bob = model.createResource("http://example.org/people/bob");
		Resource dave = model.createResource("http://example.org/people/dave");
		Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
		
		model.add(bob, fatherOf, dave);

		Assert.assertEquals(1, model.size());
	}
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaAutoCommitTrue() throws Exception {
    	// this is the default, but try setting it explicitely
    	conn.setAutoCommit(true);
    	
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.getGraph() );
    	AGModel model = closeLater( new AGModel(graph) );
    	addOne(model);
    	
    	Assert.assertEquals("a different connection, triple was already committed", 1, getConnection().size());
    	
    	model.commit();
    	Assert.assertEquals(1, conn.size());
    	
    	Assert.assertEquals("a different connection", 1, getConnection().size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaAutoCommitFalse() throws Exception {
    	//conn.setAutoCommit(false);
    	
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.getGraph() );
    	AGModel model = closeLater( new AGModel(graph) );
    	try {
    		// use begin instead of setAutoCommit(false)
        	model.begin();
        	
        	addOne(model);
        	
    		Assert.assertEquals("a different connection, empty", 0, getConnection().size());
    		
    		model.commit();
    		Assert.assertEquals(1, conn.size());
    		
    		Assert.assertEquals("a different connection", 1, getConnection().size());
    	} catch (Exception e) {
    		model.abort();
    		throw e;
    	}
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void sparqlOrderByError_bug19157_rfe9971() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.getGraph() );
    	AGModel model = closeLater( new AGModel(graph) );
    	
		Resource bob = model.createResource("http://example.org/people/bob");
		Resource dave = model.createResource("http://example.org/people/dave");
		Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
		Property age = model.createProperty("http://example.org/ontology/age");
		Literal three = model.createTypedLiteral(3);
		
		model.add(bob, fatherOf, dave);
		model.add(dave, age, three);
		
		AGQuery query = AGQueryFactory.create("select ?s ?p ?o where { ?s ?p ?o . } order by ?x ?s");
		{
			//query.setCheckVariables(false); // default is false
			AGQueryExecution qe = closeLater( AGQueryExecutionFactory.create(query, model));
			qe.execSelect();
			// extra var is ignored
		}
		{
			query.setCheckVariables(true);
			AGQueryExecution qe = closeLater( AGQueryExecutionFactory.create(query, model));
			try {
				qe.execSelect();
				Assert.fail("query should have failed because of ?x");
			} catch (Exception e) {
				if ( ! e.getMessage().contains("MALFORMED QUERY: Variables do not intersect with query: ?x")) {
					throw e;
				}
			}
		}
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaGraphs_bug19491() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph defaultGraph = closeLater( maker.getGraph() );
    	AGModel defaultModel = closeLater( new AGModel(defaultGraph) );
    	addOne(defaultModel);

    	AGGraph namedGraph = closeLater( maker.openGraph("http://example.com/named") );
    	AGModel namedModel = closeLater( new AGModel(namedGraph) );
    	addOne(namedModel);
    	
		AGReasoner reasoner = new AGReasoner();
    	defaultGraph = closeLater( maker.getGraph() );
    	defaultModel = closeLater( new AGModel(defaultGraph) );
		AGInfModel infModel = closeLater( new AGInfModel(reasoner, defaultModel));
		Assert.assertEquals("conn is full", 2, conn.size());
		Assert.assertEquals("infModel should be full", 2,
				closeLater( infModel.listStatements((Resource)null, (Property)null, (RDFNode)null)).toList().size());
		Assert.assertEquals("defaultModel should be partial", 1,
				closeLater( defaultModel.listStatements((Resource)null, (Property)null, (RDFNode)null)).toList().size());
		// TODO: size is not correct for infModel, dunno why
		//Assert.assertEquals("infModel should be full", 2, infModel.size());
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaReasoning_bug19484() throws Exception {
        AGGraphMaker maker = closeLater( new AGGraphMaker(conn));
        AGGraph graph = closeLater( maker.getGraph());
        AGModel model = closeLater( new AGModel(graph));
        
        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        Property hasFather = model.createProperty("http://example.org/ontology/hasFather");
        
        model.add(hasFather, OWL.inverseOf, fatherOf);
        model.add(bob, fatherOf, dave);
        
        AGQuery query = AGQueryFactory.create("select * where { <http://example.org/people/dave> <http://example.org/ontology/hasFather> ?o . }");
        {
        	AGInfModel inf = closeLater( new AGInfModel(new AGReasoner(), model));
        	
        	StmtIterator stmts = closeLater( inf.listStatements(dave, hasFather, bob));
        	Assert.assertTrue("with reasoning", stmts.hasNext());
        	
        	AGQueryExecution exe = closeLater( AGQueryExecutionFactory.create(query, inf));
        	ResultSet results = exe.execSelect();
        	Assert.assertTrue("with reasoning", results.hasNext());
        }
        {
        	StmtIterator stmts = closeLater( model.listStatements(dave, hasFather, bob));
        	Assert.assertFalse("without reasoning", stmts.hasNext());
        	
        	AGQueryExecution exe = closeLater( AGQueryExecutionFactory.create(query, model));
        	ResultSet results = exe.execSelect();
        	Assert.assertFalse("without reasoning", results.hasNext());
        }
    }

}
