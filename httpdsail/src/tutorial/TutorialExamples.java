package tutorial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.AllegroRepository;
import org.openrdf.repository.sail.AllegroRepositoryConnection;
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.AllegroTupleQueryResult;
import org.openrdf.repository.sail.Catalog;
import org.openrdf.repository.sail.JDBCResultSet;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import franz.exceptions.SoftException;

public class TutorialExamples {

	
	public static void test0() {
	    System.out.println("Hello World");
	}

	/**
	 * Tests getting the repository up.  Is called by the other tests to do the startup.
	 */
	public static Repository test1() throws RepositoryException {
	    AllegroSail server = new AllegroSail("localhost", 8080);
	    System.out.println("Available catalogs " + server.listCatalogs());
	    Catalog catalog = server.openCatalog("scratch");    
	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
	    		catalog.listRepositories());    
	    AllegroRepository myRepository = catalog.getRepository("agraph_test", AllegroRepository.RENEW);
	    myRepository.initialize();
	    System.out.println( "Repository " + myRepository.getName() + " is up!  It contains "
	    		+ myRepository.getConnection().size() + " statements.");
	    return myRepository;
	}
	
	public static Repository test2() throws RepositoryException {
		Repository myRepository = test1();
	    ValueFactory f = myRepository.getValueFactory();
	    // create some resources and literals to make statements out of
	    URI alice = f.createURI("http://example.org/people/alice");
	    URI bob = f.createURI("http://example.org/people/bob");
	    //BNode bob = f.createBNode();
	    URI name = f.createURI("http://example.org/ontology/name");
	    URI person = f.createURI("http://example.org/ontology/Person");
	    Literal bobsName = f.createLiteral("Bob");
	    Literal alicesName = f.createLiteral("Alice");

	    RepositoryConnection conn = myRepository.getConnection();
	    System.out.println("Triple count before inserts: " + conn.size());
	    RepositoryResult<Statement> result = conn.getStatements(null, null, null, false, null);
	    for (Statement s: result.asList()) System.out.println(s);
	    // alice is a person
	    conn.add(alice, RDF.TYPE, person);
	    // alice's name is "Alice"
	    conn.add(alice, name, alicesName);
	    // bob is a person
	    conn.add(bob, RDF.TYPE, person);
	    // bob's name is "Bob":
	    conn.add(bob, f.createURI("http://example.org/ontology/name"), bobsName);
	    System.out.println("Triple count: " + conn.size());
	    conn.remove(bob, name, bobsName);
	    System.out.println("Triple count: " + conn.size());
	    conn.add(bob, name, bobsName);
	    return myRepository;
	}
	
	public static void test3() throws Exception {
	    RepositoryConnection conn = test2().getConnection();
	    try {
	        String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
	        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	        TupleQueryResult result = tupleQuery.evaluate();
	        try {
	            while (result.hasNext()) {
	            	BindingSet bindingSet = result.next();
	                Value s = bindingSet.getValue("s");
	                Value p = bindingSet.getValue("p");
	                Value o = bindingSet.getValue("o");             
	                System.out.println("  " + s + " " + p + " " + o);
	            }
	        } finally {
	            result.close();
	        }
        } finally {
	        conn.close();
	    }	    
	}
	
	public static void test4() throws RepositoryException {
	    Repository myRepository = test2();
	    RepositoryConnection conn = myRepository.getConnection();
	    URI alice = myRepository.getValueFactory().createURI("http://example.org/people/alice");
	    RepositoryResult<Statement> statements = conn.getStatements(alice, null, null, false, null);
	    while (statements.hasNext())
	        System.out.println(statements.next());
	    System.out.println( "Same thing using JDBC:");
	    JDBCResultSet resultSet = ((AllegroRepositoryConnection)conn).getJDBCStatements(alice, null, null, false);
	    while (resultSet.next()) {
	        System.out.println("   " + resultSet.getRow());
	        System.out.println("   " + resultSet.getValue(2) + "   " + resultSet.getString(2)); 
	    }
	}

	public static void test5() throws Exception {
	    // Typed Literals
	    Repository myRepository = test1();
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    conn.clear();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI("http://example.org/people/alice");
	    URI age = f.createURI(exns, "age");
	    URI weight = f.createURI(exns, "weight");    
	    URI favoriteColor = f.createURI(exns, "favoriteColor");
	    URI birthdate = f.createURI(exns, "birthdate");
	    URI ted = f.createURI(exns, "Ted");
	    Literal red = f.createLiteral("Red");
	    Literal rouge = f.createLiteral("Rouge", "fr");
	    Literal fortyTwo = f.createLiteral("42", XMLSchema.INT);
	    Literal fortyTwoInteger = f.createLiteral("42", XMLSchema.LONG);    
	    Literal fortyTwoUntyped = f.createLiteral("42");
	    Literal date = f.createLiteral("1984-12-06", XMLSchema.DATE);     
	    Literal time = f.createLiteral("1984-12-06", XMLSchema.DATETIME);         
	    Statement stmt1 = f.createStatement(alice, age, fortyTwo);
	    Statement stmt2 = f.createStatement(ted, age, fortyTwoUntyped);    
	    conn.add(stmt1);
	    conn.add(stmt2);
	    conn.add(alice, weight, f.createLiteral("20.5"));
	    conn.add(ted, weight, f.createLiteral("20.5", XMLSchema.FLOAT));
	    conn.add(alice, favoriteColor, red);
	    conn.add(ted, favoriteColor, rouge);
	    conn.add(alice, birthdate, date);
	    // CURRENTLY, THIS LINE CAUSES HTTPD SERVER TO BREAK, BUT ITS NOT THE CLIENT'S FAULT:
	    conn.add(ted, birthdate, time);    
	    for (Literal obj : new Literal[] {null, fortyTwo, fortyTwoUntyped, f.createLiteral("20.5",
	    		                          XMLSchema.FLOAT), f.createLiteral("20.5"),
	                red, rouge}) {
	        System.out.println( "Retrieve triples matching " + obj + ".");
	        RepositoryResult<Statement> statements = conn.getStatements(null, null, obj, false, null); 
		    while (statements.hasNext()) {
		        System.out.println(statements.next());
	    	}
	    }
	    for (String obj : new String[]{"42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float",
	    		                       "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"}) {
	        System.out.println( "Query triples matching " + obj + ".");
	        String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
	        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	        TupleQueryResult result = tupleQuery.evaluate();
	        //try {
	            while (result.hasNext()) {
	            	BindingSet bindingSet = result.next();
	                Value s = bindingSet.getValue("s");
	                Value p = bindingSet.getValue("p");
	                Value o = bindingSet.getValue("o");             
	                System.out.println("  " + s + " " + p + " " + o);
	            }
	        //} finally {

	    }	    
	}
	
	public static Repository test6() throws Exception {
		AllegroRepository myRepository = (AllegroRepository)test1();
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    conn.clear(); 
	    String path1 = "src/tutorial/vc-db-1.rdf";    
	    String path2 = "src/tutorial/football.nt";            
	    String baseURI = "http://example.org/example/local";
	    Resource context = myRepository.getValueFactory().createURI("http://example.org#vcards");
	    conn.setNamespace("vcd", "http://www.w3.org/2001/vcard-rdf/3.0#");
	    // read football triples into the null context:
	    ((AllegroRepositoryConnection)conn).add(new File(path2), baseURI, RDFFormat.NTRIPLES, false, null);
	    // read vcards triples into the context 'context':
	    ((AllegroRepositoryConnection)conn).add(new File(path1), baseURI, RDFFormat.RDFXML, false, context);
	    myRepository.indexTriples(true);
	    System.out.println("After loading, repository contains " + conn.size(context) + 
	    		" vcard triples in context '" + context + "'\n    and   " +
	    		conn.size(null) + " football triples in context 'null'."); 	    
	    return myRepository;
	}
	
	public static void test7 () throws Exception {    
		RepositoryConnection conn = test6().getConnection();
	    System.out.println( "Match all and print subjects and contexts");
	    RepositoryResult<Statement> result = conn.getStatements(null, null, null, false); 
	    while (result.hasNext()) {
	    	Statement stmt = result.next();
	        System.out.println(stmt.getSubject() + "  " + stmt.getContext());
	    }	    
	    System.out.println( "Same thing with SPARQL query");
	    String queryString = "SELECT DISTINCT ?s ?c WHERE {graph ?c {?s ?p ?o .} }";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    TupleQueryResult qresult = tupleQuery.evaluate();	    
	    while (qresult.hasNext()) {
        	BindingSet bindingSet = qresult.next();
        	System.out.println(bindingSet.getBinding("s") + "  " + bindingSet.getBinding("c"));
        }	    	   
	    conn.close();
	}

	// Writing RDF or NTriples to a file
	public static void test8 () throws Exception {    
	    Repository myRepository = test6();
	    RepositoryConnection conn = myRepository.getConnection();
	    Resource context = myRepository.getValueFactory().createURI("http://example.org#vcards");
	    String outputFile = "/tmp/temp.nt";
	    outputFile = null;
	    if (outputFile == null)
	        System.out.println("Writing to Standard Out instead of to a file");
	    OutputStream output = (outputFile != null) ? new FileOutputStream(outputFile) : System.out;
	    NTriplesWriter ntriplesWriter = new NTriplesWriter(output);
	    conn.export(ntriplesWriter, context);
	    String outputFile2 = "/tmp/temp.rdf";
	    outputFile2 = null;
	    if (outputFile2 == null)
	    	System.out.println( "Writing to Standard Out instead of to a file");
	    output = (outputFile2 != null) ? new FileOutputStream(outputFile2) : System.out;
	    RDFXMLWriter rdfxmlfWriter = new RDFXMLWriter(output);    
	    conn.export(rdfxmlfWriter, context);
	}
	
	// Writing the result of a statements match to a file.
	public static void test9 () throws Exception {    
	    Repository myRepository = test6();
	    RepositoryConnection conn = myRepository.getConnection();
	    conn.exportStatements(null, RDF.TYPE, null, false, new RDFXMLWriter(System.out));
	}

	// Datasets and multiple contexts.
	public static void test10 () throws Exception {    
	    Repository myRepository = test1();	    
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI(exns, "alice");
	    URI bob = f.createURI(exns, "bob");
	    URI ted = f.createURI(exns, "ted");	    
	    URI name = f.createURI("http://example.org/ontology/name");
	    URI person = f.createURI("http://example.org/ontology/Person");
	    Literal alicesName = f.createLiteral("Alice");
	    Literal bobsName = f.createLiteral("Bob");
	    Literal tedsName = f.createLiteral("Ted");	    
	    URI context1 = f.createURI(exns, "cxt1");      
	    URI context2 = f.createURI(exns, "cxt2");         
	    conn.add(alice, RDF.TYPE, person, context1);
	    conn.add(alice, name, alicesName, context1);
	    conn.add(bob, RDF.TYPE, person, context2);
	    conn.add(bob, name, bobsName, context2);
	    conn.add(ted, RDF.TYPE, person);
	    conn.add(ted, name, tedsName);
	    RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false);
	    System.out.println("All triples in all contexts:");	    
	    while (statements.hasNext()) {
	    	System.out.println(statements.next());	    	
	    }
	    statements = conn.getStatements(null, null, null, false, context1, context2);
	    System.out.println("Triples in contexts 1 or 2:");	    
	    while (statements.hasNext()) {
	    	System.out.println(statements.next());
	    }
	    statements = conn.getStatements(null, null, null, false, null, context2);
	    System.out.println("Triples in contexts null or 2:");	    
	    while (statements.hasNext()) {
	    	System.out.println(statements.next());
	    }
	    
	    String queryString = "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }";	    
	    DatasetImpl ds = new DatasetImpl();
	    ds.addNamedGraph(context1);
	    ds.addNamedGraph(context2);
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    tupleQuery.setDataset(ds);
	    TupleQueryResult result = tupleQuery.evaluate();    
	    System.out.println("Query over contexts 1 and 2.");
	    while (result.hasNext()) {
        	BindingSet bindingSet = result.next();
        	System.out.println(bindingSet.getBinding("s") + "  " + bindingSet.getBinding("c"));
        }	
	    
	    queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . }";
	    ds = new DatasetImpl();
	    ds.addDefaultGraph(null);
	    tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    tupleQuery.setDataset(ds);
	    result = tupleQuery.evaluate();    
	    System.out.println("Query over the null context.");
	    while (result.hasNext()) {
        	System.out.println(result.next());
        }
	    
	}
	
	// Namespaces
	public static void test11 () throws Exception {    
	    AllegroRepository myRepository = (AllegroRepository)test1();	    
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI(exns, "alice");
	    URI person = f.createURI(exns, "Person");
	    conn.add(alice, RDF.TYPE, person);
	    myRepository.indexTriples(true);
	    conn.setNamespace("ex", exns);
	    //conn.removeNamespace("ex");
	    String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    TupleQueryResult result = tupleQuery.evaluate();  
	    while (result.hasNext()) {
        	System.out.println(result.next());
        }
	}                                                   

	// Text search
	public static void test12 () throws Exception {    
	    AllegroRepository myRepository = (AllegroRepository)test1();	    
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    //myRepository.registerFreeTextPredicate("http://example.org/people/name");    
	    myRepository.registerFreeTextPredicate(exns + "fullname");
	    URI alice = f.createURI(exns, "alice1");
	    URI persontype = f.createURI(exns, "Person");
	    URI fullname = f.createURI(exns, "fullname");    
	    Literal alicename = f.createLiteral("Alice B. Toklas");
	    URI book =  f.createURI(exns, "book1");
	    URI booktype = f.createURI(exns, "Book");
	    URI booktitle = f.createURI(exns, "title");    
	    Literal wonderland = f.createLiteral("Alice in Wonderland");
	    conn.clear();    
	    conn.add(alice, RDF.TYPE, persontype);
	    conn.add(alice, fullname, alicename);
	    conn.add(book, RDF.TYPE, booktype);    
	    conn.add(book, booktitle, wonderland); 
	    //myRepository.indexTriples(true);
	    conn.setNamespace("ex", exns);
	    //conn.setNamespace('fti', "http://franz.com/ns/allegrograph/2.2/textindex/");  // is already built-in    
	    String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . ?s fti:match 'Alice' . }";
	    //queryString="SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER regex(?o, "Ali") }";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    AllegroTupleQueryResult result = (AllegroTupleQueryResult)tupleQuery.evaluate();	    
	    System.out.println("Found " + result.getTupleCount() + " query results");	    
	    int count = 0;
	    while (result.hasNext()) {
	    	BindingSet bindingSet = result.next();
	    	System.out.println(bindingSet);
	        count += 1;
	        if (count > 5) break;
	    }
	}
	
	
    /** Ask, Construct, and Describe queries */ 
	public static void test13 () throws Exception {    
	    RepositoryConnection conn = test2().getConnection();
	    conn.setNamespace("ex", "http://example.org/people/");
	    conn.setNamespace("ont", "http://example.org/ontology/");
	    String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    TupleQueryResult result = tupleQuery.evaluate();
	    while (result.hasNext()) {
	    	System.out.println(result.next());
	    }
	    queryString = "ask { ?s ont:name \"Alice\" } ";
	    BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
	    boolean truth = booleanQuery.evaluate(); 
	    System.out.println("Boolean result " + truth);
	    queryString = "construct {?s ?p ?o} where { ?s ?p ?o . filter (?o = \"Alice\") } ";
	    GraphQuery constructQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
	    GraphQueryResult gresult = constructQuery.evaluate(); 
	    List statements = new ArrayList();
	    while (gresult.hasNext()) {
	    	statements.add(gresult.next());
	    }
	    System.out.println("Construct result" + statements);
	    queryString = "describe ?s where { ?s ?p ?o . filter (?o = \"Alice\") } ";
	    GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
	    gresult = describeQuery.evaluate(); 
	    System.out.println("Describe result");
	    while (gresult.hasNext()) {
	    	System.out.println(gresult.next());
	    }
    }

    /** Parametric Queries */
	public static void test14() throws Exception {
    	RepositoryConnection conn = test2().getConnection();
        ValueFactory f = conn.getValueFactory();
        URI alice = f.createURI("http://example.org/people/alice");
        URI bob = f.createURI("http://example.org/people/bob");
        String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setBinding("s", alice);
        TupleQueryResult result = tupleQuery.evaluate();
        System.out.println("Facts about Alice:");            
	    while (result.hasNext()) {
	    	System.out.println(result.next());
	    }
        tupleQuery.setBinding("s", bob);
        System.out.println("Facts about Bob:");    
        result = tupleQuery.evaluate();
	    while (result.hasNext()) {
	    	System.out.println(result.next());
	    }
    }
    
	public static void pt(String kind, TupleQueryResult rows) throws Exception {
		System.out.println("\n" + kind + " Apples:\t");
		while (rows.hasNext()) {
		  	System.out.println(rows.next());
		}
	}

    /** Federated triple stores. */
	public static void test16() throws Exception {        
    	Catalog catalog = new AllegroSail("localhost", 8080).openCatalog("scratch"); 
        // create two ordinary stores, and one federated store: 
    	RepositoryConnection redConn = catalog.getRepository("redthings", AllegroRepository.RENEW).init().getConnection();
        ValueFactory rf = redConn.getValueFactory();
        RepositoryConnection greenConn = catalog.getRepository("greenthings", AllegroRepository.RENEW).init().getConnection();
        ValueFactory gf = greenConn.getValueFactory();
        RepositoryConnection rainbowConn = catalog.getRepository("rainbowthings", AllegroRepository.RENEW)
                             .addFederatedTripleStores(Arrays.asList(new String[]{"redthings", "greenthings"})).init().getConnection();
        ValueFactory rbf = rainbowConn.getValueFactory();
        String ex = "http://www.demo.com/example#";
        // add a few triples to the red and green stores:
        redConn.add(rf.createURI(ex+"mcintosh"), RDF.TYPE, rf.createURI(ex+"Apple"));
        redConn.add(rf.createURI(ex+"reddelicious"), RDF.TYPE, rf.createURI(ex+"Apple"));    
        greenConn.add(gf.createURI(ex+"pippin"), RDF.TYPE, gf.createURI(ex+"Apple"));
        greenConn.add(gf.createURI(ex+"kermitthefrog"), RDF.TYPE, gf.createURI(ex+"Frog"));
        redConn.setNamespace("ex", ex);
        greenConn.setNamespace("ex", ex);
        rainbowConn.setNamespace("ex", ex);
        String queryString = "select ?s where { ?s rdf:type ex:Apple }";
        // query each of the stores; observe that the federated one is the union of the other two:
        pt("red", redConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
        pt("green", greenConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
        pt("federated", rainbowConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());
    }

	public static void test26() throws Exception {
		// Queries per second.
	    Repository myRepository = test6();
	    RepositoryConnection conn = myRepository.getConnection();
	    
	    int reps = 10;
	    
	    //TEMPORARY
	    URI context = myRepository.getValueFactory().createURI("http://example.org#vcards");
	    // END TEMPORARY
	    Resource[] contexts = new Resource[]{context, null};
	    Literal ajax = conn.getValueFactory().createLiteral("AFC Ajax");
	    int count = 0;
	    long begin = System.currentTimeMillis();
	    for (int i = 0; i < reps; i++) {
	        count = 0;	        
	        JDBCResultSet resultSet = ((AllegroRepositoryConnection)conn).getJDBCStatements(null, null, ajax, false, contexts);
	        while (resultSet.next()) count++;
	    }
	    long elapsed = System.currentTimeMillis() - begin;
	    System.out.println("Did " + reps + " " + count + "-row matches in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	 
	    begin = System.currentTimeMillis();
	    for (int i = 0; i < reps; i++) {
	        count = 0;
	        RepositoryResult statements = conn.getStatements(null, null, null, false, null);
	        while (statements.hasNext()) {
	        	Statement st = (Statement)statements.next();
	            st.getSubject();
	            st.getPredicate();
	            st.getObject();
	            count++;
	        }
	    }
	    elapsed = System.currentTimeMillis() - begin;
	    System.out.println("Did " + reps + " " + count + "-row matches in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	   
	    for (int size : new int[]{1, 5, 10, 100}) {
	        String queryString = "select ?x ?y ?z where {?x ?y ?z} limit " + size;
	        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	        begin = System.currentTimeMillis();
	        for (int i = 0; i < reps; i++) {
		        count = 0;
	            TupleQueryResult result = tupleQuery.evaluate(); 
	            while (result.hasNext()) {result.next(); count++;}
	        }
	        elapsed = System.currentTimeMillis() - begin;
	        System.out.println("Did " + reps + " " + count + "-row queries in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	    }
	}
	

	public static void test27() throws Exception {
		// CIA Fact book
	    AllegroSail server = new AllegroSail("localhost", 8080);
	    System.out.println("Available catalogs " + server.listCatalogs());
	    Catalog catalog = server.openCatalog("scratch");    
	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
	    		catalog.listRepositories());    
	    AllegroRepository myRepository = catalog.getRepository("agraph_test", AllegroRepository.ACCESS);
	    myRepository.initialize();
	    System.out.println( "Repository " + myRepository.getName() + " is up!  It contains "
	    		+ myRepository.getConnection().size() + " statements.");
	    RepositoryConnection conn = myRepository.getConnection();
	    conn.clear();
	    if (conn.size(null) == 0) {
	        System.out.println("Reading CIA Fact Book file.");
	        String path1 = "/FRANZ_CONSULTING/data/ciafactbook.nt";  
	        String baseURI = "http://example.org/example/local";
	        ((AllegroRepositoryConnection)conn).add(new File(path1), baseURI, RDFFormat.NTRIPLES, false, null);
	    }
	    myRepository.indexTriples(true);
	    long begin = System.currentTimeMillis();
	    int count = 0;
	    JDBCResultSet resultSet = ((AllegroRepositoryConnection)conn).getJDBCStatements(null, null, null, false);
	    while (resultSet.next()) {
	    	resultSet.getString(0);
	    	resultSet.getString(1);
	    	resultSet.getString(2);
	    	resultSet.getString(3);	    	
	        count++;
	    }
	    long elapsed = (System.currentTimeMillis() - begin);
	    System.out.println("Did " + count + "-row match in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	    
	    String queryString = "select ?x ?y ?z where {?x ?y ?z} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        begin = System.currentTimeMillis();
        count = 0;
        AllegroTupleQueryResult result = (AllegroTupleQueryResult)tupleQuery.evaluate();
        result.setSkipIllegalTuples(true);
        while (result.hasNext()) {result.next(); count++;}
        System.out.println("Found " + result.getIllegalTuples().size() + " illegal query tuples.");
        elapsed = System.currentTimeMillis() - begin;
        System.out.println("Did " + count + "-row query in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	}

	
	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		int lastChoice = 14;
		for (int i = 1; i <= lastChoice; i++)
			choices.add(new Integer(i));
		if (false) {
			choices = new ArrayList<Integer>();
			choices.add(2);
		}
		try {
		for (Integer choice : choices) {
			System.out.println("Running test " + choice);
			switch(choice) {
			case 0: test0(); break;
			case 1: test1(); break;
			case 2: test2(); break;			
			case 3: test3(); break;			
			case 4: test4(); break;						
			case 5: test5(); break;									
			case 6: test6(); break;	
			case 7: test7(); break;
			case 8: test8(); break;			
			case 9: test9(); break;	
			case 10: test10(); break;
			case 11: test11(); break;			
			case 12: test12(); break;						
			case 13: test13(); break;									
			case 14: test14(); break;									
			//case 15: test15(); break;									
			case 16: test16(); break;									
			
			case 26: test26(); break;
			case 27: test27(); break;			
			default: System.out.println("There is no choice for test " + choice);
			}
		}
		} catch (Exception ex) {
			System.out.println("Caught exception " + new SoftException(ex).getMessage());
			ex.printStackTrace();
		}	
	}
}