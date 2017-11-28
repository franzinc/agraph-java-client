/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.tutorial.sesame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.*;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.DatasetImpl;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriter;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGFreetextIndexConfig;
import com.franz.agraph.repository.AGFreetextQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGValueFactory;

public class TutorialExamples {
    private static final String HOST = getenv("AGRAPH_HOST", "localhost");
    private static final String PORT = getenv("AGRAPH_PORT", "10035");
    
    private static final String SERVER_URL = "http://" + HOST + ":" + PORT;
    private static final String CATALOG_ID = "java-catalog";
    private static final String REPOSITORY_ID = "javatutorial";
    private static final String USERNAME = getenv("AGRAPH_USER", "test");
    private static final String PASSWORD = getenv("AGRAPH_PASS", "xyzzy");
    private static final File DATA_DIRECTORY =
        new File(getenv("AGRAPH_DATA", "../data/"));
    private static final String TEMPORARY_DIRECTORY = getenv("AGRAPH_TEMP", "");

    private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

    /**
     * Gets the value of an environment variable.
     * @param name Name of the variable.
     * @param defaultValue Value to be returned if the varaible is not defined.
     * @return Value.
     */
    private static String getenv(final String name, final String defaultValue) {
    final String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Creating a Repository test
     */
    public static AGRepositoryConnection example1(boolean close)
            throws Exception {
        // Tests getting the repository up.
        println("\nStarting example1().");
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        try {
            println("Server version: " + server.getVersion());
            println("Server build date: " + server.getBuildDate());
            println("Server revision: " + server.getRevision());
            println("Available catalogs: " + server.listCatalogs());
        } catch (Exception e) {
            throw new Exception("Got error when attempting to connect to server at "
                                + SERVER_URL + ": " + e);
        }

        AGCatalog catalog = server.getCatalog(CATALOG_ID); // open catalog

        if (catalog == null) {
            throw new Exception("Catalog " + CATALOG_ID + " does not exist. Either "
                            + "define this catalog in your agraph.cfg or modify the CATALOG_ID "
                            + "in this tutorial to name an existing catalog.");
        }

        println("Available repositories in catalog " + 
                (catalog.getCatalogName()) + ": " + 
                catalog.listRepositories());
        closeAll();
        catalog.deleteRepository(REPOSITORY_ID);
        AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
        println("Got a repository.");
        myRepository.initialize();
        println("Initialized repository.");
        println("Repository is writable? " + myRepository.isWritable());
        AGRepositoryConnection conn = myRepository.getConnection();
        closeBeforeExit(conn);
        println("Got a connection.");
        println("Repository " + (myRepository.getRepositoryID()) +
                " is up! It contains " + (conn.size()) +
                " statements."              
                );
        List<String> indices = conn.listValidIndices();
        println("All valid triple indices: " + indices);
        indices = conn.listIndices();
        println("Current triple indices: " + indices);
        println("Removing graph indices...");
        conn.dropIndex("gospi");
        conn.dropIndex("gposi");
        conn.dropIndex("gspoi");
        indices = conn.listIndices();
        println("Current triple indices: " + indices);
        println("Adding one graph index back in...");
        conn.addIndex("gspoi");
        indices = conn.listIndices();
        println("Current triple indices: " + indices);
        if (close) {
            // tidy up
            conn.close();
            myRepository.shutDown();
            server.close();
            return null;
        }
        return conn;
    }
    
    /**
     * Asserting and Retracting Triples
     */
    public static AGRepositoryConnection example2(boolean close) throws Exception {
        // Asserts some statements and counts them.
        AGRepositoryConnection conn = example1(false);
        AGValueFactory vf = conn.getRepository().getValueFactory();
        println("\nStarting example2().");
        // Create some resources and literals to make statements from.
        IRI alice = vf.createIRI("http://example.org/people/alice");
        IRI bob = vf.createIRI("http://example.org/people/bob");
        IRI name = vf.createIRI("http://example.org/ontology/name");
        IRI person = vf.createIRI("http://example.org/ontology/Person");
        Literal bobsName = vf.createLiteral("Bob");
        Literal alicesName = vf.createLiteral("Alice");
        println("Triple count before inserts: " + 
                (conn.size()));
        // Alice's name is "Alice"
        conn.add(alice, name, alicesName);
        // Alice is a person
        conn.add(alice, RDF.TYPE, person);
        //Bob's name is "Bob"
        conn.add(bob, name, bobsName);
        //Bob is a person, too. 
        conn.add(bob, RDF.TYPE, person);
        println("Added four triples.");
        println("Triple count after inserts: " + 
                (conn.size()));
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
        while (result.hasNext()) {
            Statement st = result.next();
            println(st);
        }
        conn.remove(bob, name, bobsName);
        println("Removed one triple.");
        println("Triple count after deletion: " + 
                (conn.size()));
        // put it back so we can continue with other examples
        conn.add(bob, name, bobsName);
        if (close) {
            conn.close();
            conn.getRepository().shutDown();
            return null;
        }
        return conn;
    }
    
    /**
     * A SPARQL Query
     */
    public static void example3() throws Exception {
        AGRepositoryConnection conn = example2(false);
        println("\nStarting example3().");
       try {
            String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
            AGTupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result = tupleQuery.evaluate();
            try {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Value s = bindingSet.getValue("s");
                    Value p = bindingSet.getValue("p");
                    Value o = bindingSet.getValue("o");
                    System.out.format("%s %s %s\n", s, p, o);
                }
            } finally {
                result.close();
            }
            // Just the count now.  The count is done server-side,
            // and only the count is returned.
            long count = tupleQuery.count();
            println("count: " + count);
        } finally {
            conn.close();
        }
    }

    /**
     * A SPARQL Query, streaming results.
     * 
     * The handleSolution method is called while the http response
     * stream is read instead of reading all results into a list.
     * This is appropriate for processing large result sets that
     * do not fit in memory, and may also improve performance.
     * The bindingSets should not be retained beyond the handleSolution
     * method, or they will consume memory.
     */
    public static void example3a() throws Exception {
        AGRepositoryConnection conn = example2(false);
        println("\nStarting example3a().");
        try {
            String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            tupleQuery.evaluate(new TupleQueryResultHandler() {
                    
                    @Override
                        public void startQueryResult(List<String> bindingNames) {
                        System.out.format("Bindings: %s\n", bindingNames);
                    }
                    
                    @Override
                        public void handleSolution(BindingSet bindingSet) {
                        Value s = bindingSet.getValue("s");
                        Value p = bindingSet.getValue("p");
                        Value o = bindingSet.getValue("o");
                        System.out.format("%s %s %s\n", s, p, o);
                    }
                    
                    @Override
                        public void endQueryResult() {
                    }

        			@Override
        			public void handleBoolean(boolean arg0)
        					throws QueryResultHandlerException {
        			}

        			@Override
        			public void handleLinks(List<String> arg0)
        					throws QueryResultHandlerException {
        			}
                });
        } finally {
            conn.close();
        }
    }

    /**
     * Statement Matching
     */
    public static void example4() throws Exception {
        RepositoryConnection conn = example2(false);
        closeBeforeExit(conn);
        Repository myRepository = conn.getRepository();
        println("\nStarting example4().");
        IRI alice = myRepository.getValueFactory().createIRI("http://example.org/people/alice");
        RepositoryResult<Statement> statements = conn.getStatements(alice, null, null, false);
        try {
            statements.enableDuplicateFilter();
            while (statements.hasNext()) {
                println(statements.next());
            }
        } finally {
            statements.close();
            myRepository.shutDown();
        }
    }

