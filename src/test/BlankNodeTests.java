/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class BlankNodeTests extends AGAbstractTest {

	@Test
    @Category(TestSuites.Prepush.class)
	public void jenaExternalBlankNodeRoundTrips_spr38494() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.createGraph("http://aldi1.com.au") );
    	AGModel model1 = closeLater( new AGModel(graph) );

    	Resource bnode = ResourceFactory.createResource();
    	Resource bnode2 = model1.createResource(AnonId.create("ex"));
    	model1.begin();
    	model1.removeAll();
   		model1.add(bnode, RDF.type, bnode2);
    	model1.commit();
    	Assert.assertEquals(1, model1.size());
    	Assert.assertTrue(model1.contains(bnode, RDF.type, bnode2));
	}

	@Test
    @Category(TestSuites.Prepush.class)
	public void jenaModelBlankNodeRoundTrips_spr38494() throws Exception {
		AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.createGraph("http://aldi1.com.au") );
		AGModel model1 = closeLater( new AGModel(graph) );
		
    	Resource bnode = model1.createResource();
    	model1.begin();
    	model1.removeAll();
    	model1.add(bnode, RDF.type, OWL.Thing);
    	model1.commit();
    	Assert.assertEquals(1, model1.size());
    	Assert.assertTrue(model1.contains(bnode, RDF.type, OWL.Thing));
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void sesameExternalBlankNodeRoundTrips_spr38494() throws Exception {
		BNode bnode = ValueFactoryImpl.getInstance().createBNode();
		BNode bnode2 = vf.createBNode("external");
		URI p = vf.createURI("http://p");
		conn.clear();
    	try {
    		conn.add(bnode, p, bnode2, p);
    		Assert.fail("Expected an exception: can't add external blank nodes");
    	} catch (IllegalArgumentException e) {
    		// expected
    	}
    	conn.getHttpRepoClient().setAllowExternalBlankNodeIds(true);
    	conn.add(bnode, p, bnode, p);
		Assert.assertEquals(1, conn.size());
		Assert.assertTrue(conn.hasStatement(bnode, p, bnode, false, p));
		RepositoryResult<Statement> results = conn.getStatements(bnode, p, bnode, false, p);
		Assert.assertTrue(results.hasNext());
		Statement st = results.next();
		Assert.assertEquals(bnode, st.getSubject());
		Assert.assertEquals(bnode, st.getObject());
		GraphQueryResult results2 = conn.prepareGraphQuery(QueryLanguage.SPARQL, "construct {?s <http://foo> ?o} where {?s ?p ?o}").evaluate();
		Assert.assertTrue(results2.hasNext());
		st = results2.next();
		Assert.assertEquals(bnode, st.getSubject());
		Assert.assertEquals(bnode, st.getObject());
		TupleQueryResult results3 = conn.prepareTupleQuery(QueryLanguage.SPARQL, "select * where {?s ?p ?o}").evaluate();
		Assert.assertTrue(results3.hasNext());
		BindingSet result = results3.next();
		Assert.assertEquals(bnode, result.getValue("s"));
		Assert.assertEquals(bnode, result.getValue("o"));
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void sesameAGBlankNodeRoundTrips_spr38494() throws Exception {
		BNode bnode = vf.createBNode();
		URI p = vf.createURI("http://p");
		conn.clear();
		conn.add(bnode, p, bnode, p);
		Assert.assertEquals(1, conn.size());
		Assert.assertTrue(conn.hasStatement(bnode, p, bnode, false, p));
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void blankNodesPerRequest() throws Exception {
		vf.setBlankNodesPerRequest(vf.getBlankNodesPerRequest()*10);
		vf.createBNode();
		Assert.assertEquals(vf.getBlankNodesPerRequest(), vf.getBlankNodeIds().length);
	}
}
