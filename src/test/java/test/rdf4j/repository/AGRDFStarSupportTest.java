package test.rdf4j.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.RDFStarSupportTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import test.rdf4j.RDF4JTestsHelper;

@Tag("Broken") // TODO: these probably should all be passing, but only one does
public class AGRDFStarSupportTest extends RDFStarSupportTest {

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository createRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }
}
