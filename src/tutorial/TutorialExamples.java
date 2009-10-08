package tutorial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;

public class TutorialExamples {

    static private final String SERVER_URL = "http://localhost:8080";
    static private final String CATALOG_ID = "scratch";
    static private final String USERNAME = "test";
    static private final String PASSWORD = "xyzzy";

    static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

    /**
     * Creating a Repository
     */
    public static AGRepositoryConnection test1(boolean close) throws RepositoryException {
        // Tests getting the repository up. 
        System.out.println("Starting example test1().");
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        System.out.println("Available catalogs: " + (server.listCatalogs()));
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        System.out.println("Available repositories in catalog " + 
                (catalog.getCatalogName()) + ": " + 
                catalog.getAllRepositories());
        AGRepository myRepository = catalog.createRepository(CATALOG_ID);
        System.out.println("Got a repository.");
        myRepository.initialize();
        System.out.println("Initialized repository.");
        AGRepositoryConnection conn = myRepository.getConnection();
        System.out.println("Got a connection.");
        conn.clear();  // remove previous triples, if any.
        System.out.println("Cleared the connection.");
        System.out.println("Repository " + (myRepository.getRepositoryID()) +
                " is up! It contains " + (conn.size()) +
                " statements."              
                );
        if (close) {
            // tidy up
            conn.close();
            myRepository.shutDown();
            return null;
        }
        return conn;
    }
    
