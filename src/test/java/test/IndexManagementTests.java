/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class IndexManagementTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void manageIndices_rfe9930() throws Exception {
        int indexCount = 7;
        List<String> indices = conn.listValidIndices();
        Assert.assertTrue("expected more valid indices", indices.size() >= 24);
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices initially", indexCount, indices.size());
        conn.dropIndex("gospi");
        conn.dropIndex("spogi");
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices after drop", (indexCount - 2), indices.size());
        conn.addIndex("gospi");
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices after add", (indexCount - 1), indices.size());
        conn.addIndex("gospi");
        indices = conn.listIndices();
        Assert.assertEquals("unexpected number of indices after redundant add", (indexCount - 1), indices.size());
    }

}
