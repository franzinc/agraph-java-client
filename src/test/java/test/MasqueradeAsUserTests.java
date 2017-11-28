/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGRepositoryConnection;
import junit.framework.Assert;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MasqueradeAsUserTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void limitedUser() throws Exception {
        server.addUser("lim", "lim");
        AGRepositoryConnection conn = repo.getConnection();
        AGRepositoryConnection conn2 = repo.getConnection();
        conn.clear();
        IRI lim = vf.createIRI("http://lim");
        server.addUserSecurityFilter("lim", "allow", NTriplesUtil.toNTriplesString(lim), null, null, null);
        conn.add(lim, lim, lim, lim);
        conn.add(OWL.INVERSEOF, OWL.INVERSEOF, OWL.INVERSEOF);
        Assert.assertEquals("expected size 2", 2, conn.size());
        conn.setMasqueradeAsUser("lim");
        Assert.assertEquals("expected conn size 1", 1, conn.size());
        Assert.assertEquals("expected conn2 size 2", 2, conn2.size());
        conn.clear(); // should only clear lim's triples
        conn.setMasqueradeAsUser(null); // should revert to superuser
        Assert.assertEquals("expected size 1", 1, conn.size());
        conn.close();
        conn2.close();
        server.deleteUser("lim");
    }

    @Test
    @Category(TestSuites.Broken.class) // no error thrown for no such user
    public void noSuchUser() throws Exception {
        conn.clear();
        try {
            conn.setMasqueradeAsUser("noSuchUser");
            conn.size();
            Assert.fail("Expected a no such user exception");
        } catch (AGHttpException e) {
            // expected
        }
        conn.close();
    }

}
