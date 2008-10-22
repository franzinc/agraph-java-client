package tutorial;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
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

	private static void test5() {
	    // Typed Literals
	    Repository myRepository = test1();
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    conn.clear();
	    String exns = "http://example.org/people/";
	    URI alice = f.createURI("http://example.org/people/alice");
	    URI age = f.createURI(namespace=exns, localname="age");
	    URI weight = f.createURI(namespace=exns, localname="weight");    
	    URI favoriteColor = f.createURI(namespace=exns, localname="favoriteColor");
	    URI birthdate = f.createURI(namespace=exns, localname="birthdate");
	    URI ted = f.createURI(namespace=exns, localname="Ted");
	    Literal red = f.createLiteral("Red");
	    Literal rouge = f.createLiteral("Rouge", language="fr");
	    Literal fortyTwo = f.createLiteral("42", datatype=XMLSchema.INT);
	    Literal fortyTwoInteger = f.createLiteral("42", datatype=XMLSchema.LONG);    
	    Literal fortyTwoUntyped = f.createLiteral("42");
	    Literal date = f.createLiteral("1984-12-06", datatype=XMLSchema.DATE);     
	    Literal time = f.createLiteral("1984-12-06", datatype=XMLSchema.DATETIME);         
	    Statement stmt1 = f.createStatement(alice, age, fortyTwo);
	    Statement stmt2 = f.createStatement(ted, age, fortyTwoUntyped);    
	    conn.add(stmt1);
	    conn.addStatement(stmt2);
	    conn.addTriple(alice, weight, f.createLiteral("20.5"););
	    conn.addTriple(ted, weight, f.createLiteral("20.5", datatype=XMLSchema.FLOAT));
	    conn.add(alice, favoriteColor, red);
	    conn.add(ted, favoriteColor, rouge);
	    conn.add(alice, birthdate, date);
	    conn.add(ted, birthdate, time);    
	    for obj in [None, fortyTwo, fortyTwoUntyped, f.createLiteral("20.5", datatype=XMLSchema.FLOAT), f.createLiteral("20.5"),
	                red, rouge]:
	        print "Retrieve triples matching "%s"." % obj
	        statements = conn.getStatements(None, None, obj);
	        for s in statements:
	            print s
	    for String obj : new String[]{"42", "\"42\"", "20.5", "\"20.5\"", "\"20.5\"^^xsd:float", "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"}) {
	        print "Query triples matching "%s"." % obj
	        queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
	        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	        result = tupleQuery.evaluate();    
	        for bindingSet in result {
	            s = bindingSet[0]
	            p = bindingSet[1]
	            o = bindingSet[2]
	            print "%s %s %s" % (s, p, o);
	        }
	    }
	    fortyTwoInt = f.createLiteral(42);
	    print fortyTwoInt.toPython();
	}


	private static void test15() {
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
		int lastChoice = 2;
		for (int i = 1; i <= lastChoice; i++)
			choices.add(new Integer(i));
		if (true) {
			choices = new ArrayList<Integer>();
			choices.add(4);
		}
		for (Integer choice : choices) {
			System.out.println("Running test " + choice);
			switch(choice) {
			case 0: test0(); break;
			case 1: test1(); break;
			case 2: test2(); break;			
			case 3: test3(); break;			
			case 4: test4(); break;						
			default: System.out.println("There is no choice for test " + choice);
			}
		}
		
	}
}