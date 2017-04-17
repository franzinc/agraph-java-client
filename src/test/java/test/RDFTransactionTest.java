/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package test;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import static org.junit.Assert.assertEquals;

public class RDFTransactionTest extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void sendRDFTransaction() throws Exception {
    	URI context1 = vf.createURI("http://example.org/context1");
    	conn.sendRDFTransaction(Util.resourceAsStream("/test/rdftransaction.xml"));
    	assertEquals("size", 2, conn.size((Resource)null));
    	assertEquals("size", 0, conn.size(context1));
    	conn.sendRDFTransaction(Util.resourceAsStream("/test/rdftransaction-1.xml"));
    	assertEquals("size", 1, conn.size((Resource)null));
    	assertEquals("size", 2, conn.size(context1));
    	assertEquals("size", 3, conn.size());
    }

}
