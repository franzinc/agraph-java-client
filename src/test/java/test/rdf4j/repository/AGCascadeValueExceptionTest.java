package test.rdf4j.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.CascadeValueExceptionTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.rdf4j.RDF4JTestsHelper;

public class AGCascadeValueExceptionTest extends CascadeValueExceptionTest {

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
    public void testValueExceptionEqualPlain() {
        super.testValueExceptionEqualPlain();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionEqualTyped() {
        super.testValueExceptionEqualTyped();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionGreaterThanOrEqualPlain() {
        super.testValueExceptionGreaterThanOrEqualPlain();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionGreaterThanOrEqualTyped() {
        super.testValueExceptionGreaterThanOrEqualTyped();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionGreaterThanPlain() {
        super.testValueExceptionGreaterThanPlain();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionGreaterThanTyped() {
        super.testValueExceptionGreaterThanTyped();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionNotEqualPlain() {
        super.testValueExceptionNotEqualPlain();
    }

    @Disabled
    @Test
    @Override
    public void testValueExceptionNotEqualTyped() {
        super.testValueExceptionNotEqualTyped();
    }
}
