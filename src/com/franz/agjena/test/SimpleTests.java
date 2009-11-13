package com.franz.agjena.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraph;
import com.hp.hpl.jena.graph.Triple;
import com.franz.agbase.TriplesIterator;
import com.franz.agjena.AllegroGraphGraph;
import com.franz.agjena.AllegroGraphGraphMaker;
import com.franz.agjena.AllegroGraphModel;
import com.franz.agjena.AllegroGraphReasoner;
import com.franz.agjena.StartUp;
import com.franz.agjena.exceptions.NiceException;
import com.franz.agjena.query.AllegroGraphQuery;
import com.franz.agjena.query.AllegroGraphQueryExecutionFactory;
import com.franz.agjena.query.AllegroGraphQueryFactory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.VCARD;

public class SimpleTests {
	
	AllegroGraphGraphMaker maker = null;
	
	private SimpleTests (AllegroGraphGraphMaker maker) {
		this.maker = maker;
	}
	
	/**
	 * Execute 'sparqlQuery' against 'model' and print a tabular result.
	 */
	public static void doQuery (String sparqlQuery, Model model) {
        AllegroGraphQuery query = AllegroGraphQueryFactory.create(sparqlQuery) ;
        System.out.println("Query: " + sparqlQuery);
        QueryExecution qe = AllegroGraphQueryExecutionFactory.create(query, model) ;
        ResultSetRewindable rs = ResultSetFactory.makeRewindable(qe.execSelect()) ;
        ResultSetFormatter.out(rs) ;
        qe.close() ; 
	}
	
	/**
	 * Execute 'sparqlQuery' against 'graph' and print a tabular result.
	 */
	public static void doQuery (String sparqlQuery, Graph graph) {
        AllegroGraphQuery query = AllegroGraphQueryFactory.create(sparqlQuery) ;
        System.out.println("Query: " + sparqlQuery);
        // THERE MUST BE A BETTER WAY TO DEFINE THE DATASET
        QueryExecution qe = AllegroGraphQueryExecutionFactory.create(query, new DatasetImpl(new ModelCom(graph))) ;
        ResultSetRewindable rs = ResultSetFactory.makeRewindable(qe.execSelect()) ;
        ResultSetFormatter.out(rs) ;
        qe.close() ; 
	}

	/**
	 * Print all quads in 'model'.
	 * TODO: If 'model' is bound to a graph other than the default, only print
	 * quads for that model (probably we don't care enough to actually do this).
	 */
	public static void printModel (Model model) {
		AllegroGraphModel agm = (AllegroGraphModel)model;
		System.out.println("Dump of statements in model '" + agm.getName());
		AllegroGraph agStore = agm.getAllegroGraphStore();
		try {
			TriplesIterator csr = agStore.getStatements(null, null, null, null);
			while (csr.hasNext()) {
				com.franz.agbase.Triple st = csr.next();
				System.out.println(st);
			}
		} catch (Exception ex) {throw new NiceException(ex);}
		
	}
	
	
    @SuppressWarnings("unchecked")
	public void tutorial3 () throws AllegroGraphException { 
    	System.out.println("\n\nTESTING MANUAL INSERTION OF TRIPLES AND A SELECT ALL.");
        // some definitions
        String personURI    = "http://somewhere/JohnSmith";
        String givenName    = "John";
        String familyName   = "Smith";
        String fullName     = givenName + " " + familyName;
        // create an empty graph and a model for it:
        Graph graph = this.maker.createGraph();
        
        TransactionHandler th = graph.getTransactionHandler();
        System.out.println( "transactionsSupported=" + th.transactionsSupported());
        
        // This caused a stack overflow [bug18132]
        if ( !graph.isEmpty() ) 
        	throw new IllegalStateException("New graph is not empty.");
        Model model = new AllegroGraphModel(graph);
        // create the resource
        //   and add the properties cascading style
        Resource johnSmith = model.createResource(personURI)
         		.addProperty(VCARD.FN, fullName)
         			.addProperty(VCARD.N, 
                              model.createResource()
                                   .addProperty(VCARD.Given, givenName)
                                   .addProperty(VCARD.Family, familyName));
        
        // list the statements in the graph
        StmtIterator iter = model.listStatements();
        // print out the predicate, subject and object of each statement
        while (iter.hasNext()) {
            Statement stmt      = iter.nextStatement();         // get next statement
            Resource  subject   = stmt.getSubject();   // get the subject
            Property  predicate = stmt.getPredicate(); // get the predicate
            RDFNode   object    = stmt.getObject();    // get the object
            System.out.print(subject.toString());
            System.out.print(" " + predicate.toString() + " ");
            if (object instanceof Resource) {
                System.out.print(object.toString());
            } else {
                // object is a literal
                System.out.print(" \"" + object.toString() + "\"");
            }
            System.out.println(" .");
        }
        String query = "select ?s ?p ?o where {?s ?p ?o }";
		doQuery(query, model);
		// These caused stack overflows [bug18137]
		model.close();
		
		// Try to add a batch of statements.
//		AllegroGraphGraph agraph = (AllegroGraphGraph) graph;
//		agraph.getAllegroGraphStore().serverTrace(true);
		ArrayList all = new ArrayList<Object>();
		all.add(model.createStatement(model.createResource("a1"), model.createProperty("b"), "c") );
		all.add(model.createStatement(model.createResource("a2"), model.createProperty("b"), "c") );
		all.add(model.createStatement(model.createResource("a3"), model.createProperty("b"), "c") );
		long a = model.size();
		model.add(all);
		long b = model.size();
		if ( b!=(a+3) ) 
			throw new IllegalStateException("Expected " + (a+3) + " found " + b);
//		agraph.getAllegroGraphStore().serverTrace(false);
		
		graph.close();
    }
    
