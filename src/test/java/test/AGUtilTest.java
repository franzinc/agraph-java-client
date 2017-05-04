package test;

import com.franz.util.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the Util class in the client code.
 * Not to be confused with the Util class in test code, which is
 * tested by UtilTest.
 */
public class AGUtilTest extends AGAbstractTest {
    @Test
    public void testGetCatalog() {
        Assert.assertEquals(
                "cat",
                com.franz.util.Util.getCatalogFromSpec("cat:repo"));
    }

    @Test
    public void testGetCatalogNull() {
        Assert.assertNull(com.franz.util.Util.getCatalogFromSpec("repo"));
    }

    @Test
    public void testGetRepo() {
        Assert.assertEquals(
                "repo",
                com.franz.util.Util.getRepoFromSpec("cat:repo"));
    }

    @Test
    public void testGetRepoNoCat() {
        Assert.assertEquals(
                "repo",
                com.franz.util.Util.getRepoFromSpec("repo"));
    }
}
