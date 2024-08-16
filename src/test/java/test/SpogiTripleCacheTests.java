/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpogiTripleCacheTests extends AGAbstractTest {

    public static long SIZE = 12345;

    @Test

    public void spogiCache_rfe10059() throws Exception {
        assertEquals(0, conn.getTripleCacheSize(), "expected no spogi cache");
        conn.enableTripleCache(12345);
        assertEquals(SIZE, conn.getTripleCacheSize(), "expected spogi cache size" + SIZE);
        conn.setAutoCommit(false);
        assertEquals(SIZE, conn.getTripleCacheSize(), "expected spogi cache size" + SIZE);
        conn.disableTripleCache();
        assertEquals(0, conn.getTripleCacheSize(), "expected no spogi cache");
    }

}
