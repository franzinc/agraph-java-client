package test.openrdf.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.SparqlAggregatesTest;

import test.AGAbstractTest;

public class AGSparqlAggregatesTest extends SparqlAggregatesTest {

	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

}
