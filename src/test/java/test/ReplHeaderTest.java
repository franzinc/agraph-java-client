package test;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.repl.DurabilityLevel;
import com.franz.agraph.repository.repl.TransactionSettings;
import com.franz.util.Ctx;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

// Since ALL HTTP calls will carry the repl headers,
// we generally just use a size() call to capture and check
// the headers. This might need to be adjusted if we decide
// to be clever and not send the headers when not needed.

// Note: header capture only works for GET and POST calls right now.

public class ReplHeaderTest extends AGAbstractTest {
    private TransactionSettings settings = new TransactionSettings();
    private AGRepositoryConnection mock;
    private Header[] lastHeaders;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AGHTTPClient client = new AGHTTPClient(server.getServerURL()) {
            @Override
            public void get(String url, Header[] headers,
                            NameValuePair[] params, AGResponseHandler handler) throws AGHttpException {
                super.get(url, headers, params, handler);
                lastHeaders = headers;
            }

            @Override
            public void post(String url, Header[] headers, NameValuePair[] params, RequestEntity requestEntity, AGResponseHandler handler) throws AGHttpException {
                super.post(url, headers, params, requestEntity, handler);
                lastHeaders = headers;
            }
        };
        client.setUsernameAndPassword(username(), password());
        AGHttpRepoClient repoClient =
                new AGHttpRepoClient(repo, client, repo.getRepositoryURL(), null);
        mock = new AGRepositoryConnection(repo, repoClient);
    }

    // Retrieve the repl settings header from the last
    // GET or POST call. Return null if there was no repl header
    // in that call.
    private String getReplHeader() {
        if (lastHeaders == null) {
            return null;
        }
        for (final Header header : lastHeaders) {
            if (header.getName().equalsIgnoreCase(AGProtocol.X_REPL_SETTINGS)) {
                return header.getValue();
            }
        }
        return null;
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testNoReplHeaderByDefault() {
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSetIntegerDurability() {
        mock.setTransactionSettings(settings.withDurability(42));
        mock.size();
        Assert.assertEquals("durability=42", getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSetKeywordDurability() {
        mock.setTransactionSettings(settings.withDurability(DurabilityLevel.QUORUM));
        mock.size();
        Assert.assertEquals("durability=quorum", getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSetTimeout() {
        mock.setTransactionSettings(settings.withDistributedTransactionTimeout(42));
        mock.size();
        Assert.assertEquals("distributedTransactionTimeout=42", getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSetLatency() {
        mock.setTransactionSettings(settings.withTransactionLatencyCount(42));
        mock.size();
        Assert.assertEquals("transactionLatencyCount=42", getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSetLatencyTimeout() {
        mock.setTransactionSettings(settings.withTransactionLatencyTimeout(42));
        mock.size();
        Assert.assertEquals("transactionLatencyTimeout=42", getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSetAll() {
        mock.setTransactionSettings(
                settings.withDurability(1)
                        .withDistributedTransactionTimeout(2)
                        .withTransactionLatencyCount(3)
                        .withTransactionLatencyTimeout(42));
        mock.size();
        // Technically these could be reordered...
        Assert.assertEquals(
                "durability=1 distributedTransactionTimeout=2 transactionLatencyCount=3 transactionLatencyTimeout=42",
                getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testResetDurability() {
        mock.setTransactionSettings(
                settings.withDurability(DurabilityLevel.QUORUM)
                        .withDurability(DurabilityLevel.DEFAULT));
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testResetDurabilityNullInteger() {
        mock.setTransactionSettings(
                settings.withDurability(DurabilityLevel.QUORUM)
                        .withDurability((Integer) null));
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testResetDurabilityNullLevel() {
        mock.setTransactionSettings(
                settings.withDurability(DurabilityLevel.QUORUM)
                        .withDurability((DurabilityLevel) null));
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testResetTimeout() {
        mock.setTransactionSettings(
                settings.withDistributedTransactionTimeout(42)
                        .withDistributedTransactionTimeout(null));
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testResetLatency() {
        mock.setTransactionSettings(
                settings.withTransactionLatencyCount(42)
                        .withTransactionLatencyCount(null));
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testResetLatencyTimeout() {
        mock.setTransactionSettings(
                settings.withTransactionLatencyTimeout(42)
                        .withTransactionLatencyTimeout(null));
        mock.size();
        Assert.assertNull(getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testContextManager() {
        mock.setTransactionSettings(settings.withDurability(DurabilityLevel.QUORUM));
        mock.size();
        Assert.assertEquals("durability=quorum", getReplHeader());
        try (Ctx ignored = mock.transactionSettingsCtx(settings.withDurability(100))) {
            mock.size();
            Assert.assertEquals("durability=100", getReplHeader());
        }
        mock.size();
        Assert.assertEquals("durability=quorum", getReplHeader());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testCommitSettings() {
        mock.commit(settings.withDurability(42));
        Assert.assertEquals("durability=42", getReplHeader());
    }
}