    /**
     * Asserting and Retracting Triples
     */
    public static AGRepositoryConnection test2(boolean close) throws RepositoryException {
        // Asserts some statements and counts them.
        AGRepositoryConnection conn = test1(false);
        AGValueFactory vf = conn.getRepository().getValueFactory();
        System.out.println("Starting example test2().");
        // Create some resources and literals to make statements from.
        URI alice = vf.createURI("http://example.org/alice");
        URI bob = vf.createURI("http://example.org/bob");
        URI name = vf.createURI("http://example.org/name");
        URI person = vf.createURI("http://example.org/Person");
        Literal bobsName = vf.createLiteral("Bob");
        Literal alicesName = vf.createLiteral("Alice");
        System.out.println("Triple count before inserts: " + 
                (conn.size()));
        // Alice's name is "Alice"
        conn.add(alice, name, alicesName);
        // Alice is a person
        conn.add(alice, RDF.TYPE, person);
        //Bob's name is "Bob"
        conn.add(bob, name, bobsName);
        //Bob is a person, too. 
        conn.add(bob, RDF.TYPE, person);
        System.out.println("Added four triples.");
        System.out.println("Triple count after inserts: " + 
                (conn.size()));
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
        while (result.hasNext()) {
            Statement st = result.next();
            System.out.println(st);
        }
        conn.remove(bob, name, bobsName);
        System.out.println("Removed one triple.");
        System.out.println("Triple count after deletion: " + 
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
    public static void test3() throws Exception {
        AGRepositoryConnection conn = test2(false);
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
                    System.out.format("%s %s %s\n", s, p, o);
                }
            } finally {
                result.close();
            }
        } finally {
            conn.close();
        }
    }

    /**
     * Statement Matching
     */
    public static void test4() throws RepositoryException {
        RepositoryConnection conn = test2(false);
        Repository myRepository = conn.getRepository();
        URI alice = myRepository.getValueFactory().createURI("http://example.org/people/alice");
        RepositoryResult<Statement> statements = conn.getStatements(alice, null, null, false);
        try {
            statements.enableDuplicateFilter();
            while (statements.hasNext()) {
                System.out.println(statements.next());
            }
        } finally {
            statements.close();
        }
        
//        System.out.println( "Same thing using JDBC:");
//        JDBCResultSet resultSet = ((AGRepositoryConnection)conn).getJDBCStatements(alice, null, null, false);
//        while (resultSet.next()) {
//            //System.out.println("   " + resultSet.getRow());
//            System.out.println("   " + resultSet.getValue(2) + "   " + resultSet.getString(2)); 
//        }
        conn.close();
        myRepository.shutDown();
    }

    /**
     * Literal Values
     */
    public static void test5() throws Exception {
        RepositoryConnection conn = test2(false);
        Repository myRepository = conn.getRepository();
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
        Literal time = f.createLiteral("1984-12-06T09:00:00", XMLSchema.DATETIME);         
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
            RepositoryResult<Statement> statements = conn.getStatements(null, null, obj, false);
            try {
                while (statements.hasNext()) {
                    System.out.println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        // SPARQL
        for (String obj : new String[]{"42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float",
                                       "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"}) {
            System.out.println( "Query triples matching " + obj + ".");
            String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
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
        }
        {
            // Search for date using date object in triple pattern.
            System.out.println("Retrieve triples matching DATE object.");
            RepositoryResult<Statement> statements = conn.getStatements(null, null, date, false);
            try {
                while (statements.hasNext()) {
                    System.out.println(statements.next());
                }
            } finally {
                statements.close();
            }
        }

        {
            System.out.println("Match triples having a specific DATE value.");
            RepositoryResult<Statement> statements = conn.getStatements(null, null,
                    f.createLiteral("\"1984-12-06\"^^<http://www.w3.org/2001/XMLSchema#date>"), false);
            try {
                while (statements.hasNext()) {
                    System.out.println(statements.next());
                }
            } finally {
                statements.close();
            }
        }
        conn.close();
        myRepository.shutDown();
    }
    
    /**
     * Importing Triples
     */
    public static AGRepositoryConnection test6(boolean close) throws Exception {
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        AGRepository myRepository = catalog.createRepository(CATALOG_ID);
        myRepository.initialize();
        AGRepositoryConnection conn = myRepository.getConnection();
        conn.clear(); // renew
        // TODO: openDedicated
        ValueFactory f = myRepository.getValueFactory();
        String path1 = "src/tutorial/vc-db-1.rdf";    
        String path2 = "src/tutorial/football.nt";            
        String baseURI = "http://example.org/example/local";
        Resource context = f.createURI("http://example.org#vcards");
        conn.setNamespace("vcd", "http://www.w3.org/2001/vcard-rdf/3.0#");
        // read football triples into the null context:
        ((AGRepositoryConnection)conn).add(new File(path2), baseURI, RDFFormat.NTRIPLES);
        // read vcards triples into the context 'context':
        ((AGRepositoryConnection)conn).add(new File(path1), baseURI, RDFFormat.RDFXML, context);
        System.out.println("After loading, repository contains " + conn.size(context) +
                " vcard triples in context '" + context + "'\n    and   " +
                conn.size() + " football triples in context 'null'.");
        if (close) {
            conn.close();
            return null;
        }
        return conn;
    }
    
    /**
     * Importing Triples, query
     */
    public static void test7 () throws Exception {    
        RepositoryConnection conn = test6(false);
        System.out.println( "Match all and print subjects and contexts");
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
        // TODO: limit=25
        for (int i = 0; i < 25 && result.hasNext(); i++) {
            Statement stmt = result.next();
            System.out.println(stmt.getSubject() + "  " + stmt.getContext());
        }
        result.close();
        
        System.out.println("Same thing with SPARQL query (can't retrieve triples in the null context)");
        String queryString = "SELECT DISTINCT ?s ?c WHERE {graph ?c {?s ?p ?o .} }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult qresult = tupleQuery.evaluate();       
        while (qresult.hasNext()) {
            BindingSet bindingSet = qresult.next();
            System.out.println(bindingSet.getBinding("s") + "  " + bindingSet.getBinding("c"));
        }
        qresult.close();
        conn.close();
    }


	// Writing RDF or NTriples to a file
	public static void test8 () throws Exception {    
        RepositoryConnection conn = test6(false);
        Repository myRepository = conn.getRepository();
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
	    conn.close();
	}
	
	// Writing the result of a statements match to a file.
	public static void test9 () throws Exception {    
        RepositoryConnection conn = test6(false);
	    conn.exportStatements(null, RDF.TYPE, null, false, new RDFXMLWriter(System.out));
	    conn.close();
	}

	/**
	 * Datasets and multiple contexts.
	 */
	public static void test10 () throws Exception {    
        RepositoryConnection conn = test1(false);
        Repository myRepository = conn.getRepository();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI(exns, "alice");
	    URI bob = f.createURI(exns, "bob");
	    URI ted = f.createURI(exns, "ted");	    
        URI person = f.createURI("http://example.org/ontology/Person");
	    URI name = f.createURI("http://example.org/ontology/name");
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

	    // testing named graph query
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
            System.out.println(bindingSet.getBinding("s") + " " + bindingSet.getBinding("c"));
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
	    conn.close();
	}
	
	// Namespaces
	public static void test11 () throws Exception {
        RepositoryConnection conn = test1(false);
        Repository myRepository = conn.getRepository();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI(exns, "alice");
	    URI person = f.createURI(exns, "Person");
	    conn.add(alice, RDF.TYPE, person);
	    //myRepository.indexTriples(true);
	    conn.setNamespace("ex", exns);
	    //conn.removeNamespace("ex");
	    String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    TupleQueryResult result = tupleQuery.evaluate();  
	    while (result.hasNext()) {
        	System.out.println(result.next());
        }
	    result.close();
	    conn.close();
	}                                                   

	// Text search
	public static void test12 () throws Exception {    
        RepositoryConnection conn = test1(false);
        AGRepository myRepository = (AGRepository) conn.getRepository();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    //myRepository.registerFreeTextPredicate("http://example.org/people/name");    
// TODO    myRepository.registerFreeTextPredicate(exns + "fullname");
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
	    TupleQueryResult result = (TupleQueryResult)tupleQuery.evaluate();
	    //System.out.println("Found " + result.getTupleCount() + " query results");
	    int count = 0;
	    while (result.hasNext()) {
	    	BindingSet bindingSet = result.next();
	    	System.out.println(bindingSet);
	        count += 1;
	        if (count > 5) break;
	    }
        result.close();
        conn.close();
	}
	
	
    /** Ask, Construct, and Describe queries */ 
	public static void test13 () throws Exception {    
	    RepositoryConnection conn = test2(false);
	    conn.setNamespace("ex", "http://example.org/people/");
	    conn.setNamespace("ont", "http://example.org/ontology/");
	    String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    TupleQueryResult result = tupleQuery.evaluate();
	    while (result.hasNext()) {
	    	System.out.println(result.next());
	    }
        result.close();
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
        gresult.close();
        conn.close();
    }

    /** Parametric Queries */
	public static void test14() throws Exception {
    	RepositoryConnection conn = test2(false);
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
        result.close();
        tupleQuery.setBinding("s", bob);
        System.out.println("Facts about Bob:");    
        result = tupleQuery.evaluate();
	    while (result.hasNext()) {
	    	System.out.println(result.next());
	    }
        result.close();
        conn.close();
    }
    
	private static void pt(String kind, TupleQueryResult rows) throws Exception {
		System.out.println("\n" + kind + " Apples:\t");
		while (rows.hasNext()) {
		  	System.out.println(rows.next());
		}
	}

    /** Federated triple stores. */
	/*
    public static void test16() throws Exception {        
        AGRepositoryConnection conn = test6();
        AGRepository myRepository = conn.getRepository();
    	AGCatalog catalog = myRepository.getCatalog();
        // create two ordinary stores, and one federated store: 
    	RepositoryConnection redConn = catalog.createRepository("redthings", AGRepository.RENEW).init().getConnection();
        ValueFactory rf = redConn.getValueFactory();
        RepositoryConnection greenConn = catalog.getRepository("greenthings", AGRepository.RENEW).init().getConnection();
        ValueFactory gf = greenConn.getValueFactory();
        RepositoryConnection rainbowConn = catalog.getRepository("rainbowthings", AGRepository.RENEW)
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
	    RepositoryConnection conn = test6();
	    Repository myRepository = conn.getRepository();
	    
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
	        JDBCResultSet resultSet = ((AGRepositoryConnection)conn).getJDBCStatements(null, null, ajax, false, contexts);
	        while (resultSet.next()) count++;
	    }
	    long elapsed = System.currentTimeMillis() - begin;
	    System.out.println("Did " + reps + " " + count + "-row matches in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	 
	    begin = System.currentTimeMillis();
	    for (int i = 0; i < reps; i++) {
	        count = 0;
	        RepositoryResult statements = conn.getStatements(null, null, null, false);
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
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
	    System.out.println("Available catalogs " + server.listCatalogs());
	    AGCatalog catalog = server.getCatalog("scratch");
	    System.out.println("Available repositories in catalog '" + catalog.getCatalogName() + "': " +
	    		catalog.listRepositories());
	    AGRepository myRepository = catalog.getRepository("agraph_test", AGRepository.ACCESS);
	    myRepository.initialize();
	    System.out.println( "Repository " + myRepository.getRepositoryID() + " is up!  It contains "
	    		+ myRepository.getConnection().size() + " statements.");
	    AGRepositoryConnection conn = myRepository.getConnection();
	    conn.clear();
	    if (conn.size() == 0) {
	        System.out.println("Reading CIA Fact Book file.");
	        String path1 = "/FRANZ_CONSULTING/data/ciafactbook.nt";  
	        String baseURI = "http://example.org/example/local";
	        conn.add(new File(path1), baseURI, RDFFormat.NTRIPLES);
	    }
	    myRepository.indexTriples(true);
	    long begin = System.currentTimeMillis();
	    int count = 0;
	    JDBCResultSet resultSet = ((AGRepositoryConnection)conn).getJDBCStatements(null, null, null, false);
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
        TupleQueryResult result = (TupleQueryResult)tupleQuery.evaluate();
        result.setSkipIllegalTuples(true);
        while (result.hasNext()) {result.next(); count++;}
        System.out.println("Found " + result.getIllegalTuples().size() + " illegal query tuples.");
        elapsed = System.currentTimeMillis() - begin;
        System.out.println("Did " + count + "-row query in " + (elapsed / 1000) + "." + (elapsed % 1000) + " seconds.");
	}
	*/

	/**
	 * Usage: all
	 * Usage: [1-14]+
	 */
	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		if (args.length == 0) {
		    // for choosing by editing this code
		    choices.add(10);
		} else if (args[0].equals("all")) {
		    for (int i = 1; i <= 14; i++) {
		        choices.add(i);
		    }
		} else {
		    for (int i = 0; i < args.length; i++) {
                choices.add(Integer.parseInt(args[i]));
            }
		}
		for (Integer choice : choices) {
			System.out.println("Running test " + choice);
			switch(choice) {
			case 1: test1(true); break;
			case 2: test2(true); break;			
			case 3: test3(); break;			
			case 4: test4(); break;						
			case 5: test5(); break;									
			case 6: test6(true); break;	
			case 7: test7(); break;
			case 8: test8(); break;			
			case 9: test9(); break;	
			case 10: test10(); break;
			case 11: test11(); break;			
			case 12: test12(); break;						
			case 13: test13(); break;									
			case 14: test14(); break;									
			//case 15: test15(); break;									
			default: throw new IllegalArgumentException("There is no test " + choice);
			}
		}
	}
	
}