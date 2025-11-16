package test.rdf4j;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import org.eclipse.rdf4j.repository.Repository;
import test.AGAbstractTest;

public class RDF4JTestsHelper {

    private static final String CATALOG_ID = "java-client-test";
    private static final String REPO_ID = "rdf4j";

    private static AGServer server;
    private static AGRepository repo;

    private static AGServer getServer() {
        if (server == null) {
            server = AGAbstractTest.newAGServer();
        }
        return server;
    }

    private static AGRepository getRepo() {
        if (repo == null) {
            repo = getServer()
                    .createCatalog(CATALOG_ID)
                    .createRepository(REPO_ID);
            repo.init();
        }
        return repo;
    }

    public static Repository getTestRepository() {
        return getRepo();
    }

    public static void clearTestRepository() {
        try (AGRepositoryConnection conn = getRepo().getConnection()) {
            conn.begin();
            conn.clear();
            conn.commit();
        }
    }

    public static AGServer getTestServer() {
        return getServer();
    }
}
