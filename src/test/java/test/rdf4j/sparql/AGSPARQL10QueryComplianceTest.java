package test.rdf4j.sparql;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL10QueryComplianceTest;
import org.junit.jupiter.api.AfterAll;
import test.rdf4j.RDF4JTestsHelper;

public class AGSPARQL10QueryComplianceTest extends SPARQL10QueryComplianceTest {

    // TODO: some of these may need to be passing,
    //  many are failing due to "Cannot store external blank node" error
    private static final String[] ignoredTests = {
            "dawg-triple-pattern-004",
            "open-eq-01",
            "open-eq-07",
            "open-eq-08",
            "open-eq-09",
            "open-eq-10",
            "open-eq-11",
            "open-eq-12",
            "Join operator with Graph and Union",
            "Join scope - 1",
            "dawg-bnode-coreference",
            "Complex optional semantics: 2",
            "Complex optional semantics: 3",
            "Complex optional semantics: 4",
            "One optional clause",
            "Two optional clauses",
            "Union is not optional",
            "graph-09",
            "graph-10b",
            "graph-11",
            "dataset-01",
            "dataset-03",
            "dataset-05",
            "dataset-06",
            "dataset-07",
            "dataset-08",
            "dataset-11",
            "dataset-12b",
            "isLiteral",
            "str-1",
            "str-2",
            "str-3",
            "str-4",
            "isBlank-1",
            "datatype-1",
            "datatype-2 : Literals with a datatype",
            "datatype-3 : Literals with a datatype of xsd:string",
            "lang-1 : Literals with a lang tag of some kind",
            "lang-2 : Literals with a lang tag of ''",
            "lang-3 : Graph matching with lang tag being a different case",
            "isURI-1",
            "isIRI-1",
            "sameTerm-simple",
            "sameTerm-eq",
            "sameTerm-not-eq",
            "Equality 1-1 -- graph",
            "Equality 1-2 -- graph",
            "kanji-01",
            "kanji-02",
            "dawg-construct-identity",
            "dawg-construct-subgraph",
            "dawg-construct-reification-1",
            "dawg-construct-reification-2",
            "Numbers: Distinct",
            "Nodes: No distinct",
            "Nodes: Distinct",
            "Opt: No distinct",
            "Opt: Distinct",
            "All: No distinct",
            "sort-1",
            "sort-2",
            "sort-3",
            "sort-4",
            "sort-5",
            "sort-6",
            "sort-7",
            "sort-8",
            "sort-9",
            "sort-10",
    };

    public AGSPARQL10QueryComplianceTest() {
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
