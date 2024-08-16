package test.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import test.*;

@Suite
@SelectClasses({
        AGAnonGraphTest.class,
        AGGraphMakerTest.class,
        AGModelTest.class,
        AGNamedGraphTest.class,
        AGPrefixMappingTest.class,
        AGReifierTest.class,
        AGResultSetTest.class,
        JenaSparqlUpdateTest.class,
        JenaTests.class
})
public class JenaComplianceTests {
}
