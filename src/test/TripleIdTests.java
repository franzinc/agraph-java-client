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
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;

import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGTupleQuery;

public class TripleIdTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void tripleIds_rfe10177() throws Exception {
    	BNode bnode = vf.createBNode();
    	URI foo = vf.createURI("http://Foo");
    	conn.add(bnode, RDF.TYPE, foo);
    	conn.add(foo, foo, foo);
    	String queryString = "(select (?id) (q ?s !rdf:type ?o ?g ?id))";
    	AGTupleQuery q = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
    	TupleQueryResult r = q.evaluate();
    	Assert.assertTrue("Expected a result", r.hasNext());
    	Value id = r.next().getBinding("id").getValue();
    	Assert.assertFalse("Unexpected second result", r.hasNext());
    	RepositoryResult<Statement> stmts = conn.getStatements(id.stringValue());
    	Assert.assertTrue("Expected a match", stmts.hasNext());
    	Assert.assertEquals("Expected rdf:type", RDF.TYPE, stmts.next().getPredicate());
    	Assert.assertFalse("Unexpected second match", stmts.hasNext());
    }

}
