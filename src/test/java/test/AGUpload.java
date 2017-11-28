/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.FileInputStream;

public class AGUpload {

    public static final String SERVER_URL = System.getProperty("com.franz.agraph.test.serverURL", "http://localhost:10035");
    public static final String USERNAME = System.getProperty("com.franz.agraph.test.username", "test");
    public static final String PASSWORD = System.getProperty("com.franz.agraph.test.password", "xyzzy");

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: 2 args required: REPOSITORY_ID and SOURCE_FILE.");
            System.exit(1);
        }
        String REPOSITORY_ID = args[0];
        String SOURCE_FILE = args[1];

        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        AGCatalog catalog = server.getRootCatalog();
        catalog.deleteRepository(REPOSITORY_ID);
        AGRepository repo = catalog.createRepository(REPOSITORY_ID);
        try {
            repo.initialize();
            AGRepositoryConnection conn = repo.getConnection();
            try {
                System.out.println("Loading: " + SOURCE_FILE);
                long start = System.nanoTime();
                //repo.setBulkMode(true);
                conn.add(new FileInputStream(SOURCE_FILE), null, RDFFormat.NTRIPLES);
                System.out.println("Loaded: " + conn.size() + " triples in " + (System.nanoTime() - start) / 1.0e9 + " seconds.");
            } finally {
                conn.close();
            }
        } finally {
            repo.shutDown();
        }
    }

}
