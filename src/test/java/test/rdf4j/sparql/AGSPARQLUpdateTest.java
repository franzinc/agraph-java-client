package test.rdf4j.sparql;

import com.franz.agraph.http.exception.AGHttpException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.SPARQLUpdateTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.rdf4j.RDF4JTestsHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class AGSPARQLUpdateTest extends SPARQLUpdateTest {

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository newRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }

    @Disabled // TODO: RDF4J expects MalformedQueryException
    @Test
    @Override
    public void testInvalidDeleteUpdate() {
        super.testInvalidDeleteUpdate();
    }

    @Disabled // TODO: RDF4J expects MalformedQueryException
    @Test
    @Override
    public void testInvalidInsertUpdate() {
        super.testInvalidInsertUpdate();
    }

    @Test
    @Override
    public void testInsertData2() {
        assertThrows(AGHttpException.class, super::testInsertData2);
    }

    @Tag("Broken") // TODO:  Parse error: namespace mapping for "sesame" not defined when expanding QName "sesame:nil"
    @Test
    @Override
    public void testDeleteFromDefaultGraph() {
        super.testDeleteFromDefaultGraph();
    }

    @Tag("Broken") // TODO:  Parse error: namespace mapping for "sesame" not defined when expanding QName "sesame:nil"
    @Test
    @Override
    public void testDeleteFromDefaultGraphUsingWith() {
        super.testDeleteFromDefaultGraphUsingWith();
    }
}
