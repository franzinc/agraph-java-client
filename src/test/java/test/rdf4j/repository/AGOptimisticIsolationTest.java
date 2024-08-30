package test.rdf4j.repository;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.config.AGRepositoryConfig;
import com.franz.agraph.repository.config.AGRepositoryFactory;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.DeadLockTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.DeleteInsertTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.ModificationTest;
import org.eclipse.rdf4j.testsuite.repository.optimistic.RemoveIsolationTest;
import org.junit.BeforeClass;
import org.junit.runners.Suite;
import test.AGAbstractTest;

import java.io.IOException;

/**
 * Full list of classes from OptimisticIsolationTest source:
 * DeadLockTest.class, DeleteInsertTest.class, LinearTest.class, ModificationTest.class, RemoveIsolationTest.class,
 * IsolationLevelTest.class, MonotonicTest.class, SnapshotTest.class, SerializableTest.class
 * <p>
 * Below only test classes with all tests passing are specified.
 */
@Suite.SuiteClasses({
        DeadLockTest.class,
        DeleteInsertTest.class,
        ModificationTest.class,
        RemoveIsolationTest.class})
public class AGOptimisticIsolationTest extends OptimisticIsolationTest {

    // TODO: these tests expect setting a RepositoryFactory with a reusable config.
    //  AGRepositoryFactory.getConfig() simply returns a new empty config (which might not be the right behavior).
    //  To work around this we create a config of a class that extends AGRepositoryConfig with a field
    //  that contains a complete and valid repository config (computed once).
    @BeforeClass
    public static void setUpClass() throws IOException {
        try (AGRepository repo = AGAbstractTest.newTestRepository("AGOptimisticIsolationTest")) {
            AGRepositoryConfig testConfig = repo.repositoryConfig();

            setRepositoryFactory(new AGRepositoryFactory() {
                private final AGRepositoryConfig config = testConfig;

                @Override
                public AGRepositoryConfig getConfig() {
                    return config;
                }
            });
        }
    }
}
