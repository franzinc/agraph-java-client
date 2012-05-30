package test.openrdf.repository;

import junit.framework.TestSuite;

import org.openrdf.repository.EquivalentTest;
import org.openrdf.repository.Repository;

import test.AGAbstractTest;

public class AGEquivalentTest extends EquivalentTest {

	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

	public static TestSuite suite()
			throws Exception
		{
			return suite(AGEquivalentTest.class);
		}

}
