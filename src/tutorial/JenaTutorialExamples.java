/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

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
		String path1 = "src/tutorial/java-vcards.rdf";
		String path2 = "src/tutorial/java-kennedy.ntriples";
		String baseURI = "http://example.org/example/local";
		model_vcards.read(new FileInputStream(path1), baseURI);
		model.read(new FileInputStream(path2), baseURI, "N-TRIPLE");
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
	 * Writing RDF or NTriples to a file
	 */
	public static void example8() throws Exception {
		AGGraphMaker maker = example6();
		AGModel model = new AGModel(maker.getGraph());
		//AGModel model_vcards = new AGModel(maker.openGraph("http://example.org#vcards"));
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
		output.close();
		String outputFile2 = TEMPORARY_DIRECTORY + "temp.rdf";
		// outputFile2 = null;
		if (outputFile2 == null) {
			println("\nWriting RDF to Standard Out instead of to a file");
		} else {
			println("\nWriting RDF to: " + outputFile2);
		}
		output = (outputFile2 != null) ? new FileOutputStream(outputFile2)
				: System.out;
		model.write(output);
		output.close();
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
	 * Usage: all Usage: [1-9,11,13,19]+
	 */
	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		if (args.length == 0) {
			// for choosing by editing this code
			choices.add(1);
		} else if (args[0].equals("all")) {
			for (int i = 1; i <= 9; i++) {
				choices.add(i);
			}
			choices.add(11);
			choices.add(13);
			choices.add(19);
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
				case 13:
					example13();
					break;
				case 19:
					example19();
					break;
				default:
					throw new IllegalArgumentException("There is no example "
							+ choice);
				}
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

	protected static void printRows(String headerMsg, int limit,	StmtIterator rows) throws Exception {
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
		while (toClose.isEmpty() == false) {
			AGRepositoryConnection conn = toClose.get(0);
			close(conn);
			while (toClose.remove(conn)) {
			}
		}
	}

}
