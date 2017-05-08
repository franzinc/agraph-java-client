/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package test;

import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.*;
import com.franz.util.Closer;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static test.Util.*;

public class AGAbstractTest {

    static public final String CATALOG_ID = "java-catalog";
    static public final String REPO_ID = "javatest";

    public Logger log = LoggerFactory.getLogger(this.getClass());
    
    protected static AGServer server;
    protected static AGCatalog cat;
    protected AGRepository repo;
    protected AGRepositoryConnection conn;
    
    protected AGValueFactory vf;
    
    private static String serverUrl;
    private static String sslServerUrl;
    private static AGRepository sharedRepo = null;
    
    private String testName = null;

	protected Closer closer = new Closer();

    public static String findServerUrl() {
    	if (serverUrl == null) {
    		serverUrl = findServerUrl1();
    	}
    	return serverUrl;
    }

    public static String findSslServerUrl() {
    	if (sslServerUrl == null) {
    		sslServerUrl = findSslServerUrl1();
    	}
    	return sslServerUrl;
    }
    
	private static String findServerUrl1() {
		String host = coalesce(ifBlank(System.getenv("AGRAPH_HOST"), null),
				ifBlank(System.getProperty("AGRAPH_HOST"), null));
		String port = coalesce(ifBlank(System.getenv("AGRAPH_PORT"), null),
				ifBlank(System.getProperty("AGRAPH_PORT"), null));
		
		if ((host == null || host.equals("localhost")) && port == null) {
			File portFile = new File("../agraph/lisp/agraph.port");
			try {
				host = "localhost";
				if (portFile.exists()) {
					System.out.println("Reading agraph.port: " + portFile.getAbsolutePath());
					port = FileUtils.readFileToString(portFile).trim();
				} else {
					port = "10035";
				}
			} catch (Exception e) {
				throw new RuntimeException("Trying to read PortFile: " + portFile.getAbsolutePath(), e);
			}
		}
		
		return "http://" + coalesce(host, "localhost") + ":" + coalesce(port, "10035");
	}
    
	private static String findSslServerUrl1() {
		String host = coalesce(ifBlank(System.getenv("AGRAPH_SSLHOST"), null),
				ifBlank(System.getProperty("AGRAPH_SSLHOST"), null));
		String port = coalesce(ifBlank(System.getenv("AGRAPH_SSLPORT"), null),
				ifBlank(System.getProperty("AGRAPH_SSLPORT"), null));
		
		if ((host == null || host.equals("localhost")) && port == null) {
			File portFile = new File("../agraph/lisp/agraph.sslport");
			try {
				host = "localhost";
				if (portFile.exists()) {
					System.out.println("Reading agraph.sslport: " + portFile.getAbsolutePath());
					port = FileUtils.readFileToString(portFile).trim();
				} else {
					port = "10036";
				}
			} catch (Exception e) {
				throw new RuntimeException("Trying to read SSLPortFile: " + portFile.getAbsolutePath(), e);
			}
		}
		
		return "https://" + coalesce(host, "localhost") + ":" + coalesce(port, "10036");
	}
	
    public static String username() {
        return coalesce(System.getenv("AGRAPH_USER"), "test");
    }
    
    public static String password() {
        return coalesce(System.getenv("AGRAPH_PASSWORD"), "xyzzy");
    }

    public static AGServer newAGServer() {
    	String url = findServerUrl();
        try {
        	return new AGServer(url, username(), password());
		} catch (Exception e) {
			throw new RuntimeException("server url: " + url, e);
		}
    }
    
    /**
     * Returns a shared repository to use for testing purposes.
     * 
     * The shared repository is deleted/created on first use, and is
     * simply cleared on subsequent uses; this speeds up testing as
     * deleting/creating a new repository can take some time.
     * 
     * @return
     * @throws RepositoryException
     */
    public static AGRepository sharedRepository() throws RepositoryException {
    	if (sharedRepo==null) {
    		AGCatalog cat = newAGServer().getCatalog(CATALOG_ID);
    		cat.deleteRepository(REPO_ID);
    		sharedRepo = cat.createRepository(REPO_ID);
    	} else {
   			AGRepositoryConnection conn = sharedRepo.getConnection();
   			conn.clear();
   			conn.clearNamespaces();
    	}
    	return sharedRepo;
    }
    
    @BeforeClass
    public static void setUpOnce() {
        server = newAGServer();
        try {
            cat = server.getCatalog(CATALOG_ID);
			cat.deleteRepository(REPO_ID);

	        // test connection once
	        ping();
		} catch (Exception e) {
			throw new RuntimeException("server url: " + server.getServerURL(), e);
		}
    }
    
