/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SpogiTripleCacheTests extends AGAbstractTest {

    public static long SIZE = 12345;

    @Test
    @Category(TestSuites.Prepush.class)
    public void spogiCache_rfe10059() throws Exception {
        Assert.assertEquals("expected no spogi cache", 0, conn.getTripleCacheSize());
        conn.enableTripleCache(12345);
        Assert.assertEquals("expected spogi cache size" + SIZE, SIZE, conn.getTripleCacheSize());
        conn.setAutoCommit(false);
        Assert.assertEquals("expected spogi cache size" + SIZE, SIZE, conn.getTripleCacheSize());
        conn.disableTripleCache();
        Assert.assertEquals("expected no spogi cache", 0, conn.getTripleCacheSize());
    }

}
