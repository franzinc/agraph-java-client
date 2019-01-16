/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.franz.agraph.repository.AGServerVersion;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class JSONLDTest extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void test_JSONLD() throws Exception {
        AGServerVersion minVersion = new AGServerVersion("v6.5.0");
        if (server.getComparableVersion().compareTo(minVersion) >= 0) {
            long sizeBefore = conn.size();
            Util.add(conn, "/test/event.jsonld", null, RDFFormat.JSONLD);
            long sizeAfter = conn.size();
            // 3 triples should be added.  One dtstart, one location, and
            // one summary.
            long expectedSize = sizeBefore + 3;
            
            Assert.assertEquals(expectedSize, sizeAfter);
        }
    }
}



        
