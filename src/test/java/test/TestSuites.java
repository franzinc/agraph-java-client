/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import test.pool.AGConnPoolClosingTest;
import test.pool.AGConnPoolSessionTest;
import test.pool.AGConnPoolWarmupTest;
import test.stress.TransactionStressTest;

public class TestSuites {

    public interface NonPrepushTest {
    }

    /**
     * Takes too long to be included in prepush.
     */
    public interface Slow extends NonPrepushTest {
    }

    /**
     * Test is not applicable for AGraph for some reason.
     */
    public interface NotApplicableForAgraph extends NonPrepushTest {
    }

    /**
     * Suite for 'make prepush'.
     * Expected to pass.
     */
    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( {
            AGConnPoolWarmupTest.class,
            AGGraphQueryTests.class,
            AGHTTPClientTests.class,
            AGMaterializerTests.class,
            AGQueryExecutionTest.class,
            AGRepositoryConnectionTests.class,
            AGRepositoryFactoryTest.class,
            AGServerTests.class,
            AGServerVersionTests.class,
            AGTripleAttributesTest.class,
            AGUtilTest.class,
            BlankNodeTests.class,
            BulkModeTests.class,
            ContextsVarargsTest.class,
            DeleteDuplicatesTests.class,
            DownloadTest.class,
            DynamicCatalogTests.class,
            EncodableNamespaceTests.class,
            FederationTests.class,
            FreetextTests.class,
            IndexManagementTests.class,
            JSONLDTest.class,
            LiteralAndResourceResultsTest.class,
            MappingsTests.class,
            MasqueradeAsUserTests.class,
            NQuadsTests.class,
            QueryLimitOffsetTests.class,
            QuickTests.class,
            RDFTransactionTest.class,
            ReplHeaderTest.class,
            ServerCodeTests.class,
            SessionTests.class,
            SparqlDefaultDatasetTest.class,
            SparqlDefaultTest.class,
            SparqlUpdateTests.class,
            SpinTest.class,
            SpogiTripleCacheTests.class,
            TutorialTests.class,
            UntypedLiteralMatchingTest.class,
            UploadCommitPeriodTests.class,
            UserManagementTests.class,
            WarmupTests.class,

            JenaCompliance.class,
            OpenRDF.class,
            Unicode.class,

    })
    public static class Prepush {
    }

    @RunWith(Categories.class)
    @SuiteClasses( {
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
    public static class JenaCompliance { }

    @RunWith(Categories.class)
    @SuiteClasses( {
            test.openrdf.AGSparqlUpdateTest.class,
            test.openrdf.repository.AGAllRepositoryTests.class
    })
    public static class OpenRDF { }

    @RunWith(Categories.class)
    @SuiteClasses( {
            UnicodeTest.class,
            UnicodeRDFFormatTest.class,
            UnicodeTQRFormatTest.class
    })
    public static class Unicode {
    }

    /**
     * Category marker for tests known to be broken.
     * Marker should be removed when test is fixed.
     * <p>
     * Suite for 'ant test-broken'.
     * Known to fail.
     * When they are fixed, should be categorized as PrepushTest.
     */
    @RunWith(Categories.class)
    @IncludeCategory(Broken.class)
    @ExcludeCategory(NotApplicableForAgraph.class)
    @SuiteClasses( {
            AGConnPoolClosingTest.class,
            TripleIdTests.class
    })
    public static class Broken implements NonPrepushTest {
    }

    /**
     * Category marker for stress tests.
     * <p>
     * Suite for 'ant test-stress'
     */
    @RunWith(Categories.class)
    @IncludeCategory(Stress.class)
    @ExcludeCategory(Prepush.class)
    @SuiteClasses( {
            TransactionStressTest.class,
            AGConnPoolClosingTest.class,
            AGConnPoolSessionTest.class
    })
    public static class Stress implements NonPrepushTest {
    }

}
