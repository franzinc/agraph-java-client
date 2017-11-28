package test.openrdf.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.SparqlAggregatesTest;
import test.AGAbstractTest;

public class AGSparqlAggregatesTest extends SparqlAggregatesTest {

    @Override
    protected Repository newRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

}
