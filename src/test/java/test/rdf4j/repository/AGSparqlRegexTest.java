package test.rdf4j.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.SparqlRegexTest;
import org.junit.jupiter.api.AfterAll;
import test.rdf4j.RDF4JTestsHelper;

public class AGSparqlRegexTest extends SparqlRegexTest {

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository newRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }

}
