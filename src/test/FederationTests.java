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
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGVirtualRepository;

public class FederationTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void federationBNodes() throws Exception {
    	BNode bnode = vf.createBNode();
    	conn.add(bnode, RDF.TYPE, vf.createURI("http://Foo"));
    	AGVirtualRepository fed = server.federate(repo,repo);
    	AGRepositoryConnection conn2 = fed.getConnection();
    	// Should be able to create BNodes for a federation
    	conn2.getValueFactory().createBNode();
    	conn2.getValueFactory().createBNode("foo");
    	try {
    		conn2.add(bnode, RDF.TYPE, vf.createURI("http://Boo"));
    		Assert.fail("expected can't write to federation.");
    	} catch (RepositoryException e) {
    		//expected
    	}
    	AGTupleQuery q = conn2.prepareTupleQuery(QueryLanguage.SPARQL, "select ?s {?s ?p ?o}");
    	TupleQueryResult result = q.evaluate();
    	Assert.assertTrue(result.hasNext());
    	BindingSet bind = result.next();
    	Assert.assertEquals(bnode.stringValue(), bind.getValue("s").stringValue());
    }

}
