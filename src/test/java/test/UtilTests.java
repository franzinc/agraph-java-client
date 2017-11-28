/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class UtilTests {

    @Test
    public void randomLong() throws Exception {
        Util.RandomLong r = new Util.RandomLong();
        final long max = (((long) Integer.MAX_VALUE + 10L));
        long near = 0;
        long count = 0;
        final long start = System.nanoTime();
        while ((System.nanoTime() - start) < TimeUnit.SECONDS.toNanos(5)) {
            count++;
            long next = r.nextLong(max);
            if (next >= max) {
                Assert.fail("next=" + next + " i=" + max);
            }
            if (next > (max - 10000)) {
                near++;
            }
        }
        Assert.assertTrue("count=" + count + " near=" + near, near > 10);
    }

}
