/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGFreetextIndexConfig;
import com.franz.agraph.repository.AGFreetextQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGValueFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.DatasetImpl;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static test.Stmt.statementSet;
import static test.Stmt.stmts;
import static test.Stmt.stmtsSP;

public class TutorialTests extends AGAbstractTest {

    static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

    public static IRI example6_setup(AGRepositoryConnection conn, AGRepository repo) throws Exception {
        conn.clear();
        conn.setAutoCommit(false);  // dedicated session
        assertEquals(0, conn.size());
        ValueFactory f = repo.getValueFactory();
        String path1 = "/tutorial/java-vcards.rdf";
        String path2 = "/tutorial/java-kennedy.ntriples";
        String baseURI = "http://example.org/example/local";
        IRI context = f.createIRI("http://example.org#vcards");
        // read vcards triples into the context 'context':
        Util.add(conn, path1, baseURI, RDFFormat.RDFXML, context);
        // read Kennedy triples into the null context:
        Util.add(conn, path2, baseURI, RDFFormat.NTRIPLES);
        return context;
    }

    @Test
    public void example1() throws Exception {
        assertTrue(server.getVersion().length() > 0);
        assertTrue(server.getBuildDate().length() > 0);
        assertTrue(server.getRevision().length() > 0);
        assertTrue(server.listCatalogs().size() > 0);
        assertEquals(CATALOG_ID, cat.getCatalogName());
        assertEquals(REPO_ID, repo.getRepositoryID());
        assertNotNull(repo.getRepositoryURL());
        assertTrue(repo.isWritable());
        assertEquals(0, conn.size());
    }

    private Map<String, Stmt> example2inputs() throws Exception {
        AGValueFactory vf = repo.getValueFactory();
        IRI alice = vf.createIRI("http://example.org/people/alice");
        IRI bob = vf.createIRI("http://example.org/people/bob");
        IRI name = vf.createIRI("http://example.org/ontology/name");
        IRI person = vf.createIRI("http://example.org/ontology/Person");
        Literal bobsName = vf.createLiteral("Bob");
        Literal alicesName = vf.createLiteral("Alice");
        assertEquals(0, conn.size());
        Map<String, Stmt> inputs = new HashMap<String, Stmt>();
        inputs.put("an", new Stmt(alice, name, alicesName));
        inputs.put("at", new Stmt(alice, RDF.TYPE, person));
        inputs.put("bn", new Stmt(bob, name, bobsName));
        inputs.put("bt", new Stmt(bob, RDF.TYPE, person));
        return inputs;
    }

    private Map<String, Stmt> example2setup() throws Exception {
        Map<String, Stmt> inputs = example2inputs();
        addAll(inputs.values(), conn);
        assertEquals(4, conn.size());
        return inputs;
    }

    @Test
    public void example2() throws Exception {
        Map<String, Stmt> inputs = example2setup();
        Set<Stmt> stmts = statementSet(conn.getStatements(null, null, null, false));
        assertSetsEqual(inputs.values(), stmts);
        Stmt x = inputs.values().iterator().next();
        conn.remove(x);
        assertEquals(3, conn.size());
        conn.add(x);
        assertEquals(4, conn.size());
    }

    @Test
    public void example3() throws Exception {
        Map<String, Stmt> inputs = example2setup();
        String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        Set<Stmt> stmts = statementSet(tupleQuery.evaluate());
        assertSetsEqual(inputs.values(), stmts);
    }

    @Test
    public void example4() throws Exception {
        Map<String, Stmt> inputs = example2setup();
        IRI alice = repo.getValueFactory().createIRI("http://example.org/people/alice");
        RepositoryResult<Statement> result = conn.getStatements(alice, null, null, false);
        result.enableDuplicateFilter();
        Set<Stmt> stmts = statementSet(result);
        assertSetsEqual(mapKeep(new String[] {"an", "at"}, inputs).values(), stmts);
    }

