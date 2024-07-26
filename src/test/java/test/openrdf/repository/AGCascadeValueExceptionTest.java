package test.openrdf.repository;

import org.eclipse.rdf4j.repository.CascadeValueExceptionTest;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.Ignore;
import org.junit.Test;
import test.AGAbstractTest;

public class AGCascadeValueExceptionTest extends CascadeValueExceptionTest {

    @Override
    protected Repository newRepository() {
        return AGAbstractTest.sharedRepository();
    }

    @Ignore @Test @Override public void testValueExceptionEqualPlain() {}
    @Ignore @Test @Override public void testValueExceptionEqualTyped() {}
    @Ignore @Test @Override public void testValueExceptionGreaterThanOrEqualPlain() {}
    @Ignore @Test @Override public void testValueExceptionGreaterThanOrEqualTyped() {}
    @Ignore @Test @Override public void testValueExceptionGreaterThanPlain() {}
    @Ignore @Test @Override public void testValueExceptionGreaterThanTyped() {}
    @Ignore @Test @Override public void testValueExceptionNotEqualPlain() {}
    @Ignore @Test @Override public void testValueExceptionNotEqualTyped() {}

}