    public static void deleteRepository(String catalog, String repo) throws RepositoryException {
    	try (AGServer server = new AGServer(findServerUrl(), username(), password()) ){
            AGCatalog cat = server.getCatalog(catalog);
    		cat.deleteRepository(repo);
    	}
    }

	private static void ping() throws RepositoryException {
        try (AGRepository repo = cat.createRepository(REPO_ID)) {
            repo.initialize();
            try (AGRepositoryConnection conn = repo.getConnection()) {
                conn.ping();
            }
        }
	}
	
	public static Map<String, String> processes(AGServer server) throws AGHttpException {
		String url = server.getServerURL() + "/" + AGProtocol.PROCESSES;
		Map<String, String> map = new HashMap<String, String>();
		try (TupleQueryResult results = server.getHTTPClient().getTupleQueryResult(url)) {
			while (results.hasNext()) {
				BindingSet bindingSet = results.next();
				Value id = bindingSet.getValue("pid");
				Value name = bindingSet.getValue("name");
				map.put(id.stringValue(), name.stringValue());
			}
		} catch (QueryEvaluationException e) {
			throw new AGHttpException(e);
		}
		return map;
	}

	/** Asks server for a list of active sessions.
	 * @param server
	 * @return a map of uri -> description pairs (both strings). 
	 * @throws AGHttpException
	 */
	public static Map<String, String> sessions(AGServer server) throws AGHttpException {
		String url = server.getServerURL() + "/" + AGProtocol.SESSION;
		Map<String, String> map = new HashMap<String, String>();
		try (TupleQueryResult results = server.getHTTPClient().getTupleQueryResult(url)) {
			while (results.hasNext()) {
				BindingSet bindingSet = results.next();
				Value k = bindingSet.getValue("uri");
				Value v = bindingSet.getValue("description");
				map.put(k.stringValue(), v.stringValue());
			}
		} catch (QueryEvaluationException e) {
			throw new AGHttpException(e);
		}
		return map;
	}

	public void setTestName(String testName) {
		this.testName = testName;
		log = LoggerFactory.getLogger(this.getClass().getName() + "." + testName);
	}
	
	public String getTestName() {
		return testName;
	}
    
    @Before
    public void setUp() throws Exception {
//		try {
//			cat.deleteRepository(REPO_ID);
//		} catch (final RepositoryException e) {
//			// Ignore - it's probably the first test.
//		}
		repo = cat.createRepository(REPO_ID);
        repo.initialize();
        vf = repo.getValueFactory();
        conn = getConnection();
        conn.deleteStaticAttributeFilter();
        conn.clear();
        conn.clearAttributes();
        conn.clearMappings();
        conn.clearNamespaces();
        // these are the default namespaces in AG, which are not present after clearNamespaces:
        conn.setNamespace("fti", "http://franz.com/ns/allegrograph/2.2/textindex/");
        conn.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }
    
    @After
    public void tearDown() throws Exception {
		closer.close();
		conn.close();
		repo.close();
        vf = null;
        conn = null;
        repo = null;
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        server.close();
    }

	/**
	 * Causes close() to be called on an object after the test.
	 *
	 * @param closeable Object to be closed after the current test.
	 * @param <T> the type of the argument.
	 * @return the argument (for chaining).
	 */
	protected<T extends AutoCloseable> T closeLater(final T closeable) {
		closer.closeLater(closeable);
		return closeable;
	}

	// Special version, because Jena developers are ... different:
	// https://github.com/apache/jena/pull/133
	protected StmtIterator closeLater(final StmtIterator iter) {
		closeLater(iter::close);
		return iter;
	}

    AGRepositoryConnection getConnection() throws RepositoryException {
        return getConnection(repo);
   }
   
    AGRepositoryConnection getConnection(AGAbstractRepository repo) throws RepositoryException {
        return repo.getConnection();
   }
   
    public static void assertSetsEqual(Collection expected, Set actual) {
        assertSetsEqual("", expected, actual);
    }
    
    public static void assertSetsEqual(String msg, byte[] expected, byte[] actual) {
        assertSetsEqual(msg, Util.toList(expected), Util.toList(actual));
    }

    public static void assertSetsEqual(String msg, Collection expected, Collection actual) {
        expected = new ArrayList(expected);
        actual = new ArrayList(actual);
        assertEquals(msg, expected.size(), actual.size());
        for (Iterator iter = expected.iterator(); iter.hasNext();) {
            Object exp = iter.next();
            boolean found = false;
            for (Iterator ait = actual.iterator(); ait.hasNext();) {
                Object act =ait.next();
                if (exp.equals(act)) {
                    found = true;
                    ait.remove();
                    break;
                }
            }
            assertTrue(msg + ". Not found: " + exp + " in " + actual, found);
        }
        assertEquals(msg + ". Remaining: " + actual, 0, actual.size());
    }
    
