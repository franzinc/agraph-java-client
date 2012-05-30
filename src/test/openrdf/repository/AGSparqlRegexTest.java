package test.openrdf.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.SparqlRegexTest;

import test.AGAbstractTest;

public class AGSparqlRegexTest extends SparqlRegexTest {

	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

}
