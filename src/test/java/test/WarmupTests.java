package test;

import com.franz.agraph.repository.WarmupConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WarmupTests extends AGAbstractTest {
    private int logLengthBeforeTest;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        logLengthBeforeTest = Util.getLogSize(server);
    }

    private void assertTriplesWarmedUpRecently() {
        Util.assertTriplesWarmedUpRecently(server, logLengthBeforeTest, REPO_ID);
    }

    private void assertStringsWarmedUpRecently() {
        Util.assertStringsWarmedUpRecently(server, logLengthBeforeTest, REPO_ID);
    }

    private void assertTriplesNotWarmedUpRecently() {
        Util.assertTriplesNotWarmedUpRecently(server, logLengthBeforeTest, REPO_ID);
    }

    private void assertStringsNotWarmedUpRecently() {
        Util.assertStringsNotWarmedUpRecently(server, logLengthBeforeTest, REPO_ID);
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testServerDefaultWarmup() {
        conn.warmup();
        assertStringsWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testDefaultWarmup() {
        conn.warmup(WarmupConfig.create());
        assertStringsWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupNoStrings() {
        conn.warmup(WarmupConfig.create().excludeStrings());
        assertStringsNotWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupNoTriples() {
        conn.warmup(WarmupConfig.create().excludeTriples());
        assertStringsWarmedUpRecently();
        assertTriplesNotWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupNothing() {
        conn.warmup(WarmupConfig.create().excludeStrings().excludeTriples());
        assertStringsNotWarmedUpRecently();
        assertTriplesNotWarmedUpRecently();
    }
}
