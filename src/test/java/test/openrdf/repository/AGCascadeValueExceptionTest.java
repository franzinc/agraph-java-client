package test.openrdf.repository;

import org.eclipse.rdf4j.repository.CascadeValueExceptionTest;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.AGAbstractTest;

public class AGCascadeValueExceptionTest extends CascadeValueExceptionTest {

    @Override
    protected Repository newRepository() {
        return AGAbstractTest.sharedRepository();
    }

    @Disabled
    @Test @Override public void testValueExceptionEqualPlain() {}
    @Disabled
    @Test
    @Override public void testValueExceptionEqualTyped() {}
    @Disabled
    @Test @Override public void testValueExceptionGreaterThanOrEqualPlain() {}
    @Disabled
    @Test @Override public void testValueExceptionGreaterThanOrEqualTyped() {}
    @Disabled
    @Test @Override public void testValueExceptionGreaterThanPlain() {}
    @Disabled
    @Test @Override public void testValueExceptionGreaterThanTyped() {}
    @Disabled
    @Test @Override public void testValueExceptionNotEqualPlain() {}
    @Disabled
    @Test
    @Override public void testValueExceptionNotEqualTyped() {}

}
