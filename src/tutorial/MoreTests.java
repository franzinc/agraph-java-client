package tutorial;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.AllegroRepository;
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.Catalog;

import test.AGRepositoryConnectionTest;

public class MoreTests extends AGRepositoryConnectionTest {

	public MoreTests(String name) {
		super(name);
	}
	
	public void testDataset() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append("ASK ");
		queryBuilder.append("{ ?p foaf:name ?name }");

		BooleanQuery query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding("name", nameBob);

		if (false)
		assertTrue(query.evaluate());

		DatasetImpl dataset = new DatasetImpl();

		// default graph: {context1}
		dataset.addDefaultGraph(context1);
		query.setDataset(dataset);
		if (false)
		assertTrue(query.evaluate());

		// default graph: {context1, context2}
		dataset.addDefaultGraph(context2);
		query.setDataset(dataset);
		if (false)
		assertTrue(query.evaluate());

		// default graph: {context2}
		dataset.removeDefaultGraph(context1);
		query.setDataset(dataset);
		System.out.println("ASK CONTEXT2 " + query.evaluate());
		//assertFalse(query.evaluate());
	}
	
	private void test10 () throws Exception {    
//	    AllegroSail server = new AllegroSail("localhost", 8080);
//	    System.out.println("Available catalogs " + server.listCatalogs());
//	    Catalog catalog = server.openCatalog("scratch");    
//	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
//	    		catalog.listRepositories());    
//	    AllegroRepository myRepository = catalog.getRepository("agraph_test", AllegroRepository.RENEW);
//	    myRepository.initialize();
//	    System.out.println( "Repository " + myRepository.getName() + " is up!  It contains "
//	    		+ myRepository.getConnection().size() + " statements.");
//	    RepositoryConnection conn = myRepository.getConnection();
		RepositoryConnection conn = testCon;
	    ValueFactory f = conn.getValueFactory();
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

	    String queryString = "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }";	    
	    DatasetImpl ds = new DatasetImpl();
	    ds.addNamedGraph(context1);
	    ds.addNamedGraph(context2);
	    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
	    tupleQuery.setDataset(ds);
	    tupleQuery.setBinding("s", alice);
	    tupleQuery.setBinding("p", RDF.TYPE);
	    TupleQueryResult result = tupleQuery.evaluate();    
	    System.out.println("Query over contexts 1 and 2.");
	    while (result.hasNext()) {
        	BindingSet bindingSet = result.next();
        	System.out.println(bindingSet.getBinding("s") + "  " + bindingSet.getBinding("c"));
        }	
	    
	}

	
	public static void main (String[] args) throws Exception {
		MoreTests test = new MoreTests("More");
		test.setUp();
		//test.testDataset();
		test.test10();
	}

}
