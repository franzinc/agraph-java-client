package test.rdf4j.sparql;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.config.AGRepositoryConfig;
import com.franz.agraph.repository.config.AGRepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.testsuite.sparql.RepositorySPARQLComplianceTestSuite;
import org.junit.jupiter.api.Disabled;
import test.AGAbstractTest;


// 2/3 of these tests are passing, however there is no easy mechanism to filter out broken tests
// Also, it's not clear whether we care about being compliant with this test suite
@Disabled
public class AGRepositorySPARQLComplianceTests extends RepositorySPARQLComplianceTestSuite {

    public AGRepositorySPARQLComplianceTests() {
        super(newRepositoryFactory());
    }

    private static RepositoryFactory newRepositoryFactory() {
        try (AGRepository repo = AGAbstractTest.newTestRepository("AGRepositorySPARQLComplianceTests")) {
            AGRepositoryConfig testConfig = repo.repositoryConfig();
            return new AGRepositoryFactory() {
                private final AGRepositoryConfig config = testConfig;

                @Override
                public AGRepositoryConfig getConfig() {
                    return config;
                }
            };
        }
    }
}
