package test.pool;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.AGAbstractTest;
import test.TestSuites;
import test.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AGConnPoolWarmupTest {
    private static final String STORE = "pool.testPoolWarmup";

    private AGServer server;
    private int logLengthBeforeTest;

    @Before
    public void setUp() {
        server = AGAbstractTest.newAGServer();
        logLengthBeforeTest = Util.getLogSize(server);
    }

    @AfterClass
    public static void tearDownClass() {
        // Make sure the repository is deleted.
        AGAbstractTest.deleteRepository(AGAbstractTest.CATALOG_ID, STORE);
    }

    private AGConnPool makePool(Boolean warmupStrings, Boolean warmupTriples) {
        final List<Object> args = new ArrayList<>(Arrays.asList(
                AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                AGConnProp.username, AGAbstractTest.username(),
                AGConnProp.password, AGAbstractTest.password(),
                AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
                AGConnProp.repository, STORE,
                AGPoolProp.warmup, true));
        if (warmupStrings != null) {
            args.add(AGPoolProp.warmupIncludeStrings);
            args.add(warmupStrings);
        }
        if (warmupTriples != null) {
            args.add(AGPoolProp.warmupIncludeTriples);
            args.add(warmupTriples);
        }
        return AGConnPool.create(args.toArray());
    }

    private void assertTriplesWarmedUpRecently() {
        Util.assertTriplesWarmedUpRecently(server, logLengthBeforeTest, STORE);
    }

    private void assertStringsWarmedUpRecently() {
        Util.assertStringsWarmedUpRecently(server, logLengthBeforeTest, STORE);
    }

    private void assertTriplesNotWarmedUpRecently() {
        Util.assertTriplesNotWarmedUpRecently(server, logLengthBeforeTest, STORE);
    }

    private void assertStringsNotWarmedUpRecently() {
        Util.assertStringsNotWarmedUpRecently(server, logLengthBeforeTest, STORE);
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testDefaultWarmup() {
        makePool(null, null).close();
        assertStringsWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupNoStrings() {
        makePool(false, null).close();
        assertStringsNotWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupNoTriples() {
        makePool(null, false).close();
        assertStringsWarmedUpRecently();
        assertTriplesNotWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupAllExplicit() {
        makePool(true, true).close();
        assertStringsWarmedUpRecently();
        assertTriplesWarmedUpRecently();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testWarmupNothing() {
        makePool(false, false).close();
        assertStringsNotWarmedUpRecently();
        assertTriplesNotWarmedUpRecently();
    }
}
