package tutorial;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
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
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.Catalog;

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
//	    resultSet = conn.getJDBCStatements(alice, None, None)
//	    while resultSet.next():
//	        #print resultSet.getRow()
//	        print "   ", resultSet.getValue(2), "   ", resultSet.getString(2) 
	}

	
	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		int lastChoice = 2;
		for (int i = 1; i <= lastChoice; i++)
			choices.add(new Integer(i));
		if (true) {
			choices = new ArrayList<Integer>();
			choices.add(3);
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