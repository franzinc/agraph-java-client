package tutorial;

import java.io.File;
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
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.AllegroRepository;
import org.openrdf.repository.sail.AllegroRepositoryConnection;
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.Catalog;
import org.openrdf.repository.sail.JDBCResultSet;
import org.openrdf.rio.RDFFormat;

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
	    Catalog catalog = server.openCatalog("ag");    
	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
	    		catalog.listRepositories());    
	    AllegroRepository myRepository = catalog.getRepository("agraph_test4", AllegroRepository.RENEW);
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
	
	private static void test3() throws Exception {
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
	
	private static void test4() throws RepositoryException {
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

	private static void test5() throws Exception {
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
	
	private static Repository test6() throws Exception {
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
	    conn.add(new File(path2), baseURI, RDFFormat.NTRIPLES);
	    // read vcards triples into the context 'context':
	    conn.add(new File(path1), baseURI, RDFFormat.RDFXML, context);
	    myRepository.indexTriples(true);
	    System.out.println("After loading, repository contains " + conn.size(context) + 
	    		" vcard triples in context '" + context + "'\n    and   " +
	    		// QUESTION: IS THIS CORRECTLY INTERPRETED AS THE NULL CONTEXT???:
	    		conn.size(null) + " football triples in context 'null'."); 
	    return myRepository;
	}
	

	private static void test15() throws Exception {
		// Queries per second.
	    Repository myRepository = test6();
	    RepositoryConnection conn = myRepository.getConnection();
	    
	    int reps = 1; //1000;
	    
	    //TEMPORARY
	    URI context = myRepository.getValueFactory().createURI("http://example.org#vcards");
	    // END TEMPORARY
	    int count = 0;
	    long begin = System.currentTimeMillis();
	    for (int i = 0; i < reps; i++) {
	        count = 0;
	        Resource[] contexts = new Resource[]{context, null};
	        JDBCResultSet resultSet = ((AllegroRepositoryConnection)conn).getJDBCStatements(null, null, null, false, contexts);
	        while (resultSet.next()) count++;
	    }
	    long elapsed = System.currentTimeMillis() - begin;
	    System.out.println("Did " + reps + " " + count + "-row matches in " + elapsed + " seconds.");
	 
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
	    System.out.println("Did " + reps + " " + count + "-row matches in " + elapsed + " seconds.");
	   
	    for (int size : new int[]{1, 5, 10, 100}) {
	        String queryString = "select ?x ?y ?z {?x ?y ?z} limit " + size;
	        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	        begin = System.currentTimeMillis();
	        for (int i = 0; i < reps; i++) {
		        count = 0;
	            TupleQueryResult result = tupleQuery.evaluate(); 
	            while (result.hasNext()) {result.next(); count++;}
	        }	        
	        System.out.println("Did " + reps + " " + count + "-row matches in " + elapsed + " seconds.");
	    }
	}

	
	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		int lastChoice = 5;
		for (int i = 1; i <= lastChoice; i++)
			choices.add(new Integer(i));
		if (true) {
			choices = new ArrayList<Integer>();
			choices.add(6);
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
			default: System.out.println("There is no choice for test " + choice);
			}
		}
		} catch (Exception ex) {
			System.out.println("Caught exception " + new SoftException(ex).getMessage());
			ex.printStackTrace();
		}	
	}
}