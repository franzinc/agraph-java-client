/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGFreetextIndexConfig;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FreetextTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void testConfigInnerChars() throws Exception {
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        config.getInnerChars().add("alpha");
        conn.createFreetextIndex("index1", config);
        AGFreetextIndexConfig config1 = conn.getFreetextIndexConfig("index1");
        Assert.assertTrue("getInnerChars() should contain alpha", config1.getInnerChars().contains("alpha"));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testConfigBorderChars() throws Exception {
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        config.getBorderChars().add("digit");
        conn.createFreetextIndex("index2", config);
        AGFreetextIndexConfig config1 = conn.getFreetextIndexConfig("index2");
        Assert.assertTrue("getBorderChars() should contain digit", config1.getBorderChars().contains("digit"));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testConfigTokenizer() throws Exception {
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        Assert.assertEquals("getTokenizer() should be default", "default", config.getTokenizer());
        config.setTokenizer("japanese");
        conn.createFreetextIndex("index3", config);
        AGFreetextIndexConfig config1 = conn.getFreetextIndexConfig("index3");
        Assert.assertEquals("getTokenizer() should be japanese", "japanese", config1.getTokenizer());
    }

}
