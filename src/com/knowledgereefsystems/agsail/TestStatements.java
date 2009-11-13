package com.knowledgereefsystems.agsail;

import java.io.File;

import org.openrdf.sail.SailConnection;
import org.openrdf.model.URI;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.model.vocabulary.RDF;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:39:39 PM
 */
public class TestStatements extends AllegroSailTestCase {
    public TestStatements(final String name) throws Exception {
        super(name);
    }

    public void testAddStatement() throws Exception {
        SailConnection sc = sail.getConnection();
        URI ctxA = sail.getValueFactory().createURI("http://example.org/ctxA");

        int before, after;

        // Add statement to a specific context.
        sc.removeStatements(null, null, null, ctxA);
        before = count(sc.getStatements(null, null, null, false, ctxA));
        sc.addStatement(ctxA, ctxA, ctxA, ctxA);
        after = count(sc.getStatements(null, null, null, false, ctxA));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add statement to the implicit null context.
        sc.removeStatements(ctxA, ctxA, ctxA);
        before = count(sc.getStatements(ctxA, ctxA, ctxA, false));
        sc.addStatement(ctxA, ctxA, ctxA);
        after = count(sc.getStatements(ctxA, ctxA, ctxA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add statement to the explicit null context.
        sc.removeStatements(ctxA, ctxA, ctxA);
        before = count(sc.getStatements(ctxA, ctxA, ctxA, false));
        sc.addStatement(ctxA, ctxA, ctxA, (Resource) null);
        after = count(sc.getStatements(ctxA, ctxA, ctxA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add statement to both a named context and the null context.
        Resource[] contexts = {ctxA, null};
        sc.removeStatements(ctxA, ctxA, ctxA);
        before = count(sc.getStatements(ctxA, ctxA, ctxA, false));
        sc.addStatement(ctxA, ctxA, ctxA, contexts);
        after = count(sc.getStatements(ctxA, ctxA, ctxA, false));
        assertEquals(0, before);
        assertEquals(2, after);

        sc.close();
    }

    public void testGetStatements() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/testGetStatements#a");
        boolean includeInferred = false;
        int count;

        sc.removeStatements(null, null, null);
        Resource[] contexts = {uriA, null};
        sc.addStatement(uriA, uriA, uriA, contexts);

        // Get statements from all contexts.
        count = count(
                sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(2, count);

        // Get statements from a specific named context.
        count = count(
                sc.getStatements(null, null, null, includeInferred, uriA));
        assertEquals(1, count);

        // Get statements from the null context.
        Resource[] c = {null};
        count = count(
                sc.getStatements(null, null, null, includeInferred, c));
        assertTrue(count > 0);
        int countLast = count;

        // Get statements from more than one context.
        count = count(
                sc.getStatements(null, null, null, includeInferred, contexts));
        assertEquals(1 + countLast, count);

        // Test inference
        //loadTrig(TestStatements.class.getResource("queryTest.trig"));
        loadTrig(new File("resources/com/knowledgereefsystems/agsail/queryTest.trig"));
        
        URI instance1 = sail.getValueFactory().createURI("http://knowledgereefsystems.com/agsail/test/instance1");
        count = count(
                sc.getStatements(instance1, RDF.TYPE, null, false));
        assertEquals(1, count);
        count = count(
                sc.getStatements(instance1, RDF.TYPE, null, true));
        assertEquals(2, count);
    }

    public void testGetStatementsWithVariableContexts() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/uriA");
        boolean includeInferred = false;
        int count;

        sc.removeStatements(uriA, uriA, uriA);
        sc.commit();
        Resource[] contexts = {uriA, null};
        sc.addStatement(uriA, uriA, uriA, contexts);
        sc.commit();

        // Get statements from all contexts.
        count = count(
                sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(2, count);

        // Get statements from a specific named context.
        count = count(
                sc.getStatements(null, null, null, includeInferred, uriA));
        assertEquals(1, count);

        // Get statements from the null context.
        Resource[] c = {null};
        count = count(
                sc.getStatements(null, null, null, includeInferred, c));
        assertTrue(count > 0);
        int countLast = count;

        // Get statements from more than one context.
        count = count(
                sc.getStatements(null, null, null, includeInferred, contexts));
        assertEquals(1 + countLast, count);

        sc.close();
    }

    /* Disabled -- getContextIDs is thus far not implementable
    public void testGetContextIDs() throws Exception {
		SailConnection sc = sail.getConnection();

        URI ctxX = sail.getValueFactory().createURI("http://example.org/ctxX");
        sc.removeStatements(null, null, null, ctxX);

        Set<Resource> before = toSet(sc.getContextIDs());
        sc.addStatement(ctxX, ctxX, ctxX, ctxX);
        Set<Resource> during = toSet(sc.getContextIDs());
        sc.removeStatements(null, null, null, ctxX);
        Set<Resource> after = toSet(sc.getContextIDs());

        assertEquals(0, before.size());
        assertEquals(1, during.size());
        assertEquals(0, after.size());

        sc.close();
    }
    //*/

    public void testSize() throws Exception {
        URI uriA = sail.getValueFactory().createURI("http://example.org/testSize#a");

        SailConnection sc = sail.getConnection();
        sc.removeStatements(null, null, null, uriA);

        assertEquals(0, sc.size());
        sc.addStatement(uriA, uriA, uriA, uriA);
        sc.commit();
        assertEquals(1, sc.size());
        sc.removeStatements(uriA, uriA, uriA, uriA);
        sc.commit();
        assertEquals(0, sc.size());

        // TODO: size() using multiple statements, specific contexts

        sc.close();
    }

    public void testRemoveStatements() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/uriA");
        Resource[] contexts = {uriA, null};
        boolean includeInferred = false;
        int count;

        // Remove from all contexts.
        sc.removeStatements(uriA, null, null);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, count);
        sc.addStatement(uriA, uriA, uriA, contexts);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(2, count);
        sc.removeStatements(uriA, null, null);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, count);

        // Remove from one named context.
        sc.removeStatements(uriA, null, null);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, count);
        sc.addStatement(uriA, uriA, uriA, contexts);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(2, count);
        Resource[] oneContext = {uriA};
        sc.removeStatements(uriA, null, null, oneContext);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(1, count);

        // Remove from the null context.
        sc.removeStatements(uriA, null, null);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, count);
        sc.addStatement(uriA, uriA, uriA, contexts);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(2, count);
        Resource[] nullContext = {null};
        sc.removeStatements(uriA, null, null, nullContext);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(1, count);

