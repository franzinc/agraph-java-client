package test.rdf4j.sparql;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11QueryComplianceTest;
import org.junit.jupiter.api.AfterAll;
import test.rdf4j.RDF4JTestsHelper;

public class AGSPARQL11QueryComplianceTest extends SPARQL11QueryComplianceTest {

    // TODO: some of these may need to be passing,
    //  many are failing due to "Cannot store external blank node" error
    private static final String[] ignoredTests = new String[]{
            "Error in AVG",
            "Protect from error in AVG",
            "constructwhere04 - CONSTRUCT WHERE",
            "tsv01 - TSV Result Format",
            "tvs02 - TSV Result Format",
            "isNumeric()",
            "ABS()",
            "CEIL()",
            "FLOOR()",
            "ROUND()",
            "plus-1",
            "plus-2",
            "HOURS()",
            "COALESCE()",
            "jsonres01 - JSON Result Format",
            "jsonres02 - JSON Result Format",
            "jsonres03 - JSON Result Format",
            "jsonres04 - JSON Result Format",
            "(pp36) Arbitrary path with bound endpoints",

            // TODO: see AG-1151
            "MD5() over Unicode data",
            "SHA1() on Unicode data",
            "SHA256() on Unicode data",
            "SHA512() on Unicode data",

    };

    public AGSPARQL11QueryComplianceTest() {
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
