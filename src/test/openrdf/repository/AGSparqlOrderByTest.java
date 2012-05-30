package test.openrdf.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.SparqlOrderByTest;

import test.AGAbstractTest;

public class AGSparqlOrderByTest extends SparqlOrderByTest {

	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

}