        // Remove from more than one context.
        sc.removeStatements(uriA, null, null);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, count);
        sc.addStatement(uriA, uriA, uriA, contexts);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(2, count);
        sc.removeStatements(uriA, null, null);
        sc.commit();
        count = count(sc.getStatements(uriA, null, null, includeInferred, contexts));
        assertEquals(0, count);

        sc.close();
    }

    // specific triple patterns ////////////////////////////////////////////////

    public void testGetStatementsS_POG() throws Exception {
        boolean includeInferred = false;

        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/test/S_POG#a");
        URI uriB = sail.getValueFactory().createURI("http://example.org/test/S_POG#b");
        URI uriC = sail.getValueFactory().createURI("http://example.org/test/S_POG#c");
        URI uriD = sail.getValueFactory().createURI("http://example.org/test/S_POG#d");
        int before, after;

        // default context, different S,P,O
        sc.removeStatements(uriA, null, null);
        sc.commit();
        before = count(sc.getStatements(uriA, null, null, includeInferred));
        sc.addStatement(uriA, uriB, uriC);
        sc.commit();
        after = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, before);
        assertEquals(1, after);

        // one specific context, different S,P,O
        sc.removeStatements(uriA, null, null, uriD);
        sc.commit();
        before = count(sc.getStatements(uriA, null, null, includeInferred, uriD));
        sc.addStatement(uriA, uriB, uriC, uriD);
        sc.commit();
        after = count(sc.getStatements(uriA, null, null, includeInferred, uriD));
        assertEquals(0, before);
        assertEquals(1, after);

        // one specific context, same S,P,O,G
        sc.removeStatements(uriA, null, null, uriA);
        sc.commit();
        before = count(sc.getStatements(uriA, null, null, includeInferred, uriA));
        sc.addStatement(uriA, uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(uriA, null, null, includeInferred, uriA));
        assertEquals(0, before);
        assertEquals(1, after);

        // default context, same S,P,O
        sc.removeStatements(uriA, null, null);
        sc.commit();
        before = count(sc.getStatements(uriA, null, null, includeInferred));
        sc.addStatement(uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(uriA, null, null, includeInferred));
        assertEquals(0, before);
        assertEquals(1, after);

        sc.close();
    }

    public void testGetStatementsSP_OG() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/test/SP_OG");
        int before, after;

        // Add statement to the implicit null context.
