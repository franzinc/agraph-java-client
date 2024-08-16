package test;

import com.franz.agraph.repository.WarmupConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarmupTests extends AGAbstractTest {
    private int logLengthBeforeTest;

    @BeforeEach
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
    public void testServerDefaultWarmup() {
        conn.warmup();
        assertStringsWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    public void testDefaultWarmup() {
        conn.warmup(WarmupConfig.create());
        assertStringsWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    public void testWarmupNoStrings() {
        conn.warmup(WarmupConfig.create().excludeStrings());
        assertStringsNotWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    public void testWarmupNoTriples() {
        conn.warmup(WarmupConfig.create().excludeTriples());
        assertStringsWarmedUpRecently();
        assertTriplesNotWarmedUpRecently();
    }

    @Test
    public void testWarmupNothing() {
        conn.warmup(WarmupConfig.create().excludeStrings().excludeTriples());
        assertStringsNotWarmedUpRecently();
        assertTriplesNotWarmedUpRecently();
    }
}
