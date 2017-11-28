package test.openrdf.repository;

import org.eclipse.rdf4j.repository.CascadeValueExceptionTest;
import org.eclipse.rdf4j.repository.Repository;
import test.AGAbstractTest;

public class AGCascadeValueExceptionTest extends CascadeValueExceptionTest {

    @Override
    protected Repository newRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

}
