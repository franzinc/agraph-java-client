package test.suites;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import test.*;
import test.pool.AGConnPoolWarmupTest;

/**
 * Suite for 'make prepush'.
 * Expected to pass.
 */
@Suite
@ExcludeTags("Broken")
@SelectClasses({
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

        JenaComplianceTests.class,
        OpenRDFTests.class,
        UnicodeTests.class,

})
public class PrepushTests {
}