    private void queryTest2() {
    	System.out.println("\n\nTESTING RETRIEVAL VIA QUERY SOLUTION.");
        Graph graph = this.maker.createGraph();
        Model model = new AllegroGraphModel(graph);
        Resource r1 = model.createResource("http://example.org/book#1") ;
        Resource r2 = model.createResource("http://example.org/book#2") ;
        r1.addProperty(DC.title, "SPARQL - the book")
          .addProperty(DC.description, "A book about SPARQL") ;
        r2.addProperty(DC.title, "Advanced techniques for SPARQL") ;
        String queryString = "prefix dc: <" + DC.NS + ">\n" +
            "select ?title where {?x dc:title ?title}" ;         
        QueryExecution qexec = AllegroGraphQueryExecutionFactory.create(queryString, model) ;
        System.out.println("Titles: ") ;
        int titles = 0;
        try {
            ResultSet rs = qexec.execSelect() ;
            // The order of results is undefined. 
            while(rs.hasNext()) {
                QuerySolution rb = rs.nextSolution() ;
                titles++;
                RDFNode x = rb.get("title") ;
                if ( x.isLiteral() )  {
                    Literal titleStr = (Literal)x  ;
                    System.out.println("    "+titleStr) ;
                }
                else
                    System.out.println("Strange - not a literal: "+x) ;
            }
        } finally  {
            qexec.close() ;
        }
        if ( titles!=2 ) 
        	throw new IllegalStateException("Expected 2, found " + titles + " results.");
        model.close();
    }
    
    private void tutorial8 () {
    	System.out.println("\n\nTESTING LOAD FROM RDF FILE INTO GRAPH.");
        String inputFileName = root+"src/com/franz/agjena/test/vc-db-1.rdf";
        Graph graph = this.maker.createGraph("http://foo#bar");
        Model model = new AllegroGraphModel(graph);
        // read the RDF/XML file
        model.read( inputFileName, "RDF/XML" );
        // select all the resources with a VCARD.FN property
        // whose value ends with "Smith"
        System.out.println("Model has " + model.size() + " triples.");
        StmtIterator iter = model.listStatements(
            new SimpleSelector(null, VCARD.FN, (RDFNode) null) {
                    public boolean selects(Statement s) {
                            return s.getString().endsWith("Smith");
                    }
                });
        if (iter.hasNext()) {
            System.out.println("The database contains vcards for:");
            while (iter.hasNext()) {
                System.out.println("  " + iter.nextStatement()
                                              .getString());
            }
        } else {
            System.out.println("No Smith's were found in the database");
        }
//        printModel(model);
        model.close();
    }

