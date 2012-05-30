package test.openrdf.repository;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AGAllRepositoryTests extends TestCase {

	public static TestSuite suite() throws Exception {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(AGCascadeValueExceptionTest.class);
		suite.addTest(AGEquivalentTest.suite());
		suite.addTestSuite(AGRDFSchemaRepositoryConnectionTest.class);
		suite.addTestSuite(AGSparqlAggregatesTest.class);
		suite.addTestSuite(AGSparqlOrderByTest.class);
		suite.addTestSuite(AGSparqlRegexTest.class);
		return suite;
	}

}
