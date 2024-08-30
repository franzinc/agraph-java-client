package test.suites;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Suite for 'make prepush'.
 * Expected to pass.
 */
@Suite
@ExcludeTags("Broken")
@SelectClasses({
        test.pool.AGConnPoolWarmupTest.class,
        test.AGGraphQueryTests.class,
        test.AGHTTPClientTests.class,
        test.AGMaterializerTests.class,
        test.AGQueryExecutionTest.class,
        test.AGRepositoryConnectionTests.class,
        test.AGRepositoryFactoryTest.class,
        test.AGServerTests.class,
        test.AGServerVersionTests.class,
        test.AGTripleAttributesTest.class,
        test.AGUtilTest.class,
        test.BlankNodeTests.class,
        test.BulkModeTests.class,
        test.ContextsVarargsTest.class,
        test.DeleteDuplicatesTests.class,
        test.DownloadTest.class,
        test.DynamicCatalogTests.class,
        test.EncodableNamespaceTests.class,
        test.FederationTests.class,
        test.FreetextTests.class,
        test.IndexManagementTests.class,
        test.JSONLDTest.class,
        test.LiteralAndResourceResultsTest.class,
        test.MappingsTests.class,
        test.MasqueradeAsUserTests.class,
        test.NQuadsTests.class,
        test.QueryLimitOffsetTests.class,
        test.QuickTests.class,
        test.RDFTransactionTest.class,
        test.ReplHeaderTest.class,
        test.ServerCodeTests.class,
        test.SessionTests.class,
        test.SparqlDefaultDatasetTest.class,
        test.SparqlDefaultTest.class,
        test.SparqlUpdateTests.class,
        test.SpinTest.class,
        test.SpogiTripleCacheTests.class,
        test.TutorialTests.class,
        test.UntypedLiteralMatchingTest.class,
        test.UploadCommitPeriodTests.class,
        test.UserManagementTests.class,
        test.WarmupTests.class,

        JenaComplianceTests.class,
        RDF4JTests.class,
        UnicodeTests.class,

})
public class PrepushTests {
}
