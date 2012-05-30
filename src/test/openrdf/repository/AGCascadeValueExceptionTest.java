package test.openrdf.repository;

import org.openrdf.repository.CascadeValueExceptionTest;
import org.openrdf.repository.Repository;

import test.AGAbstractTest;

public class AGCascadeValueExceptionTest extends CascadeValueExceptionTest {

	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

}
