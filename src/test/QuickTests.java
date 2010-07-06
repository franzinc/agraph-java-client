/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
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
import org.openrdf.repository.RepositoryResult;

import test.TestSuites.NonPrepushTest;

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
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;

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
    @Category(TestSuites.Broken.class)
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
        System.out.println(new Stmt(st));
        AGAbstractTest.assertSetsEqual("",
                Stmt.stmts(new Stmt(st)),
                Stmt.statementSet(conn.getStatements(s, st.getPredicate(), st.getObject(), false)));
        AGAbstractTest.assertSetsEqual("",
                Stmt.stmts(new Stmt(st)),
                Stmt.statementSet(conn.getStatements(st.getSubject(), st.getPredicate(), st.getObject(), false)));
    }

    /**
     * Simplified from tutorial example13 to show the error.
     * Example13 now has a workaround: setting the prefix in the sparql query.
     * Namespaces are cleared in setUp(), otherwise the first errors don't happen.
     * After the (expected) failure for xxx, setting the ont namespace
     * does not hold, so the query with ont fails.
     */
    @Test
    @Category(TestSuites.Broken.class)
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
            //e.printStackTrace();
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
        List input = new ArrayList<Statement>();
        input.add(vf.createStatement(alice, firstname, alicesName));
        input.add(vf.createStatement(alice, lastname, alicesName));
        conn.add(input);
        assertEquals("size", 2, conn.size());
        conn.remove(input);
        assertEquals("size", 0, conn.size());
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaReasoning() throws Exception {
        AGGraphMaker maker = new AGGraphMaker(conn);
        AGGraph graph = maker.getGraph();
        AGModel model = new AGModel(graph);
        try {
        	Resource bob = model.createResource("http://example.org/people/bob");
        	Resource dave = model.createResource("http://example.org/people/dave");
        	Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        	Property hasFather = model.createProperty("http://example.org/ontology/hasFather");
        	
        	model.add(hasFather, OWL.inverseOf, fatherOf);
        	model.add(bob, fatherOf, dave);
            
        	AGQuery query = AGQueryFactory.create("select * where { <http://example.org/people/dave> <http://example.org/ontology/hasFather> ?o . }");
        	{
                AGInfModel inf = new AGInfModel(new AGReasoner(), model);
                
            	StmtIterator stmts = inf.listStatements(dave, hasFather, bob);
    			Assert.assertTrue("with reasoning", stmts.hasNext());
    			stmts.close();
    			
        		AGQueryExecution exe = AGQueryExecutionFactory.create(query, inf);
        		try {
        			ResultSet results = exe.execSelect();
        			Assert.assertTrue("with reasoning", results.hasNext());
        		} finally {
                	Util.close(exe);
                	Util.close(inf);
        		}
        	}
        	{
        		StmtIterator stmts = model.listStatements(dave, hasFather, bob);
    			Assert.assertFalse("without reasoning", stmts.hasNext());
    			stmts.close();
    			
    			AGQueryExecution exe = AGQueryExecutionFactory.create(query, model);
        		try {
        			ResultSet results = exe.execSelect();
        			Assert.assertFalse("without reasoning", results.hasNext());
        		} finally {
                	Util.close(exe);
        		}
        	}
        } finally {
        	Util.close(model);
        	Util.close(graph);
        	Util.close(maker);
        }
    }

}
