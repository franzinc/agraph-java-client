package test.rdf4j.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.TupleQueryResultTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.rdf4j.RDF4JTestsHelper;

public class AGTupleQueryResultTest extends TupleQueryResultTest {
    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository newRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }

    @Disabled // TODO: RDF4J expects an exception here, and we don't throw it
    @Test
    @Override
    public void testNotClosingResultThrowsException() {
        super.testNotClosingResultThrowsException();
    }
}
