package test;

import com.franz.agraph.repository.AGServerVersion;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/** Tests for the AGServerVersion class. */
public class AGServerVersionTests {
    @RunWith(Parameterized.class)
    public static class CompareTests {
        // First version string, second version string, result
        // A result of null means that NPE should be thrown.
        // Note that all tests are also run with swapped parameters.
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { "1", "1", 0 },
                    { "1.0", "1.0", 0 },
                    { "1.0", "1-0", 0 },
                    { "ab1cd2ef3gh", "x1y2z3", 0 },
                    { "2", "1", 1 },
                    { "2.0", "1.1", 1 },
                    { "2.0.1", "2.0", 1 },
                    { "2.0.0", "2.0", 0 },
            });
        }

        private final AGServerVersion a;
        private final AGServerVersion b;
        private final Integer expected;

        public CompareTests(String a, String b, Integer expected) {
            this.a = new AGServerVersion(a);
            this.b = new AGServerVersion(b);
            this.expected = expected;
        }

        @Test
        public void test() {
            try {
                Integer actual = Integer.signum(a.compareTo(b));
                Assert.assertEquals(expected, actual);
            } catch (NullPointerException e) {
                Assert.assertNull("Expected exception not thrown", expected);
            }
        }

        @Test
        public void testReversed() {
            try {
                Integer actual = Integer.signum(-b.compareTo(a));
                Assert.assertEquals(expected, actual);
            } catch (NullPointerException e) {
                Assert.assertNull("Expected exception not thrown", expected);
            }
        }
    }

    public static class EqualsTests {
        // First version string, second version string, result
        // Note that all tests are also run with swapped parameters.
        // If arguments are expected to be equal hash codes are
        // also compared.
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { "1", "1", true },
                    { "1.0", "1.0", true },
                    { "1.0", "1-0", true },
                    { "ab1cd2ef3gh", "x1y2z3", false },
                    { "2", "1", false },
                    { "2.0", "1.1", false },
                    { "2.0.1", "2.0", false },
                    { "2.0.0", "2.0", true },
            });
        }

        private final AGServerVersion a;
        private final AGServerVersion b;
        private final boolean expected;

        public EqualsTests(String a, String b, boolean expected) {
            this.a = new AGServerVersion(a);
            this.b = new AGServerVersion(b);
            this.expected = expected;
        }

        @Test
        public void test() {
            boolean actual = a.equals(b);
            Assert.assertEquals(expected, actual);
        }

        @Test
        public void testReversed() {
            boolean actual = b.equals(a);
            Assert.assertEquals(expected, actual);
        }

        @Test
        public void testHashCode() {
            if (expected) {
                Assert.assertEquals(a.hashCode(), b.hashCode());
            }
        }
    }

    @Test(expected=NullPointerException.class)
    public void testCompareWithNull() {
        //noinspection ConstantConditions
        new AGServerVersion("v6.4.1").compareTo(null);
    }

    @Test
    public void testEqualsWithNull() {
        Assert.assertNotEquals(new AGServerVersion("v6.4.1"), null);
    }

    @Test
    public void testEqualsWithWrongClass() {
        Assert.assertNotEquals(new AGServerVersion("v6.4.1"), "v6.4.1");
    }

    @Test
    public void testToString() {
        // Just verify that the original string is there, we might want
        // to add other stuff (e.g. to identify the output as
        // an AGServerVersion object).
        Assert.assertTrue(
                new AGServerVersion("1-blorp0").toString().contains("blorp"));
    }
}
