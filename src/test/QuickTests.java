/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.Stmt.statementSet;
import static test.Stmt.stmts;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Category;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import test.TestSuites.NonPrepushTest;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

public class QuickTests extends AGAbstractTest {

    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( { QuickTests.class })
    public static class Prepush {}

    @RunWith(Categories.class)
    @IncludeCategory(TestSuites.Broken.class)
    @SuiteClasses( { QuickTests.class })
    public static class Broken {}

    public static final String NS = "http://franz.com/test/";

    @Test
    @Category(TestSuites.Prepush.class)
    public void bnode() throws Exception {
        assertEquals("size", 0, conn.size());
        BNode s = vf.createBNode();
        URI p = vf.createURI(NS, "a");
        Literal o = vf.createLiteral("aaa");
        conn.add(s, p, o);
        assertEquals("size", 1, conn.size());
        assertSetsEqual("a", stmts(new Stmt(null, p, o)),
                Stmt.dropSubjects(statementSet(conn.getStatements(s, p, o, true))));
        RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false);
        Statement st = statements.next();
        AGAbstractTest.assertSetsEqual("",
                Stmt.stmts(new Stmt(st)),
                Stmt.statementSet(conn.getStatements(s, st.getPredicate(), st.getObject(), false)));
        AGAbstractTest.assertSetsEqual("",
                Stmt.stmts(new Stmt(st)),
                Stmt.statementSet(conn.getStatements(st.getSubject(), st.getPredicate(), st.getObject(), false)));
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void bnode_rfe9776() throws Exception {
    	URI orderedCollection = vf.createURI("http://lumas#orderedCollection");
    	URI property = vf.createURI("http://lumas#hasId");
    	BNode node = vf.createBNode("newId");
    	
    	conn.getHttpRepoClient().setAllowExternalBlankNodeIds(true);
    	conn.add(orderedCollection, property, node);
    	
    	RepositoryResult<Statement> result = closeLater( conn.getStatements(null, null, null, false));
    	BNode bnode = null;
    	if (result.hasNext()) {
    		Statement st = result.next();
    		bnode = (BNode) st.getObject();
    		Assert.assertNotNull(st);
    	} else {
    		fail("expected 1 result");
    	}
    	Assert.assertNotNull(bnode);
    	
    	// load triple from blank-node
    	result = closeLater( conn.getStatements(null, null, bnode, false));
    	if (result.hasNext()){
    		Statement st = result.next();
    		Assert.assertNotNull(st);
    	} else {
    		fail("no triple found!");
    	}
    }

    /**
     * Namespaces are cleared in setUp(), otherwise the first errors don't happen.
     * After the (expected) failure for xxx, ensure setting the ont namespace
     * holds; this test used to fail, but now passes, adding to Prepush.
     */
    @Test
    @Category(TestSuites.Prepush.class)
    public void namespaceAfterError() throws Exception {
        URI alice = vf.createURI("http://example.org/people/alice");
        URI name = vf.createURI("http://example.org/ontology/name");
        Literal alicesName = vf.createLiteral("Alice");
        conn.add(alice, name, alicesName);
        try {
            conn.prepareBooleanQuery(QueryLanguage.SPARQL,
            "ask { ?s xxx:name \"Alice\" } ").evaluate();
            fail("");
        } catch (Exception e) {
            // expected
        }
        conn.setNamespace("ont", "http://example.org/ontology/");
        assertTrue("Boolean result",
                conn.prepareBooleanQuery(QueryLanguage.SPARQL,
                "ask { ?s ont:name \"Alice\" } ").evaluate());
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void bulkDelete() throws Exception {
        URI alice = vf.createURI("http://example.org/people/alice");
        URI firstname = vf.createURI("http://example.org/ontology/firstname");
        URI lastname = vf.createURI("http://example.org/ontology/lastname");
        Literal alicesName = vf.createLiteral("Alice");
        List<Statement> input = new ArrayList<Statement>();
        input.add(vf.createStatement(alice, firstname, alicesName));
        input.add(vf.createStatement(alice, lastname, alicesName));
        conn.add(input);
        assertEquals("size", 2, conn.size());
        conn.remove(input);
        assertEquals("size", 0, conn.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void openRepo_rfe9837() throws Exception {
    	AGRepository repo2 = closeLater( cat.openRepository(repo.getRepositoryID()));
    	AGRepositoryConnection conn2 = closeLater( repo2.getConnection());
    	assertEquals("size", conn.size(), conn2.size());
    	assertEquals("id", repo.getRepositoryID(), repo2.getRepositoryID());
    	try {
    		AGRepository repo3 = closeLater( cat.createRepository(repo.getRepositoryID(), true));
    		fail("strict should cause an exception: " + repo3.getRepositoryURL());
    	} catch (RepositoryException e) {
    		if (e.getMessage().contains("There is already a repository")) {
    			// good
    		} else {
    			throw e;
    		}
    	}
    	try {
    		AGRepository repo3 = closeLater( cat.openRepository("no-such-repo"));
    		fail("should not exist: " + repo3.getRepositoryURL());
    	} catch (RepositoryException e) {
    		if (e.getMessage().contains("Repository not found with ID:")) {
    			// good
    		} else {
    			throw e;
    		}
    	}
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void serverUrlTrailingSlashRemoved() throws Exception {
    	String serverURL = server.getServerURL();
    	AGServer server2 = new AGServer(serverURL+"/",username(),password());
    	Assert.assertEquals(serverURL, server2.getServerURL());
    }
    
}
