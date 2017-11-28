/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.openrdf;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.AGAbstractTest;
import test.TestSuites;

import java.io.IOException;
import java.io.InputStream;

public class AGSparqlUpdateTest extends SPARQLUpdateTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void batchUpdate() throws Exception {
        IRI s = f.createIRI("http://example/book1");
        IRI p = f.createIRI("http://purl.org/dc/elements/1.1/title");
        Literal o_wrong = f.createLiteral("Fundamentals of Compiler Desing");
        Literal o_right = f.createLiteral("Fundamentals of Compiler Design");
        IRI g = f.createIRI("http://example/bookStore");
        con.add(s, p, o_wrong, g);

        // Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
                + "\n"
                + "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        Update u = con.prepareUpdate(QueryLanguage.SPARQL, queryString);
        u.execute();
        Assert.assertTrue("Title should be correct", con.hasStatement(s, p, o_right, false, g));
        Assert.assertFalse("Incorrect title should be gone", con.hasStatement(s, p, o_wrong, false, g));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void updateViaBooleanQuery() throws Exception {
        IRI s = f.createIRI("http://example/book1");
        IRI p = f.createIRI("http://purl.org/dc/elements/1.1/title");
        Literal o_wrong = f.createLiteral("Fundamentals of Compiler Desing");
        Literal o_right = f.createLiteral("Fundamentals of Compiler Design");
        IRI g = f.createIRI("http://example/bookStore");
        con.add(s, p, o_wrong, g);

        // Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
                + "\n"
                + "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        // SPARQL Update queries can also be executed using a BooleanQuery (for side effect)
        // Useful for older versions of Sesame that don't have a prepareUpdate method.
        con.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate();
        Assert.assertTrue("Title should be correct", con.hasStatement(s, p, o_right, false, g));
        Assert.assertFalse("Incorrect title should be gone", con.hasStatement(s, p, o_wrong, false, g));
    }

    @Override
    protected Repository newRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

    /* protected methods */

    protected void loadDataset(String datasetFile)
            throws RDFParseException, RepositoryException, IOException {
        logger.debug("loading dataset...");
        InputStream dataset = org.eclipse.rdf4j.query.parser.sparql.SPARQLUpdateTest.class.getResourceAsStream(datasetFile);
        try {
            RDFParser parser = Rio.createParser(RDFFormat.TRIG, f);
            parser.setPreserveBNodeIDs(true);
            StatementCollector collector = new StatementCollector();
            parser.setRDFHandler(collector);
            parser.parse(dataset, "");
            con.add(collector.getStatements());
        } catch (RDFParseException e) {
            throw new RuntimeException(e);
        } catch (RDFHandlerException e) {
            throw new RuntimeException(e);
        } finally {
            dataset.close();
        }
    }

}