    /**
     * Literal Values
     */
    public static void example5() throws Exception {
        RepositoryConnection conn = example2(false);
        Repository myRepository = conn.getRepository();
        ValueFactory f = myRepository.getValueFactory();
        println("\nStarting example5().");
        conn.clear();
        String exns = "http://people/";
        IRI alice = f.createIRI("http://people/alice");
        IRI bob = f.createIRI("http://people/bob");
        IRI carol = f.createIRI("http://people/carol");
        IRI dave = f.createIRI("http://people/dave");
        IRI eric = f.createIRI("http://people/eric");
        IRI fred = f.createIRI("http://people/fred");
        IRI greg = f.createIRI("http://people/greg");
        IRI age = f.createIRI(exns, "age");
        // Automatic typing of numbers
        Literal fortyTwo = f.createLiteral(42);          // creates int
        Literal fortyTwoDouble = f.createLiteral(42.0);  // creates double
        Literal fortyTwoInt = f.createLiteral("42", XMLSchema.INT);
        Literal fortyTwoLong = f.createLiteral("42", XMLSchema.LONG);
        Literal fortyTwoFloat = f.createLiteral("42", XMLSchema.FLOAT);
        Literal fortyTwoString = f.createLiteral("42", XMLSchema.STRING); 
        Literal fortyTwoPlain = f.createLiteral("42");   // creates plain literal
        Statement stmt1 = f.createStatement(alice, age, fortyTwo);
        Statement stmt2 = f.createStatement(bob, age, fortyTwoDouble);
        Statement stmt3 = f.createStatement(carol, age, fortyTwoInt);
        Statement stmt4 = f.createStatement(dave, age, fortyTwoLong);
        Statement stmt5 = f.createStatement(eric, age, fortyTwoFloat);
        Statement stmt6 = f.createStatement(fred, age, fortyTwoString);
        Statement stmt7 = f.createStatement(greg, age, fortyTwoPlain);
        conn.add(stmt1);
        conn.add(stmt2);
        conn.add(stmt3);
        conn.add(stmt4);
        conn.add(stmt5);
        conn.add(stmt6);
        conn.add(stmt7);

        // This section retrieves the age triples to see what datatypes are present. 
        {
            println("\nShowing all age triples using getStatements().  Seven matches.");
            RepositoryResult<Statement> statements = conn.getStatements(null, age, null, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        println("\ngetStatements() request for 42, errors out. Not a Value.");
        /* getStatements() won't accept 42 as a value.
        {
            println("\ngetStatements() request for 42, matches ints.");
            RepositoryResult<Statement> statements = conn.getStatements(null, age, 42, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
*/
        println( "\nSPARQL matches for 42 (filter match) finds multiple numeric types.");
        String queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = 42)}";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }

        println( "\nSPARQL matches for 42 (direct match). No results.");
        queryString = "SELECT ?s ?p WHERE {?s ?p 42 .}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        
        println("\ngetStatements() request for 42.0, errors out. Not a Value.");
/*        {
            println("\ngetStatements() request for 42.0 matches float and double.");
            RepositoryResult<Statement> statements = conn.getStatements(null, age, 42.0, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
*/
        {
            println("\ngetStatements() request for 42.0 is illegal.");
/*        RepositoryResult<Statement> statements = conn.getStatements(null, age, fortyTwoDouble, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
*/        }

        println( "\nSPARQL matches for 42.0 (filter match) finds multiple numeric types.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = 42.0)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
    
        println( "\nSPARQL matches for 42.0 (direct match). No results.");
        queryString = "SELECT ?s ?p WHERE {?s ?p 42.0 .}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        
        println("------------------------------------------------------------------------------------");
        // Matches against ints. 
        {
            println("\ngetStatements() request for fortyTwoInt:" + fortyTwoInt );
            RepositoryResult<Statement> statements = conn.getStatements(null, age, fortyTwoInt, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#int> (filter match) finds multple types.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '42'^^<http://www.w3.org/2001/XMLSchema#int>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#int> (direct match) finds ints.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '42'^^<http://www.w3.org/2001/XMLSchema#int>}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        println("------------------------------------------------------------------------------------");
        // Matches against longs. 
        {
            println("\ngetStatements() request for fortyTwoLong:" + fortyTwoLong );
            RepositoryResult<Statement> statements = conn.getStatements(null, age, fortyTwoLong, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#long> (filter match) finds multple types.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '42'^^<http://www.w3.org/2001/XMLSchema#long>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#long> (direct match) finds longs.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '42'^^<http://www.w3.org/2001/XMLSchema#long>}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        
        println("------------------------------------------------------------------------------------");
        // Matches against doubles. 
        {
            println("\ngetStatements() request for fortyTwoDouble:" + fortyTwoDouble );
            RepositoryResult<Statement> statements = conn.getStatements(null, age, fortyTwoDouble, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#double> (filter match) finds multple types.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '42'^^<http://www.w3.org/2001/XMLSchema#double>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#double> (direct match) finds a double.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '42'^^<http://www.w3.org/2001/XMLSchema#double>}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }

    
        println("------------------------------------------------------------------------------------");
        // Matches against declared strings. 
        {
            println("\ngetStatements() request for fortyTwoString:" + fortyTwoString );
            RepositoryResult<Statement> statements = conn.getStatements(null, age, fortyTwoString, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#string> (filter match).");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '42'^^<http://www.w3.org/2001/XMLSchema#string>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"42\"^^<http://www.w3.org/2001/XMLSchema#string> (direct match).");
        queryString = "SELECT ?s ?p WHERE {?s ?p '42'^^<http://www.w3.org/2001/XMLSchema#string>}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        
        println("------------------------------------------------------------------------------------");
        // Matches against undeclared strings. 
        {
            println("\ngetStatements() request for fortyTwoPlain.  Matches plain literal.");
            RepositoryResult<Statement> statements = conn.getStatements(null, age, fortyTwoPlain, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }

        }

        println( "\nSPARQL matches for \"42\" (filter match).");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '42')}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"42\" (direct match).");
        queryString = "SELECT ?s ?p WHERE {?s ?p '42'}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        println("------------------------------------------------------------------------------------");
        println("\nTests of string matching.");
 
        IRI favoriteColor = f.createIRI(exns, "favoriteColor");
        Literal UCred = f.createLiteral("Red", XMLSchema.STRING);
        Literal LCred = f.createLiteral("red", XMLSchema.STRING);
        Literal RedPlain = f.createLiteral("Red");
        Literal rouge = f.createLiteral("rouge", XMLSchema.STRING);
        Literal Rouge = f.createLiteral("Rouge", XMLSchema.STRING);
        Literal RougePlain = f.createLiteral("Rouge");
        Literal FrRouge = f.createLiteral("Rouge", "fr");
        
        conn.add(alice, favoriteColor, UCred);
        conn.add(bob, favoriteColor, LCred);
        conn.add(carol, favoriteColor, RedPlain);
        conn.add(dave, favoriteColor, rouge);
        conn.add(eric, favoriteColor, Rouge);
        conn.add(fred, favoriteColor, RougePlain);
        conn.add(greg, favoriteColor, FrRouge);
        {
            println("\nShowing all color triples using getStatements().  Should be seven.");
            RepositoryResult<Statement> statements = conn.getStatements(null, favoriteColor, null, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        // Matches against undeclared strings. These are capitalized Red.
        {
            println("\ngetStatements() triples that match \"Red\".  Illegal, wrong type.");
/*            RepositoryResult<Statement> statements = conn.getStatements(null, favoriteColor, "Red", false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
*/
        }

        println( "\nSPARQL matches for \"Red\" (filter match) find exact match.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = 'Red')}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"Red\" (direct match) finds exact match.");
        queryString = "SELECT ?s ?p WHERE {?s ?p 'Red'}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        println("------------------------------------------------------------------------------------");
    
        {
            println("\ngetStatements() triples that match \"Rouge\".  Illegal, wrong type.");
/*            RepositoryResult<Statement> statements = conn.getStatements(null, favoriteColor, "Rouge", false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
*/
        }

        println( "\nSPARQL matches for \"Rouge\" (filter match) find exact match.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = 'Rouge')}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"Rouge\" (direct match) finds exact match.");
        queryString = "SELECT ?s ?p WHERE {?s ?p 'Rouge'}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
     
        println("------------------------------------------------------------------------------------");
        
        {
            println("\ngetStatements() triples that match \"Rouge\"@fr.  Match French only.");
            RepositoryResult<Statement> statements = conn.getStatements(null, favoriteColor, FrRouge, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }

        }

        println( "\nSPARQL matches for \"Rouge\"@fr (filter match) find exact match.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = 'Rouge'@fr)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"Rouge\"@fr (direct match) finds exact match.");
        queryString = "SELECT ?s ?p WHERE {?s ?p 'Rouge'@fr}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }


    println("\nSPARQL matches for (fn:lower-case(str(?o)) = \'rouge\') (filter match) finds three.");
    queryString = "PREFIX fn: <http://www.w3.org/2005/xpath-functions#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (fn:lower-case(str(?o)) = 'rouge')}";
    tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    result = tupleQuery.evaluate();
    try {
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value s = bindingSet.getValue("s");
            Value p = bindingSet.getValue("p");
            Value o = bindingSet.getValue("o");
            println("  " + s + " " + p + " " + o);
        }
    } finally {
        result.close();
    }

        
        println("------------------------------------------------------------------------------------");
        // Boolean experiments.
        IRI senior = f.createIRI(exns, "senior");
        println("true = " + true);
        println("false = " + false);
//      conn.add(alice, senior, true);  // illegal
//      conn.add(bob, senior, false);   // illegal
        Literal trueValue = f.createLiteral("true", XMLSchema.BOOLEAN);  
        Literal falseValue = f.createLiteral("false", XMLSchema.BOOLEAN);
        conn.add(alice, senior, trueValue);
        conn.add(bob, senior, falseValue);
        {
            println("\ngetStatements() all senior triple, should be two.");
            RepositoryResult<Statement> statements = conn.getStatements(null, senior, null, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        {
            println("\ngetStatements() triples that match trueValue.  One.");
           RepositoryResult<Statement> statements = conn.getStatements(null, senior, trueValue, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
 
        }
        {
            println("\ngetStatements() triples that match trueValue.  One.");
            RepositoryResult<Statement> statements = conn.getStatements(null, senior, trueValue, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }     
            
        println( "\nSPARQL matches for true (filter match).  One.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = true)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for true (direct match). One.");
        queryString = "SELECT ?s ?p WHERE {?s ?p true}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        
        println( "\nSPARQL matches for \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> (filter match). One.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = 'true'^^<http://www.w3.org/2001/XMLSchema#boolean>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> (direct match). One.");
        queryString = "SELECT ?s ?p WHERE {?s ?p 'true'^^<http://www.w3.org/2001/XMLSchema#boolean>}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }
        println("------------------------------------------------------------------------------------");
        // Dates, times and datetimes.
        IRI birthdate = f.createIRI(exns, "birthdate");
        Literal date = f.createLiteral("1984-12-06", XMLSchema.DATE);
        Literal datetime = f.createLiteral("1984-12-06T09:00:00", XMLSchema.DATETIME);
        Literal time = f.createLiteral("09:00:00", XMLSchema.TIME);
        Literal datetimeOffset = f.createLiteral("1984-12-06T09:00:00+01:00", XMLSchema.DATETIME);
        println("Printing out Literals for date, datetime, time, and datetime with Zulu offset.");
        println(date);
        println(datetime);
        println(time);
        println(datetimeOffset);
        conn.add(alice, birthdate, date);
        conn.add(bob, birthdate, datetime);
        conn.add(carol, birthdate, time);
        conn.add(dave, birthdate, datetimeOffset);
        
        {
            println("\ngetStatements() all birthdates.  Four matches.");
            RepositoryResult<Statement> statements = conn.getStatements(null, birthdate, null, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }     
        println("----------------------------------------------------------------------------");
        println("\ngetStatements() triples that match date: " + date + " One match."  );        
        {
            RepositoryResult<Statement> statements = conn.getStatements(null, birthdate, date, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }           
        println( "\nSPARQL matches for \'1984-12-06\'^^<http://www.w3.org/2001/XMLSchema#date> (filter match) finds one.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '1984-12-06'^^<http://www.w3.org/2001/XMLSchema#date>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \'1984-12-06\'^^<http://www.w3.org/2001/XMLSchema#date> (direct match) finds one.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '1984-12-06'^^<http://www.w3.org/2001/XMLSchema#date> .}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }        
        
        println("----------------------------------------------------------------------------");
        println("\ngetStatements() triples that match datetime: " + datetime + " One match."  );        
        {
            RepositoryResult<Statement> statements = conn.getStatements(null, birthdate, datetime, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }           
        println( "\nSPARQL matches for \"1984-12-06T09:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> (filter match) finds one.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '1984-12-06T09:00:00Z'^^<http://www.w3.org/2001/XMLSchema#dateTime>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \'1984-12-06T09:00:00Z\'^^<http://www.w3.org/2001/XMLSchema#dateTime> (direct match) finds one.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '1984-12-06T09:00:00Z'^^<http://www.w3.org/2001/XMLSchema#dateTime> .}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }        
        
        println("----------------------------------------------------------------------------");
        println("\ngetStatements() triples that match time: " + time + " One match."  );        
        {
            RepositoryResult<Statement> statements = conn.getStatements(null, birthdate, time, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }           
        println( "\nSPARQL matches for \"09:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#time> (filter match) finds one.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '09:00:00Z'^^<http://www.w3.org/2001/XMLSchema#time>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println( "\nSPARQL matches for \"09:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#time (direct match) finds one.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '09:00:00Z'^^<http://www.w3.org/2001/XMLSchema#time> .}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }        
                
        
        println("----------------------------------------------------------------------------");
        println("\ngetStatements() triples that match datetimeOffset: " + datetimeOffset + " One match."  );        
        {
            RepositoryResult<Statement> statements = conn.getStatements(null, birthdate, datetimeOffset, false);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }           
        println( "\nSPARQL matches for \"1984-12-06T09:00:00+01:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime> (filter match) finds one.");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = '1984-12-06T09:00:00+01:00'^^<http://www.w3.org/2001/XMLSchema#dateTime>)}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                println("  " + s + " " + p + " " + o);
            }
        } finally {
            result.close();
        }
        println("\nSPARQL matches for \"1984-12-06T09:00:00+01:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime> (direct match) finds one.");
        queryString = "SELECT ?s ?p WHERE {?s ?p '1984-12-06T09:00:00+01:00'^^<http://www.w3.org/2001/XMLSchema#dateTime> .}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                println("  " + s + " " + p );
            }
        } finally {
            result.close();
        }        
                

        conn.close();
        myRepository.shutDown();
    }
    
    /**
     * Importing Triples
     */
    
    public static AGRepositoryConnection example6() throws Exception {
        AGRepositoryConnection conn = AGServer.createRepositoryConnection(
                REPOSITORY_ID, CATALOG_ID, SERVER_URL, USERNAME, PASSWORD);
        closeBeforeExit(conn);
        conn.clear();
        
        conn.begin();  // start a transaction
        ValueFactory f = conn.getValueFactory();
        final File path1 = new File(DATA_DIRECTORY, "java-vcards.rdf");
        final File path2 = new File(DATA_DIRECTORY, "java-kennedy.ntriples");
        String baseURI = "http://example.org/example/local";
        IRI context = f.createIRI("http://example.org#vcards");
        // read vcards triples into the context 'context':
        conn.add(path1, baseURI, RDFFormat.RDFXML, context);
        // read Kennedy triples into the null context:
        conn.add(path2, baseURI, RDFFormat.NTRIPLES);
        println("After loading, repository contains " + conn.size(context) +
                " vcard triples in context '" + context + "'\n    and   " +
                conn.size((Resource)null) + " kennedy triples in context 'null'.");
        conn.commit();
        return conn;
    }
    
    /**
     * Importing Triples, query
     */
    public static void example7() throws Exception {
        RepositoryConnection conn = example6();
        println("\nMatch all and print subjects and contexts");
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
        for (int i = 0; i < 25 && result.hasNext(); i++) {
            Statement stmt = result.next();
            println(stmt.getSubject() + "  " + stmt.getContext());
        }
        result.close();
        
        println("\nSame thing with SPARQL query (can't retrieve triples in the null context)");
        String queryString = "SELECT DISTINCT ?s ?c WHERE {graph ?c {?s ?p ?o .} }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult qresult = tupleQuery.evaluate();       
        while (qresult.hasNext()) {
            BindingSet bindingSet = qresult.next();
            println(bindingSet.getBinding("s") + "  " + bindingSet.getBinding("c"));
        }
        qresult.close();
    }

    /**
     * Writing RDF or NTriples to a file
     */
    public static void example8() throws Exception {
        RepositoryConnection conn = example6();
        IRI context = conn.getValueFactory().createIRI(
                "http://example.org#vcards");
        String outputFile = TEMPORARY_DIRECTORY + "TutorialExamples.example8.nt";
        // outputFile = null;
        if (outputFile == null) {
            println("\nWriting n-triples to Standard Out instead of to a file");
        } else {
            println("\nWriting n-triples to: " + outputFile);
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStream output = (outputFile != null) ? fos : System.out;
        NTriplesWriter ntriplesWriter = new NTriplesWriter(output);
        conn.export(ntriplesWriter, context);
        String outputFile2 =  TEMPORARY_DIRECTORY + "TutorialExamples.example8.rdf";
        // outputFile2 = null;
        if (outputFile2 == null) {
            println("\nWriting RDF to Standard Out instead of to a file");
        } else {
            println("\nWriting RDF to: " + outputFile2);
        }
        FileOutputStream fos2 = new FileOutputStream(outputFile2);
        output = (outputFile2 != null) ? fos2 : System.out;
        RDFXMLWriter rdfxmlfWriter = new RDFXMLWriter(output);
        conn.export(rdfxmlfWriter, context);
        output.write('\n');
        output.close();
        fos.close();
        fos2.close();
    }
    
    /**
     * Writing the result of a statements match to a file.
     */
    public static void example9() throws Exception {    
        RepositoryConnection conn = example6();
        conn.exportStatements(null, RDF.TYPE, null, false, new RDFXMLWriter(System.out));
    }

    /**
     * Datasets and multiple contexts.
     */
    public static void example10 () throws Exception {
        RepositoryConnection conn = example1(false);
        ValueFactory f = conn.getValueFactory();
        String exns = "http://example.org/people/";
        // Create URIs for resources, predicates and classes.
        IRI alice = f.createIRI(exns, "alice");
        IRI bob = f.createIRI(exns, "bob");
        IRI ted = f.createIRI(exns, "ted");
        IRI person = f.createIRI("http://example.org/ontology/Person");
        IRI name = f.createIRI("http://example.org/ontology/name");
        // Create literal name values.
        Literal alicesName = f.createLiteral("Alice");
        Literal bobsName = f.createLiteral("Bob");
        Literal tedsName = f.createLiteral("Ted");   
        // Create URIs to identify the named contexts.
        IRI context1 = f.createIRI(exns, "context1");
        IRI context2 = f.createIRI(exns, "context2");
        // Assemble new statements and add them to the contexts. 
        conn.add(alice, RDF.TYPE, person, context1);
        conn.add(alice, name, alicesName, context1);
        conn.add(bob, RDF.TYPE, person, context2);
        conn.add(bob, name, bobsName, context2);
        conn.add(ted, RDF.TYPE, person);
        conn.add(ted, name, tedsName);
// GetStatements() examples. 
        println("--------------------------------------------------------------------");
        RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false);
        println("\ngetStatements: All triples in all contexts: " + (conn.size()));       
        while (statements.hasNext()) {
            println(statements.next());            
        }
        println("--------------------------------------------------------------------");
        statements = conn.getStatements(null, null, null, false, context1, context2);
        println("\ngetStatements:Triples in contexts 1 or 2: " + (conn.size(context1) + conn.size(context2)));
        while (statements.hasNext()) {
            println(statements.next());
        }
        println("--------------------------------------------------------------------");
        statements = conn.getStatements(null, null, null, false, null, context2);
        println("\ngetStatements:Triples in contexts null or 2: " + (conn.size((Resource)null) + conn.size(context2)));
        while (statements.hasNext()) {
            println(statements.next());
        }
// SPARQL examples, some using FROM and FROM NAMED.
        println("--------------------------------------------------------------------");
        String queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();    
        println("\n" + queryString + " No dataset restrictions.");
        while (result.hasNext()) {
            println(result.next());
        }
        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o ?c WHERE {GRAPH ?c {?s ?p ?o . }}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();    
        println("\n" + queryString + " No dataset. SPARQL GRAPH query only.");
        while (result.hasNext()) {
            println(result.next());
        }
        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o FROM DEFAULT WHERE {?s ?p ?o . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();    
        println("\n" + queryString);
        while (result.hasNext()) {
            println(result.next());
        }

        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o FROM <http://example.org/people/context1> WHERE {?s ?p ?o . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();    
        println("\n" + queryString);
        while (result.hasNext()) {
            println(result.next());
        }
        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o FROM NAMED <http://example.org/people/context1> WHERE {?s ?p ?o . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();    
        println("\n" + queryString);
        while (result.hasNext()) {
            println(result.next());
        }
        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o ?g FROM NAMED <http://example.org/people/context1> WHERE {GRAPH ?g {?s ?p ?o . }}";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();    
        println("\n" + queryString);
        while (result.hasNext()) {
            println(result.next());
        }
        println("--------------------------------------------------------------------");
        // Test case for bug19681
        queryString = 
        "SELECT ?s ?p ?o ?g \n" +
          "FROM <http://example.org/people/context1> \n" +
          "FROM DEFAULT \n" +
          "FROM NAMED <http://example.org/people/context2> \n" +
//          "FROM NAMED <http://foo> \n" +
          "WHERE {{GRAPH ?g {?s ?p ?o . }} UNION {?s ?p ?o .}} \n";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = tupleQuery.evaluate();    
        println("\n" + queryString);
        while (result.hasNext()) {
            println(result.next());
        }
        
        // SPARQL examples using Dataset objects. 
        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . }";
        DatasetImpl ds = new DatasetImpl();
        ds.addDefaultGraph(null);    // AG default graph as SPARQL default graph.
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setDataset(ds);
        result = tupleQuery.evaluate();    
        println("\n" + queryString + " Datasest for null graph.");
        while (result.hasNext()) {
            println(result.next());
        }

        println("--------------------------------------------------------------------");
        // testing named graph query
        queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . }";
        ds = new DatasetImpl();
        ds.addNamedGraph(context1);
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setDataset(ds);
        result = tupleQuery.evaluate();    
        println("\n" + queryString + " Datasest for context1.");
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            println(bindingSet.getBinding("s") + " " + 
                    bindingSet.getBinding("p") + " " +
                    bindingSet.getBinding("o"));
        }    
        
        println("--------------------------------------------------------------------");
        queryString = "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }";        
        ds = new DatasetImpl();
        ds.addNamedGraph(context1);
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setDataset(ds);
        result = tupleQuery.evaluate();    
        println("\n" + queryString + " Datasest for context1, using GRAPH.");
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            println(bindingSet.getBinding("s") + " " + 
                    bindingSet.getBinding("p") + " " +
                    bindingSet.getBinding("o") + " " +
                    bindingSet.getBinding("c"));
        }    
        
    }
    /**
     * Namespaces
     */
    public static void example11 () throws Exception {
        RepositoryConnection conn = example1(false);
        ValueFactory f = conn.getValueFactory();
        String exns = "http://example.org/people/";
        IRI alice = f.createIRI(exns, "alice");
        IRI person = f.createIRI(exns, "Person");
        conn.add(alice, RDF.TYPE, person);
        conn.setNamespace("ex", exns);
        String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();  
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
    }                                                   

    /**
     * Text indexing and search
     */
    public static void example12 () throws Exception {    
        AGRepositoryConnection conn = example1(false);
        ValueFactory f = conn.getValueFactory();
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        // Create index1
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        config.getPredicates().add(f.createIRI(exns,"fullname"));
        conn.createFreetextIndex("index1", config);
        println("listFreetextIndices(): " + conn.listFreetextIndices());
        println("index1 configuration: ");
        println(conn.getFreetextIndexConfig("index1"));
        
        // Create parts of person resources.        
        IRI alice = f.createIRI(exns, "alice1");
        IRI carroll = f.createIRI(exns, "carroll");
        IRI persontype = f.createIRI(exns, "Person");
        IRI fullname = f.createIRI(exns, "fullname");
        Literal alicename = f.createLiteral("Alice B. Toklas");
        Literal lewisCarroll = f.createLiteral("Lewis Carroll");
        // Create parts of book resources.
        IRI book =  f.createIRI(exns, "book1");
        IRI booktype = f.createIRI(exns, "Book");
        IRI booktitle = f.createIRI(exns, "title");
        IRI author = f.createIRI(exns, "author");
        Literal wonderland = f.createLiteral("Alice in Wonderland");
        // Add Alice B. Toklas triples
        conn.clear();    
        conn.add(alice, RDF.TYPE, persontype);
        conn.add(alice, fullname, alicename);
        // Add Alice in Wonderland triples
        conn.add(book, RDF.TYPE, booktype);    
        conn.add(book, booktitle, wonderland); 
        conn.add(book, author, carroll);
        // Add Lewis Carroll triples
        conn.add(carroll, RDF.TYPE, persontype);
        conn.add(carroll, fullname, lewisCarroll);
        // Check triples
        println("\nListing all triples.");
        RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false);
        printRows(statements);

        println("\nFreetext Pattern search for Alice.");
        AGFreetextQuery query = new AGFreetextQuery(conn);
        query.setPattern("Alice");
        query.setIndex("index1");
        statements = query.evaluate();
        printRows(statements);
        
        println("\nFreetext Expression search for Alice or Lewis, limit 1.");
        query = new AGFreetextQuery(conn);
        query.setExpression("(or \"Alice\" \"Lewis\")");
        query.setIndex("index1");
        query.setLimit(1);
        statements = query.evaluate();
        printRows(statements);
        
        println("\nFreetext Expression search for Alice or Lewis, limit 1, offset 1.");
        query.setOffset(1); 
        statements = query.evaluate();
        printRows(statements);
        
        String queryString = 
            "SELECT ?s ?p ?o " +
            "WHERE { ?s ?p ?o . " +
            "        ?s fti:match 'Alice' . }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = (TupleQueryResult)tupleQuery.evaluate();
        printRows("\nWhole-word match for 'Alice'.",result);
        
        queryString = 
            "SELECT ?s ?p ?o " +
            "WHERE { ?s ?p ?o . ?s fti:match 'Ali*' . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = (TupleQueryResult)tupleQuery.evaluate();
        printRows("\nWildcard match for 'Ali*'.",result);
            
        queryString = 
            "SELECT ?s ?p ?o " +
            "WHERE { ?s ?p ?o . ?s fti:match '?l?c?' . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = (TupleQueryResult)tupleQuery.evaluate();
        printRows("\nWildcard match for '?l?ce?.",result);
            
        queryString = 
            "SELECT ?s ?p ?o " +
            "WHERE { ?s ?p ?o . FILTER regex(?o, \"lic\") }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        result = (TupleQueryResult)tupleQuery.evaluate();
        printRows("\nSubstring match for 'lic'.",result);

        // Create index2, for searching short names in URI's
        // that are objects of the author predicate
        println("\nCreate index2.");
        config = AGFreetextIndexConfig.newInstance();
        config.getPredicates().add(author);
        config.setIndexResources("short");
        conn.createFreetextIndex("index2", config);
        println(conn.listFreetextIndices());
        
        println("\nSearch for Carroll in index2.");
        query = new AGFreetextQuery(conn);
        query.setPattern("Carroll");
        query.setIndex("index2");
        statements = query.evaluate();
        printRows(statements);
        
        println("\nFreetext indices after deleting index1:");
        conn.deleteFreetextIndex("index1");
        println(conn.listFreetextIndices());
    }
    
    
    /**
     * Ask, Construct, Describe, and Update queries
     */ 
    public static void example13 () throws Exception {
        AGRepositoryConnection conn = example6();
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        // We don't want the vcards this time. This is how to delete an entire subgraph.
        ValueFactory vf = conn.getValueFactory();
        IRI context = vf.createIRI("http://example.org#vcards");
        conn.remove((Resource)null, (IRI)null, (Value)null, context);
        println("\nRemoved vcards.");
        // SELECT query
        String queryString = "select ?s where { ?s rdf:type kdy:person} limit 5";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        println("\nSELECT some persons:");
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        // ASK query
        queryString = "ask { ?s kdy:first-name 'John' } ";
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        boolean truth = booleanQuery.evaluate(); 
        println("\nASK: Is there anyone named John? " + truth);
        queryString = "ask { ?s kdy:first-name 'Alice' } ";
        BooleanQuery booleanQuery2 = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        boolean truth2 = booleanQuery2.evaluate(); 
        println("\nASK: Is there anyone named Alice? " + truth2);
        // CONSTRUCT query
        println("\nConstructing has-grandchild triples.");
        queryString = "construct {?a kdy:has-grandchild ?c}" + 
                      "    where { ?a kdy:has-child ?b . " +
                      "            ?b kdy:has-child ?c . }";
        AGGraphQuery constructQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        GraphQueryResult gresult = constructQuery.evaluate(); 
        while (gresult.hasNext()) {
            conn.add(gresult.next());  // adding new triples to the store
        }
        gresult.close();
        // Just the count now.  The count is done server-side,
        // and only the count is returned.
        long count = constructQuery.count();
        println("count: " + count);

        String queryString3 = "select ?s ?o where { ?s kdy:has-grandchild ?o}";
        TupleQuery tupleQuery3 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString3);
        TupleQueryResult result3 = tupleQuery3.evaluate();
        println("\nShow the has-grandchild triples:");
        while (result3.hasNext()) {
            println(result3.next());
        }
        result3.close();
        // DESCRIBE query
        queryString = "describe ?s ?o where { ?s kdy:has-grandchild ?o . } limit 1";
        GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        gresult = describeQuery.evaluate(); 
        println("\nDescribe one grandparent and one grandchild:");
        while (gresult.hasNext()) {
            println(gresult.next());
        }
        gresult.close();
        
        // SPARQL UPDATE queries
        String updateString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
            + "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } }";
        println("\nPerforming SPARQL Update:\n" + updateString);
        conn.prepareUpdate(QueryLanguage.SPARQL, updateString).execute(); 
        queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
            + "ASK { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } }";
        println("\nPerforming query:\n" + queryString);
        println("Result: " + conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate()); 
        
        updateString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
            + "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
            + "\n"
            + "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
            + "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        println("\nPerforming a sequence of SPARQL Updates in one request (to correct the title):\n" + updateString);
        conn.prepareUpdate(QueryLanguage.SPARQL, updateString).execute();
        queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
            + "ASK { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } }";
        println("\nPerforming query:\n" + queryString);
        println("Result: " + conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate()); 
        queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
            + "ASK { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";
        println("\nPerforming query:\n" + queryString);
        println("Result: " + conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate()); 
    }


    /**
     * Parametric Queries
     */
    public static void example14() throws Exception {
        RepositoryConnection conn = example2(false);
        ValueFactory f = conn.getValueFactory();
        /* Start a transaction so that the query results below
         * are based off the same consistent view of the repository
         */
        conn.begin();
        IRI alice = f.createIRI("http://example.org/people/alice");
        IRI bob = f.createIRI("http://example.org/people/bob");
        String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setBinding("s", alice);
        TupleQueryResult result = tupleQuery.evaluate();
        println("\nFacts about Alice:");            
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        tupleQuery.setBinding("s", bob);
        println("\nFacts about Bob:");    
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        conn.rollback();
    }

    /**
     * Range matches
     */
    public static void example15() throws Exception {
        println("Starting example15().");
        AGRepositoryConnection conn = example1(false);
        ValueFactory f = conn.getValueFactory();
        conn.clear();
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        IRI alice = f.createIRI(exns, "alice");
        IRI bob = f.createIRI(exns, "bob");
        IRI carol = f.createIRI(exns, "carol");
        IRI age = f.createIRI(exns, "age");
        conn.add(alice, age, f.createLiteral(42));
        conn.add(bob, age, f.createLiteral(45.1));
        conn.add(carol, age, f.createLiteral("39"));

        println("\nRange query for integers and floats.");
        String queryString = 
            "SELECT ?s ?p ?o  " +
            "WHERE { ?s ?p ?o . " +
            "FILTER ((?o >= 30) && (?o <= 50)) }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                System.out.format("%s %s %s\n", s, p, o);
            }
        } finally {
            result.close();
        }
        conn.close();
        println("\nRange query for integers, floats, and integers in strings.");
        String queryString2 = 
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
            "SELECT ?s ?p ?o  " +
            "WHERE { ?s ?p ?o . " +
            "FILTER ((xsd:integer(?o) >= 30) && (xsd:integer(?o) <= 50)) }";
        TupleQuery tupleQuery2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString2);
        TupleQueryResult result2 = tupleQuery2.evaluate();
        try {
            while (result2.hasNext()) {
                BindingSet bindingSet = result2.next();
                Value s = bindingSet.getValue("s");
                Value p = bindingSet.getValue("p");
                Value o = bindingSet.getValue("o");
                System.out.format("%s %s %s\n", s, p, o);
            }
        } finally {
            result2.close();
        }
    }
    
    /**
     * Federated triple stores.
     */
    public static void example16() throws Exception {
        AGRepositoryConnection conn = example6();
        
        AGServer server = conn.getServer();
        // create two ordinary stores, and one federated store:
        AGRepositoryConnection redConn = server.createRepositoryConnection(
                "redthingsjv", CATALOG_ID, false);
        closeBeforeExit(redConn);
        redConn.clear();
        ValueFactory rf = redConn.getValueFactory();
        AGRepositoryConnection greenConn = server.createRepositoryConnection(
                "greenthingsjv", CATALOG_ID, false);
        closeBeforeExit(greenConn);
        greenConn.clear();
        ValueFactory gf = greenConn.getValueFactory();
        
        AGAbstractRepository rainbowRepo = server.federate(
                redConn.getRepository(), greenConn.getRepository());
        rainbowRepo.initialize();
        println("Federation is writable? " + rainbowRepo.isWritable());
        AGRepositoryConnection rainbowConn = rainbowRepo.getConnection();
        closeBeforeExit(rainbowConn);
        String ex = "http://example.com/";
        // add a few triples to the red and green stores:
        redConn.add(rf.createIRI(ex+"mcintosh"), RDF.TYPE, rf.createIRI(ex+"Apple"));
        redConn.add(rf.createIRI(ex+"reddelicious"), RDF.TYPE, rf.createIRI(ex+"Apple"));
        greenConn.add(gf.createIRI(ex+"pippin"), RDF.TYPE, gf.createIRI(ex+"Apple"));
        greenConn.add(gf.createIRI(ex+"kermitthefrog"), RDF.TYPE, gf.createIRI(ex+"Frog"));
        redConn.setNamespace("ex", ex);
        greenConn.setNamespace("ex", ex);
        rainbowConn.setNamespace("ex", ex);
        String queryString = "select ?s where { ?s rdf:type ex:Apple }";
        // query each of the stores; observe that the federated one is the union of the other two:
        printRows("Red Apples:", redConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
        printRows("Green Apples:", greenConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
        printRows("Federated Apples:", rainbowConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
    }

    /**
     * Prolog queries
     */
    public static void example17() throws Exception {
        AGRepositoryConnection conn = example6();
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        String rules1 =
            "(<-- (woman ?person) ;; IF\n" +
            "     (q ?person !kdy:sex !kdy:female)\n" +
            "     (q ?person !rdf:type !kdy:person))\n" +
            "(<-- (man ?person) ;; IF\n" +
            "     (q ?person !kdy:sex !kdy:male)\n" +
            "     (q ?person !rdf:type !kdy:person))";
        conn.addRules(rules1);
        println("\nFirst and Last names of all \"men.\"");
        String queryString =
            "(select (?first ?last)\n" +
            "        (man ?person)\n" +
            "        (q ?person !kdy:first-name ?first)\n" +
            "        (q ?person !kdy:last-name ?last))";
        TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value f = bindingSet.getValue("first");
            Value l = bindingSet.getValue("last");
            println(f.stringValue() + " " + l.stringValue());
        }
        println("\nGetting the triple id of <kdy:person11 rdf:type kdy:person>.");
        queryString =
            "(select (?id)\n" +
            "        (q !kdy:person11 !rdf:type !kdy:person ?g ?id))\n";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value id = bindingSet.getValue("id");
            println("id: " + id);
        }
        
        result.close();
        conn.close();
    }

    /**
     * Loading Prolog rules
     */
    public static void example18() throws Exception {
        AGRepositoryConnection conn = example6();
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        conn.setNamespace("rltv", "http://www.franz.com/simple#");
        final File path = new File(DATA_DIRECTORY, "java-rules.txt");
        try (final FileInputStream stream = new FileInputStream(path)) {
            conn.addRules(stream);
        }
        String queryString = 
            "(select (?ufirst ?ulast ?cfirst ?clast)" +
                     "(uncle ?uncle ?child)" +
                     "(name ?uncle ?ufirst ?ulast)" +
                     "(name ?child ?cfirst ?clast))";
        TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        TupleQueryResult result = tupleQuery.evaluate();     
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value u1 = bindingSet.getValue("ufirst");
            Value u2 = bindingSet.getValue("ulast");
            String ufull = u1.stringValue() + " " + u2.stringValue() ;
            Value c1 = bindingSet.getValue("cfirst");
            Value c2 = bindingSet.getValue("clast");
            String cfull = c1.stringValue() + " " + c2.stringValue() ;
            println(ufull + " is the uncle of " + cfull);
        }
        result.close();
    }
    /**
     * RDFS++ Reasoning
     */
    public static void example19() throws Exception {
        AGRepositoryConnection conn = example1(false);
        // Examples of RDFS++ inference.  Was originally example 2A.
        ValueFactory f = conn.getValueFactory();
        IRI robert = f.createIRI("http://example.org/people/robert");
        IRI roberta = f.createIRI("http://example.org/people/roberta");
        IRI bob = f.createIRI("http://example.org/people/bob");
        IRI bobby = f.createIRI("http://example.org/people/bobby");
        // create name and child predicates, and Person class.
        IRI name = f.createIRI("http://example.org/ontology/name");
        IRI fatherOf = f.createIRI("http://example.org/ontology/fatherOf");
        IRI person = f.createIRI("http://example.org/ontology/Person");
        // create literal values for names    
        Literal bobsName = f.createLiteral("Bob");
        Literal bobbysName = f.createLiteral("Bobby");
        Literal robertsName = f.createLiteral("Robert");
        Literal robertasName = f.createLiteral("Roberta");
        // Bob is the same person as Robert
        conn.add(bob, OWL.SAMEAS, robert);
        // Robert, Bob, and children are people
        conn.add(robert, RDF.TYPE, person);
        conn.add(roberta, RDF.TYPE, person);
        conn.add(bob, RDF.TYPE, person);
        conn.add(bobby, RDF.TYPE, person);
        // They all have names.
        conn.add(robert, name, robertsName);
        conn.add(roberta, name, robertasName);
        conn.add(bob, name, bobsName);
        conn.add(bobby, name, bobbysName);
        // robert has a child
        conn.add(robert, fatherOf, roberta);
        // bob has a child
        conn.add(bob, fatherOf, bobby);
        
        // List the children of Robert, with inference OFF.
        String queryString = "SELECT ?child WHERE {?robert ?fatherOf ?child .}";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setIncludeInferred(false);  // Turn off inference
        tupleQuery.setBinding("robert", robert);
        tupleQuery.setBinding("fatherOf", fatherOf);
        TupleQueryResult result = tupleQuery.evaluate();
        println("\nChildren of Robert, inference OFF");            
        while (result.hasNext()) {
            println(result.next());
        }
        result.close();
        // List the children of Robert, with inference ON.
        tupleQuery.setIncludeInferred(true);  // Turn on inference
        TupleQueryResult result2 = tupleQuery.evaluate();
        println("\nChildren of Robert, inference ON");            
        while (result2.hasNext()) {
            println(result2.next());
        }
        result2.close();
        // Remove the owl:sameAs link so we can try the next example.
        conn.remove(bob, OWL.SAMEAS, robert);
        
        // Define new predicate, hasFather, as the inverse of fatherOf.
        IRI hasFather = f.createIRI("http://example.org/ontology/hasFather");
        conn.add(hasFather, OWL.INVERSEOF, fatherOf);
        // Search for people who have fathers, even though there are no hasFather triples.
        // With inference OFF.
        println("\nPeople with fathers, inference OFF");
        printRows( conn.getStatements(null, hasFather, null, false) );
        // With inference ON. The owl:inverseOf link allows AllegroGraph to
        // deduce the inverse links.
        println("\nPeople with fathers, inference ON");
        printRows( conn.getStatements(null, hasFather, null, true) );
        // Remove owl:inverseOf property.
        conn.remove(hasFather, OWL.INVERSEOF, fatherOf);

        IRI parentOf = f.createIRI("http://example.org/ontology/parentOf");
        conn.add(fatherOf, RDFS.SUBPROPERTYOF, parentOf);
        // Now search for inferred parentOf links.
        // Search for parentOf links, even though there are no parentOf triples.
        // With inference OFF.
        println("\nPeople with parents, inference OFF");
        printRows( conn.getStatements(null, parentOf, null, false) );
        // With inference ON. The rdfs:subpropertyOf link allows AllegroGraph to 
        // deduce that fatherOf links imply parentOf links.
        println("\nPeople with parents, inference ON");
        printRows( conn.getStatements(null, parentOf, null, true) );
        conn.remove(fatherOf, RDFS.SUBPROPERTYOF, parentOf);
        
        // The next example shows rdfs:range and rdfs:domain in action.
        // We'll create two new rdf:type classes.  Note that classes are capitalized.
        IRI parent = f.createIRI("http://example.org/ontology/Parent");
        IRI child = f.createIRI("http://exmaple.org/ontology/Child");
        // The following triples say that a fatherOf link points from a parent to a child.
        conn.add(fatherOf, RDFS.DOMAIN, parent);
        conn.add(fatherOf, RDFS.RANGE, child);
        // Now we can search for rdf:type parent.
        println("\nWho are the parents?  Inference ON.");
        printRows( conn.getStatements(null, RDF.TYPE, parent, true) );
        // And we can search for rdf:type child.
        println("\nWho are the children?  Inference ON.");
        printRows( conn.getStatements(null, RDF.TYPE, child, true) );
    }
        
    /**
     * Geospatial Reasoning
     */
    
    public static void example20() throws Exception {
        AGRepositoryConnection conn = example1(false);
        AGValueFactory vf = conn.getValueFactory();
        conn.clear();
        println("Starting example20().");
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        IRI alice = vf.createIRI(exns, "alice");
        IRI bob = vf.createIRI(exns, "bob");
        IRI carol = vf.createIRI(exns, "carol");
        println("\nCARTESIAN COORDINATE SYSTEM");
        IRI cartSystem = conn.registerCartesianType(10, 0, 100, 0, 100);
        IRI location = vf.createIRI(exns, "location");
        Literal alice_loc = vf.createLiteral("+30.0+30.0", cartSystem);
        Literal bob_loc = vf.createLiteral("+40.0+40.0", cartSystem);
        Literal carol_loc = vf.createLiteral("+50.0+50.0", cartSystem);
        conn.add(alice, location, alice_loc);
        conn.add(bob, location, bob_loc);
        conn.add(carol, location, carol_loc);
        println("\nFind people located within box1.");
        RepositoryResult<Statement> result1 = conn.getStatementsInBox(cartSystem, location, 20, 40, 20, 40, 0, false);
        printRows(result1);
        result1.close();
        //printRows( conn.getStatementsInBox(cartSystem, location, 20, 40, 20, 40, 0, false) );
        println("\nFind people located within circle1.");
        RepositoryResult<Statement> result2 = conn.getStatementsInCircle(cartSystem, location, 35, 35, 10, 0, false);
        printRows(result2);
        result2.close();
        //printRows( conn.getStatementsInCircle(cartSystem, location, 35, 35, 10, 0, false) ); 
        IRI polygon1 = vf.createIRI("http://example.org/polygon1");
        List<Literal> polygon1_points = new ArrayList<Literal>(4);

        polygon1_points.add(vf.createLiteral("+10.0+40.0", cartSystem));
        polygon1_points.add(vf.createLiteral("+50.0+10.0", cartSystem));
        polygon1_points.add(vf.createLiteral("+35.0+40.0", cartSystem));
        polygon1_points.add(vf.createLiteral("+50.0+70.0", cartSystem));
        //println(polygon1_points);
        conn.registerPolygon(polygon1, polygon1_points);
        println("\nFind people located within ploygon1.");
        RepositoryResult<Statement> result3 = conn.getStatementsInPolygon(cartSystem, location, polygon1, 0, false);
        printRows(result3);
        result3.close();
        //printRows( conn.getStatementsInPolygon(cartSystem, location, polygon1, 0, false) );
        // now we switch to a Spherical (Lat/Long) coordinate system
        println("\nSPHERICAL COORDINATE SYSTEM");
        //URI sphericalSystemKM = conn.registerSphericalType(5, AGProtocol.KM_PARAM_VALUE);
        //URI sphericalSystemDegree = conn.registerSphericalType(5, AGProtocol.DEGREE_PARAM_VALUE);
        IRI sphericalSystemDegree = conn.registerSphericalType(5, "degree");

        IRI amsterdam = vf.createIRI(exns, "amsterdam");
        IRI london = vf.createIRI(exns, "london");
        IRI sanfrancisco = vf.createIRI(exns, "sanfrancisco");
        IRI salvador = vf.createIRI(exns, "salvador");
        location = vf.createIRI(exns, "geolocation");
        conn.add(amsterdam, location, vf.createLiteral("+52.366665+004.883333",sphericalSystemDegree));
        conn.add(london, location, vf.createLiteral("+51.533333-000.08333333",sphericalSystemDegree));
        conn.add(sanfrancisco, location, vf.createLiteral("+37.783333-122.433334",sphericalSystemDegree));
        conn.add(salvador, location, vf.createLiteral("+13.783333-088.45",sphericalSystemDegree));
        println("\nLocate entities within box2.");
        RepositoryResult<Statement> result4 = conn.getStatementsInBox(sphericalSystemDegree, location, -130.0f, -70.0f, 25.0f, 50.0f, 0, false);
        printRows(result4);
        result4.close();
        //printRows(conn.getStatementsInBox(sphericalSystemDegree, location, -130.0f, -70.0f, 25.0f, 50.0f, 0, false) );
        println("\nLocate entities within haversine circle.");
        RepositoryResult<Statement> result5 = conn.getGeoHaversine(sphericalSystemDegree, location, 19.3994f, -99.08f, 2000.0f, "km", 0, false);
        printRows(result5);
        result5.close();
        //printRows(conn.getGeoHaversine(sphericalSystemDegree, location, 19.3994f, -99.08f, 2000.0f, "km", 0, false) );
        IRI polygon2 = vf.createIRI("http://example.org/polygon2");
        List<Literal> polygon2_points = new ArrayList<Literal>(3);
        polygon2_points.add(vf.createLiteral("+51.0+002.0", sphericalSystemDegree));
        polygon2_points.add(vf.createLiteral("+60.0-005.0", sphericalSystemDegree));
        polygon2_points.add(vf.createLiteral("+48.0-012.5", sphericalSystemDegree));
        //println(polygon2_points);
        conn.registerPolygon(polygon2, polygon2_points);
        println("\nLocate entities within polygon2.");
        RepositoryResult<Statement> result6 = conn.getStatementsInPolygon(sphericalSystemDegree, location, polygon2, 0, false);
        printRows(result6);
        result6.close();
        //printRows( conn.getStatementsInPolygon(sphericalSystemDegree, location, polygon2, 0, false) );
        conn.close();
    }
    

    /**
     * Social Network Analysis
    */
    public static void example21() throws Exception {
        AGRepositoryConnection conn = example1(false);
        AGValueFactory vf = conn.getValueFactory();
        conn.add(new File(DATA_DIRECTORY, "lesmis.rdf"), null, RDFFormat.RDFXML);
        println("Loaded " + conn.size() + " lesmis.rdf triples.");
        
        // Create URIs for relationship predicates.
        String lmns = "http://www.franz.com/lesmis#";
        conn.setNamespace("lm", lmns);
        IRI knows = vf.createIRI(lmns, "knows");
        IRI barelyKnows = vf.createIRI(lmns, "barely_knows");
        IRI knowsWell = vf.createIRI(lmns, "knows_well");

        // Create URIs for some characters.
        IRI valjean = vf.createIRI(lmns, "character11");
        //URI bossuet = vf.createIRI(lmns, "character64");

        // Create some generators
        //print "\nSNA generators known (should be none): '%s'" % (conn.listSNAGenerators())
        List<IRI> intimates = new ArrayList<IRI>(1);
        Collections.addAll(intimates, knowsWell);
        conn.registerSNAGenerator("intimates", null, null, intimates, null);
        List<IRI> associates = new ArrayList<IRI>(2);
        Collections.addAll(associates, knowsWell, knows);
        conn.registerSNAGenerator("associates", null, null, associates, null);
        List<IRI> everyone = new ArrayList<IRI>(3);
        Collections.addAll(everyone, knowsWell, knows, barelyKnows);
        conn.registerSNAGenerator("everyone", null, null, everyone, null);
        println("Created three generators.");

        // Create neighbor matrix.
        List<IRI> startNodes = new ArrayList<IRI>(1);
        startNodes.add(valjean);
        conn.registerSNANeighborMatrix("matrix1", "intimates", startNodes, 2);
        conn.registerSNANeighborMatrix("matrix2", "associates", startNodes, 5);
        conn.registerSNANeighborMatrix("matrix3", "everyone", startNodes, 2);
        println("Created three matrices.");
        
        // Explore Valjean's ego group.
        println("\nValjean's ego group members (using associates).");
        String queryString = "(select (?member ?name)" +
          "(ego-group-member !lm:character11 1 associates ?member)" +
          "(q ?member !dc:title ?name))";
        TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        int count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("member");
            Value n = bindingSet.getValue("name");
            println("Member: " + p + ", name: " + n);
            count++;
        }
        println("Number of results: " + count);
        result.close();
        
        // Valjean's ego group using neighbor matrix.
        println("\nValjean's ego group (using associates matrix).");
        queryString = "(select (?member ?name)" +
          "(ego-group-member !lm:character11 1 matrix2 ?member)" +
          "(q ?member !dc:title ?name))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("member");
            Value n = bindingSet.getValue("name");
            println("Member: " + p + ", name: " + n);
            count++;
        }
        println("Number of results: " + count);
        result.close();
       
        println("\nValjean's ego group in one list depth 1 (using associates).");
        queryString = "(select (?member)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(member ?member ?group))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("member");
            println("Group: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();

        println("\nValjean's ego group in one list depth 2 (using associates).");
        queryString = "(select (?member)" +
        "(ego-group !lm:character11 2 associates ?group)" +
        "(member ?member ?group))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("member");
            println("Group: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();

        println("\nValjean's ego group in one list depth 3 (using associates).");
        queryString = "(select (?member)" +
        "(ego-group !lm:character11 3 associates ?group)" +
        "(member ?member ?group))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("member");
            println("Group: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();

        println("\nShortest breadth-first path connecting Valjean to Bossuet using intimates. (Should be no path.)");
        queryString = "(select (?node)" +
          "(breadth-first-search-path !lm:character11 !lm:character64 intimates 5 ?path)" +
          "(member ?node ?path))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("node");
            println("Path: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();
        
        println("\nShortest breadth-first path connecting Valjean to Bossuet using associates. ");
        queryString = "(select (?node)" +
          "(breadth-first-search-path !lm:character11 !lm:character64 associates 5 ?path)" +
          "(member ?node ?path))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("node");
            println("Node on path: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();


        println("\nShortest breadth-first path connecting Valjean to Bossuet using everyone.");
        queryString = "(select (?node)" +
          "(breadth-first-search-path !lm:character11 !lm:character64 everyone 5 ?path)" +
          "(member ?node ?path))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("node");
            println("Node on Path: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();
 
       
        println("\nReturn depth-first path connecting Valjean to Bossuet using associates (should be one).");
        queryString = "(select (?node)" +
          "(depth-first-search-path !lm:character11 !lm:character64 associates 10 ?path)" +
          "(member ?node ?path))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("node");
            println("Node on Path: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();
        
        println("\nShortest bidirectional path connecting Valjean to Bossuet using associates (should be two).");
        queryString = "(select (?node)" +
          "(bidirectional-search-path !lm:character11 !lm:character64 associates ?path)" +
          "(member ?node ?path))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("node");
            println("Path: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();
        
        println("\nBidirectional paths connecting Valjean to Bossuet using associates (should be two).");
        queryString = "(select (?pathid ?node)" +
                   "(bidirectional-search-paths !lm:character11 !lm:character64 associates ?path)" +
                   // make a ?pathid for ?path here, must be an RDF value
                   "(lisp ?pathid (new-blank-node))" +
                   "(member ?node ?path))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();        
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("pathid");
            Value n = bindingSet.getValue("node");
            println("Path " + p + ": " + n);
        }
        result.close();
        
        println("\nNodal degree of Valjean (should be seven).");
        queryString = "(select (?degree)" +
          "(nodal-degree !lm:character11 associates ?degree))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("degree");
            println("Degree: " + p );
            println("Degree: " + p.stringValue());
        }
        result.close();
        
        println("\nHow many neighbors are around Valjean? (should be 36).");
        queryString = "(select (?neighbors)" +
          "(nodal-degree !lm:character11 everyone ?neighbors))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("neighbors");
            println("Neighbors: " + p );
            println("Neighbors: " + p.stringValue());
        }
        result.close();
 
        println("\nWho are Valjean's neighbors? (using everyone).");
        queryString = "(select (?name)" +
          "(nodal-neighbors !lm:character11 everyone ?member)" +
          "(q ?member !dc:title ?name))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("name");
            count++;
            println(count + ". " + p.stringValue());
        }
        result.close();

        println("\nGraph density of Valjean's ego group? (using associates).");
        queryString = "(select (?density)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(graph-density ?group associates ?density))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("density");
            println("Graph density: " + p.stringValue());
        }
        result.close();
 
/*        println("\nValjean's cliques? Should be two (using associates).");
        queryString = "(select (?member)" +
          "(clique !lm:character11 associates ?clique)" +
          "(member ?member ?clique))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        count = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("member");
            println("Clique: " + p );
            count++;
        }
        println("Number of results: " + count);
        result.close();
*/        
        println("\nValjean's actor-degree-centrality to his ego group at depth 1 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(actor-degree-centrality !lm:character11 ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();
        
        println("\nValjean's actor-degree-centrality to his ego group at depth 2 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 2 associates ?group)" +
          "(actor-degree-centrality !lm:character11 ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        println("\nValjean's actor-closeness-centrality to his ego group at depth 1 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(actor-closeness-centrality !lm:character11 ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        println("\nValjean's actor-closeness-centrality to his ego group at depth 2 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 2 associates ?group)" +
          "(actor-closeness-centrality !lm:character11 ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        println("\nValjean's actor-betweenness-centrality to his ego group at depth 2 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 2 associates ?group)" +
          "(actor-betweenness-centrality !lm:character11 ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        //  "Group centrality measures the cohesion a group relative to
        //  some measure of actor-centrality. `group-degree-centrality measures
        //  group cohesion by finding the maximum actor centrality in the group,
        //  summing the difference between this and each other actor's degree
        //  centrality and then normalizing. It ranges from 0 (when all actors have
        //  equal degree) to 1 (when one actor is connected to every other and no
        //  other actors have connections."
        println("\nGroup-degree-centrality of Valjean's ego group at depth 1 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(group-degree-centrality ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        println("\nGroup-degree-centrality of Valjean's ego group at depth 2 (using associatese).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 2 associates ?group)" +
          "(group-degree-centrality ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        //  "Group centrality measures the cohesion a group relative to
        //  some measure of actor-centrality. `group-closeness-centrality` is
        //  measured by first finding the actor whose `closeness-centrality`
        //  is maximized and then summing the difference between this maximum
        //  value and the [actor-closeness-centrality][] of all other actors.
        //  This value is then normalized so that it ranges between 0 and 1."
        println("\nGroup-closeness-centrality of Valjean's ego group at depth 1 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(group-closeness-centrality ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        println("\nGroup-closeness-centrality of Valjean's ego group at depth 2 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 2 associates ?group)" +
          "(group-closeness-centrality ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        //  "Group centrality measures the cohesion a group relative to
        //  some measure of actor-centrality. `group-betweenness-centrality` is
        //  measured by first finding the actor whose `betweenness-centrality`
        //  is maximized and then summing the difference between this maximum
        //  value and the [actor-betweenness-centrality][] of all other actors.
        //  This value is then normalized so that it ranges between 0 and 1.
        println("\nGroup-betweenness-centrality of Valjean's ego group at depth 1 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 1 associates ?group)" +
          "(group-betweenness-centrality ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();

        println("\nGroup-betweenness-centrality of Valjean's ego group at depth 1 (using associates).");
        queryString = "(select (?centrality)" +
          "(ego-group !lm:character11 2 associates ?group)" +
          "(group-betweenness-centrality ?group associates ?centrality))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value p = bindingSet.getValue("centrality");
            println("Centrality: " + p.stringValue());
        }
        result.close();



        
    }

    
    /**
     * Transactions
     */
    public static void example22() throws Exception {
        AGRepository myRepository = AGServer.createRepository(REPOSITORY_ID,
                CATALOG_ID, SERVER_URL, USERNAME, PASSWORD);
        AGValueFactory vf = myRepository.getValueFactory();
        // Create conn1 (autoCommit) and conn2 (no autoCommit).
        AGRepositoryConnection conn1 = myRepository.getConnection();
        closeBeforeExit(conn1);
        conn1.clear();
        AGRepositoryConnection conn2 = myRepository.getConnection();
        closeBeforeExit(conn2);
        conn2.setSessionLifetime(120);
        conn2.begin();
        String baseURI = "http://example.org/example/local";
        conn1.add(new File(DATA_DIRECTORY, "lesmis.rdf"), baseURI, RDFFormat.RDFXML);
        println("Loaded " + conn1.size() + " lesmis.rdf triples via conn1.");
        conn2.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        println("Loaded " + conn2.size() + " java-kennedy.ntriples via conn2.");
        
        println("\nSince conn1 is in autoCommit mode, lesmis.rdf triples are committed " +
                "and retrievable via conn2.  Since conn2 is not in autoCommit mode, and " +
                "no commit() has yet been issued on conn2, kennedy.rdf triples are not " +
                " retrievable via conn1.");
        // Check transaction isolation semantics:
        Literal valjean = vf.createLiteral("Valjean");
        Literal kennedy = vf.createLiteral("Kennedy");
        printRows("\nUsing getStatements() on conn1; should find Valjean:",
                1, conn1.getStatements(null, null, valjean, false));
        printRows("\nUsing getStatements() on conn1; should not find Kennedy:",
                1, conn1.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on conn2; should not find Valjean (until a rollback or commit occurs on conn2):",
                1, conn2.getStatements(null, null, valjean, false));
        printRows("\nUsing getStatements() on conn2; should find Kennedy:",
                1, conn2.getStatements(null, null, kennedy, false));
        
        // Rollback
        println("\nRolling back contents of conn2.");
        conn2.rollback();
        println("There are now " + conn2.size() + " triples visible via conn2.");
        printRows("\nUsing getStatements() on conn1; should find Valjean:",
                1, conn1.getStatements(null, null, valjean, false));
        printRows("\nUsing getStatements() on conn1; should not find Kennedys:",
                1, conn1.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on conn2; should not find Kennedys:",
                1, conn2.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on conn2; should find Valjean:",
                1, conn2.getStatements(null, null, valjean, false));
        // Reload and Commit
        println("\nReload java-kennedy.ntriples into conn2.");
        conn2.begin(); // start a new transaction
        conn2.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        println("There are now " + conn1.size() + " triples visible on conn1.");
        println("There are now " + conn2.size() + " triples visible on conn2.");
        println("\nCommitting contents of conn2.");
        conn2.commit();
        println("There are now " + conn1.size() + " triples visible on conn1.");
        println("There are now " + conn2.size() + " triples visible on conn2.");
        printRows("\nUsing getStatements() on conn1; should find Valjean:",
                1, conn1.getStatements(null, null, valjean, false));
        printRows("\nUsing getStatements() on conn1; should find Kennedys:",
                1, conn1.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on conn2; should find Kennedys:",
                1, conn2.getStatements(null, null, kennedy, false));
        printRows("\nUsing getStatements() on conn2; should find Valjean:",
                1, conn2.getStatements(null, null, valjean, false));
    }
    /**
     * Duplicate triples and duplicate results
     */
    public static void example23() throws Exception {
        AGRepository myRepository = AGServer.createRepository(
            REPOSITORY_ID, CATALOG_ID, SERVER_URL, USERNAME, PASSWORD);
        AGRepositoryConnection conn = myRepository.getConnection();

        AGValueFactory vf = conn.getValueFactory();
        closeBeforeExit(conn);
        conn.clear();
        String baseURI = "http://example.org/";

        // Demonstrate blank node behavior. 
        conn.add(new File(DATA_DIRECTORY, "java-blankNodes1.rdf"), baseURI, RDFFormat.RDFXML);
        println("\nLoaded " + conn.size() + " java-blankNodes1.rdf triples via conn.");
        printRows("\nTwo books, with one author as blank node in each book.",
                10000, conn.getStatements(null, null, null, false));
        conn.clear();
        
        conn.add(new File(DATA_DIRECTORY, "java-blankNodes2.rdf"), baseURI, RDFFormat.RDFXML);
        println("\nLoaded " + conn.size() + " java-blankNodes2.rdf triples via conn.");
        printRows("\nTwo books, with one author identified by URI but in striped syntax in each book.",
                10000, conn.getStatements(null, null, null, false));
        conn.clear();

        conn.add(new File(DATA_DIRECTORY, "java-blankNodes3.rdf"), baseURI, RDFFormat.RDFXML);
        println("\nLoaded " + conn.size() + " java-blankNodes3.rdf triples via conn.");
        printRows("\nTwo books, with one author linked by a URI.",
                10000, conn.getStatements(null, null, null, false));
        conn.clear();

        conn.add(new File(DATA_DIRECTORY, "java-blankNodes4.rdf"), baseURI, RDFFormat.RDFXML);
        println("\nLoaded " + conn.size() + " java-blankNodes4.rdf triples via conn.");
        printRows("\nTwo books, with one author as a literal value.",
                10000, conn.getStatements(null, null, null, false));
        conn.clear();

        // Load Kennedy file.
        conn.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        println("\nAfter loading there are " + conn.size() + " kennedy triples.");
        
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        String exns = "http://www.franz.com/simple#";
        conn.setNamespace("exns", exns);
        IRI TedKennedy = vf.createIRI(exns, "person17");
        IRI hasChild = vf.createIRI(exns, "has-child");
        printRows("\nUsing getStatements() find children of Ted Kennedy: three children.",
                10000, conn.getStatements(TedKennedy, hasChild, null, false));

        println("\nSPARQL matches for two children of Ted Kennedy, inept pattern.");
        String queryString = 
            "SELECT ?o1 ?o2 " +
            "WHERE {kdy:person17 kdy:has-child ?o1 . " +
            "       kdy:person17 kdy:has-child ?o2 . }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();
        try {
            while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value o1 = bindingSet.getValue("o1");
            Value o2 = bindingSet.getValue("o2");
            println(o1 + " and " + o2);
            }    
        } finally {
            result.close();
        }

        println("\nSPARQL matches for two children of Ted Kennedy, better pattern.");
        String queryString2 = 
         "SELECT ?o1 ?o2 " +
            "WHERE {kdy:person17 kdy:has-child ?o1 . " +
            "       kdy:person17 kdy:has-child ?o2 . " +
            "       filter (?o1 < ?o2)}";
        TupleQuery tupleQuery2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString2);
        TupleQueryResult result2 = tupleQuery2.evaluate();
        try {
            while (result2.hasNext()) {
            BindingSet bindingSet = result2.next();
            Value o1 = bindingSet.getValue("o1");
            Value o2 = bindingSet.getValue("o2");
            println(o1 + " and " + o2);
            }    
        } finally {
         result2.close();
        }
       
        println("\nProlog select query to parallel the previous SPARQL query.");
        queryString = "(select (?o1 ?o2)" +
          "(q !kdy:person17 !kdy:has-child ?o1)" +
          "(q !kdy:person17 !kdy:has-child ?o2)" +
          "(lispp (upi< ?o1 ?o2)))";
        tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        result = tupleQuery.evaluate();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value o1 = bindingSet.getValue("o1");
            Value o2 = bindingSet.getValue("o2");
            println(o1.stringValue() + " and " + o2.stringValue());
        }
        result.close();

        println("\nSPARQL matches for two children of Ted Kennedy, even better pattern.");
        String queryString3 = 
         "SELECT ?o1 ?o2 " +
            "WHERE {kdy:person17 kdy:has-child ?o1 . " +
            "       kdy:person17 kdy:has-child ?o2 . " +
            "       filter (?o1 < ?o2)}" +
            "       LIMIT 1";
        TupleQuery tupleQuery3 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString3);
        TupleQueryResult result3 = tupleQuery3.evaluate();
        try {
            while (result3.hasNext()) {
            BindingSet bindingSet = result3.next();
            Value o1 = bindingSet.getValue("o1");
            Value o2 = bindingSet.getValue("o2");
            println(o1 + " and " + o2);
            }    
        } finally {
            result3.close();
        }

        // Load Kennedy triples again.
        println("\nReload 1214 java-kennedy.ntriples.");
        conn.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        println("\nAfter loading there are " + conn.size() + " kennedy triples.");

        printRows("\nUsing getStatements() find children of Ted Kennedy: duplicate triples present.",
                10000, conn.getStatements(TedKennedy, hasChild, null, false));

        println("\nSPARQL matches for children of Ted Kennedy.");
        String queryString4 = 
            "SELECT ?o WHERE {kdy:person17 kdy:has-child ?o} ORDER BY ?o";
        TupleQuery tupleQuery4 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString4);
        TupleQueryResult result4 = tupleQuery4.evaluate();
        try {
            while (result4.hasNext()) {
            BindingSet bindingSet = result4.next();
            Value o = bindingSet.getValue("o");
            println(o);
            }    
        } finally {
            result4.close();
        }

        println("\nSPARQL DISTINCT matches for children of Ted Kennedy.");
        String queryString5 = 
            "SELECT DISTINCT ?o WHERE {kdy:person17 kdy:has-child ?o} ORDER BY ?o";
        TupleQuery tupleQuery5 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString5);
        TupleQueryResult result5 = tupleQuery5.evaluate();
        try {
            while (result5.hasNext()) {
            BindingSet bindingSet = result5.next();
            Value o = bindingSet.getValue("o");
            println(o);
            }    
        } finally {
            result5.close();
        }
        
        println("\nSPARQL REDUCED matches for children of Ted Kennedy.");
        String queryString6 = 
            "SELECT REDUCED ?o WHERE {kdy:person17 kdy:has-child ?o} ORDER BY ?o";
        TupleQuery tupleQuery6 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString6);
        TupleQueryResult result6 = tupleQuery6.evaluate();
        try {
            while (result6.hasNext()) {
            BindingSet bindingSet = result6.next();
            Value o = bindingSet.getValue("o");
            println(o);
            }    
        } finally {
            result6.close();
        }
        
        println("\nSPARQL matches for children of Ted Kennedy, limit 2.");
        String queryString7 = 
            "SELECT ?o WHERE {kdy:person17 kdy:has-child ?o} LIMIT 2";
        TupleQuery tupleQuery7 = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString7);
        TupleQueryResult result7 = tupleQuery7.evaluate();
        try {
            while (result7.hasNext()) {
            BindingSet bindingSet = result7.next();
            Value o = bindingSet.getValue("o");
            println(o);
            }    
        } finally {
            result7.close();
        }

        // Explicit duplicate deletion
        println("\nDuplicate deletion demo:");
        conn.clear();
        conn.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        conn.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        
        println("Triple count before duplicate deletion: " + conn.size());
        conn.deleteDuplicates("spog");
        println("Triple count after duplicate deletion: " + conn.size());

        // Enable duplicate suppression
        final String oldPolicy = myRepository.getDuplicateSuppressionPolicy();
        myRepository.setDuplicateSuppressionPolicy("spog");
        println("Trying to import the same set of triples a second time.");
        conn.add(new File(DATA_DIRECTORY, "java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES);
        println("Triple count after import: " + conn.size());
        
        // Disable duplicate suppression to avoid problem in further examples
        myRepository.setDuplicateSuppressionPolicy(oldPolicy);
        
        conn.close();
        myRepository.shutDown();
    }
    
    /**
     * Connection pooling
     * 
     * Demonstrates how to create and configure a connection pool, associate it with
     * a particular repository, and borrow/return connections.
     * 
     */    
    public static void example24() throws Exception { 
        println("\nStarting example24().");
        
        // Set up our connection pool.
        AGConnPool pool = AGConnPool.create(
            	AGConnProp.serverUrl, SERVER_URL,
            	AGConnProp.username, USERNAME,
            	AGConnProp.password, PASSWORD,
            	AGConnProp.catalog, CATALOG_ID,
            	AGConnProp.repository, REPOSITORY_ID,
            	AGConnProp.session, AGConnProp.Session.DEDICATED,
            	AGPoolProp.shutdownHook, true,
            	AGPoolProp.maxActive, 10,
            	AGPoolProp.initialSize, 2);

        // Get a reference to an AGRepository instance
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGRepository repo =
            	server.createRepository(REPOSITORY_ID, CATALOG_ID, SERVER_URL, USERNAME, PASSWORD);

        // Assign our pool to the repository
        repo.setConnPool(pool);
        
        // The way to acquire a connection from an AGRepository
        // instance is the same when using a connection pool or not using
        // one. When called, getConnection() sees the pool saved to the
        // Repository via setConnPool() and borrows a connection from it. If
        // no pool is assigned, an independent connection is created
        // and returned.
        AGRepositoryConnection conn = repo.getConnection();
        
        // Now that we have a connection, we can create and add some triples.
        AGValueFactory vf = conn.getRepository().getValueFactory();          
        
        println("pool getNumActive is: "+pool.getNumActive());
        println("pool getNumIdle is: "+pool.getNumIdle());        

        try {            
            IRI alice = vf.createIRI("http://example.org/people/alice");
            IRI bob = vf.createIRI("http://example.org/people/bob");
            IRI name = vf.createIRI("http://example.org/ontology/name");
            IRI person = vf.createIRI("http://example.org/ontology/Person");
            Literal bobsName = vf.createLiteral("Bob");
            Literal alicesName = vf.createLiteral("Alice");
            println("Triple count before inserts: " + 
            		(conn.size()));
            conn.add(alice, name, alicesName);            
            conn.add(alice, RDF.TYPE, person);            
            conn.add(bob, name, bobsName);            
            conn.add(bob, RDF.TYPE, person);            
            conn.commit();
        } finally {
            // To return a connection to the pool use conn.close(). Connections that
            // do not belong to a pool will simply be closed.
            conn.close(); 
            println("pool getNumActive is: "+pool.getNumActive());
            println("pool getNumIdle is: "+pool.getNumIdle());        
        }

    }
    
    /**
     * Usage: all
     * Usage: [1-24]+
     */
    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        List<Integer> choices = new ArrayList<Integer>();
        if (args.length == 0 || args[0].equals("all")) {
            for (int i = 1; i <= 24; i++) {
                choices.add(i);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                choices.add(Integer.parseInt(args[i]));
            }
        }
        try {
            for (Integer choice : choices) {
                println("\n** Running example " + choice);
                switch(choice) {
                case 1: example1(true); break;
                case 2: example2(true); break;
                case 3:
                    example3();
                    example3a();
                    break;
                case 4: example4(); break;
                case 5: example5(); break;
                case 6: example6(); break;
                case 7: example7(); break;
                case 8: example8(); break;
                case 9: example9(); break;
                case 10: example10(); break;
                case 11: example11(); break;
                case 12: example12(); break;
                case 13: example13(); break;
                case 14: example14(); break;
                case 15: example15(); break;
                case 16: example16(); break;
                case 17: example17(); break;
                case 18: example18(); break;
                case 19: example19(); break;
                case 20: example20(); break;
                case 21: example21(); break;
                case 22: example22(); break;
                case 23: example23(); break;
                case 24: example24(); break;
                default: println("Example" + choice + "() is not available in this release.");
                }
            }
        } finally {
            closeAll();
            println("Elapsed time: " + (System.currentTimeMillis() - now)/1000.00 + " seconds.");
        }
    }
    
    public static void println(Object x) {
        System.out.println(x);
    }
    
    static void printRows(RepositoryResult<Statement> rows) throws Exception {
        while (rows.hasNext()) {
            println(rows.next());
        }
        rows.close();
    }

    static void printRows(String headerMsg, int limit, RepositoryResult<Statement> rows) throws Exception {
        println(headerMsg);
        int count = 0;
        while (count < limit && rows.hasNext()) {
            println(rows.next());
            count++;
        }
        println("Number of results: " + count);
        rows.close();
    }

    static void printRows(String headerMsg, TupleQueryResult rows) throws Exception {
        println(headerMsg);
        try {
            while (rows.hasNext()) {
            	println(rows.next());
            }
        } finally {
            rows.close();
        }
    }

    static void close(RepositoryConnection conn) {
        try {
            conn.close();
        } catch (Exception e) {
            System.err.println("Error closing repository connection: " + e);
            e.printStackTrace();
        }
    }
    
    private static List<RepositoryConnection> toClose = new ArrayList<RepositoryConnection>();
    
    /**
     * This is just a quick mechanism to make sure all connections get closed.
     */
    private static void closeBeforeExit(RepositoryConnection conn) {
        toClose.add(conn);
    }
    
    private static void closeAll() {
        while (!toClose.isEmpty()) {
            RepositoryConnection conn = toClose.get(0);
            close(conn);
            while (toClose.remove(conn)) {
                // ...
            }
        }
    }
}
