package com.knowledgereefsystems.agsail;

import org.openrdf.sail.SailConnection;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.Literal;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.EmptyBindingSet;
import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.Set;
import java.util.HashSet;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:39:39 PM
 */
public class TestTupleQueries extends AllegroSailTestCase {
    public TestTupleQueries(final String name) throws Exception {
        super(name);
    }

    public void testEvaluate() throws Exception {
        //loadTrig(AllegroSail.class.getResource("queryTest.trig"));
        loadTrig(new File("resources/com/knowledgereefsystems/agsail/queryTest.trig"));

        SailConnection sc = sail.getConnection();
        URI ctxA = sail.getValueFactory().createURI("http://example.org/ctxA");
        sc.addStatement(ctxA, ctxA, ctxA);
        sc.commit();

        SPARQLParser parser = new SPARQLParser();
        BindingSet bindings = new EmptyBindingSet();
        String baseURI = "http://example.org/bogus/";
        String queryStr;
        ParsedQuery query;
        CloseableIteration<? extends BindingSet, QueryEvaluationException> results;
        int count;

        // s ?p ?o SELECT
        queryStr = "SELECT ?y ?z WHERE { <http://example.org/ctxA> ?y ?z }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI y = (URI) set.getValue("y");
            Value z = set.getValue("z");
            assertNotNull(y);
            assertNotNull(z);
//System.out.println("y = " + y + ", z = " + z);
        }
        results.close();
        assertTrue(count > 0);

        // s p ?o SELECT using a namespace prefix
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "SELECT ?z WHERE { <http://knowledgereefsystems.com/agsail/test/thor> foaf:name ?z }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        Set<String> languages = new HashSet<String>();
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            Literal z = (Literal) set.getValue("z");
            assertNotNull(z);
            languages.add(z.getLanguage());
        }
        results.close();
        assertTrue(count > 0);
        assertEquals(2, languages.size());
        assertTrue(languages.contains("en"));
        assertTrue(languages.contains("se"));

        // ?s p o SELECT using a plain literal value with no language tag
        queryStr = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT ?s WHERE { ?s rdfs:comment \"he really knows where his towel is\" }";
        URI fordUri = sail.getValueFactory().createURI("http://knowledgereefsystems.com/agsail/test/ford");
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI s = (URI) set.getValue("s");
            assertNotNull(s);
            assertEquals(s, fordUri);
        }
        results.close();
        assertTrue(count > 0);

        // ?s p o SELECT using a language-specific literal value
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "SELECT ?s WHERE { ?s foaf:name \"Thor\"@en }";
        URI thorUri = sail.getValueFactory().createURI("http://knowledgereefsystems.com/agsail/test/thor");
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI s = (URI) set.getValue("s");
            assertNotNull(s);
            assertEquals(s, thorUri);
        }
        results.close();
        assertTrue(count > 0);
        // The language tag is necessary
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "SELECT ?s WHERE { ?s foaf:name \"Thor\" }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            results.next();
        }
        results.close();
        assertEquals(0, count);

        // ?s p o SELECT using a typed literal value
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                + "SELECT ?s WHERE { ?s foaf:msnChatID \"Thorster123\"^^xsd:string }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI s = (URI) set.getValue("s");
            assertNotNull(s);
            assertEquals(s, thorUri);
        }
        results.close();
        assertTrue(count > 0);
        // The data type is necessary
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                + "SELECT ?s WHERE { ?s foaf:msnChatID \"Thorster123\" }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            results.next();
        }
        results.close();
        assertEquals(0, count);

        // s ?p o SELECT
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                + "SELECT ?p WHERE { <http://knowledgereefsystems.com/agsail/test/thor> ?p \"Thor\"@en }";
        query = parser.parseQuery(queryStr, baseURI);
        URI foafNameUri = sail.getValueFactory().createURI("http://xmlns.com/foaf/0.1/name");
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI p = (URI) set.getValue("p");
            assertNotNull(p);
            assertEquals(p, foafNameUri);
        }
        results.close();
        assertTrue(count > 0);

        // context-specific SELECT
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "SELECT ?z\n"
                + "FROM <http://knowledgereefsystems.com/agsail/test/ctx1>\n"
                + "WHERE { <http://knowledgereefsystems.com/agsail/test/thor> foaf:name ?z }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        languages = new HashSet<String>();
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            Literal z = (Literal) set.getValue("z");
            assertNotNull(z);
            languages.add(z.getLanguage());
        }
        results.close();
        assertTrue(count > 0);
        assertEquals(2, languages.size());
        assertTrue(languages.contains("en"));
        assertTrue(languages.contains("se"));
        queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "SELECT ?z\n"
                + "FROM <http://example.org/emptycontext>\n"
                + "WHERE { <http://knowledgereefsystems.com/agsail/test/thor> foaf:name ?z }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            results.next();
        }
        results.close();
        assertEquals(0, count);

        // s p o? select without and with inferencing
        queryStr = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "SELECT ?o\n"
                + "WHERE { <http://knowledgereefsystems.com/agsail/test/instance1> rdf:type ?o }";
        query = parser.parseQuery(queryStr, baseURI);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, false);
        count = 0;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI o = (URI) set.getValue("o");
            assertEquals("http://knowledgereefsystems.com/agsail/test/classB", o.toString());
        }
        results.close();
        assertEquals(1, count);
        results = sc.evaluate(query.getTupleExpr(), query.getDataset(), bindings, true);
        count = 0;
        boolean foundA = false, foundB = false;
        while (results.hasNext()) {
            count++;
            BindingSet set = results.next();
            URI o = (URI) set.getValue("o");
            String s = o.toString();
            if (s.equals("http://knowledgereefsystems.com/agsail/test/classA")) {
                foundA = true;
            } else if (s.equals("http://knowledgereefsystems.com/agsail/test/classB")) {
                foundB = true;
            }
        }
        results.close();
        assertEquals(2, count);
        assertTrue(foundA);
        assertTrue(foundB);

        sc.close();
    }
}