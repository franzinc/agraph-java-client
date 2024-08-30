package test.rdf4j.sparql;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11UpdateComplianceTest;
import org.junit.jupiter.api.AfterAll;
import test.rdf4j.RDF4JTestsHelper;

public class AGSPARQL11UpdateComplianceTest extends SPARQL11UpdateComplianceTest {

    // TODO: these may need to be passing
    private static final String[] ignoredTests = new String[]{
            "INSERT 03",
            "INSERT 04",
            "INSERT USING 01",
            "Graph-specific DELETE WHERE 1",
            "Simple DELETE 1 (WITH)",
            "Graph-specific DELETE 1 (WITH)",
            "Simple DELETE 2 (USING)",
            "Graph-specific DELETE 1 (USING)",
            "Graph-specific DELETE 2 (USING)",
    };

    public AGSPARQL11UpdateComplianceTest() {
        super();
        for (String test : ignoredTests) {
            addIgnoredTest(test);
        }
    }

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository newRepository() {
        return RDF4JTestsHelper.getTestRepository();
    }
}
