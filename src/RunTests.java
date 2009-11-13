import junit.framework.TestCase;
import junit.framework.TestResult;

public class RunTests {

	public static void main(String[] args) {
		// org.junit.runner.JUnitCore.main(args);
		for (String testName : args) {
			TestCase test = new AGRepositoryConnectionTest(testName);
			TestResult result = test.run();
			System.out.println(testName + ": " + result.wasSuccessful());
		}
	}

}
