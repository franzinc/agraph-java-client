/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.rdf4j.repository.RepositoryException;

// Contains logic common to multiple Jena-based tests
// Should be used as a TestSetup fixture returned by the
// suite method of each test class.
public class JenaUtil extends TestSetup {
    private static final String SERVER_URL = AGAbstractTest.findServerUrl();
    private static final String CATALOG_ID = AGAbstractTest.CATALOG_ID;
    private static final String REPOSITORY_ID = AGAbstractTest.REPO_ID;
    private static final String USERNAME = AGAbstractTest.username();
    private static final String PASSWORD = AGAbstractTest.password();

    // Initialized lazily
    private AGRepositoryConnection conn = null;
    private AGGraphMaker maker = null;

    public JenaUtil(final Class<? extends Test> test) {
        super(new TestSuite(test));
    }

    private void connect() {
        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        try {
            catalog.deleteRepository(REPOSITORY_ID);
            AGRepository repo = catalog.createRepository(REPOSITORY_ID);
            repo.initialize();
            conn = repo.getConnection();
            maker = new AGGraphMaker(conn);
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to connect", e);
        }
    }

    /**
     * Releases resources.
     */
    public void disconnect() {
        if (conn != null) {
            maker.close();
            try {
                conn.close();
            } catch (RepositoryException e) {
                throw new RuntimeException("Unable to close connection.", e);
            }
            conn = null;
            maker = null;
        }
    }

    /**
     * Gets a connection to the test repository and clears it.
     *
     * @return A connection object.
     */
    public AGRepositoryConnection getConn() {
        if (conn == null) {
            connect();
        }
        try {
            conn.clear();
            conn.clearNamespaces();
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to clear connection.", e);
        }
        return conn;
    }

    /**
     * Gets the graph maker to be used during tests.
     *
     * @return A graph maker instance.
     */
    public AGGraphMaker getMaker() {
        if (maker == null) {
            connect();
        }
        return maker;
    }

    @Override
    protected void tearDown() throws Exception {
        disconnect();
        super.tearDown();
    }
}