	public void queryTest3 () {
		System.out.println("\n\nTESTING ENABLING INFERENCE ON A GRAPH.");
        String inputFileName = root+"src/com/franz/agjena/test/inference.nt";
        String demoNamespace = "http://ag.franz.com/demo#";
        String graphName1 = demoNamespace + "context1";
        //String graphName2 = demoNamespace + "context2";
        Graph graphOne = this.maker.createGraph(graphName1);
        //Graph graphTwo = new AllegroGraphGraph(graphOne, new AllegroGraphReasoner());
        Graph graphTwo = new AllegroGraphReasoner().bind(graphOne);
        Model model = null;
        switch (2) {
        case 1: model = new AllegroGraphModel(graphOne); // no inference
        	break;
        case 2: 
        	 model = new AllegroGraphModel(graphTwo); // yes inference
        	 break;
        }
        // read the ntriples file
        model.read( inputFileName, "N-TRIPLE" );
        String rdf = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";
        String rdfs = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";
        String dcterms = "prefix dcterms: <http://purl.org/dc/terms/>\n";
        String demo = "prefix demo: <http://ag.franz.com/demo#>\n";
        String prefixes = rdf + rdfs + dcterms + demo;
        String query = prefixes + "select ?s ?p ?o where {?s ?p ?o . ?s rdf:type demo:FootballTeam . }";        
		doQuery(query, model);
        String query2 = prefixes + "select ?name where {?s rdfs:label ?name . ?s rdf:type demo:SportsTeam . }";
		doQuery(query2, model);
        String query4 = prefixes + "select ?name where {?s rdfs:label ?name . ?s dcterms:coverage ?city . " +
        							"?city dcterms:isPartOf ?country . " +
        							"?country dcterms:isPartOf demo:europe . }";
		doQuery(query4, model);
        String query3 = prefixes + "select ?name where {?s rdfs:label ?name . ?s dcterms:coverage ?city . ?city dcterms:isPartOf demo:europe }";
		doQuery(query3, model);
		model.close();
	}
	
	String root = "w:/franz-work/AllegroGraph/java3-ws/java3-jena/";
	
	public void queryTest4 () {		
		System.out.println("\n\nTESTING LOADING INTO TWO DISTINCT GRAPHS, AND KEEPING THEIR QUERIES DISTINCT.");
		String inputFileName1 = root+"src/com/franz/agjena/test/vc-db-1.rdf";
        String inputFileName2 = root+"src/com/franz/agjena/test/inference.nt";
        String demoNamespace = "http://ag.franz.com/demo#";
        String graphName1 = demoNamespace + "context1";
        String graphName2 = demoNamespace + "context2";
        this.maker.setDefaultIsGraphOfAllGraphs(true);
        Graph graphAll = this.maker.getGraph(); // default graph represents the graph containing all triples 
        Graph graphOne = this.maker.createGraph(graphName1);
        Graph graphTwo = this.maker.createGraph(graphName2);
        Graph graphThree = new AllegroGraphReasoner().bind(graphTwo);
        Model modelAll = new AllegroGraphModel(graphAll);
        Model modelOne = new AllegroGraphModel(graphOne);
        Model modelTwo = new AllegroGraphModel(graphTwo);
        modelOne.read(inputFileName1, "RDF/XML" );
        modelTwo.read(inputFileName2, "N-TRIPLE" );
        String query = "select ?s ?p ?o where {?s ?p ?o }";
        
//        query = "select ?s ?p ?o " +
//        "from named <http://ag.franz.com/demo#context1>  from named <http://ag.franz.com/demo#context2> " +
//		"where { {graph ?g {?s ?p ?o } } }";
//        
//        query = "select ?s ?p ?o " +
//        "from  <http://ag.franz.com/demo#context1> " +
//        "from  <http://ag.franz.com/demo#context2> " +        
//		"where { ?s ?p ?o }";
        
        query = "select ?super where {<http://ag.franz.com/demo#liverpooluk> <http://purl.org/dc/terms/isPartOf> ?super }";
        
        // FAILS:
        query = "select ?super where {<http://ag.franz.com/demo#liverpooluk> <http://purl.org/dc/terms/isPartOf> ?super . " +
                                     " ?super <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ag.franz.com/demo#Continent>}";
        
        switch (3) {
        case 0: 
        	// return all triples (or none)
        	doQuery(query, modelAll);
        	break;
        case 1: 
        	// retrieve all vcard triples
        	doQuery(query, modelOne);
        	//printModel(modelOne);
        	break;
        case 2: 
        	// retrieve all football triples (no inference)        	
        	doQuery(query, modelTwo);
        	break;
        case 3: 
        	// retrieve all football triples with inference
        	doQuery(query, graphThree);
        	break;
        case 4: 
        	// NOPE: THERE IS NO NAME HERE:
        	// retrieve all football triples with inference
        	String name = ((AllegroGraphGraph)graphThree).getName();
        	query = "select ?s ?p ?o from <" + name + "> where {?s ?p ?o }";
        	doQuery(query, modelTwo);
        	break;
        }
        for (Iterator it = maker.listGraphs(); it.hasNext();) {
        	System.out.println("GraphName: " + it.next());
        }
        modelAll.close();
        modelOne.close();
        modelTwo.close();
	}
        
