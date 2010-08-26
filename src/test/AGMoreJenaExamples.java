/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.FileInputStream;

import tutorial.JenaTutorialExamples;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.jena.AGReasoner;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGVirtualRepository;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class AGMoreJenaExamples extends JenaTutorialExamples {

	public static AGGraphMaker exampleMakerForRestrictionReasoningRepository() throws Exception {
		AGServer server = new AGServer(SERVER_URL,USERNAME,PASSWORD);
		AGCatalog catalog = server.getCatalog(CATALOG_ID);
		catalog.deleteRepository(REPOSITORY_ID);
		catalog.createRepository(REPOSITORY_ID);
		String spec = AGVirtualRepository.reasoningSpec("<"+CATALOG_ID+":"+REPOSITORY_ID+">", "restriction"); 
		AGVirtualRepository repo = server.virtualRepository(spec);
		AGRepositoryConnection conn = repo.getConnection();
		return new AGGraphMaker(conn);
	}
	
	public static void exampleRestrictionReasoning() throws Exception {
		//AGGraphMaker maker = exampleMakerForRestrictionReasoningRepository();
		AGGraphMaker maker = example1(false);
		AGModel model = new AGModel(maker.getGraph());
		AGReasoner reasoner = new AGReasoner("restriction");
		InfModel infmodel = new AGInfModel(reasoner, model);
		Resource a = model.createResource("http://a");
		Resource c = model.createResource("http://C");
		Property p = model.createProperty("http://p");
		Resource v = model.createResource("http://v");
		model.add(c,OWL.equivalentClass,c);
		model.add(c,RDF.type,OWL.Restriction);
		model.add(c,OWL.onProperty,p);
		model.add(c,OWL.hasValue,v);
		model.add(a,RDF.type,c);
		StmtIterator results = infmodel.listStatements();
		while (results.hasNext()) {
			System.out.println(results.next());
		}
		
		System.out.println(infmodel.contains(a,RDF.type,c));
		System.out.println(infmodel.contains(a,p,v));
	}
	
	public static void exampleBulkUpdate() throws Exception {
		AGGraphMaker maker = example6();
		AGModel model = new AGModel(maker.getGraph());
		AGModel model2 = new AGModel(maker.createGraph("http://example.org/foo"));
		StmtIterator statements = model.listStatements(null,RDF.type, (RDFNode)null);
		model2.add(statements);
		System.out.println("Size: "+model2.size());
		model2.write(System.out);
		statements = model.listStatements(null,RDF.type, (RDFNode)null);
		model2.remove(statements);
		System.out.println("Size: "+model2.size());
		model2.write(System.out);
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
        String path = "src/tutorial/java-rules.txt";
        maker.getRepositoryConnection().addRules(new FileInputStream(path));
        String queryString = 
        	"(select (?person ?uncle) " +
        		"(uncle ?y ?x)" +
        		"(name ?x ?person)" +
        		"(name ?y ?uncle))";
			AGQuery prolog = AGQueryFactory.create(AGQueryLanguage.PROLOG,queryString);
			QueryExecution qe = AGQueryExecutionFactory.create(prolog, model);
			try {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution result = results.next();
					RDFNode p = result.get("person");
					RDFNode u = result.get("uncle");
					println(u + " is the uncle of " + p);
				}
			} finally {
				qe.close();
			}
		} finally {
			model.close();
		}
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
        model1.read(new FileInputStream("src/tutorial/java-lesmis.rdf"), baseURI);
        println("Loaded " + model1.size() + " java-lesmis.rdf triples via conn1.");
        model2.read(new FileInputStream("src/tutorial/java-kennedy.ntriples"), baseURI, "N-TRIPLE");
        println("Loaded " + model2.size() + " java-kennedy.ntriples via conn2.");
        
        println("\nSince model1 is not in a transaction, java-lesmis.rdf triples are committed " +
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
        printRows("\nUsing listStatements() on model2; should not find Valjean (until a rollback or commit occurs on model2):",
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
        model2.read(new FileInputStream("src/tutorial/java-kennedy.ntriples"), baseURI, "N-TRIPLE");
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

	public static void main(String[] args) throws Exception {
		/*example17();
		example18();
		example22();
		exampleBulkUpdate();*/
		exampleRestrictionReasoning();
		closeAll();
	}
}