    public static void assertEqualsDeep(String msg, Object expected, Object actual) {
    	if (expected == null) {
    		Assert.assertEquals(msg, expected, actual);
    	} else if (actual == null) {
    		Assert.assertEquals(msg, expected, actual);
    	} else if (expected instanceof List) {
    		List expList = (List) expected;
    		Assert.assertTrue(msg + "; expected Collection type, actual: " + actual.getClass(), actual instanceof List);
    		List actList = (List) actual;
    		Assert.assertTrue(msg + "; expected same size=" + expList.size() + ", actual=" + actList.size(),
    				expList.size() == actList.size());
    		for (int i = 0; i < expList.size(); i++) {
				assertEqualsDeep("[" + i +"]" + msg, expList.get(i), actList.get(i));
			}
    	} else if (expected instanceof Object[]) {
    		Object[] expList = (Object[]) expected;
    		Assert.assertTrue(msg + "; expected Object[] type, actual: " + actual.getClass(), actual instanceof Object[]);
    		Object[] actList = (Object[]) actual;
    		Assert.assertTrue(msg + "; expected same size=" + expList.length + ", actual=" + actList.length,
    				expList.length == actList.length);
    		for (int i = 0; i < expList.length; i++) {
				assertEqualsDeep("[" + i +"]" + msg, expList[i], actList[i]);
			}
    	} else if (expected instanceof byte[]) {
    		byte[] expList = (byte[]) expected;
    		Assert.assertTrue(msg + "; expected byte[] type, actual: " + actual.getClass(), actual instanceof byte[]);
    		byte[] actList = (byte[]) actual;
    		Assert.assertTrue(msg + "; expected same size=" + expList.length + ", actual=" + actList.length,
    				expList.length == actList.length);
    		for (int i = 0; i < expList.length; i++) {
				assertEqualsDeep("[" + i +"]" + msg, expList[i], actList[i]);
			}
    	} else if (expected instanceof Set) {
    		assertSetsEqual(msg, (Set) expected, (Collection) actual);
    	} else {
    		assertEquals(msg, expected, actual);
    	}
    }
    
    public static void assertSetsSome(String msg, Collection expected, Collection actual) {
        for (Iterator ait = actual.iterator(); ait.hasNext();) {
            Object act = ait.next();
            boolean found = false;
            for (Iterator iter = expected.iterator(); iter.hasNext();) {
                Object exp =iter.next();
                if (exp.equals(act)) {
                    found = true;
                    break;
                }
            }
            assertTrue(msg + "; unexpected: " + act, found);
        }
    }
    
    public static void assertFiles(File expected, File actual) throws Exception {
        assertSetsEqual("diff " + expected.getCanonicalPath() + " " + actual.getCanonicalPath(),
            stripBlankNodes(FileUtils.readLines(expected, StandardCharsets.UTF_8)),
            stripBlankNodes(FileUtils.readLines(actual, StandardCharsets.UTF_8)));
    }
    
    private static List<String> stripBlankNodes(List<String> strings) {
        List<String> ret = new ArrayList<String>(strings.size());
        for (String str : strings) {
            ret.add(str.replaceAll("b........x.", "b00000000x0"));
        }
        return ret;
    }

    public static Map mapKeep(Object[] keys, Map map) {
        Map ret = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            ret.put(keys[i], map.get(keys[i]));
        }
        return ret;
    }
    
    public void addAll(Collection stmts, RepositoryConnection conn) throws RepositoryException {
        for (Iterator iter = stmts.iterator(); iter.hasNext();) {
            Statement st = (Statement) iter.next();
            conn.add(st);
        }
    }

    public static String readFileAsString(String filePath)
			throws java.io.IOException {
		return new String(Files.readAllBytes(Paths.get(filePath)));
	}

	public static String readResourceAsString(final String path)
			throws java.io.IOException {
		try (final InputStream input = AGAbstractTest.class.getResourceAsStream(path)) {
			return IOUtils.toString(input, "utf-8");
		}
	}

	public static void println(Object x) {
    	System.out.println(x);
    }
    
    public static void printRows(CloseableIteration rows) throws Exception {
		// We need to declare a variable. Ugh.
		// https://bugs.openjdk.java.net/browse/JDK-8068948
        try (CloseableIteration ignored = rows) {
			while (rows.hasNext()) {
				println(rows.next());
			}
		}
    }

    public static void printRows(String headerMsg, int limit, CloseableIteration rows) throws Exception {
        println(headerMsg);
		int count = 0;
		// We need to declare a variable. Ugh.
		// https://bugs.openjdk.java.net/browse/JDK-8068948
		try (CloseableIteration ignored = rows) {
			while (count < limit && rows.hasNext()) {
				println(rows.next());
				count++;
			}
		}
        println("Number of results: " + count);
    }

}
