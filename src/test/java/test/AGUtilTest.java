package test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import com.franz.util.Util;

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
                Util.getCatalogFromSpec("cat:repo"));
    }

    @Test
    public void testGetCatalogNull() {
        Assert.assertNull(Util.getCatalogFromSpec("repo"));
    }

    @Test
    public void testGetRepo() {
        Assert.assertEquals(
                "repo",
                Util.getRepoFromSpec("cat:repo"));
    }

    @Test
    public void testGetRepoNoCat() {
        Assert.assertEquals(
                "repo",
                Util.getRepoFromSpec("repo"));
    }

    @RunWith(Parameterized.class)
    public static class IntArrayMismatchTests {
        // First array, second array, result
        // A result of null means that NPE should be thrown.
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { new int[] {}, new int[] {}, -1 },
                    { new int[] { 1 }, new int[] { 2 }, 0 },
                    { new int[] { 42 }, new int[] { 42 }, -1 },
                    { new int[] { 1, 2, 3 }, new int[] { 2, 2, 3 }, 0 },
                    { new int[] { 1, 2, 3 }, new int[] { 1, 0, 3 }, 1 },
                    { new int[] { 1, 2, 3 }, new int[] { 1, 2 }, 3 },
                    { new int[] { 1, 2 }, new int[] { 1, 2, 3 }, 3 },
                    { new int[] { 0, 1 }, new int[] { 1, 2, 3 }, 0 },
                    { null, new int[] { 1, 2, 3 }, null },
                    { new int[] { 1, 2, 3 }, null, null },
                    { null, null, null },
            });
        }

        private final int[] a;
        private final int[] b;
        private final Integer expected;

        public IntArrayMismatchTests(int[] a, int[] b, Integer expected) {
            this.a = a;
            this.b = b;
            this.expected = expected;
        }

        @Test
        public void test() {
            try {
                Integer actual = Util.mismatch(a, b);
                Assert.assertEquals(expected, actual);
            } catch (NullPointerException e) {
                Assert.assertNull("Expected exception not thrown", expected);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class IntArrayCompareTests {
        // First array, second array, result
        // A result of null means that NPE should be thrown.
        // -1 will match any negative value.
        // 1 will match any positive value.
        // Note that all tests are also run with swapped parameters.
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { new int[] {}, new int[] {}, 0 },
                    { new int[] { 1 }, new int[] { 2 }, -1 },
                    { new int[] { 42 }, new int[] { 42 }, 0 },
                    { new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, 0 },
                    { new int[] { 1, 2, 3 }, new int[] { 2, 2, 3 }, -1 },
                    { new int[] { 1, 2, 3 }, new int[] { 1, 0, 3 }, 1 },
                    { new int[] { 1, 2, 3 }, new int[] { 1, 2 }, 1 },
                    { new int[] { 0, 1 }, new int[] { 1, 2, 3 }, -1 },
                    { new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }, -1 },
                    { null, new int[] { 1, 2, 3 }, null },
                    { new int[] { 1, 2, 3 }, null, null },
                    { null, null, null },
            });
        }

        private final int[] a;
        private final int[] b;
        private final Integer expected;

        public IntArrayCompareTests(int[] a, int[] b, Integer expected) {
            this.a = a;
            this.b = b;
            this.expected = expected;
        }

        @Test
        public void test() {
            try {
                Integer actual = Integer.signum(Util.compare(a, b));
                Assert.assertEquals(expected, actual);
            } catch (NullPointerException e) {
                Assert.assertNull("Expected exception not thrown", expected);
            }
        }

        @Test
        public void testReversed() {
            try {
                Integer actual = Integer.signum(-Util.compare(b, a));
                Assert.assertEquals(expected, actual);
            } catch (NullPointerException e) {
                Assert.assertNull("Expected exception not thrown", expected);
            }
        }
    }
}
