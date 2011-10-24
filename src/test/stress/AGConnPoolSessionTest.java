package test.stress;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.AGAbstractTest;
import test.TestSuites;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closer;

public class AGConnPoolSessionTest extends Closer {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private static final String ns = "http://example.com/#";

    @After
    public void closeAfter() {
    	close();
    }

    @Test
    //@Category(TestSuites.Temp.class)
    public void testPlain() throws Exception {
        AGServer server = closeLater( new AGServer(AGAbstractTest.findServerUrl(), AGAbstractTest.username(), AGAbstractTest.password()));
        AGCatalog catalog = server.getCatalog("/");
        AGRepository repo = closeLater( catalog.createRepository("AGConnPoolSessionTest.testPlain"));
        AGRepositoryConnection conn = closeLater( repo.getConnection());
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.setAutoCommit(true);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
    }
    
    @Test
//    @Category(TestSuites.Temp.class)
    public void testPoolDedicated() throws Exception {
    	AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "AGConnPoolSessionTest.testPoolDedicated",
				AGConnProp.session, AGConnProp.Session.DEDICATED,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(5),
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
		));
        AGRepositoryConnection conn = closeLater( pool.borrowConnection());
        Assert.assertTrue(conn.getHttpRepoClient().isSession());
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
    }

    @Test
//    @Category(TestSuites.Temp.class)
    public void testPoolTx() throws Exception {
    	AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "AGConnPoolSessionTest.testPoolTx",
				AGConnProp.session, AGConnProp.Session.TX,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(5),
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
		));
        AGRepositoryConnection conn = closeLater( pool.borrowConnection());
        Assert.assertTrue(conn.getHttpRepoClient().isSession());
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
    }

    @Test
    @Category(TestSuites.Temp.class)
    public void deleteDatatypeMapping() throws Exception {
    	final int NUM = 40;
    	final AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "AGConnPoolSessionTest.deleteDatatypeMapping",
				AGConnProp.session, AGConnProp.Session.SHARED,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(5),
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
		));
    	{
            AGRepositoryConnection conn = closeLater( pool.borrowConnection());
            AGValueFactory vf = conn.getValueFactory();
            for (int i = 0; i < NUM; i++) {
             	conn.add(new File("src/tutorial/java-kennedy.ntriples"), ns, RDFFormat.NTRIPLES, vf.createURI(ns + i));
			}
         	close(conn);
    	}
        ExecutorService exec = Executors.newFixedThreadPool(NUM);
        final List<Throwable> errors = new ArrayList<Throwable>();
        for (int i = 0; i < NUM; i++) {
        	exec.execute(new Runnable() {
        		public void run() {
        			try {
        				AGRepositoryConnection conn = pool.borrowConnection();
        				try {
        			        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        			        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        				} finally {
        					conn.close();
        				}
        			} catch (Throwable e) {
        				errors.add(e);
        			}
        		}
        	});
        }
        exec.awaitTermination(30, TimeUnit.SECONDS);
        Assert.assertEquals(pool.toString(), 0, pool.getNumActive());
        if (!errors.isEmpty()) {
        	for (Throwable e : errors) {
        		log.error("error", e);
        	}
        	Assert.fail("see log for details: " + errors.toString());
        }
    }

}
