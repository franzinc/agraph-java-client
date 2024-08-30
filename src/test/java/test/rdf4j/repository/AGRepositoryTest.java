package test.rdf4j.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.rdf4j.RDF4JTestsHelper;


public class AGRepositoryTest extends RepositoryTest {

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository createRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }

    @Disabled
    @Test
    @Override
    public void testAutoInit() {
        super.testAutoInit();
    }
}
