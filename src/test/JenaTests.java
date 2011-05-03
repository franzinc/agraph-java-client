/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

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
    	// this is the default, but try setting it explicitly
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
			    if ( ! (e.getMessage().contains("MALFORMED QUERY: Variables do not intersect with query: ?x") || e.getMessage().contains("unknown variable in order expression: ?x")) ) {
					throw e;
				}
			}
		}
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaGraphs_bug19491() throws Exception {
    	// This test is largely obsolete/superseded by jenaGraphScopedReasoning
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
		Assert.assertEquals("infModel should be partial", 1,
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
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void savingModel_spr37167() throws Exception {
        AGGraphMaker maker = closeLater( new AGGraphMaker(conn));
        AGGraph graph = closeLater( maker.getGraph());
        AGModel model = closeLater( new AGModel(graph));
        
        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        model.add(bob, fatherOf, dave);

        Resource blankNode = model.createResource();
        Property has = model.createProperty("http://example.org/ontology/has");
        model.add(blankNode, has, dave);
        
        model.write(closeLater( new FileOutputStream( File.createTempFile("agraph-java-test", "txt"))));
        
        graph = closeLater( maker.getGraph());
        model = closeLater( new AGModel(graph));
        model.write(closeLater( new FileOutputStream( File.createTempFile("agraph-java-test", "txt"))));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaRestrictionReasoning() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn));
    	AGGraph graph = closeLater( maker.getGraph());
    	AGModel model = closeLater( new AGModel(graph));
		AGReasoner reasoner = AGReasoner.RESTRICTION;
		AGInfModel infmodel = closeLater( new AGInfModel(reasoner, model));
		Resource a = model.createResource("http://a");
		Resource c = model.createResource("http://C");
		Resource d = model.createResource("http://D");
		Property p = model.createProperty("http://p");
		Resource r = model.createResource("http://R");
		Resource v = model.createResource("http://v");
		Resource w = model.createResource("http://w");

		model.add(c,OWL.equivalentClass,r);
		model.add(r,RDF.type,OWL.Restriction);
		model.add(r,OWL.onProperty,p);
		model.add(r,OWL.hasValue,v);
		model.add(a,RDF.type,c);
		Assert.assertTrue("missing hasValue inference 1", infmodel.contains(a,p,v));

		model.removeAll();
		model.add(c,OWL.equivalentClass,r);
    	model.add(r,RDF.type,OWL.Restriction);
    	model.add(r,OWL.onProperty,p);
    	model.add(r,OWL.hasValue,v);
    	model.add(a,p,v);
    	Assert.assertTrue("missing hasValue inference 2", infmodel.contains(a,RDF.type,c));
    	
    	model.removeAll();
    	model.add(c,OWL.equivalentClass,r);
    	model.add(r,RDF.type,OWL.Restriction);
    	model.add(r,OWL.onProperty,p);
    	model.add(r,OWL.someValuesFrom,d);
    	model.add(a,p,v);
    	model.add(a,p,w);
    	model.add(v,RDF.type,d);
    	Assert.assertTrue("missing someValuesFrom inference", infmodel.contains(a,RDF.type,c));
    	Assert.assertFalse("unexpected someValuesFrom inference", infmodel.contains(w,RDF.type,d));
    	
    	model.removeAll();
    	model.add(c,OWL.equivalentClass,r);
    	model.add(r,RDF.type,OWL.Restriction);
    	model.add(r,OWL.onProperty,p);
    	model.add(r,OWL.allValuesFrom,d);
    	model.add(a,p,v);
    	model.add(a,RDF.type,c);
    	Assert.assertTrue("missing allValuesFrom inference", infmodel.contains(v,RDF.type,d));

    	// check for unsoundness
    	model.removeAll();
    	model.add(c,OWL.equivalentClass,r);
    	model.add(r,RDF.type,OWL.Restriction);
    	model.add(r,OWL.onProperty,p);
    	model.add(r,OWL.allValuesFrom,d);
    	model.add(a,p,v);
    	model.add(a,p,w);
    	model.add(v,RDF.type,d);
    	model.add(w,RDF.type,d);
    	Assert.assertFalse("unexpected allValuesFrom inference", infmodel.contains(a,RDF.type,c));
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaGraphScopedReasoning() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn));
    	AGGraph gd = closeLater( maker.getGraph());
    	AGGraph g1 = closeLater( maker.createGraph("http://example.org/g1"));
    	AGGraph g2 = closeLater( maker.createGraph("http://example.org/g2"));
    	AGGraph g3 = closeLater( maker.createGraph("http://example.org/g3"));
    	AGGraph gAll = closeLater( maker.getUnionOfAllGraphs());
    	AGGraph gAllb = closeLater( maker.createUnion());
    	AGGraph gd12 = closeLater( maker.createUnion(gd, g1, g2));
    	AGGraph gd23 = closeLater( maker.createUnion(gd, g2, g3));
    	AGGraph g123 = closeLater( maker.createUnion(g1, g2, g3));
    	
    	AGModel md = closeLater( new AGModel(gd));
    	AGModel m1 = closeLater( new AGModel(g1));
    	AGModel m2 = closeLater( new AGModel(g2));
    	AGModel m3 = closeLater( new AGModel(g3));
    	AGModel mAll = closeLater( new AGModel(gAll));
    	AGModel mAllb = closeLater( new AGModel(gAllb));
    	AGModel md12 = closeLater( new AGModel(gd12));
    	AGModel md23 = closeLater( new AGModel(gd23));
    	AGModel m123 = closeLater( new AGModel(g123));
    	
    	Resource a = md.createResource("http://a");
    	Resource b = md.createResource("http://b");
    	Resource c = md.createResource("http://c");
    	Resource d = md.createResource("http://d");
    	Property p = md.createProperty("http://p");
    	Property q = md.createProperty("http://q");
    	
    	md.add(p,RDF.type, OWL.TransitiveProperty);
    	m1.add(a,p,b);
    	m1.add(p,RDFS.subPropertyOf,q);
    	m2.add(b,p,c);
    	m3.add(c,p,d);
    	Assert.assertTrue("size of md", md.size()==1);
    	Assert.assertTrue("size of m1", m1.size()==2);
    	Assert.assertTrue("size of m2", m2.size()==1);
    	Assert.assertTrue("size of m3", m3.size()==1);
    	Assert.assertTrue("size of mAll", mAll.size()==5);
    	Assert.assertTrue("size of mAllb", mAllb.size()==5);
    	Assert.assertTrue("size of md12", md12.size()==4);
    	Assert.assertTrue("size of md23", md23.size()==3);
    	Assert.assertTrue("size of m123", m123.size()==4);
    	
    	AGReasoner reasoner = AGReasoner.RDFS_PLUS_PLUS;
    	AGInfModel infAll = closeLater( new AGInfModel(reasoner, mAll));
    	AGInfModel infd = closeLater( new AGInfModel(reasoner, md)); 
    	AGInfModel inf1 = closeLater( new AGInfModel(reasoner, m1)); 
    	AGInfModel infd12 = closeLater( new AGInfModel(reasoner, md12));
    	reasoner = AGReasoner.RESTRICTION;
    	AGInfModel infd23 = closeLater( new AGInfModel(reasoner, md23));
    	AGInfModel inf123 = closeLater( new AGInfModel(reasoner, m123));

    	Assert.assertTrue("missing inference All", infAll.contains(a,p,d));
    	Assert.assertFalse("unsound inference d", infd.contains(a,p,b));
    	Assert.assertTrue("missing inference 1", inf1.contains(a,q,b));
    	Assert.assertFalse("unsound inference 1", inf1.contains(a,p,c));
    	Assert.assertTrue("missing inference d12", infd12.contains(a,p,c));
    	Assert.assertFalse("unsound inference d12", infd12.contains(a,p,d));
    	Assert.assertTrue("missing inference d23", infd23.contains(b,p,d));
    	Assert.assertFalse("unsound inference d23", infd23.contains(a,p,d));
    	Assert.assertTrue("missing inference 123", inf123.contains(b,p,c));
    	Assert.assertFalse("unsound inference 123", inf123.contains(a,p,d));
    	Statement s = inf123.createStatement(p,RDF.type, OWL.TransitiveProperty);
    	inf123.add(s);
    	Assert.assertTrue("missing added statement in m123", m123.contains(s));
    	Assert.assertTrue("missing added statement in m1", m1.contains(s));
    	Assert.assertTrue("missing added statement in md12", md12.contains(s));
    	Assert.assertTrue("missing inference 123", inf123.contains(a,p,d));
    	inf1.remove(a,p,b);
    	Assert.assertFalse("unexpected statement in inf1", inf1.contains(a,p,b));
    	Assert.assertFalse("unexpected statement in m1", m1.contains(a,p,b));
    	Assert.assertTrue("missing statement in m1", m1.contains(s));
    	Assert.assertFalse("unexpected statement in infAll", m1.contains(a,p,d));
    	Assert.assertTrue("missing inference in infAll", infAll.contains(b,p,d));
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaReadTurtle() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn));
    	AGGraph g = closeLater( maker.getGraph());
    	AGModel m = closeLater( new AGModel(g));
    	
    	m.read(new FileInputStream("src/test/default-graph.ttl"), null, "TURTLE");
    	Assert.assertTrue("size of m", m.size()==4);
    }
    
}
