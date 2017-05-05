package test.openrdf.repository;

import junit.framework.TestSuite;
import org.openrdf.repository.EquivalentTest;
import org.openrdf.repository.Repository;
import test.AGAbstractTest;

public class AGEquivalentTest extends EquivalentTest {
    // This test extends a parameterized test class
    public AGEquivalentTest(String operator,
			    String term1,
			    String term2) {
	super(operator, term1, term2);
    }
	@Override
	protected Repository newRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}
}
