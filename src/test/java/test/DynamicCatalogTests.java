/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGCatalog;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DynamicCatalogTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void dynamicCatalogs_rfe10175() throws Exception {
        String catalogID = "dynamicCatalog1";
        String repoID = "repo1";
        AGCatalog catalog = server.createCatalog(catalogID);
        Assert.assertNotNull("missing the expected catalog", server.getCatalog(catalogID));
        // should be ok to create an existing catalog
        int numCatalogs = server.listCatalogs().size();
        catalog = server.createCatalog(catalogID);
        Assert.assertEquals("expected no change in catalogs", numCatalogs, server.listCatalogs().size());
        catalog.createRepository(repoID);
        Assert.assertTrue("expected repository", catalog.hasRepository(repoID));
        catalog.deleteRepository(repoID);
        Assert.assertTrue("unexpected repository", !catalog.hasRepository(repoID));
        server.deleteCatalog(catalogID);
        Assert.assertNull("expected catalog to be deleted", server.getCatalog(catalogID));
        try {
            // catalog object shouldn't work either
            catalog.hasRepository(repoID);
            Assert.fail("expected catalog not found exception.");
        } catch (AGHttpException e) {
            // TODO: want a subclass of AGHttpException here?
        }
        // should be ok to delete a non-existent catalog
        numCatalogs = server.listCatalogs().size();
        server.deleteCatalog(catalogID);
        Assert.assertEquals("expected no change in catalogs", numCatalogs, server.listCatalogs().size());
        server.createCatalog(catalogID);
        // catalog object now works again (allow that for now. TODO: reconsider?)
        // When a catalog is deleted, repositories aren't accessible from the catalog
        // TODO: check that repositories are also deleted from disk?
        Assert.assertTrue("Expected no repositories in catalog.", !catalog.hasRepository(repoID));
        server.deleteCatalog(catalogID);
        Assert.assertNull("expected catalog to be deleted", server.getCatalog(catalogID));
        // TODO confirm that these are indeed harmless
        server.deleteCatalog("/");
        server.deleteCatalog("blahdiblah");
    }

}