	public void queryTest5 () {		
		System.out.println("\n\nTESTING IF GRAPH OF ALL GRAPHS WORKS.");
		String inputFileName1 = root+"src/com/franz/agjena/test/vc-db-1.rdf";
//		inputFileName1 = "/Users/bmacgregor/Documents/eclipse-jag/my_base/src/com/franz/ag/jena/test/vc-db-1.rdf";
//		File file = new File(inputFileName1);
//		System.out.println("Exists: " + file.exists() + "  at " + file.getAbsolutePath());
        String demoNamespace = "http://ag.franz.com/demo#";
        String graphName1 = demoNamespace + "context1";
        this.maker.setDefaultIsGraphOfAllGraphs(true);
        Graph graphOne = this.maker.createGraph(graphName1);
        Model modelOne = new AllegroGraphModel(graphOne);
        modelOne.read(inputFileName1, "RDF/XML" );
        String query = "select ?s ?p ?o where {?s ?p ?o }";
        doQuery(query, this.maker.getDefaultGraph());
        modelOne.close();
	}
	
	/**
	 * THIS IS RANDOM.  ADD MORE TESTS, AND GRADUALLY MAKE IT INTO SOMETHING SENSIBLE - RMM
	 * @throws AllegroGraphException 
	 */
	public void runTests () throws AllegroGraphException {
		tutorial3();
		queryTest2();
		tutorial8();
		queryTest3();
		queryTest4();
		queryTest5();
	}
	
	/**
	 * Common Lisp commands to start up AG:
	 * Launch mlisp  (not alisp8!)
	   (require :agraph)  
       (in-package :triple-store-user)  
       (setf *synchronize-automatically* t)
       (db.agraph:start-agj-server) 
       (create-triple-store "temp/test" :if-exists :supersede) 
     *
	 */
    public static void main(String[] args) throws AllegroGraphException, IOException {
    	AllegroGraph agStore = StartUp.startUpTripleStore(
    			StartUp.AccessTripleStore.RENEW,
    			"localhost", "test", 
    			//"/Users/bmacgregor/Desktop/AGFolder"
    			"/tmp/agtest",
    			args);
	    AllegroGraphGraphMaker maker = new AllegroGraphGraphMaker(agStore);
    	try {
    		new SimpleTests(maker).runTests();
    	} catch (Exception ex) {
    		System.out.println("Failure in SimpleTests\n" + ex.toString() + ex.getMessage());
    		ex.printStackTrace();
    	} finally {
    		StartUp.shutDownTripleStore(agStore);
    	}
    }


}
