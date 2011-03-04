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
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryResult;

public class TripleIdTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void tripleIds_rfe10177() throws Exception {
    	BNode bnode = vf.createBNode();
    	conn.add(bnode, RDF.TYPE, vf.createURI("http://Foo"));
    	RepositoryResult<Statement> stmts = conn.getStatements("1");
    	Assert.assertTrue("Expected a match", stmts.hasNext());
    	stmts = conn.getStatements("2");
    	Assert.assertFalse("Unexpected match", stmts.hasNext());
    }

}