    @Test
    public void example5() throws Exception {
        example2setup();
        conn.clear();
        ValueFactory f = repo.getValueFactory();
        String exns = "http://example.org/people/";
        IRI alice = f.createIRI("http://example.org/people/alice");
        IRI ted = f.createIRI(exns, "ted");
        IRI age = f.createIRI(exns, "age");
        IRI weight = f.createIRI(exns, "weight");
        IRI favoriteColor = f.createIRI(exns, "favoriteColor");
        IRI birthdate = f.createIRI(exns, "birthdate");
        Literal red = f.createLiteral("Red");
        Literal rouge = f.createLiteral("Rouge", "fr");
        Literal fortyTwoInt = f.createLiteral("42", XMLSchema.INT);
        Literal fortyTwoLong = f.createLiteral("42", XMLSchema.LONG);
        Literal fortyTwoUntyped = f.createLiteral("42");
        Literal date = f.createLiteral("1984-12-06", XMLSchema.DATE);
        // TODO: added Z to fix the error
        Literal time = f.createLiteral("1984-12-06T09:00:00Z", XMLSchema.DATETIME);
        Literal weightUntyped = f.createLiteral("120.5");
        Literal weightFloat = f.createLiteral("120.5", XMLSchema.FLOAT);
        Statement stmt1 = f.createStatement(alice, age, fortyTwoInt);
        Statement stmt2 = f.createStatement(ted, age, fortyTwoLong);
        Statement stmt3 = f.createStatement(ted, age, fortyTwoUntyped);
        Set<Statement> inputs = new HashSet<Statement>();
        inputs.add(stmt1);
        inputs.add(stmt2);
        inputs.add(stmt3);
        inputs.add(new Stmt(alice, weight, weightFloat));
        inputs.add(new Stmt(ted, weight, weightUntyped));
        inputs.add(new Stmt(alice, favoriteColor, red));
        inputs.add(new Stmt(ted, favoriteColor, rouge));
        inputs.add(new Stmt(alice, birthdate, date));
        inputs.add(new Stmt(ted, birthdate, time));
        addAll(inputs, conn);
        assertEquals(9, conn.size());
        assertSetsEqual(inputs, statementSet(conn.getStatements(null, null, null, false)));
        for (Literal obj : new Literal[] {fortyTwoInt, fortyTwoLong, fortyTwoUntyped, weightFloat, weightUntyped,
                red, rouge}) {
            assertSetsSome("Retrieve triples matching: " + obj,
                    inputs, statementSet(conn.getStatements(null, null, obj, false)));
        }
        // SPARQL
        for (String obj : new String[] {"42", "\"42\"", "120.5", "\"120.5\"", "\"120.5\"^^xsd:float",
                "\"Rouge\"@fr", "\"Rouge\"", "\"1984-12-06\"^^xsd:date"}) {
            String queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?o = " + obj + ")}";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            assertSetsSome("Query triples matching " + obj + ".",
                    inputs, statementSet(tupleQuery.evaluate()));
        }
        assertSetsEqual("Retrieve triples matching DATE object.",
                stmts(new Stmt(alice, birthdate, date)),
                statementSet(conn.getStatements(null, null, date, false)));
        assertSetsEqual("Match triples having a specific DATE value.",
                stmts(new Stmt(alice, birthdate, date)),
                statementSet(conn.getStatements(null, null,
                        f.createLiteral("1984-12-06", XMLSchema.DATE), false)));
        assertSetsEqual("Retrieve triples matching DATETIME object.",
                stmts(new Stmt(ted, birthdate, time)),
                statementSet(conn.getStatements(null, null, time, false)));
        assertSetsEqual("Match triples having a specific DATETIME value.",
                stmts(new Stmt(ted, birthdate, time)),
                statementSet(conn.getStatements(null, null,
                        f.createLiteral("1984-12-06T09:00:00Z", XMLSchema.DATETIME), false)));
    }

    @Test
    public void example6() throws Exception {
        IRI context = example6_setup(conn, repo);
        assertEquals(16, conn.size(context));
        assertEquals(1214, conn.size((Resource) null));
        assertEquals(1230, conn.size());
    }

    @Test
    public void example7() throws Exception {
        example6();
        assertEquals(1230, statementSet(conn.getStatements(null, null, null, false)).size());
        assertEquals(8, statementSet(
                conn.prepareTupleQuery(
                        QueryLanguage.SPARQL,
                        "SELECT DISTINCT ?s ?g WHERE {graph ?g {?s ?p ?o .} }"
                ).evaluate()).size());
    }

    @Test
    public void example8() throws Exception {
        cat.deleteRepository("example8");
        repo = cat.createRepository("example8");
        closeLater(() -> cat.deleteRepository("example8"));
        closeLater(repo);
        repo.initialize();
        conn = getConnection(repo);
        vf = repo.getValueFactory();
        example6();
        IRI context = repo.getValueFactory().createIRI("http://example.org#vcards");
        {
            File outputFile = File.createTempFile(getClass().getSimpleName(), ".nt");
            println("Writing n-triples to: " + outputFile.getCanonicalPath());
            try (OutputStream out = new FileOutputStream(outputFile)) {
                NTriplesWriter ntriplesWriter = new NTriplesWriter(out);
                conn.export(ntriplesWriter, context);
            }
            assertFiles(Util.resourceAsTempFile("/test/tutorial-test8-expected.nt"), outputFile);
            outputFile.delete(); // delete if success
        }
        {
            File outputFile = File.createTempFile(getClass().getSimpleName(), ".rdf");
            println("Writing RDF to: " + outputFile);
            try (OutputStream out = new FileOutputStream(outputFile)) {
                RDFXMLWriter rdfxmlfWriter = new RDFXMLWriter(out);
                conn.export(rdfxmlfWriter, context);
                out.write('\n');
            }
            // Sesame 2.9 started adding a datatype declaration to strings
            // Since this test is likely going to break after every single
            // upgrade, I've disabled it. We need a more robust method of
            // checking equivalence.
            // assertFiles(Util.resourceAsTempFile("/test/tutorial-test8-expected.rdf"), outputFile);
            outputFile.delete(); // delete if success
        }
    }

    /**
     * Writing the result of a statements match to a file.
     */
    @Test
    public void example9() throws Exception {
        example6();
        File f = File.createTempFile(getClass().getSimpleName(), ".rdf");
        try (FileWriter out = new FileWriter(f)) {
            println("export to " + f.getCanonicalFile());
            conn.exportStatements(null, RDF.TYPE, null, false, new RDFXMLWriter(out));
        }
        assertRDFFilesIsomorphic(
                Util.resourceAsTempFile("/test/tutorial-test9-expected.rdf"),
                f,
                RDFFormat.RDFXML);
        f.delete(); // delete if success
    }

    /**
     * Datasets and multiple contexts.
     */
    @Test
    public void example10() throws Exception {
        ValueFactory f = repo.getValueFactory();
        String exns = "http://example.org/people/";
        IRI alice = f.createIRI(exns, "alice");
        IRI bob = f.createIRI(exns, "bob");
        IRI ted = f.createIRI(exns, "ted");
        IRI person = f.createIRI("http://example.org/ontology/Person");
        IRI name = f.createIRI("http://example.org/ontology/name");
        Literal alicesName = f.createLiteral("Alice");
        Literal bobsName = f.createLiteral("Bob");
        Literal tedsName = f.createLiteral("Ted");
        IRI context1 = f.createIRI(exns, "cxt1");
        IRI context2 = f.createIRI(exns, "cxt2");
        conn.add(alice, RDF.TYPE, person, context1);
        conn.add(alice, name, alicesName, context1);
        conn.add(bob, RDF.TYPE, person, context2);
        conn.add(bob, name, bobsName, context2);
        conn.add(ted, RDF.TYPE, person);
        conn.add(ted, name, tedsName);

        assertSetsEqual(
                stmts(new Stmt[] {
                        new Stmt(alice, RDF.TYPE, person, context1),
                        new Stmt(alice, name, alicesName, context1),
                        new Stmt(bob, RDF.TYPE, person, context2),
                        new Stmt(bob, name, bobsName, context2),
                        new Stmt(ted, RDF.TYPE, person),
                        new Stmt(ted, name, tedsName)
                }),
                statementSet(conn.getStatements(null, null, null, false)));
        assertSetsEqual(
                stmts(new Stmt[] {
                        new Stmt(alice, RDF.TYPE, person, context1),
                        new Stmt(alice, name, alicesName, context1),
                        new Stmt(bob, RDF.TYPE, person, context2),
                        new Stmt(bob, name, bobsName, context2)
                }),
                statementSet(conn.getStatements(null, null, null, false, context1, context2)));
        assertSetsEqual(
                stmts(new Stmt[] {
                        new Stmt(bob, RDF.TYPE, person, context2),
                        new Stmt(bob, name, bobsName, context2),
                        new Stmt(ted, RDF.TYPE, person),
                        new Stmt(ted, name, tedsName)
                }),
                statementSet(conn.getStatements(null, null, null, false, null, context2)));

        // testing named graph query
        DatasetImpl ds = new DatasetImpl();
        ds.addNamedGraph(context1);
        ds.addNamedGraph(context2);
        AGTupleQuery tupleQuery = conn.prepareTupleQuery(
                QueryLanguage.SPARQL, "SELECT ?s ?p ?o ?g WHERE { GRAPH ?g {?s ?p ?o . } }");
        tupleQuery.setDataset(ds);
    /* rfe10117 and rfe10294
        String analysis = tupleQuery.analyze();
        assertTrue(analysis.contains("desired") && analysis.contains("actual"));
    */
        assertSetsEqual(
                stmts(new Stmt[] {
                        new Stmt(alice, RDF.TYPE, person, context1),
                        new Stmt(alice, name, alicesName, context1),
                        new Stmt(bob, RDF.TYPE, person, context2),
                        new Stmt(bob, name, bobsName, context2)
                }),
                statementSet(tupleQuery.evaluate()));

        ds = new DatasetImpl();
        ds.addDefaultGraph(null);
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?s ?p ?o WHERE {?s ?p ?o . }");
        tupleQuery.setDataset(ds);
        assertSetsEqual(
                stmts(new Stmt[] {
                        new Stmt(ted, RDF.TYPE, person),
                        new Stmt(ted, name, tedsName)
                }),
                statementSet(tupleQuery.evaluate()));
    }

    @Test
    public void example11() throws Exception {
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        IRI alice = vf.createIRI(exns, "alice");
        IRI person = vf.createIRI(exns, "Person");
        conn.add(alice, RDF.TYPE, person);
        String queryString = "PREFIX ex:" + "<" + exns + ">\n" +
                "SELECT ?s ?p ?o WHERE { ?s ?p ?o . FILTER ((?p = rdf:type) && (?o = ex:Person) ) }";
        assertSetsEqual(stmts(new Stmt[] {new Stmt(alice, RDF.TYPE, person)}),
                statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate()));
    }

    /**
     * Text search
     */
    @Test
    public void example12() throws Exception {
        ValueFactory f = conn.getValueFactory();
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        IRI fullname = f.createIRI(exns, "fullname");
        config.getPredicates().add(fullname);
        conn.createFreetextIndex("index1", config);
        List<String> indices = new ArrayList<String>();
        indices.add("index1");
        assertSetsEqual("listFreetextIndices() expects index1", indices, conn.listFreetextIndices());
        List<IRI> preds = new ArrayList<IRI>();
        preds.add(fullname);
        AGFreetextIndexConfig config1 = conn.getFreetextIndexConfig("index1");
        assertSetsEqual("getPredicates() expects fullname", preds, config1.getPredicates());
        IRI alice = f.createIRI(exns, "alice1");
        IRI carroll = f.createIRI(exns, "carroll");
        IRI person = f.createIRI(exns, "Person");
        Literal alicename = f.createLiteral("Alice B. Toklas");
        Literal lewisCarroll = f.createLiteral("Lewis Carroll");
        IRI book = f.createIRI(exns, "book1");
        IRI booktype = f.createIRI(exns, "Book");
        IRI booktitle = f.createIRI(exns, "title");
        IRI author = f.createIRI(exns, "author");
        Literal wonderland = f.createLiteral("Alice in Wonderland");
        conn.clear();
        conn.add(alice, RDF.TYPE, person);
        conn.add(alice, fullname, alicename);
        conn.add(book, RDF.TYPE, booktype);
        conn.add(book, booktitle, wonderland);
        conn.add(book, author, carroll);
        conn.add(carroll, RDF.TYPE, person);
        conn.add(carroll, fullname, lewisCarroll);

        String queryString =
                "SELECT ?s ?p ?o " +
                        "WHERE { ?s ?p ?o . ?s fti:match 'Alice' . }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        Set<Stmt> stmts = stmts(new Stmt[] {
                new Stmt(alice, fullname, alicename),
                new Stmt(alice, RDF.TYPE, person)});
        assertSetsEqual("Whole-word match for 'Alice'.", stmts,
                statementSet(tupleQuery.evaluate()));

        AGFreetextQuery query = new AGFreetextQuery(conn);
        query.setPattern("Alice");
        query.setIndex("index1");
        stmts = stmts(new Stmt[] {
                new Stmt(alice, fullname, alicename)});
        assertSetsEqual("Whole-word match for 'Alice' another way.", stmts,
                statementSet(query.evaluate()));

        queryString =
                "SELECT ?s ?p ?o " +
                        "WHERE { ?s ?p ?o . ?s fti:match 'Ali*' . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        assertSetsEqual("Wildcard match for 'Ali*'.", stmts(new Stmt[] {
                        new Stmt(alice, fullname, alicename),
                        new Stmt(alice, RDF.TYPE, person)}),
                statementSet(tupleQuery.evaluate()));

        queryString =
                "SELECT ?s ?p ?o " +
                        "WHERE { ?s ?p ?o . ?s fti:match '?l?c?' . }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        assertSetsEqual("Wildcard match for '?l?ce?.", stmts(new Stmt[] {
                        new Stmt(alice, fullname, alicename),
                        new Stmt(alice, RDF.TYPE, person)}),
                statementSet(tupleQuery.evaluate()));

        queryString =
                "SELECT ?s ?p ?o " +
                        "WHERE { ?s ?p ?o . FILTER regex(?o, \"lic\") }";
        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        assertSetsEqual("Substring match for 'lic'.",
                stmts(new Stmt[] {
                        new Stmt(alice, fullname, alicename),
                        new Stmt(book, booktitle, wonderland)}),
                statementSet(tupleQuery.evaluate()));

        // Create index2, for searching short names in URI's
        // that are objects of the author predicate
        config = AGFreetextIndexConfig.newInstance();
        config.getPredicates().add(author);
        config.setIndexResources("short");
        conn.createFreetextIndex("index2", config);
        indices.add("index2");
        assertSetsEqual("listFreetextIndices() expects index1 and index2", indices, conn.listFreetextIndices());
        preds = new ArrayList<IRI>();
        preds.add(author);
        assertSetsEqual("getPredicates() expects author", preds, config.getPredicates());
        assertEquals("getIndexResources() expects short", "short", config.getIndexResources());

        //Search for Carroll in index2.
        query = new AGFreetextQuery(conn);
        query.setPattern("Carroll");
        query.setIndex("index2");
        stmts = stmts(new Stmt[] {
                new Stmt(book, author, carroll)});
        assertSetsEqual("Search for Carroll on index2", stmts, statementSet(query.evaluate()));

        conn.deleteFreetextIndex("index1");
        indices.remove("index1");
        assertSetsEqual("listFreetextIndices() expects index2", indices, conn.listFreetextIndices());
    }

    /**
     * Ask, Construct, and Describe queries
     */
    @Test
    public void example13() throws Exception {
        Map<String, Stmt> inputs = example2setup();
        conn.setNamespace("ex", "http://example.org/people/");
        conn.setNamespace("ont", "http://example.org/ontology/");
        String prefix = "PREFIX ont: " + "<http://example.org/ontology/>\n";

        String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        AGTupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    /* rfe10117 and rfe10294
        String analysis = tupleQuery.analyze();
        assertTrue(analysis.contains("desired") && analysis.contains("actual"));
    */
        assertSetsEqual("SELECT result:", inputs.values(),
                statementSet(tupleQuery.evaluate()));

        AGBooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
                prefix + "ask { ?s ont:name \"Alice\" } ");
        //TODO: add when this is supported
        //analysis = booleanQuery.analyze();
        //assertTrue(analysis.contains("desired") && analysis.contains("actual"));
        assertTrue("Boolean result", booleanQuery.evaluate());
        assertFalse("Boolean result",
                conn.prepareBooleanQuery(QueryLanguage.SPARQL,
                        prefix + "ask { ?s ont:name \"NOT Alice\" } ").evaluate());

        queryString = "construct {?s ?p ?o} where { ?s ?p ?o . filter (?o = \"Alice\") } ";
        AGGraphQuery constructQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        //TODO: add when this is supported
        //analysis = constructQuery.analyze();
        //assertTrue(analysis.contains("desired") && analysis.contains("actual"));
        assertSetsEqual("Construct result",
                mapKeep(new String[] {"an"}, inputs).values(),
                statementSet(constructQuery.evaluate()));

        queryString = "describe ?s where { ?s ?p ?o . filter (?o = \"Alice\") } ";
        AGGraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        //TODO: add when this is supported
        //analysis = describeQuery.analyze();
        //assertTrue(analysis.contains("desired") && analysis.contains("actual"));
        assertSetsEqual("Describe result",
                mapKeep(new String[] {"an", "at"}, inputs).values(),
                statementSet(describeQuery.evaluate()));
    }

    /**
     * Parametric Queries
     */
    @Test
    public void example14() throws Exception {
        Map<String, Stmt> inputs = example2setup();
        ValueFactory f = conn.getValueFactory();
        conn.setAutoCommit(false);
        IRI alice = f.createIRI("http://example.org/people/alice");
        IRI bob = f.createIRI("http://example.org/people/bob");
        String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setBinding("s", alice);
        assertSetsEqual("Facts about Alice",
                mapKeep(new String[] {"an", "at"}, inputs).values(),
                statementSet(tupleQuery.evaluate()));

        tupleQuery.setBinding("s", bob);
        assertSetsEqual("Facts about Bob",
                mapKeep(new String[] {"bn", "bt"}, inputs).values(),
                statementSet(tupleQuery.evaluate()));
    }

    /**
     * Range matches
     */
    @Test
    public void example15() throws Exception {
        ValueFactory f = conn.getValueFactory();
        conn.clear();
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        IRI alice = f.createIRI(exns, "alice");
        IRI bob = f.createIRI(exns, "bob");
        IRI carol = f.createIRI(exns, "carol");
        IRI age = f.createIRI(exns, "age");
        Literal l42 = f.createLiteral(42);
        conn.add(alice, age, l42);
        Literal l451 = f.createLiteral(45.1);
        conn.add(bob, age, l451);
        Literal l39 = f.createLiteral("39");
        conn.add(carol, age, l39);

        assertSetsEqual("Range query for integers and floats.",
                stmts(new Stmt(bob, age, l451),
                        new Stmt(alice, age, l42)),
                statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL,
                        "SELECT ?s ?p ?o  " +
                                "WHERE { ?s ?p ?o . " +
                                "FILTER ((?o >= 30) && (?o <= 50)) }").evaluate()));

        assertSetsEqual("Range query for integers, floats, and integers in strings.",
                stmts(new Stmt(bob, age, l451),
                        new Stmt(alice, age, l42),
                        new Stmt(carol, age, l39)),
                statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL,
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "SELECT ?s ?p ?o  " +
                                "WHERE { ?s ?p ?o . " +
                                "FILTER ((xsd:integer(?o) >= 30) && (xsd:integer(?o) <= 50)) }").evaluate()));
    }

    /**
     * Federated triple stores.
     */
    @Test
    public void example16() throws Exception {
        example6();
        // create two ordinary stores, and one federated store:
        AGRepository redRepo = cat.createRepository("redthingsjv-ex16");
        closeLater(() -> cat.deleteRepository("redthingsjv-ex16"));
        closeLater(redRepo);
        redRepo.initialize();
        AGRepositoryConnection redConn = getConnection(redRepo);
        redConn.clear();
        ValueFactory rf = redConn.getValueFactory();
        AGRepository greenRepo = cat.createRepository("greenthingsjv-ex16");
        closeLater(() -> cat.deleteRepository("greenthingsjv-ex16"));
        closeLater(greenRepo);
        greenRepo.initialize();
        AGRepositoryConnection greenConn = getConnection(greenRepo);
        greenConn.clear();
        ValueFactory gf = greenConn.getValueFactory();
        AGServer server = cat.getServer();
        AGAbstractRepository rainbowRepo = server.federate(redRepo, greenRepo);
        closeLater(rainbowRepo);
        rainbowRepo.initialize();
        assertFalse("Federation is writable?", rainbowRepo.isWritable());
        AGRepositoryConnection rainbowConn = getConnection(rainbowRepo);
        String ex = "http://example.org/";
        // add a few triples to the red and green stores:
        IRI mac = rf.createIRI(ex + "mcintosh");
        redConn.add(mac, RDF.TYPE, rf.createIRI(ex + "Apple"));
        IRI red = rf.createIRI(ex + "reddelicious");
        redConn.add(red, RDF.TYPE, rf.createIRI(ex + "Apple"));
        IRI pippen = gf.createIRI(ex + "pippin");
        greenConn.add(pippen, RDF.TYPE, gf.createIRI(ex + "Apple"));
        IRI kermit = gf.createIRI(ex + "kermitthefrog");
        greenConn.add(kermit, RDF.TYPE, gf.createIRI(ex + "Frog"));
        redConn.setNamespace("ex", ex);
        greenConn.setNamespace("ex", ex);
        rainbowConn.setNamespace("ex", ex);
        String queryString = "select ?s where { ?s rdf:type ex:Apple }";
        // query each of the stores; observe that the federated one is the union of the other two:
        assertSetsEqual("Red",
                stmts(new Stmt(mac, null, null),
                        new Stmt(red, null, null)),
                statementSet(redConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate()));
        assertSetsEqual("Green",
                stmts(new Stmt(pippen, null, null)),
                statementSet(greenConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate()));
        assertSetsEqual("Federated",
                stmts(new Stmt(red, null, null),
                        new Stmt(mac, null, null),
                        new Stmt(pippen, null, null)),
                statementSet(rainbowConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate()));
    }

    /**
     * Prolog queries
     */
    @Test
    public void example17() throws Exception {
        example6();
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        String rules1 =
                "(<-- (woman ?person) ;; IF\n" +
                        "     (q ?person !kdy:sex !kdy:female)\n" +
                        "     (q ?person !rdf:type !kdy:person))\n" +
                        "(<-- (man ?person) ;; IF\n" +
                        "     (q ?person !kdy:sex !kdy:male)\n" +
                        "     (q ?person !rdf:type !kdy:person))";
        conn.addRules(rules1);
        // First and Last names of all men
        String queryString =
                "(select (?first ?last)\n" +
                        "        (man ?person)\n" +
                        "        (q ?person !kdy:first-name ?first)\n" +
                        "        (q ?person !kdy:last-name ?last))";
        AGTupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        //TODO: add when this is supported
        //String analysis = tupleQuery.analyze();
        //assertTrue(analysis.contains("desired") && analysis.contains("actual"));
        assertEquals(38, statementSet(tupleQuery.evaluate()).size());
    }

    /**
     * Loading Prolog rules
     */
    @Test
    public void example18() throws Exception {
        example6();
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        conn.setNamespace("rltv", "http://www.franz.com/simple#");
        conn.addRules(Util.resourceAsStream("/tutorial/java-rules.txt"));
        String queryString =
                "(select (?ufirst ?ulast ?cfirst ?clast)" +
                        "(uncle ?uncle ?child)" +
                        "(name ?uncle ?ufirst ?ulast)" +
                        "(name ?child ?cfirst ?clast))";
        TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        assertEquals(52, statementSet(tupleQuery.evaluate()).size());
    }

    /**
     * RDFS++ Reasoning
     */
    @Test
    public void example19() throws Exception {
        // Examples of RDFS++ inference.  Was originally example 2A.
        ValueFactory f = conn.getValueFactory();
        IRI robert = f.createIRI("http://example.org/people/robert");
        IRI roberta = f.createIRI("http://example.org/people/roberta");
        IRI bob = f.createIRI("http://example.org/people/bob");
        IRI bobby = f.createIRI("http://example.org/people/bobby");
        // create name and child predicates, and Person class.
        IRI name = f.createIRI("http://example.org/ontology/name");
        IRI fatherOf = f.createIRI("http://example.org/ontology/fatherOf");
        IRI person = f.createIRI("http://example.org/ontology/Person");
        // create literal values for names    
        Literal bobsName = f.createLiteral("Bob");
        Literal bobbysName = f.createLiteral("Bobby");
        Literal robertsName = f.createLiteral("Robert");
        Literal robertasName = f.createLiteral("Roberta");
        // Bob is the same person as Robert
        conn.add(bob, OWL.SAMEAS, robert);
        // Robert, Bob, and children are people
        conn.add(robert, RDF.TYPE, person);
        conn.add(roberta, RDF.TYPE, person);
        conn.add(bob, RDF.TYPE, person);
        conn.add(bobby, RDF.TYPE, person);
        // They all have names.
        conn.add(robert, name, robertsName);
        conn.add(roberta, name, robertasName);
        conn.add(bob, name, bobsName);
        conn.add(bobby, name, bobbysName);
        // robert has a child
        conn.add(robert, fatherOf, roberta);
        // bob has a child
        conn.add(bob, fatherOf, bobby);

        // List the children of Robert, with inference OFF.
        assertSetsEqual("Children of Robert, inference OFF",
                stmts(new Stmt(robert, fatherOf, roberta)),
                statementSet(conn.getStatements(robert, fatherOf, null, false)));
        // List the children of Robert with inference ON. The owl:sameAs
        // link combines the children of Bob with those of Robert.
        assertSetsEqual("Children of Robert, inference ON",
                stmts(new Stmt(robert, fatherOf, roberta),
                        new Stmt(robert, fatherOf, bobby)),
                statementSet(conn.getStatements(robert, fatherOf, null, true)));

        // Remove the owl:sameAs link so we can try the next example.
        conn.remove(bob, OWL.SAMEAS, robert);

        // Define new predicate, hasFather, as the inverse of fatherOf.
        IRI hasFather = f.createIRI("http://example.org/ontology/hasFather");
        conn.add(hasFather, OWL.INVERSEOF, fatherOf);
        // Search for people who have fathers, even though there are no hasFather triples.
        // With inference OFF.
        assertSetsEqual("People with fathers, inference OFF",
                stmts(),
                statementSet(conn.getStatements(null, hasFather, null, false)));

        // With inference ON. The owl:inverseOf link allows AllegroGraph to
        // deduce the inverse links.
        assertSetsEqual("People with fathers, inference ON",
                stmts(new Stmt(roberta, hasFather, robert),
                        new Stmt(bobby, hasFather, bob)),
                statementSet(conn.getStatements(null, hasFather, null, true)));

        // Remove owl:inverseOf property.
        conn.remove(hasFather, OWL.INVERSEOF, fatherOf);

        IRI parentOf = f.createIRI("http://example.org/ontology/parentOf");
        conn.add(fatherOf, RDFS.SUBPROPERTYOF, parentOf);
        // Now search for inferred parentOf links.
        // Search for parentOf links, even though there are no parentOf triples.
        // With inference OFF.
        assertSetsEqual("People with parents, inference OFF",
                stmts(),
                statementSet(conn.getStatements(null, parentOf, null, false)));
        // With inference ON. The rdfs:subpropertyOf link allows AllegroGraph to 
        // deduce that fatherOf links imply parentOf links.
        assertSetsEqual("People with parents, inference ON",
                stmts(new Stmt(robert, parentOf, roberta),
                        new Stmt(bob, parentOf, bobby)),
                statementSet(conn.getStatements(null, parentOf, null, true)));

        conn.remove(fatherOf, RDFS.SUBPROPERTYOF, parentOf);

        // The next example shows rdfs:range and rdfs:domain in action.
        // We'll create two new rdf:type classes.  Note that classes are capitalized.
        IRI parent = f.createIRI("http://example.org/ontology/Parent");
        IRI child = f.createIRI("http://exmaple.org/ontology/Child");
        // The following triples say that a fatherOf link points from a parent to a child.
        conn.add(fatherOf, RDFS.DOMAIN, parent);
        conn.add(fatherOf, RDFS.RANGE, child);

        // Now we can search for rdf:type parent.
        assertSetsEqual("Who are the parents?  Inference ON.",
                stmts(new Stmt(robert, RDF.TYPE, parent),
                        new Stmt(bob, RDF.TYPE, parent)),
                statementSet(conn.getStatements(null, RDF.TYPE, parent, true)));
        // And we can search for rdf:type child.
        assertSetsEqual("Who are the children?  Inference ON.",
                stmts(new Stmt(roberta, RDF.TYPE, child),
                        new Stmt(bobby, RDF.TYPE, child)),
                statementSet(conn.getStatements(null, RDF.TYPE, child, true)));
    }

    /**
     * Geospatial Reasoning
     */
    @Test
    public void example20() throws Exception {
        AGValueFactory vf = conn.getValueFactory();
        conn.clear();
        String exns = "http://example.org/people/";
        conn.setNamespace("ex", exns);
        IRI alice = vf.createIRI(exns, "alice");
        IRI bob = vf.createIRI(exns, "bob");
        IRI carol = vf.createIRI(exns, "carol");
        IRI cartSystem = conn.registerCartesianType(10, 0, 100, 0, 100);
        IRI location = vf.createIRI(exns, "location");
        Literal alice_loc = vf.createLiteral("+30.0+30.0", cartSystem);
        Literal bob_loc = vf.createLiteral("+40.0+40.0", cartSystem);
        Literal carol_loc = vf.createLiteral("+50.0+50.0", cartSystem);
        conn.add(alice, location, alice_loc);
        conn.add(bob, location, bob_loc);
        conn.add(carol, location, carol_loc);
        assertSetsEqual("Find people located within box1.",
                stmts(new Stmt(alice, location, null),
                        new Stmt(bob, location, null)),
                stmtsSP(statementSet(conn.getStatementsInBox(cartSystem, location, 20, 40, 20, 40, 0, false))));
        //printRows( conn.getStatementsInBox(cartSystem, location, 20, 40, 20, 40, 0, false) );
        assertSetsEqual("Find people located within circle1.",
                stmts(new Stmt(alice, location, null),
                        new Stmt(bob, location, null)),
                stmtsSP(statementSet(conn.getStatementsInCircle(cartSystem, location, 35, 35, 10, 0, false))));
        //printRows( conn.getStatementsInCircle(cartSystem, location, 35, 35, 10, 0, false) ); 
        IRI polygon1 = vf.createIRI("http://example.org/polygon1");
        List<Literal> polygon1_points = new ArrayList<Literal>(4);
        polygon1_points.add(vf.createLiteral("+10.0+40.0", cartSystem));
        polygon1_points.add(vf.createLiteral("+50.0+10.0", cartSystem));
        polygon1_points.add(vf.createLiteral("+35.0+40.0", cartSystem));
        polygon1_points.add(vf.createLiteral("+50.0+70.0", cartSystem));
        //println(polygon1_points);
        conn.registerPolygon(polygon1, polygon1_points);
        assertSetsEqual("Find people located within ploygon1.",
                stmts(new Stmt(alice, location, null)),
                stmtsSP(statementSet(conn.getStatementsInPolygon(cartSystem, location, polygon1, 0, false))));
        //printRows( conn.getStatementsInPolygon(cartSystem, location, polygon1, 0, false) );

        // now we switch to a Spherical (Lat/Long) coordinate system
        //URI sphericalSystemKM = conn.registerSphericalType(5, AGProtocol.KM_PARAM_VALUE);
        //URI sphericalSystemDegree = conn.registerSphericalType(5, AGProtocol.DEGREE_PARAM_VALUE);
        IRI sphericalSystemDegree = conn.registerSphericalType(5, "degree");
        IRI amsterdam = vf.createIRI(exns, "amsterdam");
        IRI london = vf.createIRI(exns, "london");
        IRI sanfrancisco = vf.createIRI(exns, "sanfrancisco");
        IRI salvador = vf.createIRI(exns, "salvador");
        location = vf.createIRI(exns, "geolocation");
        conn.add(amsterdam, location, vf.createLiteral("+52.366665+004.883333", sphericalSystemDegree));
        conn.add(london, location, vf.createLiteral("+51.533333-000.08333333", sphericalSystemDegree));
        conn.add(sanfrancisco, location, vf.createLiteral("+37.783333-122.433334", sphericalSystemDegree));
        conn.add(salvador, location, vf.createLiteral("+13.783333-088.45", sphericalSystemDegree));
        assertSetsEqual("Locate entities within box2.",
                stmts(new Stmt(sanfrancisco, location, null)),
                stmtsSP(statementSet(conn.getStatementsInBox(sphericalSystemDegree, location, -130.0f, -70.0f, 25.0f, 50.0f, 0, false))));
        //printRows(conn.getStatementsInBox(sphericalSystemDegree, location, -130.0f, -70.0f, 25.0f, 50.0f, 0, false) );
        assertSetsEqual("Locate entities within haversine circle.",
                stmts(new Stmt(salvador, location, null)),
                stmtsSP(statementSet(conn.getGeoHaversine(sphericalSystemDegree, location, 19.3994f, -99.08f, 2000.0f, "km", 0, false))));
        //printRows(conn.getGeoHaversine(sphericalSystemDegree, location, 19.3994f, -99.08f, 2000.0f, "km", 0, false) );
        IRI polygon2 = vf.createIRI("http://example.org/polygon2");
        List<Literal> polygon2_points = new ArrayList<Literal>(3);
        polygon2_points.add(vf.createLiteral("+51.0+002.0", sphericalSystemDegree));
        polygon2_points.add(vf.createLiteral("+60.0-005.0", sphericalSystemDegree));
        polygon2_points.add(vf.createLiteral("+48.0-012.5", sphericalSystemDegree));
        //println(polygon2_points);
        conn.registerPolygon(polygon2, polygon2_points);
        assertSetsEqual("Locate entities within polygon2.",
                stmts(new Stmt(london, location, null)),
                stmtsSP(statementSet(conn.getStatementsInPolygon(sphericalSystemDegree, location, polygon2, 0, false))));
        //printRows( conn.getStatementsInPolygon(sphericalSystemDegree, location, polygon2, 0, false) );
    }

    void assert21(String msg, Collection expected, String prolog, String... bindings) throws Exception {
        assertSetsEqual(msg, expected,
                statementSet(conn.prepareTupleQuery(AGQueryLanguage.PROLOG, prolog).evaluate(), bindings));
    }

    /**
     * Social Network Analysis
     */
    @Category(TestSuites.Broken.class)
    @Test
    public void example21() throws Exception {
        AGValueFactory vf = repo.getValueFactory();
        Util.add(conn, "/tutorial/lesmis.rdf", null, RDFFormat.RDFXML);
        assertEquals("Loaded lesmis.rdf triples.", 916, conn.size());

        // Create URIs for relationship predicates.
        String lmns = "http://www.franz.com/lesmis#";
        conn.setNamespace("lm", lmns);
        String dc = "http://purl.org/dc/elements/1.1/";
        conn.setNamespace("dc", dc);
        IRI knows = vf.createIRI(lmns, "knows");
        IRI barelyKnows = vf.createIRI(lmns, "barely_knows");
        IRI knowsWell = vf.createIRI(lmns, "knows_well");

        // Create URIs for some characters.
        IRI valjean = vf.createIRI(lmns, "character11");
        //URI bossuet = vf.createIRI(lmns, "character64");

        // Create some generators
        //print "\nSNA generators known (should be none): '%s'" % (conn.listSNAGenerators())
        List<IRI> intimates = new ArrayList<IRI>(1);
        Collections.addAll(intimates, knowsWell);
        conn.registerSNAGenerator("intimates", null, null, intimates, null);
        List<IRI> associates = new ArrayList<IRI>(2);
        Collections.addAll(associates, knowsWell, knows);
        conn.registerSNAGenerator("associates", null, null, associates, null);
        List<IRI> everyone = new ArrayList<IRI>(3);
        Collections.addAll(everyone, knowsWell, knows, barelyKnows);
        conn.registerSNAGenerator("everyone", null, null, everyone, null);

        // Create neighbor matrix.
        List<IRI> startNodes = new ArrayList<IRI>(1);
        startNodes.add(valjean);
        conn.registerSNANeighborMatrix("matrix1", "intimates", startNodes, 2);
        conn.registerSNANeighborMatrix("matrix2", "associates", startNodes, 5);
        conn.registerSNANeighborMatrix("matrix3", "everyone", startNodes, 2);

        Set<Stmt> valjeansEgoGroup = stmts(
                new Stmt(vf.createIRI(lmns, "character11"), null, vf.createLiteral("Valjean")),
                new Stmt(vf.createIRI(lmns, "character23"), null, vf.createLiteral("Fantine")),
                new Stmt(vf.createIRI(lmns, "character24"), null, vf.createLiteral("MmeThenardier")),
                new Stmt(vf.createIRI(lmns, "character25"), null, vf.createLiteral("Thenardier")),
                new Stmt(vf.createIRI(lmns, "character26"), null, vf.createLiteral("Cosette")),
                new Stmt(vf.createIRI(lmns, "character27"), null, vf.createLiteral("Javert")),
                new Stmt(vf.createIRI(lmns, "character28"), null, vf.createLiteral("Fauchelevent")),
                new Stmt(vf.createIRI(lmns, "character55"), null, vf.createLiteral("Marius")));

        // Explore Valjean's ego group.
        assert21("Valjean's ego group members (using associates).",
                valjeansEgoGroup,
                "(select (?member ?name)" +
                        "(ego-group-member !lm:character11 1 associates ?member)" +
                        "(q ?member !dc:title ?name))",
                "member", null, "name");

        // Valjean's ego group using neighbor matrix.
        assert21("Valjean's ego group (using associates matrix).",
                valjeansEgoGroup,
                "(select (?member ?name)" +
                        "(ego-group-member !lm:character11 1 matrix2 ?member)" +
                        "(q ?member !dc:title ?name))",
                "member", null, "name");

        assert21("Valjean's ego group in one list depth 1 (using associates).",
                null,
                "(select (?group)" +
                        "(ego-group !lm:character11 1 associates ?group))",
                "group");

        assert21("Valjean's ego group in one list depth 2 (using associates).",
                null,
                "(select (?group)" +
                        "(ego-group !lm:character11 2 associates ?group))",
                "group");

        assert21("Valjean's ego group in one list depth 3 (using associates).",
                null,
                "(select (?group)" +
                        "(ego-group !lm:character11 3 associates ?group))",
                "group");

        assert21("Shortest breadth-first path connecting Valjean to Bossuet using intimates. (Should be no path.)",
                null,
                "(select (?path)" +
                        "(breadth-first-search-paths !lm:character11 !lm:character64 intimates 10 ?path))",
                "path");

        assert21("Shortest breadth-first path connecting Valjean to Bossuet using associates.",
                null,
                "(select (?path)" +
                        "(breadth-first-search-paths !lm:character11 !lm:character64 associates 10 ?path))",
                "path");

        assert21("Shortest breadth-first path connecting Valjean to Bossuet using everyone.",
                null,
                "(select (?path)" +
                        "(breadth-first-search-paths !lm:character11 !lm:character64 everyone 10 ?path))",
                "path");

        assert21("Shortest breadth-first path connecting Valjean to Bossuet using associates (should be two).",
                null,
                "(select (?path)" +
                        "(breadth-first-search-paths !lm:character11 !lm:character64 associates ?path))",
                "path");

        // Note that depth-first-search-paths may return more than one path of different lengths.  
        // None of them are guaranteed to be "the shortest path."
        assert21("Return depth-first path connecting Valjean to Bossuet using associates (should be one).",
                null,
                "(select (?path)" +
                        "(depth-first-search-path !lm:character11 !lm:character64 associates 10 ?path))",
                "path");

        assert21("Shortest bidirectional path connecting Valjean to Bossuet using associates (should be two).",
                null,
                "(select (?path)" +
                        "(bidirectional-search-paths !lm:character11 !lm:character64 associates ?path))",
                "path");

        assert21("Nodal degree of Valjean (should be seven).",
                null,
                "(select (?degree)" +
                        "(nodal-degree !lm:character11 associates ?degree))",
                "degree");

        assert21("How many neighbors are around Valjean? (should be 36).",
                null,
                "(select (?neighbors)" +
                        "(nodal-degree !lm:character11 everyone ?neighbors))",
                "neighbors");

        assert21("Who are Valjean's neighbors? (using everyone).",
                null,
                "(select (?name)" +
                        "(nodal-neighbors !lm:character11 everyone ?member)" +
                        "(q ?member !dc:title ?name))",
                "name");

        assert21("Graph density of Valjean's ego group? (using associates).",
                null,
                "(select (?density)" +
                        "(ego-group !lm:character11 1 associates ?group)" +
                        "(graph-density ?group associates ?density))",
                "density");

        assert21("Valjean's cliques? Should be two (using associates).",
                null,
                "(select (?clique)" +
                        "(clique !lm:character11 associates ?clique))",
                "clique");

        assert21("Valjean's actor-degree-centrality to his ego group at depth 1 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 1 associates ?group)" +
                        "(actor-degree-centrality !lm:character11 ?group associates ?centrality))",
                "centrality");

        assert21("Valjean's actor-degree-centrality to his ego group at depth 2 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 2 associates ?group)" +
                        "(actor-degree-centrality !lm:character11 ?group associates ?centrality))",
                "centrality");

        assert21("Valjean's actor-closeness-centrality to his ego group at depth 1 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 1 associates ?group)" +
                        "(actor-closeness-centrality !lm:character11 ?group associates ?centrality))",
                "centrality");

        assert21("Valjean's actor-closeness-centrality to his ego group at depth 2 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 2 associates ?group)" +
                        "(actor-closeness-centrality !lm:character11 ?group associates ?centrality))",
                "centrality");

        assert21("\nValjean's actor-betweenness-centrality to his ego group at depth 2 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 2 associates ?group)" +
                        "(actor-betweenness-centrality !lm:character11 ?group associates ?centrality))",
                "centrality");

        //  "Group centrality measures the cohesion a group relative to
        //  some measure of actor-centrality. `group-degree-centrality measures
        //  group cohesion by finding the maximum actor centrality in the group,
        //  summing the difference between this and each other actor's degree
        //  centrality and then normalizing. It ranges from 0 (when all actors have
        //  equal degree) to 1 (when one actor is connected to every other and no
        //  other actors have connections."
        assert21("Group-degree-centrality of Valjean's ego group at depth 1 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 1 associates ?group)" +
                        "(group-degree-centrality ?group associates ?centrality))",
                "centrality");

        assert21("Group-degree-centrality of Valjean's ego group at depth 2 (using associatese).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 2 associates ?group)" +
                        "(group-degree-centrality ?group associates ?centrality))",
                "centrality");

        //  "Group centrality measures the cohesion a group relative to
        //  some measure of actor-centrality. `group-closeness-centrality` is
        //  measured by first finding the actor whose `closeness-centrality`
        //  is maximized and then summing the difference between this maximum
        //  value and the [actor-closeness-centrality][] of all other actors.
        //  This value is then normalized so that it ranges between 0 and 1."
        assert21("Group-closeness-centrality of Valjean's ego group at depth 1 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 1 associates ?group)" +
                        "(group-closeness-centrality ?group associates ?centrality))",
                "centrality");

        assert21("Group-closeness-centrality of Valjean's ego group at depth 2 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 2 associates ?group)" +
                        "(group-closeness-centrality ?group associates ?centrality))",
                "centrality");

        //  "Group centrality measures the cohesion a group relative to
        //  some measure of actor-centrality. `group-betweenness-centrality` is
        //  measured by first finding the actor whose `betweenness-centrality`
        //  is maximized and then summing the difference between this maximum
        //  value and the [actor-betweenness-centrality][] of all other actors.
        //  This value is then normalized so that it ranges between 0 and 1.
        assert21("Group-betweenness-centrality of Valjean's ego group at depth 1 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 1 associates ?group)" +
                        "(group-betweenness-centrality ?group associates ?centrality))",
                "centrality");

        assert21("Group-betweenness-centrality of Valjean's ego group at depth 1 (using associates).",
                null,
                "(select (?centrality)" +
                        "(ego-group !lm:character11 2 associates ?group)" +
                        "(group-betweenness-centrality ?group associates ?centrality))",
                "centrality");
    }

    private void assert22(String msg, Stmt expected, AGRepositoryConnection conn, Value obj) throws Exception {
        assertSetsEqual(msg, stmts(expected),
                statementSet(conn.getStatements(null, null, obj, false)));
    }

    private void assert22k(String msg, Stmt expected, AGRepositoryConnection conn, IRI pred, Value obj) throws Exception {
        assertSetsSome(msg, stmts(expected),
                Stmt.dropSubjects(statementSet(conn.getStatements(null, pred, obj, false))));
    }

    /**
     * Transactions
     */
    @Test
    public void example22() throws Exception {
        AGValueFactory vf = repo.getValueFactory();
        // Create conn1 (autoCommit) and conn2 (no autoCommit).
        try (AGRepositoryConnection conLesmis = getConnection();
             AGRepositoryConnection conKennedy = getConnection()) {
            conLesmis.clear();
            conKennedy.setAutoCommit(false);
            String baseURI = "http://example.org/example/local";
            Util.add(conLesmis, "/tutorial/lesmis.rdf", baseURI, RDFFormat.RDFXML);
            assertEquals("Loaded lesmis.rdf triples via conn1.", 916, conLesmis.size());

            Util.add(conKennedy, "/tutorial/java-kennedy.ntriples", baseURI, RDFFormat.NTRIPLES);
            assertEquals("Loaded java-kennedy.ntriples via conn2.", 1214, conKennedy.size());

            //        println("Since conn1 is in autoCommit mode, lesmis.rdf triples are committed " +
            //                "and retrievable via conn2.  Since conn2 is not in autoCommit mode, and " +
            //                "no commit() has yet been issued on conn2, kennedy.rdf triples are not " +
            //                " retrievable via conn1.");
            // Check transaction isolation semantics:
            Literal valjean = vf.createLiteral("Valjean");
            Literal kennedy = vf.createLiteral("Kennedy");
            String lmns = "http://www.franz.com/lesmis#";
            //conn.setNamespace("lm", lmns);
            IRI char11 = vf.createIRI(lmns, "character11");
            IRI title = vf.createIRI("http://purl.org/dc/elements/1.1/title");
            IRI lastname = vf.createIRI("http://www.franz.com/simple#last-name");

            assert22("Using getStatements() on conn1; should find Valjean:",
                    new Stmt(char11, title, valjean),
                    conLesmis, valjean);
            assert22("Using getStatements() on conn1; should not find Kennedys:",
                    null,
                    conLesmis, kennedy);
            assert22("Using getStatements() on conn2; should not find Valjean (until a rollback or commit occurs on conn2):",
                    null,
                    conKennedy, valjean);
            assert22k("Using getStatements() on conn2; should find Kennedys:",
                    new Stmt(null, lastname, kennedy),
                    conKennedy, lastname, kennedy);

            // Rollback
            //println("Rolling back contents of conn2.");
            conKennedy.rollback();
            // rollback turns on autocommit with sesame 2.7 transaction
            // semantics, make sure it's off
            conKennedy.setAutoCommit(false);
            assertEquals("There are now triples visible via conn2.", 916, conKennedy.size());
            assert22("Using getStatements() on conn1; should find Valjean:",
                    new Stmt(char11, title, valjean),
                    conLesmis, valjean);
            assert22("Using getStatements() on conn1; should not find Kennedys:",
                    null,
                    conLesmis, kennedy);
            assert22("Using getStatements() on conn2; should not find Kennedys:",
                    null,
                    conKennedy, kennedy);
            assert22("Using getStatements() on conn2; should find Valjean:",
                    new Stmt(char11, title, valjean),
                    conKennedy, valjean);

            // Reload and Commit
            //println("Reload java-kennedy.ntriples into conn2.");
            Util.add(conKennedy, "/tutorial/java-kennedy.ntriples", baseURI, RDFFormat.NTRIPLES);
            assertEquals("There are now triples visible on conn1.", 916, conLesmis.size());
            assertEquals("There are now triples visible on conn2.", 2130, conKennedy.size());
            //println("Committing contents of conn2.");
            conKennedy.commit();
            assertEquals("There are now triples visible on conn1.", 2130, conLesmis.size());
            assertEquals("There are now triples visible on conn2.", 2130, conKennedy.size());
            assert22("Using getStatements() on conn1; should find Valjean:",
                    new Stmt(char11, title, valjean),
                    conLesmis, valjean);
            assert22k("Using getStatements() on conn1; should find Kennedys:",
                    new Stmt(null, lastname, kennedy),
                    conLesmis, lastname, kennedy);
            assert22k("Using getStatements() on conn2; should find Kennedys:",
                    new Stmt(null, lastname, kennedy),
                    conKennedy, lastname, kennedy);
            assert22("Using getStatements() on conn2; should find Valjean:",
                    new Stmt(char11, title, valjean),
                    conKennedy, valjean);
        }
    }

}
