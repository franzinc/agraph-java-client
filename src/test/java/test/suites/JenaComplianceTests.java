package test.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        test.AGAnonGraphTest.class,
        test.AGGraphMakerTest.class,
        test.AGModelTest.class,
        test.AGNamedGraphTest.class,
        test.AGPrefixMappingTest.class,
        test.AGReifierTest.class,
        test.AGResultSetTest.class,
        test.JenaSparqlUpdateTest.class,
        test.JenaTests.class
})
public class JenaComplianceTests {
}
