/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IndexManagementTests extends AGAbstractTest {

    @Test
    public void manageIndices_rfe9930() throws Exception {
        /**
         * before 7.1.0    gposi, gspoi, ospgi, posgi, gospi, spogi, i
         * 7.1.0 and after gposi, gspoi, ospgi, posgi, psogi, spogi, i
         *                                             ^^^^^^
         */
        int indexCount = 7;
        List<String> indices = conn.listValidIndices();
        Assert.assertTrue("expected more valid indices", indices.size() >= 24);
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices initially", indexCount, indices.size());
        conn.dropIndex("gspoi");
        conn.dropIndex("spogi");
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices after drop", (indexCount - 2), indices.size());
        conn.addIndex("gspoi");
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices after add", (indexCount - 1), indices.size());
        conn.addIndex("gspoi");
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices after redundant add", (indexCount - 1), indices.size());
    }

}
