package test.openrdf.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.SparqlRegexTest;
import test.AGAbstractTest;

public class AGSparqlRegexTest extends SparqlRegexTest {

    @Override
    protected Repository newRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

}
