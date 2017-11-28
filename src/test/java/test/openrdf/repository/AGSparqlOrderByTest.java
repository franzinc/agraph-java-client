package test.openrdf.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.SparqlOrderByTest;
import test.AGAbstractTest;

public class AGSparqlOrderByTest extends SparqlOrderByTest {

    @Override
    protected Repository newRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

}
