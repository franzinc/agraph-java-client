/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.tutorial.jena;

import com.franz.agraph.jena.*;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.repository.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JenaTutorialExamples {
    private static final String HOST = getenv("AGRAPH_HOST", "localhost");
    private static final String PORT = getenv("AGRAPH_PORT", "10035");
    
    public static final String SERVER_URL = "http://" + HOST + ":" + PORT;
    public static final String CATALOG_ID = "java-catalog";
    public static final String REPOSITORY_ID = "jenatutorial";
    public static final String USERNAME = getenv("AGRAPH_USER", "test");
    public static final String PASSWORD = getenv("AGRAPH_PASS", "xyzzy");
    public static final File DATA_DIR =
        new File(getenv("AGRAPH_DATA", "../data/"));
    public static final String TEMPORARY_DIRECTORY = getenv("AGRAPH_TEMP", "");

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
     * Creating a Repository
     */
    public static AGGraphMaker example1(boolean close) throws Exception {
        // Tests getting the repository up.
        println("\nStarting example1().");
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);

        try {
            println("Available catalogs: " + server.listCatalogs());
        } catch (Exception e) {
            throw new Exception("Got error when attempting to connect to server at "
                                + SERVER_URL + ": " + e);
        }

        AGCatalog catalog = server.getCatalog(CATALOG_ID);

        if (catalog == null) {
            throw new Exception("Catalog " + CATALOG_ID + " does not exist. Either "
                            + "define this catalog in your agraph.cfg or modify the CATALOG_ID "
                            + "in this tutorial to name an existing catalog.");
        }
        
        println("Available repositories in catalog "
        		+ (catalog.getCatalogName()) + ": "
        		+ catalog.listRepositories());
        closeAll();
        catalog.deleteRepository(REPOSITORY_ID);
        AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
        println("Got a repository.");
        myRepository.initialize();
        println("Initialized repository.");
        AGRepositoryConnection conn = myRepository.getConnection();
        closeBeforeExit(conn);
        println("Got a connection.");
        println("Repository " + (myRepository.getRepositoryID())
        		+ " is up! It contains " + (conn.size()) + " statements.");
        AGGraphMaker maker = new AGGraphMaker(conn);
        println("Got a graph maker for the connection.");
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
        	maker.close();
        	conn.close();
        	myRepository.shutDown();
        	return null;
        }
        return maker;
    }

    /**
     * Asserting and Retracting Triples
     */
    public static AGModel example2(boolean close) throws Exception {
        AGGraphMaker maker = example1(false);
        AGGraph graph = maker.getGraph();
        AGModel model = new AGModel(graph);
        println("\nStarting example2().");
        // Create some resources and literals to make statements from.
        Resource alice = model
        		.createResource("http://example.org/people/alice");
        Resource bob = model.createResource("http://example.org/people/bob");
        Property name = model
        		.createProperty("http://example.org/ontology/name");
        Resource person = model
        		.createResource("http://example.org/ontology/Person");
        Literal bobsName = model.createLiteral("Bob");
        Literal alicesName = model.createLiteral("Alice");
        println("Triple count before inserts: " + model.size());
        // Alice's name is "Alice"
        model.add(alice, name, alicesName);
        // Alice is a person
        model.add(alice, RDF.type, person);
        // Bob's name is "Bob"
        model.add(bob, name, bobsName);
        // Bob is a person, too.
        model.add(bob, RDF.type, person);
        println("Added four triples.");
        println("Triple count after inserts: " + (model.size()));
        StmtIterator result = model.listStatements();
        while (result.hasNext()) {
        	Statement st = result.next();
        	println(st);
        }
        model.remove(bob, name, bobsName);
        println("Removed one triple.");
        println("Triple count after deletion: " + (model.size()));
        // put it back so we can continue with other examples
        model.add(bob, name, bobsName);
        if (close) {
        	model.close();
        	graph.close();
        	maker.close();
        	return null;
        }
        return model;
    }

    /**
     * A SPARQL Query
     */
    public static void example3() throws Exception {
        AGModel model = example2(false);
        println("\nStarting example3().");
        try {
        	String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
        	AGQuery sparql = AGQueryFactory.create(queryString);
        	QueryExecution qe = AGQueryExecutionFactory.create(sparql, model);
        	try {
        		ResultSet results = qe.execSelect();
        		while (results.hasNext()) {
        			QuerySolution result = results.next();
        			RDFNode s = result.get("s");
        			RDFNode p = result.get("p");
        			RDFNode o = result.get("o");
        			// System.out.format("%s %s %s\n", s, p, o);
        			System.out.println(" { " + s + " " + p + " " + o + " . }");
        		}
        	} finally {
        		qe.close();
        	}
        } finally {
        	model.close();
        }
    }

    /**
     * Statement Matching
     */
    public static void example4() throws Exception {
        AGModel model = example2(false);
        println("\nStarting example4().");
        Resource alice = model
        		.createResource("http://example.org/people/alice");
        StmtIterator statements = model.listStatements(alice, null,
        		(RDFNode) null);
        try {
        	while (statements.hasNext()) {
        		println(statements.next());
        	}
        } finally {
        	statements.close();
        	model.close();
        }
    }

    /**
     * Literal Values
     */
    public static void example5() throws Exception {
        AGModel model = example2(false);
        println("\nStarting example5().");
        model.removeAll();
        String exns = "http://example.org/people/";
        Resource alice = model.createResource("http://example.org/people/alice");
        Resource ted = model.createResource(exns + "ted");
        Property age = model.createProperty(exns,"age");
        Property weight = model.createProperty(exns, "weight");
        Property favoriteColor = model.createProperty(exns, "favoriteColor");
        Property birthdate = model.createProperty(exns, "birthdate");
        Literal red = model.createLiteral("Red");
        Literal rouge = model.createLiteral("Rouge", "fr");
        Literal fortyTwoInt = model.createTypedLiteral("42", XSDDatatype.XSDint);
        Literal fortyTwoLong = model.createTypedLiteral("42", XSDDatatype.XSDlong);
        Literal fortyTwoUntyped = model.createLiteral("42");
        Literal date = model.createTypedLiteral("1984-12-06", XSDDatatype.XSDdate);
        Literal time = model.createTypedLiteral("1984-12-06T09:00:00", XSDDatatype.XSDdateTime);
        Literal weightUntyped = model.createLiteral("120.5");
        Literal weightFloat = model.createTypedLiteral("120.5", XSDDatatype.XSDfloat);
        Statement stmt1 = model.createStatement(alice, age, fortyTwoInt);
        Statement stmt2 = model.createStatement(ted, age, fortyTwoLong);
        Statement stmt3 = model.createStatement(ted, age, fortyTwoUntyped);
        model.add(stmt1);
        model.add(stmt2);
        model.add(stmt3);
        model.add(alice, weight, weightFloat);
        model.add(ted, weight, weightUntyped);
        model.add(alice, favoriteColor, red);
        model.add(ted, favoriteColor, rouge);
        model.add(alice, birthdate, date);
        model.add(ted, birthdate, time);
        for (Literal obj : new Literal[] {null, fortyTwoInt, fortyTwoLong, fortyTwoUntyped,  weightFloat, weightUntyped,
                    red, rouge}) {
            println( "\nRetrieve triples matching " + obj + ".");
            StmtIterator statements = model.listStatements(null, null, obj);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        // SPARQL
        for (String obj : new String[]{"42", "\"42\"", "120.5", "\"120.5\"", "\"120.5\"^^xsd:float",
                                       "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"}) {
            println( "\nQuery triples matching " + obj + ".");
            String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" + 
                "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
            AGQuery query = AGQueryFactory.create(queryString);
            QueryExecution qe = AGQueryExecutionFactory.create(query, model);
        	try {
        		ResultSet results = qe.execSelect();
        		while (results.hasNext()) {
        			QuerySolution result = results.next();
        			RDFNode s = result.get("s");
        			RDFNode p = result.get("p");
        			RDFNode o = result.get("o");
        			println("  " + s + " " + p + " " + o);
        		}
        	} finally {
        		qe.close();
        	}
        }
        {
            // Search for date using date object in triple pattern.
            println("\nRetrieve triples matching DATE object.");
             StmtIterator statements = model.listStatements(null, null, date);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        {
            println("\nMatch triples having a specific DATE value.");
            StmtIterator statements = model.listStatements(null, null,
                    model.createTypedLiteral("1984-12-06",XSDDatatype.XSDdate));
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        {
            // Search for time using datetime object in triple pattern.
            println("\nRetrieve triples matching DATETIME object.");
            StmtIterator statements = model.listStatements(null, null, time);
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        {
            println("\nMatch triples having a specific DATETIME value.");
            StmtIterator statements = model.listStatements(null, null,
                    model.createTypedLiteral("1984-12-06T09:00:00",XSDDatatype.XSDdateTime));
            try {
                while (statements.hasNext()) {
                    println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        model.close();
    }

    /**
     * Importing Triples
     */

    public static AGGraphMaker example6() throws Exception {
        AGGraphMaker maker = example1(false);
        AGModel model = new AGModel(maker.getGraph());
        AGModel model_vcards = new AGModel(maker.createGraph("http://example.org#vcards"));
        final File path1 = new File(DATA_DIR, "java-vcards.rdf");
        final File path2 = new File(DATA_DIR, "java-kennedy.ntriples");
        String baseURI = "http://example.org/example/local";
        try (final InputStream is = new FileInputStream(path1)) {
        	model_vcards.read(is, baseURI);
        }
        try (final InputStream is = new FileInputStream(path2)) {
        	model.read(is, baseURI, "N-TRIPLE");
        }
        println("After loading, model_vcards contains " + model_vcards.size()
        		+ " triples in graph '" + model_vcards.getGraph().getName() 
        		+ "'\n    and model contains " + model.size() 
        		+ " triples in graph '" + model.getGraph().getName() + "'.");
        return maker;
    }

    /**
     * Importing Triples, query
     */
    public static void example7() throws Exception {
        AGGraphMaker maker = example6();
        AGModel model = new AGModel(maker.getGraph());
        AGModel model_vcards = new AGModel(maker.openGraph("http://example.org#vcards"));
        println("\nMatch all and print subjects and graph (model)");
        String graphName = model.getGraph().getName();
        StmtIterator statements = model.listStatements();
        for (int i = 0; i < 25 && statements.hasNext(); i++) {
        	Statement stmt = statements.next();
        	println(stmt.getSubject() + "  " + graphName);
        }
        println("\nMatch all and print subjects and graph (model_vcards)");
        String vcardsName = model_vcards.getGraph().getName();
        statements = model_vcards.listStatements();
        for (int i = 0; i < 25 && statements.hasNext(); i++) {
        	Statement stmt = statements.next();
        	println(stmt.getSubject() + "  " + vcardsName);
        }
        statements.close();
        
        println("\nSPARQL query over the default graph (model).");
        String queryString = "SELECT DISTINCT ?s ?p ?o WHERE {?s ?p ?o . } LIMIT 25";
        AGQuery query = AGQueryFactory.create(queryString);
        QueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
        	ResultSet results = qe.execSelect();
        	while (results.hasNext()) {
        		QuerySolution result = results.next();
        		RDFNode s = result.get("s");
        		RDFNode p = result.get("p");
        		RDFNode o = result.get("o");
        		println("  " + s + " " + p + " " + o);
        	}
        } finally {
        	qe.close();
        }
        println("\nSPARQL query over the named graph (model_vcards).");
        qe = AGQueryExecutionFactory.create(query, model_vcards);
        try {
        	ResultSet results = qe.execSelect();
        	while (results.hasNext()) {
        		QuerySolution result = results.next();
        		RDFNode s = result.get("s");
        		RDFNode p = result.get("p");
        		RDFNode o = result.get("o");
        		println("  " + s + " " + p + " " + o);
        	}
        } finally {
        	qe.close();
        }
        println("\nSPARQL query for triples in any named graph (model).");
        queryString = "SELECT DISTINCT ?s ?p ?o ?g WHERE {graph ?g {?s ?p ?o .} } LIMIT 25";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
        try {
        	ResultSet results = qe.execSelect();
        	while (results.hasNext()) {
        		QuerySolution result = results.next();
        		RDFNode s = result.get("s");
        		RDFNode p = result.get("p");
        		RDFNode o = result.get("o");
        		RDFNode g = result.get("g");
        		println("  " + s + " " + p + " " + o + " " + g);
        	}
        } finally {
        	qe.close();
        }
        println("\nSPARQL query for triples in any named graph (model_vcards).");
        qe = AGQueryExecutionFactory.create(query, model_vcards);
        try {
        	ResultSet results = qe.execSelect();
        	while (results.hasNext()) {
        		QuerySolution result = results.next();
        		RDFNode s = result.get("s");
        		RDFNode p = result.get("p");
        		RDFNode o = result.get("o");
        		RDFNode g = result.get("g");
        		println("  " + s + " " + p + " " + o + " " + g);
        	}
        } finally {
        	qe.close();
        }
    }

    /**
     * Writing RDF coalesce NTriples to a file
     */
    public static void example8() throws Exception {
        AGGraphMaker maker = example6();
        AGModel model = new AGModel(maker.getGraph());
        //AGModel model_vcards = new AGModel(maker.openGraph("http://example.org#vcards"));
        String outputFile = TEMPORARY_DIRECTORY + "JenaTutorialExamples.example8.nt";
        // outputFile = null;
        if (outputFile == null) {
        	println("\nWriting n-triples to Standard Out instead of to a file");
        } else {
        	println("\nWriting n-triples to: " + outputFile);
        }
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStream output = (outputFile != null) ? fos : System.out;
        model.write(output, "N-TRIPLE");
        output.close();
        String outputFile2 = TEMPORARY_DIRECTORY + "JenaTutorialExamples.example8.rdf";
        // outputFile2 = null;
        if (outputFile2 == null) {
        	println("\nWriting RDF to Standard Out instead of to a file");
        } else {
        	println("\nWriting RDF to: " + outputFile2);
        }
        FileOutputStream fos2 = new FileOutputStream(outputFile2);
        output = (outputFile2 != null) ? fos2 : System.out;
        model.write(output);
        output.close();
        fos.close();
        fos2.close();
    }

    /**
     * Writing the result of a statements match.
     */
    public static void example9() throws Exception {
        AGGraphMaker maker = example6();
        AGModel model = new AGModel(maker.getGraph());
        StmtIterator statements = model.listStatements(null,RDF.type, (RDFNode)null);
        Model m = ModelFactory.createDefaultModel();
        m.add(statements);
        m.write(System.out);
    }

    /**
     * Namespaces
     */
    public static void example11() throws Exception {
        AGGraphMaker maker = example1(false);
        AGModel model = new AGModel(maker.getGraph());
        String exns = "http://example.org/people/";
        Resource alice = model.createResource(exns + "alice");
        Resource person = model.createResource(exns + "Person");
        model.add(alice, RDF.type, person);
        model.setNsPrefix("ex", exns);
        String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }";
        AGQuery query = AGQueryFactory.create(queryString);
        QueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
        	ResultSet results = qe.execSelect();
        	while (results.hasNext()) {
        		println(results.next());
        	}
        } finally {
        	qe.close();
        }
    }

    /**
     * Text search
     */
    public static void example12() throws Exception {
        AGGraphMaker maker = example1(false);
        AGGraph graph = maker.getGraph();
        AGModel model = new AGModel(graph);
        String exns = "http://example.org/people/";
        model.setNsPrefix("ex", exns);
        // Create index1
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        Property fullname = model.createProperty(exns + "fullname");
        AGRepositoryConnection conn = maker.getRepositoryConnection();
        config.getPredicates().add( // need a Sesame URI
        		conn.getValueFactory().createIRI(exns, "fullname"));
        conn.createFreetextIndex("index1", config);
        // Create parts of person resources.
        Resource alice = model.createResource(exns + "alice1");
        Resource carroll = model.createResource(exns + "carroll");
        Resource persontype = model.createResource(exns + "Person");
        Literal alicename = model.createLiteral("Alice B. Toklas");
        Literal lewisCarroll = model.createLiteral("Lewis Carroll");
        // Create parts of book resources.
        Resource book = model.createResource(exns + "book1");
        Resource booktype = model.createResource(exns + "Book");
        Property booktitle = model.createProperty(exns + "title");
        Property author = model.createProperty(exns + "author");
        Literal wonderland = model.createLiteral("Alice in Wonderland");
        // Add Alice B. Toklas triples
        model.removeAll();
        model.add(alice, RDF.type, persontype);
        model.add(alice, fullname, alicename);
        // Add Alice in Wonderland triples
        model.add(book, RDF.type, booktype);
        model.add(book, booktitle, wonderland);
        model.add(book, author, carroll);
        // Add Lewis Carroll triples
        model.add(carroll, RDF.type, persontype);
        model.add(carroll, fullname, lewisCarroll);
        // Check triples
        StmtIterator statements = model.listStatements();
        try {
        	while (statements.hasNext()) {
        		println(statements.next());
        	}
        } finally {
        	statements.close();
        }

        println("\nWhole-word match for 'Alice'.");
        String queryString = "SELECT ?s ?p ?o " + "WHERE { ?s ?p ?o . "
        		+ "        ?s fti:match 'Alice' . }";
        AGQuery sparql = AGQueryFactory.create(queryString);
        QueryExecution qe = AGQueryExecutionFactory.create(sparql, model);
        ResultSet results = qe.execSelect();
        while (results.hasNext()) {
        	println(results.next());
        }

        println("\nWildcard match for 'Ali*'.");
        queryString = "SELECT ?s ?p ?o "
        		+ "WHERE { ?s ?p ?o . ?s fti:match 'Ali*' . }";
        sparql = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(sparql, model);
        results = qe.execSelect();
        while (results.hasNext()) {
        	println(results.next());
        }

        println("\nWildcard match for '?l?ce?.");
        queryString = "SELECT ?s ?p ?o "
        		+ "WHERE { ?s ?p ?o . ?s fti:match '?l?c?' . }";
        sparql = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(sparql, model);
        results = qe.execSelect();
        while (results.hasNext()) {
        	println(results.next());
        }

        println("\nSubstring match for 'lic'.");
        queryString = "SELECT ?s ?p ?o "
        		+ "WHERE { ?s ?p ?o . FILTER regex(?o, \"lic\") }";
        sparql = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(sparql, model);
        results = qe.execSelect();
        while (results.hasNext()) {
        	println(results.next());
        }
    }    
    
    /**
     * Ask, Construct, Describe, and Update
     */
    public static void example13() throws Exception {
        AGGraphMaker maker = example6();
        AGModel model = new AGModel(maker.getGraph());
        model.setNsPrefix("kdy", "http://www.franz.com/simple#");
        // We don't want the vcards this time. This is how to delete an entire subgraph.
        maker.removeGraph("http://example.org#vcards");
        println("\nRemoved vcards.");
        // SELECT query
        String queryString = "select ?s where { ?s rdf:type kdy:person} limit 5";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        println("\nSELECT some persons:");        
        try {
        	ResultSet results = qe.execSelect();
        	while (results.hasNext()) {
        		println(results.next());
        	}
        } finally {
        	qe.close();
        }
        // ASK query
        queryString = "ask { ?s kdy:first-name 'John' } ";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
        try {
        	println("\nASK: Is there anyone named John? " + qe.execAsk());
        } finally {
        	qe.close();
        }
        queryString = "ask { ?s kdy:first-name 'Alice' } ";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
        try {
        	println("\nASK: Is there anyone named Alice? " + qe.execAsk());
        } finally {
        	qe.close();
        }
        // CONSTRUCT query
        println("\nConstructing has-grandchild triples.");
        queryString = "construct {?a kdy:has-grandchild ?c}" + 
        "    where { ?a kdy:has-child ?b . " +
        "            ?b kdy:has-child ?c . }";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
        try {
        	Model m = qe.execConstruct();
        	model.add(m);   // add new triples to the store
        } finally {
        	qe.close();
        }
        // DESCRIBE query
        queryString = "describe ?s ?o where { ?s kdy:has-grandchild ?o . } limit 1";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
        try {
        	Model m = qe.execDescribe();
        	println("\nDescribe one grandparent and one grandchild:");
        	m.write(System.out);
        } finally {
        	qe.close();
        }
        // Update
        queryString = "insert data { kdy:person4 kdy:nickname 'Jack'}";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
        try {
        	qe.execUpdate();
        } finally {
        	qe.close();
        }
        
    }
    /**
     * Prolog queries
     */
    public static void example17() throws Exception {
        AGGraphMaker maker = example6();
        AGGraph graph = maker.getGraph();
        AGModel model = new AGModel(graph);
        try {
        model.setNsPrefix("kdy", "http://www.franz.com/simple#");
        String rules1 =
            "(<-- (woman ?person) ;; IF\n" +
            "     (q ?person !kdy:sex !kdy:female)\n" +
            "     (q ?person !rdf:type !kdy:person))\n" +
            "(<-- (man ?person) ;; IF\n" +
            "     (q ?person !kdy:sex !kdy:male)\n" +
            "     (q ?person !rdf:type !kdy:person))";
        maker.getRepositoryConnection().addRules(rules1);
        println("\nFirst and Last names of all \"men.\"");
        String queryString =
            "(select (?first ?last)\n" +
            "        (man ?person)\n" +
            "        (q ?person !kdy:first-name ?first)\n" +
            "        (q ?person !kdy:last-name ?last))";
        	AGQuery prolog = AGQueryFactory.create(AGQueryLanguage.PROLOG,queryString);
        	QueryExecution qe = AGQueryExecutionFactory.create(prolog, model);
        	try {
        		ResultSet results = qe.execSelect();
        		while (results.hasNext()) {
        			QuerySolution result = results.next();
        			RDFNode f = result.get("first");
        			RDFNode l = result.get("last");
        			System.out.println(f + " " + l);
        		}
        	} finally {
        		qe.close();
        	}
        } finally {
        	model.close();
        }
    }

    /**
     * Loading Prolog rules
     */
    public static void example18() throws Exception {
        AGGraphMaker maker = example6();
        AGGraph graph = maker.createGraph("http://example18.org");
        AGModel model = new AGModel(graph);
        try {
        	model.setNsPrefix("kdy", "http://www.franz.com/simple#");
        	model.setNsPrefix("rltv", "http://www.franz.com/simple#");
        	final File path = new File(DATA_DIR, "java-rules.txt");
        	try (final InputStream is = new FileInputStream(path)) {
        		maker.getRepositoryConnection().addRules(is);
        	}
        	String queryString = "(select (?ufirst ?ulast ?cfirst ?clast)"
        			+ "(uncle ?uncle ?child)" + "(name ?uncle ?ufirst ?ulast)"
        			+ "(name ?child ?cfirst ?clast))";
        	AGQuery prolog = AGQueryFactory.create(AGQueryLanguage.PROLOG,
        			queryString);
        	QueryExecution qe = AGQueryExecutionFactory.create(prolog, model);
        	try {
        		ResultSet results = qe.execSelect();
        		while (results.hasNext()) {
        			QuerySolution result = results.next();
        			RDFNode ufirst = result.get("ufirst");
        			RDFNode ulast = result.get("ulast");
        			String ufull = ufirst + " " + ulast;
        			RDFNode cfirst = result.get("cfirst");
        			RDFNode clast = result.get("clast");
        			String cfull = cfirst + " " + clast;
        			println(ufull + " is the uncle of " + cfull);
        		}
        	} finally {
        		qe.close();
        	}
        } finally {
        	model.close();
        }
    }

    /**
     * RDFS++ Reasoning
     */
    public static void example19() throws Exception {
        AGGraphMaker maker = example1(false);
        AGModel model = new AGModel(maker.getGraph());
        Resource robert = model.createResource("http://example.org/people/robert");
        Resource roberta = model.createResource("http://example.org/people/roberta");
        Resource bob = model.createResource("http://example.org/people/bob");
        Resource bobby = model.createResource("http://example.org/people/bobby");
        // create name and child predicates, and Person class.
        Property name = model.createProperty("http://example.org/ontology/name");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        Resource person = model.createResource("http://example.org/ontology/Person");
        // create literal values for names
        Literal bobsName = model.createLiteral("Bob");
        Literal bobbysName = model.createLiteral("Bobby");
        Literal robertsName = model.createLiteral("Robert");
        Literal robertasName = model.createLiteral("Roberta");
        // Robert, Bob, and children are people
        model.add(robert, RDF.type, person);
        model.add(roberta, RDF.type, person);
        model.add(bob, RDF.type, person);
        model.add(bobby, RDF.type, person);
        // They all have names.
        model.add(robert, name, robertsName);
        model.add(roberta, name, robertasName);
        model.add(bob, name, bobsName);
        model.add(bobby, name, bobbysName);
        // robert has a child
        model.add(robert, fatherOf, roberta);
        // bob has a child
        model.add(bob, fatherOf, bobby);
        // Bob is the same person as Robert
        model.add(bob, OWL.sameAs, robert);

        // List the children of Robert, with inference OFF.
        println("\nChildren of Robert, inference OFF");
        printRows(model.listStatements(robert, fatherOf, (RDFNode) null));
        // List the children of Robert with inference ON. The owl:sameAs
        // link combines the children of Bob with those of Robert.
        println("\nChildren of Robert, inference ON");
        AGReasoner reasoner = new AGReasoner();
        InfModel infmodel = new AGInfModel(reasoner, model);
        printRows(infmodel.listStatements(robert, fatherOf, (RDFNode)null));
        // Remove the owl:sameAs link so we can try the next example.
        model.remove(bob, OWL.sameAs, robert);

        // Define new predicate, hasFather, as the inverse of fatherOf.
        Property hasFather = model.createProperty("http://example.org/ontology/hasFather");
        model.add(hasFather, OWL.inverseOf, fatherOf);
        // Search for people who have fathers, even though there are no
        // hasFather triples.
        // With inference OFF.
        println("\nPeople with fathers, inference OFF");
        printRows(model.listStatements(null, hasFather, (RDFNode)null));
        // With inference ON. The owl:inverseOf link allows AllegroGraph to
        // deduce the inverse links.
        println("\nPeople with fathers, inference ON");
        printRows(infmodel.listStatements(null, hasFather, (RDFNode)null));
        // Remove owl:inverseOf property.
        model.remove(hasFather, OWL.inverseOf, fatherOf);

        Property parentOf = model.createProperty("http://example.org/ontology/parentOf");
        model.add(fatherOf, RDFS.subPropertyOf, parentOf);
        // Now search for inferred parentOf links.
        // Search for parentOf links, even though there are no parentOf triples.
        // With inference OFF.
        println("\nPeople with parents, inference OFF");
        printRows(model.listStatements(null, parentOf, (RDFNode)null));
        // With inference ON. The rdfs:subpropertyOf link allows AllegroGraph to
        // deduce that fatherOf links imply parentOf links.
        println("\nPeople with parents, inference ON");
        printRows(infmodel.listStatements(null, parentOf, (RDFNode)null));
        model.remove(fatherOf, RDFS.subPropertyOf, parentOf);

        // The next example shows rdfs:range and rdfs:domain in action.
        // We'll create two new rdf:type classes. Note that classes are
        // capitalized.
        Resource parent = model.createResource("http://example.org/ontology/Parent");
        Resource child = model.createResource("http://example.org/ontology/Child");
        // The following triples say that a fatherOf link points from a parent
        // to a child.
        model.add(fatherOf, RDFS.domain, parent);
        model.add(fatherOf, RDFS.range, child);
        // Now we can search for rdf:type parent.
        println("\nWho are the parents?  Inference ON.");
        printRows(infmodel.listStatements(null, RDF.type, parent));
        // And we can search for rdf:type child.
        println("\nWho are the children?  Inference ON.");
        printRows(infmodel.listStatements(null, RDF.type, child));
    }

    /**
     * Transactions
     */
    public static void example22() throws Exception {
        AGGraphMaker maker1 = example1(false);
        // Get another graph maker for the repository on a another connection
        AGRepositoryConnection conn2 = maker1.getRepositoryConnection().getRepository().getConnection();
        closeBeforeExit(conn2);
        AGGraphMaker maker2 = new AGGraphMaker(conn2);
        AGModel model1 = new AGModel(maker1.getGraph());
        AGModel model2 = new AGModel(maker2.getGraph());
        model2.begin();
        String baseURI = "http://example.org/example/local";

        try (final InputStream is = new FileInputStream(new File(DATA_DIR, "lesmis.rdf"))) {
        	model1.read(is, baseURI);
        }
        println("Loaded " + model1.size() + " lesmis.rdf triples via conn1.");
        try (final InputStream is = new FileInputStream(new File(DATA_DIR, "java-kennedy.ntriples"))) {
        	model2.read(is, baseURI, "N-TRIPLE");
        }
        println("Loaded " + model2.size() + " java-kennedy.ntriples via conn2.");
        
        println("\nSince model1 is not in a transaction, lesmis.rdf triples are committed " +
                "and retrievable via model2.  Since model2 is in a transaction, and " +
                "no commit() has yet been issued on model2, kennedy.rdf triples are not " +
                " retrievable via model1.");
        // Check transaction isolation semantics:
        Literal valjean = model1.createLiteral("Valjean");
        Literal kennedy = model1.createLiteral("Kennedy");
        printRows("\nUsing listStatements() on model1; should find Valjean:",
                1, model1.listStatements(null, null, valjean));
        printRows("\nUsing listStatements() on model1; should not find Kennedy:",
                1, model1.listStatements(null, null, kennedy));
        printRows("\nUsing listStatements() on model2; should not find Valjean (until a rollback coalesce commit occurs on model2):",
                1, model2.listStatements(null, null, valjean));
        printRows("\nUsing listStatements() on model2; should find Kennedy:",
                1, model2.listStatements(null, null, kennedy));
        
        // Rollback
        println("\nRolling back contents of model2.");
        model2.abort();
        println("There are now " + model2.size() + " triples visible via model2.");
        printRows("\nUsing listStatements() on model1; should find Valjean:",
                1, model1.listStatements(null, null, valjean));
        printRows("\nUsing listStatements() on model1; should not find Kennedys:",
                1, model1.listStatements(null, null, kennedy));
        printRows("\nUsing listStatements() on model2; should not find Kennedys:",
                1, model2.listStatements(null, null, kennedy));
        printRows("\nUsing listStatements() on model2; should find Valjean:",
                1, model2.listStatements(null, null, valjean));
        // Reload and Commit
        println("\nReload java-kennedy.ntriples into model2.");
        model2.begin();

        try (final InputStream is = new FileInputStream(new File(DATA_DIR, "java-kennedy.ntriples"))) {
        	model2.read(is, baseURI, "N-TRIPLE");
        }
        println("There are now " + model1.size() + " triples visible on model1.");
        println("There are now " + model2.size() + " triples visible on model2.");
        println("\nCommitting contents of model2.");
        model2.commit();
        println("There are now " + model1.size() + " triples visible on model1.");
        println("There are now " + model2.size() + " triples visible on model2.");
        printRows("\nUsing listStatements() on model1; should find Valjean:",
                1, model1.listStatements(null, null, valjean));
        printRows("\nUsing listStatements() on model1; should find Kennedys:",
                1, model1.listStatements(null, null, kennedy));
        printRows("\nUsing listStatements() on model2; should find Kennedys:",
                1, model2.listStatements(null, null, kennedy));
        printRows("\nUsing listStatements() on model2; should find Valjean:",
                1, model2.listStatements(null, null, valjean));
        maker1.close();
        maker2.close();
    }
    
    /**
     * execConstructTriples(), execDescribeTriples()
     */
    public static void example23() throws Exception {
        AGGraphMaker maker = example6();
        AGModel model = new AGModel(maker.getGraph());
        model.setNsPrefix("kdy", "http://www.franz.com/simple#");
        // We don't want the vcards this time. This is how to delete an entire subgraph.
        maker.removeGraph("http://example.org#vcards");
        println("\nRemoved vcards.");
        
        // CONSTRUCT query
        println("\nConstructing has-grandchild triples.");
        String queryString = "construct {?a kdy:has-grandchild ?c}" + 
        "    where { ?a kdy:has-child ?b . " +
        "            ?b kdy:has-child ?c . }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
        	Iterator<Triple> tripleIterConstruct = qe.execConstructTriples();			
        	while(tripleIterConstruct.hasNext())
        	{				
        		println(tripleIterConstruct.next());
        	}			
        } finally {
        	qe.close();
        }
        		
        // DESCRIBE query
        queryString = "describe ?s ?p ?o where { ?s ?p ?o . } limit 1";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);        
        try {
        	Iterator<Triple> tripleIterDescribe = qe.execDescribeTriples();
        	while(tripleIterDescribe.hasNext())
        	{
        		println(tripleIterDescribe.next());				
        	}
        }
        finally
        {
        	qe.close();
        }		
                
    }
    
    
    /**
     * Graph createUnion  spr41131
     */
    public static void example24() throws Exception {
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);    	
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        closeAll();
        catalog.deleteRepository(REPOSITORY_ID);		
        AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);        
        myRepository.initialize();        
        AGRepositoryConnection conn = myRepository.getConnection();
        closeBeforeExit(conn);
        AGGraphMaker maker =  new AGGraphMaker(conn);
        AGGraph gd = maker.getGraph();
        AGGraph g1 = maker.createGraph("http://example.org/g1");
        AGGraph g2 = maker.createGraph("http://example.org/g2");
        AGGraphUnion gd12 = maker.createUnion(gd, g1, g2);        
        
        AGModel md = new AGModel(gd);
        AGModel m1 = new AGModel(g1);
        AGModel m2 = new AGModel(g2);
        
        Resource a = md.createResource("http://a");
        Resource b = md.createResource("http://b");
        Resource c = md.createResource("http://c");
        Property p = md.createProperty("http://p");
        Property q = md.createProperty("http://q");
        
        md.add(p,RDF.type, OWL.TransitiveProperty);
        m1.add(a,p,b);
        m1.add(p,RDFS.subPropertyOf,q);
        m2.add(b,p,c);
        
        AGModel md12 = new AGModel(gd12);
        
        System.out.println(" Model md Size:"+md.size());
        System.out.println(" Model m1 Size:"+m1.size());
        System.out.println(" Model m2 Size:"+m2.size());       
        System.out.println(" Model Model3 Size:"+md12.size());
        
        AGQuery sparql = AGQueryFactory.create("select ?s ?p ?o where {?s ?p ?o .}");
        AGQueryExecution qe = AGQueryExecutionFactory.create(sparql, md12);
        
        try {
        	ResultSet rs = qe.execSelect();						
        	while(rs.hasNext())
        	{				
        		println(rs.next());
        	}
        }
        finally
        {
        	qe.close();
        }    	
                
    }

    
             

    /**
     * Usage: all Usage: [1-9,11-13,17-19,22-24]+
     */
    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        List<Integer> choices = new ArrayList<Integer>();
        if (args.length == 0) {
        	// for choosing by editing this code
        	choices.add(1);
        } else if (args[0].equals("all")) {
        	for (int i = 1; i <= 9; i++) {
        		choices.add(i);
        	}
        	choices.add(11);
        	choices.add(12);
        	choices.add(13);
        	choices.add(17);
        	choices.add(18);
        	choices.add(19);
        	choices.add(22);
        	choices.add(23);			
        	choices.add(24);			
        } else {
        	for (int i = 0; i < args.length; i++) {
        		choices.add(Integer.parseInt(args[i]));
        	}
        }
        try {
        	for (Integer choice : choices) {
        		println("\n** Running example " + choice);
        		switch (choice) {
        		case 1:
        			example1(true);
        			break;
        		case 2:
        			example2(true);
        			break;
        		case 3:
        			example3();
        			break;
        		case 4:
        			example4();
        			break;
        		case 5:
        			example5();
        			break;
        		case 6:
        			example6();
        			break;
        		case 7:
        			example7();
        			break;
        		case 8:
        			example8();
        			break;
        		case 9:
        			example9();
        			break;
        		case 11:
        			example11();
        			break;
        		case 12:
        			example12();
        			break;
        		case 13:
        			example13();
        			break;
        		case 17:
        			example17();
        			break;
        		case 18:
        			example18();
        			break;
        		case 19:
        			example19();
        			break;
        		case 22:
        			example22();
        			break;
        		case 23:
        			example23();
        			break;
        		case 24:
        			example24();
        			break;
        		default:
        			throw new IllegalArgumentException("There is no example "
        					+ choice);
        		}
        	}
        } finally {
        	closeAll();
            println("Elapsed time: " + (System.currentTimeMillis() - now)
                    / 1000.00 + " seconds.");
        }
    }

    public static void println(Object x) {
        System.out.println(x);
    }

    static void printRows(StmtIterator rows) throws Exception {
        while (rows.hasNext()) {
        	println(rows.next());
        }
        rows.close();
    }

    protected static void printRows(String headerMsg, int limit,    StmtIterator rows) throws Exception {
        println(headerMsg);
        int count = 0;
        while (count < limit && rows.hasNext()) {
        	println(rows.next());
        	count++;
        }
        println("Number of results: " + count);
        rows.close();
    }

    static void close(AGRepositoryConnection conn) {
        try {
        	conn.close();
        } catch (Exception e) {
        	System.err.println("Error closing repository connection: " + e);
        	e.printStackTrace();
        }
    }

    private static List<AGRepositoryConnection> toClose = new ArrayList<AGRepositoryConnection>();

    /**
     * This is just a quick mechanism to make sure all connections get closed.
     */
    protected static void closeBeforeExit(AGRepositoryConnection conn) {
        toClose.add(conn);
    }

    protected static void closeAll() {
        while (!toClose.isEmpty()) {
        	AGRepositoryConnection conn = toClose.get(0);
        	close(conn);
        	while (toClose.remove(conn)) {
        		// ...
        	}
        }
    }

}
