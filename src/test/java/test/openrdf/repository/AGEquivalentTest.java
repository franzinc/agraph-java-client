package test.openrdf.repository;

import org.eclipse.rdf4j.repository.EquivalentTest;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.Assume;
import test.AGAbstractTest;

import java.util.Arrays;
import java.util.List;

public class AGEquivalentTest extends EquivalentTest {

    private static final List<String> IGNORED_EXPRS = Arrays.asList(new String[]{
            "\"xyz\" ? \"xyz\"^^xsd:integer",
            "\"xyz\"@en != \"xyz\"^^xsd:integer",
            "\"xyz\"@EN != \"xyz\"^^xsd:integer",
            "\"xyz\"^^xsd:string ? \"xyz\"^^xsd:integer",
            "\"xyz\"^^xsd:integer ? \"xyz\"",
            "\"xyz\"^^xsd:integer != \"xyz\"@en",
            "\"xyz\"^^xsd:integer != \"xyz\"@EN",
            "\"xyz\"^^xsd:integer ? \"xyz\"^^xsd:string",
            "\"xyz\"^^xsd:integer = \"xyz\"^^xsd:integer",
            "\"xyz\"^^xsd:integer ? \"xyz\"^^ex:unknown",
            "\"xyz\"^^xsd:integer != :xyz",
            "\"xyz\"^^ex:unknown ? \"xyz\"^^xsd:integer",
            ":xyz != \"xyz\"^^xsd:integer",
            "\"xyz\" ? \"abc\"^^xsd:integer",
            "\"xyz\"@en != \"abc\"^^xsd:integer",
            "\"xyz\"@EN != \"abc\"^^xsd:integer",
            "\"xyz\"^^xsd:string ? \"abc\"^^xsd:integer",
            "\"xyz\"^^xsd:integer ? \"abc\"",
            "\"xyz\"^^xsd:integer != \"abc\"@en",
            "\"xyz\"^^xsd:integer != \"abc\"@EN",
            "\"xyz\"^^xsd:integer ? \"abc\"^^xsd:string",
            "\"xyz\"^^xsd:integer ? \"abc\"^^xsd:integer",
            "\"xyz\"^^xsd:integer ? \"abc\"^^:unknown",
            "\"xyz\"^^xsd:integer != :abc",
            "\"xyz\"^^:unknown ? \"abc\"^^xsd:integer",
            ":xyz != \"abc\"^^xsd:integer"
    });

    private final boolean ignored;

    // This test extends a parameterized test class
    public AGEquivalentTest(String operator,
                            String term1,
                            String term2) {
        super(operator, term1, term2);
        String testExpr = term1 + " " + operator + " " + term2;
        ignored = IGNORED_EXPRS.contains(testExpr);
    }

    @Override
    protected Repository newRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

    @Override
    public void setUp() throws Exception {
        Assume.assumeFalse(ignored);
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        if (!ignored) {
            super.tearDown();
        }
    }
}

