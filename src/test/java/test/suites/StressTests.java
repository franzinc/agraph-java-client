package test.suites;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import test.pool.AGConnPoolClosingTest;
import test.pool.AGConnPoolSessionTest;
import test.stress.TransactionStressTest;

@Suite
@ExcludeTags("Broken")
@SelectClasses({
        TransactionStressTest.class,
        AGConnPoolClosingTest.class,
        AGConnPoolSessionTest.class
})
public class StressTests {
}