//        sc.removeStatements(null, null, null, uriA);
        before = count(sc.getStatements(uriA, uriA, null, false));
        sc.addStatement(uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(uriA, uriA, null, false));
        assertEquals(0, before);
        assertEquals(1, after);

        sc.close();
    }

    public void testGetStatementsO_SPG() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/test/O_SPG");
        Literal plainLitA = sail.getValueFactory().createLiteral("arbitrary plain literal 9548734867");
        Literal stringLitA = sail.getValueFactory().createLiteral("arbitrary string literal 8765", XMLSchema.STRING);
        int before, after;

        // Add statement to a specific context.
        sc.removeStatements(null, null, uriA, uriA);
        sc.commit();
        before = count(sc.getStatements(null, null, uriA, false));
        sc.addStatement(uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(null, null, uriA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add plain literal statement to the default context.
        sc.removeStatements(null, null, plainLitA);
        sc.commit();
        before = count(sc.getStatements(null, null, plainLitA, false));
        sc.addStatement(uriA, uriA, plainLitA);
        sc.commit();
        after = count(sc.getStatements(null, null, plainLitA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add string-typed literal statement to the default context.
        sc.removeStatements(null, null, plainLitA);
        sc.commit();
        before = count(sc.getStatements(null, null, stringLitA, false));
        sc.addStatement(uriA, uriA, stringLitA);
        sc.commit();
        after = count(sc.getStatements(null, null, stringLitA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        sc.close();
    }

    public void testGetStatementsPO_SG() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/test/PO_SG#a");
        URI uriB = sail.getValueFactory().createURI("http://example.org/test/PO_SG#b");
        URI marko = sail.getValueFactory().createURI("http://knowledgereefsystems.com/thing/q");
        URI firstName = sail.getValueFactory().createURI("http://knowledgereefsystems.com/2007/11/core#firstName");
        Literal plainLitA = sail.getValueFactory().createLiteral("arbitrary plain literal 8765675");
        Literal markoName = sail.getValueFactory().createLiteral("Marko", XMLSchema.STRING);
        int before, after;

        // Add statement to the implicit null context.
        sc.removeStatements(null, null, null, uriA);
        sc.commit();
        before = count(sc.getStatements(null, uriA, uriA, false));
        sc.addStatement(uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(null, uriA, uriA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add plain literal statement to the default context.
        sc.removeStatements(null, null, plainLitA);
        sc.commit();
        before = count(sc.getStatements(null, uriA, plainLitA, false));
        sc.addStatement(uriA, uriA, plainLitA);
        sc.addStatement(uriA, uriB, plainLitA);
        sc.addStatement(uriB, uriB, plainLitA);
        sc.commit();
        after = count(sc.getStatements(null, uriA, plainLitA, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add string-typed literal statement to the default context.
        sc.removeStatements(null, null, markoName);
        sc.commit();
        before = count(sc.getStatements(null, firstName, markoName, false));
        sc.addStatement(marko, firstName, markoName);
        sc.commit();
        after = count(sc.getStatements(null, firstName, markoName, false));
        assertEquals(0, before);
        assertEquals(1, after);
        assertEquals(marko, toSet(sc.getStatements(null, firstName, markoName, false)).iterator().next().getSubject());

        sc.close();
    }

    public void testGetStatementsSPO_G() throws Exception {
        boolean includeInferred = false;

        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/test/S_POG#a");
        URI uriB = sail.getValueFactory().createURI("http://example.org/test/S_POG#b");
        URI uriC = sail.getValueFactory().createURI("http://example.org/test/S_POG#c");
        URI uriD = sail.getValueFactory().createURI("http://example.org/test/S_POG#d");
        int before, after;

        // default context, different S,P,O
        sc.removeStatements(uriA, null, null);
        sc.commit();
        before = count(sc.getStatements(uriA, uriB, uriC, includeInferred));
        sc.addStatement(uriA, uriB, uriC);
        sc.commit();
        after = count(sc.getStatements(uriA, uriB, uriC, includeInferred));
        assertEquals(0, before);
        assertEquals(1, after);

        // default context, same S,P,O
        sc.removeStatements(uriA, null, null);
        sc.commit();
        before = count(sc.getStatements(uriA, uriA, uriA, includeInferred));
        sc.addStatement(uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(uriA, uriA, uriA, includeInferred));
        assertEquals(0, before);
        assertEquals(1, after);

        // one specific context, different S,P,O
        sc.removeStatements(uriA, null, null, uriD);
        sc.commit();
        before = count(sc.getStatements(uriA, uriB, uriC, includeInferred, uriD));
        sc.addStatement(uriA, uriB, uriC, uriD);
        sc.commit();
        after = count(sc.getStatements(uriA, uriB, uriC, includeInferred, uriD));
        assertEquals(0, before);
        assertEquals(1, after);

        // one specific context, same S,P,O,G
        sc.removeStatements(uriA, null, null, uriA);
        sc.commit();
        before = count(sc.getStatements(uriA, uriA, uriA, includeInferred, uriA));
        sc.addStatement(uriA, uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(uriA, uriA, uriA, includeInferred, uriA));
        assertEquals(0, before);
        assertEquals(1, after);

        sc.close();
    }

    public void testGetStatementsP_SOG() throws Exception {
        SailConnection sc = sail.getConnection();
        URI uriA = sail.getValueFactory().createURI("http://example.org/test/P_SOG#a");
        URI uriB = sail.getValueFactory().createURI("http://example.org/test/P_SOG#b");
        URI uriC = sail.getValueFactory().createURI("http://example.org/test/P_SOG#c");
        URI marko = sail.getValueFactory().createURI("http://knowledgereefsystems.com/thing/q");
        URI firstName = sail.getValueFactory().createURI("http://knowledgereefsystems.com/2007/11/core#firstName");
        Literal plainLitA = sail.getValueFactory().createLiteral("arbitrary plain literal 238445");
        Literal markoName = sail.getValueFactory().createLiteral("Marko", XMLSchema.STRING);
        int before, after;

        // Add statement to the implicit null context.
        sc.removeStatements(null, uriA, null);
        sc.commit();
        before = count(sc.getStatements(null, uriA, null, false));
        sc.addStatement(uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(null, uriA, null, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add plain literal statement to the default context.
        sc.removeStatements(null, uriA, null);
        sc.commit();
        before = count(sc.getStatements(null, uriA, null, false));
        sc.addStatement(uriA, uriA, plainLitA);
        sc.addStatement(uriA, uriB, plainLitA);
        sc.addStatement(uriB, uriB, plainLitA);
        sc.commit();
        after = count(sc.getStatements(null, uriA, null, false));
        assertEquals(0, before);
        assertEquals(1, after);

        // Add string-typed literal statement to the default context.
        sc.removeStatements(null, firstName, null);
        sc.commit();
        before = count(sc.getStatements(null, firstName, null, false));
        sc.addStatement(marko, firstName, markoName);
        sc.commit();
        after = count(sc.getStatements(null, firstName, null, false));
        assertEquals(0, before);
        assertEquals(1, after);
        assertEquals(marko, toSet(sc.getStatements(null, firstName, null, false)).iterator().next().getSubject());

        // Add statement to a non-null context.
        sc.removeStatements(null, uriA, null);
        sc.commit();
        before = count(sc.getStatements(null, uriA, null, false));
        sc.addStatement(uriA, uriA, uriA, uriA);
        sc.commit();
        after = count(sc.getStatements(null, uriA, null, false));
        assertEquals(0, before);
        assertEquals(1, after);

        sc.removeStatements(null, uriA, null);
        sc.commit();
        before = count(sc.getStatements(null, uriA, null, false));
        sc.addStatement(uriB, uriA, uriC, uriC);
        sc.addStatement(uriC, uriA, uriA, uriA);
        sc.commit();
        sc.addStatement(uriA, uriA, uriB, uriB);
        sc.commit();
        after = count(sc.getStatements(null, uriA, null, false));
        assertEquals(0, before);
        assertEquals(3, after);

        sc.close();
    }
}
