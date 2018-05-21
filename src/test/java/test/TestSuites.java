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
            AGMaterializerTests.class,
            MasqueradeAsUserTests.class,
            UserManagementTests.class,
            TutorialTests.class,
            LiteralAndResourceResultsTest.class,
            QuickTests.class,
            AGRepositoryConnectionTests.class,
            RDFTransactionTest.class,
            JenaTests.class,
            BulkModeTests.class,
            IndexManagementTests.class,
            EncodableNamespaceTests.class,
            FederationTests.class,
            SessionTests.class,
            ServerCodeTests.class,
            SpogiTripleCacheTests.class,
            UploadCommitPeriodTests.class,
            NQuadsTests.class,
            AGGraphQueryTests.class,
            QueryLimitOffsetTests.class,
            UntypedLiteralMatchingTest.class,
            AGConnPoolSessionTest.class,
            BlankNodeTests.class,
            MappingsTests.class,
            DynamicCatalogTests.class,
            SpinTest.class,
            FreetextTests.class,
            DeleteDuplicatesTests.class,
            SparqlUpdateTests.class,
            SparqlDefaultTest.class,
            SparqlDefaultDatasetTest.class,
            AGRepositoryFactoryTest.class,
            AGQueryExecutionTest.class,
            AGTripleAttributesTest.class,
            Unicode.class,
            DownloadTest.class,
            AGUtilTest.class,
            ReplHeaderTest.class,
            AGServerVersionTests.class,
    })
    public static class Prepush {
    }

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
    @SuiteClasses( {TutorialTests.class,
            QuickTests.class,
            AGConnPoolClosingTest.class,
            AGRepositoryConnectionTests.class})
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
    @SuiteClasses( {QuickTests.class,
            TransactionStressTest.class,
            AGConnPoolClosingTest.class,
            AGConnPoolSessionTest.class
    })
    public static class Stress implements NonPrepushTest {
    }

}
