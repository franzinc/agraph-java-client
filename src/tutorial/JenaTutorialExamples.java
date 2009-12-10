package tutorial;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.jena.AGReasoner;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class JenaTutorialExamples {

	static private final String SERVER_URL = "http://localhost:10035";
	static private final String CATALOG_ID = "java-catalog";
	static private final String REPOSITORY_ID = "jenatutorial";
	static private final String USERNAME = "test";
	static private final String PASSWORD = "xyzzy";
	static private final String TEMPORARY_DIRECTORY = "";

	static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

	/**
	 * Creating a Repository
	 */
	public static AGGraphMaker example1(boolean close)
			throws Exception {
		// Tests getting the repository up.
		println("\nStarting example1().");
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		println("Available catalogs: " + server.listCatalogs());
		AGCatalog catalog = server.getCatalog(CATALOG_ID);
		println("Available repositories in catalog "
				+ (catalog.getCatalogName()) + ": "
				+ catalog.listRepositories());
		catalog.deleteRepository(REPOSITORY_ID);
		AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
		println("Got a repository.");
		myRepository.initialize();
		println("Initialized repository.");
		println("Repository is writable? " + myRepository.isWritable());
		AGRepositoryConnection conn = myRepository.getConnection();
		closeBeforeExit(conn);
		println("Got a connection.");
		println("Repository " + (myRepository.getRepositoryID())
				+ " is up! It contains " + (conn.size()) + " statements.");
		AGGraphMaker maker = new AGGraphMaker(conn);
		println("Got a graph maker for the connection.");
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
        Resource alice = model.createResource("http://example.org/people/alice");
        String exns = "http://example.org/people/";
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
            String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
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
		AGModel model_vcards = model;//TODO:new AGModel(maker.createGraph("http://example.org#vcards"));
		String path1 = "src/tutorial/java-vcards.rdf";
		String path2 = "src/tutorial/java-kennedy.ntriples";
		String baseURI = "http://example.org/example/local";
		model_vcards.read(new FileInputStream(path1), baseURI);
		model.read(new FileInputStream(path2), baseURI, "N-TRIPLE");
		println("After loading, model_vcards contains " + model_vcards.size()
				+ " triples in graph '" + model_vcards.getGraph() 
				+ "'\n    and model contains " + model.size() 
				+ " triples in graph '" + model.getGraph() + "'.");
		return maker;
	}

	/**
	 * Importing Triples, query
	 */
	public static void example7() throws Exception {
		AGGraphMaker maker = example6();
		AGModel model = new AGModel(maker.getGraph());
		AGModel model_vcards = model; //TODO: new AGModel(maker.openGraph("http://example.org#vcards"));
		println("\nMatch all and print subjects and graph (model)");
		StmtIterator statements = model.listStatements();
		for (int i = 0; i < 25 && statements.hasNext(); i++) {
			Statement stmt = statements.next();
			println(stmt.getSubject() + "  " + stmt.getModel().getGraph());
		}
		println("\nMatch all and print subjects and graph (model_vcards)");
		statements = model_vcards.listStatements();
		for (int i = 0; i < 25 && statements.hasNext(); i++) {
			Statement stmt = statements.next();
			println(stmt.getSubject() + "  " + stmt.getModel().getGraph());
		}
		statements.close();
		
		println("\nSame thing with SPARQL query (model).");
		String queryString = "SELECT DISTINCT ?s ?g WHERE {graph ?g {?s ?p ?o .} } LIMIT 25";
        AGQuery query = AGQueryFactory.create(queryString);
        QueryExecution qe = AGQueryExecutionFactory.create(query, model);
		try {
			ResultSet results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				RDFNode s = result.get("s");
				RDFNode g = result.get("g");
				println("  " + s + " " + g);
			}
		} finally {
			qe.close();
		}
		println("\nSame thing with SPARQL query (model_vcards).");
		qe = AGQueryExecutionFactory.create(query, model_vcards);
		try {
			ResultSet results = qe.execSelect();
			while (results.hasNext()) {
				QuerySolution result = results.next();
				RDFNode s = result.get("s");
				RDFNode g = result.get("g");
				println("  " + s + " " + g);
			}
		} finally {
			qe.close();
		}
	}

	/**
	 * Writing RDF or NTriples to a file
	 */
	public static void example8() throws Exception {
		AGGraphMaker maker = example6();
		AGModel model = new AGModel(maker.getGraph());
		AGModel model_vcards = model;//TODO:new AGModel(maker.openGraph("http://example.org#vcards"));
		String outputFile = TEMPORARY_DIRECTORY + "temp.nt";
		// outputFile = null;
		if (outputFile == null) {
			println("\nWriting n-triples to Standard Out instead of to a file");
		} else {
			println("\nWriting n-triples to: " + outputFile);
		}
		OutputStream output = (outputFile != null) ? new FileOutputStream(
				outputFile) : System.out;
		model.write(output, "N-TRIPLE");
		String outputFile2 = TEMPORARY_DIRECTORY + "temp.rdf";
		// outputFile2 = null;
		if (outputFile2 == null) {
			println("\nWriting RDF to Standard Out instead of to a file");
		} else {
			println("\nWriting RDF to: " + outputFile2);
		}
		output = (outputFile2 != null) ? new FileOutputStream(outputFile2)
				: System.out;
		model_vcards.write(output);
	}

	/**
	 * Writing the result of a statements match.
	 */
	public static void example9() throws Exception {
		AGGraphMaker maker = example6();
		AGModel model_vcards = new AGModel(maker.openGraph("http://example.org#vcards"));
		StmtIterator statements = model_vcards.listStatements(null,RDF.type, (RDFNode)null);
		Model m = ModelFactory.createDefaultModel();
		m.add(statements);
		m.write(System.out);
	}

	/**
	 * Datasets and multiple contexts.
	 *
	public static void example10() throws Exception {
		RepositoryConnection conn = example1(false);
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
		RepositoryResult<Statement> statements = conn.getStatements(null, null,
				null, false);
		println("\nAll triples in all contexts:");
		while (statements.hasNext()) {
			println(statements.next());
		}
		statements = conn.getStatements(null, null, null, false, context1,
				context2);
		println("\nTriples in contexts 1 or 2:");
		while (statements.hasNext()) {
			println(statements.next());
		}
		statements = conn
				.getStatements(null, null, null, false, null, context2);
		println("\nTriples in contexts null or 2:");
		while (statements.hasNext()) {
			println(statements.next());
		}

		// testing named graph query
		String queryString = "SELECT ?s ?p ?o ?c WHERE { GRAPH ?c {?s ?p ?o . } }";
		DatasetImpl ds = new DatasetImpl();
		ds.addNamedGraph(context1);
		ds.addNamedGraph(context2);
		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryString);
		tupleQuery.setDataset(ds);
		TupleQueryResult result = tupleQuery.evaluate();
		println("\nQuery over contexts 1 and 2.");
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			println(bindingSet.getBinding("s") + " "
					+ bindingSet.getBinding("p") + " "
					+ bindingSet.getBinding("o") + " "
					+ bindingSet.getBinding("c"));
		}

		queryString = "SELECT ?s ?p ?o WHERE {?s ?p ?o . }";
		ds = new DatasetImpl();
		ds.addDefaultGraph(null);
		tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		tupleQuery.setDataset(ds);
		result = tupleQuery.evaluate();
		println("\nQuery over the null context.");
		while (result.hasNext()) {
			println(result.next());
		}
	}*/

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
	 *
	public static void example12() throws Exception {
		AGGraphMaker maker = example1(false);
		String exns = "http://example.org/people/";
		model.setNamespace("ex", exns);
		conn.registerFreetextPredicate(f.createURI(exns, "fullname"));
		URI alice = f.createURI(exns, "alice1");
		URI persontype = f.createURI(exns, "Person");
		URI fullname = f.createURI(exns, "fullname");
		Literal alicename = f.createLiteral("Alice B. Toklas");
		URI book = f.createURI(exns, "book1");
		URI booktype = f.createURI(exns, "Book");
		URI booktitle = f.createURI(exns, "title");
		Literal wonderland = f.createLiteral("Alice in Wonderland");
		conn.clear();
		conn.add(alice, RDF.TYPE, persontype);
		conn.add(alice, fullname, alicename);
		conn.add(book, RDF.TYPE, booktype);
		conn.add(book, booktitle, wonderland);

		println("\nWhole-word match for 'Alice'.");
		String queryString = "SELECT ?s ?p ?o "
				+ "WHERE { ?s ?p ?o . ?s fti:match 'Alice' . }";
		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryString);
		TupleQueryResult result = (TupleQueryResult) tupleQuery.evaluate();
		int count = 0;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			if (count < 5) {
				println(bindingSet);
			}
			count += 1;
		}
		result.close();

		println("\nWildcard match for 'Ali*'.");
		queryString = "SELECT ?s ?p ?o "
				+ "WHERE { ?s ?p ?o . ?s fti:match 'Ali*' . }";
		tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		result = (TupleQueryResult) tupleQuery.evaluate();
		count = 0;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			if (count < 5) {
				println(bindingSet);
			}
			count += 1;
		}
		result.close();

		println("\nWildcard match for '?l?ce?.");
		queryString = "SELECT ?s ?p ?o "
				+ "WHERE { ?s ?p ?o . ?s fti:match '?l?c?' . }";
		tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		result = (TupleQueryResult) tupleQuery.evaluate();
		count = 0;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			if (count < 5) {
				println(bindingSet);
			}
			count += 1;
		}
		result.close();

		println("\nSubstring match for 'lic'.");
		queryString = "SELECT ?s ?p ?o "
				+ "WHERE { ?s ?p ?o . FILTER regex(?o, \"lic\") }";
		tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		result = (TupleQueryResult) tupleQuery.evaluate();
		count = 0;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			if (count < 5) {
				println(bindingSet);
			}
			count += 1;
		}
		result.close();
	}*/

	/**
	 * Ask, Construct, and Describe queries
	 */
	public static void example13() throws Exception {
		AGModel model = example2(false);
		model.setNsPrefix("ex", "http://example.org/people/");
		model.setNsPrefix("ont", "http://example.org/ontology/");
		println("\nSELECT result:");
		String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
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
		queryString = "ask { ?s ont:name \"Alice\" } ";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
		try {
			println("\nBoolean result: " + qe.execAsk());
		} finally {
			qe.close();
		}
		queryString = "construct {?s ?p ?o} where { ?s ?p ?o . filter (?o = \"Alice\") } ";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
		try {
			Model m = qe.execConstruct();
			println("\nConstruct result:");
			m.write(System.out);
		} finally {
			qe.close();
		}
		queryString = "describe ?s where { ?s ?p ?o . filter (?o = \"Alice\") } ";
        query = AGQueryFactory.create(queryString);
        qe = AGQueryExecutionFactory.create(query, model);
		try {
			Model m = qe.execDescribe();
			println("\nDescribe result:");
			m.write(System.out);
		} finally {
			qe.close();
		}
	}

	/**
	 * Parametric Queries
	 *
	public static void example14() throws Exception {
		AGModel model = example2(false);
		Resource alice = model.createResource("http://example.org/people/alice");
		Resource bob = model.createResource("http://example.org/people/bob");
		String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        Query query = AGQueryFactory.create(queryString);
        QueryExecution qe = AGQueryExecutionFactory.create(query, model);
		try {
			// tupleQuery.setBinding("s", alice);
			ResultSet results = qe.execSelect();
			println("\nFacts about Alice:");
			while (results.hasNext()) {
				println(results.next());
			}
			// tupleQuery.setBinding("s", bob);
			println("\nFacts about Bob:");
			results = qe.execSelect();
			while (results.hasNext()) {
				println(results.next());
			}
		} finally {
			qe.close();
		}
	}*/

	/**
	 * Range matches
	 *
	public static void example15() throws Exception {
		println("Starting example15().");
		AGRepositoryConnection conn = example1(false);
		ValueFactory f = conn.getValueFactory();
		conn.clear();
		String exns = "http://example.org/people/";
		conn.setNamespace("ex", exns);
		URI alice = f.createURI(exns, "alice");
		URI bob = f.createURI(exns, "bob");
		URI carol = f.createURI(exns, "carol");
		URI age = f.createURI(exns, "age");
		conn.add(alice, age, f.createLiteral(42));
		conn.add(bob, age, f.createLiteral(45.1));
		conn.add(carol, age, f.createLiteral("39"));

		println("\nRange query for integers and floats.");
		String queryString = "SELECT ?s ?p ?o  " + "WHERE { ?s ?p ?o . "
				+ "FILTER ((?o >= 30) && (?o <= 50)) }";
		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryString);
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
		String queryString2 = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT ?s ?p ?o  "
				+ "WHERE { ?s ?p ?o . "
				+ "FILTER ((xsd:integer(?o) >= 30) && (xsd:integer(?o) <= 50)) }";
		TupleQuery tupleQuery2 = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryString2);
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

	private static void pt(String kind, TupleQueryResult rows) throws Exception {
		println("\n" + kind + " Apples:\t");
		while (rows.hasNext()) {
			println(rows.next());
		}
		rows.close();
	}*/

	/**
	 * Federated triple stores.
	 *
	public static void example16() throws Exception {
		AGRepositoryConnection conn = example6();
		AGRepository myRepository = conn.getRepository();
		AGCatalog catalog = myRepository.getCatalog();
		// create two ordinary stores, and one federated store:
		AGRepository redRepo = catalog.createRepository("redthingsjv");
		redRepo.initialize();
		AGRepositoryConnection redConn = redRepo.getConnection();
		closeBeforeExit(redConn);
		redConn.clear();
		ValueFactory rf = redConn.getValueFactory();
		AGRepository greenRepo = catalog.createRepository("greenthingsjv");
		greenRepo.initialize();
		AGRepositoryConnection greenConn = greenRepo.getConnection();
		closeBeforeExit(greenConn);
		greenConn.clear();
		ValueFactory gf = greenConn.getValueFactory();
		AGServer server = myRepository.getCatalog().getServer();
		AGRepository rainbowRepo = server.createFederation("rainbowthingsjv",
				redRepo, greenRepo);
		rainbowRepo.initialize();
		println("Federation is writable? " + rainbowRepo.isWritable());
		AGRepositoryConnection rainbowConn = rainbowRepo.getConnection();
		closeBeforeExit(rainbowConn);
		String ex = "http://www.demo.com/example#";
		// add a few triples to the red and green stores:
		redConn.add(rf.createURI(ex + "mcintosh"), RDF.TYPE, rf.createURI(ex
				+ "Apple"));
		redConn.add(rf.createURI(ex + "reddelicious"), RDF.TYPE, rf
				.createURI(ex + "Apple"));
		greenConn.add(gf.createURI(ex + "pippin"), RDF.TYPE, gf.createURI(ex
				+ "Apple"));
		greenConn.add(gf.createURI(ex + "kermitthefrog"), RDF.TYPE, gf
				.createURI(ex + "Frog"));
		redConn.setNamespace("ex", ex);
		greenConn.setNamespace("ex", ex);
		rainbowConn.setNamespace("ex", ex);
		String queryString = "select ?s where { ?s rdf:type ex:Apple }";
		// query each of the stores; observe that the federated one is the union
		// of the other two:
		pt("Red", redConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString)
				.evaluate());
		pt("Green", greenConn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryString).evaluate());
		pt("Federated", rainbowConn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryString).evaluate());
	}*/

	/**
	 * Prolog queries
	 *
	public static void example17() throws Exception {
		AGRepositoryConnection conn = example6();
		conn.setNamespace("kdy", "http://www.franz.com/simple#");
		String rules1 = "(<-- (woman ?person) ;; IF\n"
				+ "     (q ?person !kdy:sex !kdy:female)\n"
				+ "     (q ?person !rdf:type !kdy:person))\n"
				+ "(<-- (man ?person) ;; IF\n"
				+ "     (q ?person !kdy:sex !kdy:male)\n"
				+ "     (q ?person !rdf:type !kdy:person))";
		conn.addRules(rules1);
		println("\nFirst and Last names of all \"men.\"");
		String queryString = "(select (?first ?last)\n"
				+ "        (man ?person)\n"
				+ "        (q ?person !kdy:first-name ?first)\n"
				+ "        (q ?person !kdy:last-name ?last))";
		TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG,
				queryString);
		TupleQueryResult result = tupleQuery.evaluate();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			Value f = bindingSet.getValue("first");
			Value l = bindingSet.getValue("last");
			println(f + " " + l);
		}
		result.close();
		conn.close();
	}*/

	/**
	 * Loading Prolog rules
	 *
	public static void example18() throws Exception {
		AGRepositoryConnection conn = example6();
		conn.setNamespace("kdy", "http://www.franz.com/simple#");
		conn.setNamespace("rltv", "http://www.franz.com/simple#");
		String path = "src/tutorial/relative_rules.txt";
		conn.addRules(new FileInputStream(path));
		String queryString = "(select (?person ?uncle) " + "(uncle ?y ?x)"
				+ "(name ?x ?person)" + "(name ?y ?uncle))";
		TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG,
				queryString);
		TupleQueryResult result = tupleQuery.evaluate();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			Value p = bindingSet.getValue("person");
			Value u = bindingSet.getValue("uncle");
			println(u + " is the uncle of " + p);
		}
		result.close();
	}*/

	/**
	 * RDFS++ Reasoning
	 */
	public static void example19() throws Exception {
		AGGraphMaker maker = example1(false);
		AGModel model = new AGModel(maker.getGraph());
		// Examples of RDFS++ inference. Was originally example 2A.
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
		// Bob is the same person as Robert
		model.add(bob, OWL.sameAs, robert);
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
		Resource child = model.createResource("http://exmaple.org/ontology/Child");
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
	 * Geospatial Reasoning
	 *

	public static void example20() throws Exception {
		AGRepositoryConnection conn = example1(false);
		AGValueFactory vf = conn.getValueFactory();
		conn = example1(false);
		conn.clear();
		println("Starting example20().");
		String exns = "http://example.org/people/";
		conn.setNamespace("ex", exns);
		URI alice = vf.createURI(exns, "alice");
		URI bob = vf.createURI(exns, "bob");
		URI carol = vf.createURI(exns, "carol");
		URI cartSystem = conn.registerCartesianType(1, 0, 100, 0, 100);
		URI location = vf.createURI(exns, "location");
		Literal alice_loc = vf.createLiteral("+30.0+30.0", cartSystem);
		Literal bob_loc = vf.createLiteral("+40.0+40.0", cartSystem);
		Literal coral_loc = vf.createLiteral("+50.0+50.0", cartSystem);
		conn.add(alice, location, alice_loc);
		conn.add(bob, location, bob_loc);
		conn.add(carol, location, coral_loc);
		println("Find people located within box1.");
		printRows(conn.getStatementsInBox(cartSystem, location, 20, 40, 20, 40,
				0, false));
		println("Find people located within circle1.");
		printRows(conn.getStatementsInCircle(cartSystem, location, 35, 35, 10,
				0, false));
		URI polygon1 = vf.createURI("http://example.org/polygon1");
		List<Literal> polygon1_points = new ArrayList<Literal>(4);
		polygon1_points.add(vf.createLiteral("+10.0+40.0", cartSystem));
		polygon1_points.add(vf.createLiteral("+50.0+10.0", cartSystem));
		polygon1_points.add(vf.createLiteral("+35.0+40.0", cartSystem));
		polygon1_points.add(vf.createLiteral("+50.0+70.0", cartSystem));
		println(polygon1_points);
		conn.registerPolygon(polygon1, polygon1_points);
		println("Find people located within ploygon1.");
		printRows(conn.getStatementsInPolygon(cartSystem, location, polygon1,
				0, false));
		// now we switch to a Spherical (Lat/Long) coordinate system
		URI sphericalSystemKM = conn.registerSphericalType(5,
				AGProtocol.KM_PARAM_VALUE);
		// URI sphericalSystemDegree = conn.registerSphericalType(5,
		// AGProtocol.DEGREE_PARAM_VALUE);
		URI amsterdam = vf.createURI(exns, "amsterdam");
		URI london = vf.createURI(exns, "london");
		URI sanfrancisco = vf.createURI(exns, "sanfrancisco");
		URI salvador = vf.createURI(exns, "salvador");
		location = vf.createURI(exns, "geolocation");
		conn.add(amsterdam, location, vf.createLiteral("+52.366665+004.883333",
				sphericalSystemKM));
		conn.add(london, location, vf.createLiteral("+51.533333-000.08333333",
				sphericalSystemKM));
		conn.add(sanfrancisco, location, vf.createLiteral(
				"+37.783333-122.433334", sphericalSystemKM));
		conn.add(salvador, location, vf.createLiteral("-13.783333-038.45",
				sphericalSystemKM));
		println("Locate entities within box2.");
		printRows(conn.getStatementsInBox(sphericalSystemKM, location, 0.08f,
				0.09f, 51.0f, 52.0f, 0, false));
		println("Locate entities within haversine circle.");
		printRows(conn.getGeoHaversine(sphericalSystemKM, location, 50.0f,
				0.0f, 1000.0f, "km", 0, false));
		URI polygon2 = vf.createURI("http://example.org/polygon2");
		List<Literal> polygon2_points = new ArrayList<Literal>(3);
		polygon2_points.add(vf.createLiteral("+51.0+002.0", sphericalSystemKM));
		polygon2_points.add(vf.createLiteral("+60.0-005.0", sphericalSystemKM));
		polygon2_points.add(vf.createLiteral("+48.0-012.5", sphericalSystemKM));
		println(polygon2_points);
		conn.registerPolygon(polygon2, polygon2_points);
		println("Locate entities within polygon2.");
		printRows(conn.getStatementsInPolygon(sphericalSystemKM, location,
				polygon2, 0, false));
	}*/

	/**
	 * Social Network Analysis
	 *
	public static void example21() throws Exception {
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getCatalog(CATALOG_ID);
		catalog.deleteRepository(REPOSITORY_ID);
		AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
		myRepository.initialize();
		AGValueFactory vf = myRepository.getValueFactory();
		AGRepositoryConnection conn = myRepository.getConnection();
		closeBeforeExit(conn);
		conn.add(new File("src/tutorial/java-lesmis.rdf"), null,
				RDFFormat.RDFXML);
		println("Loaded " + conn.size() + " java-lesmis.rdf triples.");

		// Create URIs for relationship predicates.
		String lmns = "http://www.franz.com/lesmis#";
		conn.setNamespace("lm", lmns);
		URI knows = vf.createURI(lmns, "knows");
		URI barelyKnows = vf.createURI(lmns, "barely_knows");
		URI knowsWell = vf.createURI(lmns, "knows_well");

		// Create URIs for some characters.
		URI valjean = vf.createURI(lmns, "character11");
		// URI bossuet = vf.createURI(lmns, "character64");

		// Create some generators
		// print "\nSNA generators known (should be none): '%s'" %
		// (conn.listSNAGenerators())
		List<URI> intimates = new ArrayList<URI>(1);
		Collections.addAll(intimates, knowsWell);
		conn.registerSNAGenerator("intimates", null, null, intimates, null);
		List<URI> associates = new ArrayList<URI>(2);
		Collections.addAll(associates, knowsWell, knows);
		conn.registerSNAGenerator("associates", null, null, associates, null);
		List<URI> everyone = new ArrayList<URI>(3);
		Collections.addAll(everyone, knowsWell, knows, barelyKnows);
		conn.registerSNAGenerator("everyone", null, null, everyone, null);
		println("Created three generators.");

		// Create neighbor matrix.
		List<URI> startNodes = new ArrayList<URI>(1);
		startNodes.add(valjean);
		conn.registerSNANeighborMatrix("matrix1", "intimates", startNodes, 2);
		conn.registerSNANeighborMatrix("matrix2", "associates", startNodes, 5);
		conn.registerSNANeighborMatrix("matrix3", "everyone", startNodes, 2);
		println("Created three matrices.");

		// Explore Valjean's ego group.
		println("\nValjean's ego group members (using associates).");
		String queryString = "(select (?member ?name)"
				+ "(ego-group-member !lm:character11 1 associates ?member)"
				+ "(q ?member !dc:title ?name))";
		TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG,
				queryString);
		TupleQueryResult result = tupleQuery.evaluate();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			Value p = bindingSet.getValue("member");
			Value n = bindingSet.getValue("name");
			println("member: " + p + ", name: " + n);
		}
		result.close();

		// Valjean's ego group using neighbor matrix.
		println("\nValjean's ego group (using associates matrix).");
		queryString = "(select (?member ?name)"
				+ "(ego-group-member !lm:character11 1 matrix2 ?member)"
				+ "(q ?member !dc:title ?name))";
		tupleQuery = conn
				.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
		result = tupleQuery.evaluate();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			Value p = bindingSet.getValue("member");
			Value n = bindingSet.getValue("name");
			println("member: " + p + ", name: " + n);
		}
		result.close();
	}*/

	/**
	 * Transactions
	 *
	public static void example22() throws Exception {
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getCatalog(CATALOG_ID);
		AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
		myRepository.initialize();
		AGValueFactory vf = myRepository.getValueFactory();
		// Create conn1 (autoCommit) and conn2 (no autoCommit).
		AGRepositoryConnection conn1 = myRepository.getConnection();
		closeBeforeExit(conn1);
		conn1.clear();
		AGRepositoryConnection conn2 = myRepository.getConnection();
		closeBeforeExit(conn2);
		conn2.setAutoCommit(false);
		String baseURI = "http://example.org/example/local";
		conn1.add(new File("src/tutorial/java-lesmis.rdf"), baseURI,
				RDFFormat.RDFXML);
		println("Loaded " + conn1.size()
				+ " java-lesmis.rdf triples via conn1.");
		conn2.add(new File("src/tutorial/java-kennedy.ntriples"), baseURI,
				RDFFormat.NTRIPLES);
		println("Loaded " + conn2.size() + " java-kennedy.ntriples via conn2.");

		println("\nSince conn1 is in autoCommit mode, java-lesmis.rdf triples are committed "
				+ "and retrievable via conn2.  Since conn2 is not in autoCommit mode, and "
				+ "no commit() has yet been issued on conn2, kennedy.rdf triples are not "
				+ " retrievable via conn1.");
		// Check transaction isolation semantics:
		Literal valjean = vf.createLiteral("Valjean");
		Literal kennedy = vf.createLiteral("Kennedy");
		printRows("\nUsing getStatements() on conn1; should find Valjean:", 1,
				conn1.getStatements(null, null, valjean, false));
		printRows("\nUsing getStatements() on conn1; should not find Kennedy:",
				1, conn1.getStatements(null, null, kennedy, false));
		printRows(
				"\nUsing getStatements() on conn2; should not find Valjean (until a rollback or commit occurs on conn2):",
				1, conn2.getStatements(null, null, valjean, false));
		printRows("\nUsing getStatements() on conn2; should find Kennedy:", 1,
				conn2.getStatements(null, null, kennedy, false));

		// Rollback
		println("\nRolling back contents of conn2.");
		conn2.rollback();
		println("There are now " + conn2.size() + " triples visible via conn2.");
		printRows("\nUsing getStatements() on conn1; should find Valjean:", 1,
				conn1.getStatements(null, null, valjean, false));
		printRows(
				"\nUsing getStatements() on conn1; should not find Kennedys:",
				1, conn1.getStatements(null, null, kennedy, false));
		printRows(
				"\nUsing getStatements() on conn2; should not find Kennedys:",
				1, conn2.getStatements(null, null, kennedy, false));
		printRows("\nUsing getStatements() on conn2; should find Valjean:", 1,
				conn2.getStatements(null, null, valjean, false));
		// Reload and Commit
		println("\nReload java-kennedy.ntriples into conn2.");
		conn2.add(new File("src/tutorial/java-kennedy.ntriples"), baseURI,
				RDFFormat.NTRIPLES);
		println("There are now " + conn1.size() + " triples visible on conn1.");
		println("There are now " + conn2.size() + " triples visible on conn2.");
		println("\nCommitting contents of conn2.");
		conn2.commit();
		println("There are now " + conn1.size() + " triples visible on conn1.");
		println("There are now " + conn2.size() + " triples visible on conn2.");
		printRows("\nUsing getStatements() on conn1; should find Valjean:", 1,
				conn1.getStatements(null, null, valjean, false));
		printRows("\nUsing getStatements() on conn1; should find Kennedys:", 1,
				conn1.getStatements(null, null, kennedy, false));
		printRows("\nUsing getStatements() on conn2; should find Kennedys:", 1,
				conn2.getStatements(null, null, kennedy, false));
		printRows("\nUsing getStatements() on conn2; should find Valjean:", 1,
				conn2.getStatements(null, null, valjean, false));
	}*/

	/**
	 * Usage: all Usage: [1-22]+
	 */
	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		if (args.length == 0) {
			// for choosing by editing this code
			choices.add(1);
		} else if (args[0].equals("all")) {
			for (int i = 1; i <= 22; i++) {
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
				case 10:
					//example10();
					break;
				case 11:
					example11();
					break;
				case 12:
					//example12();
					break;
				case 13:
					//example13();
					break;
				case 14:
					//example14();
					break;
				case 15:
					//example15();
					break;
				case 16:
					//example16();
					break;
				case 17:
					//example17();
					break;
				case 18:
					//example18();
					break;
				case 19:
					example19();
					break;
				case 20:
					//example20();
					break;
				case 21:
					//example21();
					break;
				case 22:
					//example22();
					break;
				default:
					throw new IllegalArgumentException("There is no example "
							+ choice);
				}
				closeAll();
			}
		} finally {
			closeAll();
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

	static void printRows(String headerMsg, int limit,	StmtIterator rows) throws Exception {
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
	private static void closeBeforeExit(AGRepositoryConnection conn) {
		toClose.add(conn);
	}

	private static void closeAll() {
		while (toClose.isEmpty() == false) {
			AGRepositoryConnection conn = toClose.get(0);
			close(conn);
			while (toClose.remove(conn)) {
			}
		}
	}

}
