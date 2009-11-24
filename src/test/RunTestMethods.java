package test;
import junit.framework.TestCase;
import junit.framework.TestResult;

public class RunTestMethods {

	/**
	 * One way to run a subset of the AGRepositoryConnectionTest methods.
	 * TODO: generalize.
	 * 
	 * @param args the test method names to run
	 */
	public static void main(String[] args) {
		for (String testName : args) {
			TestCase test = new AGRepositoryConnectionTest(testName);
			TestResult result = test.run();
			System.out.println(testName + ": " + result.wasSuccessful());
		}
	}

}
