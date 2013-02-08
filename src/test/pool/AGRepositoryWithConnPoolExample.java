package test.pool;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.openrdf.model.vocabulary.OWL;

import test.AGAbstractTest;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

/**
 * Shows how a Sesame app can configure an AGRepository to
 * benefit transparently from connection pooling.
 * 
 * See the tests referenced below for more details and examples
 * involving multiple threads.
 * 
 * @see AGConnPoolSessionTest
 * @see AGConnPoolCloseTest
 */
public class AGRepositoryWithConnPoolExample {

	@Test
	public void testRepositoryWithConnPool() throws Exception {
		String myServerUrl = AGAbstractTest.findServerUrl(); // "http://localhost:10035";
		String myUsername = AGAbstractTest.username(); // "test";
		String myPassword = AGAbstractTest.password(); // "xyzzy";
		String myCatalog = AGAbstractTest.CATALOG_ID; // "java-catalog";
		String myRepo = AGAbstractTest.REPO_ID; // "javatest";
		AGConnPool pool = AGConnPool.create(
				AGConnProp.serverUrl, myServerUrl,
				AGConnProp.username, myUsername,
				AGConnProp.password, myPassword,
				AGConnProp.catalog, myCatalog,
				AGConnProp.repository, myRepo,
				// The above values must match the repo defined below;
				// that redundancy should go away in a future release,
				// as part of rfe11963.
				AGConnProp.session,	AGConnProp.Session.TX, 
				AGConnProp.sessionLifetime,	TimeUnit.MINUTES.toSeconds(1),
				AGPoolProp.testOnBorrow, true,
				AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30));
		AGServer server = new AGServer(myServerUrl, myUsername, myPassword);
		try {
			AGCatalog catalog = server.getCatalog(myCatalog);
			AGRepository repo = catalog.createRepository(myRepo);
			repo.setConnPool(pool);
			repo.initialize();
			try {
				AGRepositoryConnection conn = repo.getConnection(); // borrows a connection
				try {
					// do transactional work on the connection ...
					conn.add(OWL.INVERSEOF, OWL.INVERSEOF, OWL.INVERSEOF); // :)
					conn.commit();
				} finally {
					conn.close(); // returns connection to the pool
				}
			} finally {
				repo.shutDown(); // closes the pool
			}
		} finally {
			server.close();
		}
	}
}
