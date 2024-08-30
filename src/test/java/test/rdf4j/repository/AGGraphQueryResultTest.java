package test.rdf4j.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.GraphQueryResultTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.rdf4j.RDF4JTestsHelper;

class AGGraphQueryResultTest extends GraphQueryResultTest {

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository newRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }

    @Disabled
    @Test
    @Override
    public void testDescribeMultiple() {
        super.testDescribeMultiple();
    }

    @Disabled
    @Test
    @Override
    public void testDescribeSingle() {
        super.testDescribeSingle();
    }
}
