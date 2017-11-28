package test.openrdf.repository;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
        AGCascadeValueExceptionTest.class,
        AGEquivalentTest.class,
        AGRDFSchemaRepositoryConnectionTest.class,
        AGSparqlAggregatesTest.class,
        AGSparqlOrderByTest.class,
        AGSparqlRegexTest.class,
})
public class AGAllRepositoryTests {
}
