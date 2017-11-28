/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.lubm;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import org.eclipse.rdf4j.OpenRDFException;

import java.util.Calendar;

/**
 * Demonstrates the LUBM benchmark Prolog Select queries.
 * <p>
 * The program looks for the following properties (default values shown):
 * <p>
 * -Dcom.franz.agraph.serverURL=http://localhost:10035
 * -Dcom.franz.agraph.catalogID=/
 * -Dcom.franz.agraph.repositoryID=LUBM-50
 * -Dcom.franz.agraph.username=test
 * -Dcom.franz.agraph.password=xyzzy
 * -Dcom.franz.agraph.lubm.ubnamespace="http://www.lehigh.edu/%7Ezhp2/2004/0401/univ-bench.owl#"
 * -Dcom.franz.agraph.lubm.iterations=3
 * <p>
 * This would run 3 iterations of all 14 LUBM queries against a
 * repository 'LUBM-50' in the server's root catalog.
 */
public class AGLubmProlog {

    // Edit the following defaults for your local configuration
    public static String SERVER_URL = System.getProperty(
            "com.franz.agraph.serverURL", "http://localhost:10035");
    public static String CATALOG_ID = System.getProperty(
            "com.franz.agraph.catalogID", "/"); // this is the root catalog
    public static String REPOSITORY_ID = System.getProperty(
            "com.franz.agraph.repositoryID", "LUBM-50");
    public static String USERNAME = System.getProperty(
            "com.franz.agraph.username", "test");
    public static String PASSWORD = System.getProperty(
            "com.franz.agraph.password", "xyzzy");
    public static String UBNAMESPACE = System.getProperty(
            "com.franz.agraph.lubm.ubnamespace",
            "http://www.lehigh.edu/%7Ezhp2/2004/0401/univ-bench.owl#");
    public static int ITERATIONS = Integer.parseInt(System.getProperty(
            "com.franz.agraph.lubm.iterations", "3"));

    public static void main(String[] args) throws OpenRDFException {

        AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        if (!server.listCatalogs().contains(CATALOG_ID)) {
            System.out.println("No catalog '" + CATALOG_ID + "' found.");
            System.exit(1);
        }
        AGCatalog catalog = server.getCatalog(CATALOG_ID);
        if (!catalog.listRepositories().contains(REPOSITORY_ID)) {
            System.out.println("No repository '" + REPOSITORY_ID
                    + "' found in catalog " + CATALOG_ID);
            System.exit(1);
        }
        AGRepository repo = catalog.createRepository(REPOSITORY_ID);
        repo.initialize();
        AGRepositoryConnection conn = repo.getConnection();
        System.out.println("Connected to " + repo.getRepositoryURL() + ": "
                + conn.size() + " triples.");

        // Register namespaces used in queries
        conn.setNamespace("ub", UBNAMESPACE);
        conn.setNamespace("u0d0", "http://www.Department0.University0.edu/");

        // Add Prolog rule needed for query 12.
        String rule = "(<-- (Chair ?x1)" + "(q ?x1 !ub:headOf ?y)"
                + "(q ?x1 !rdf:type !ub:Person)"
                + "(q ?y !rdf:type !ub:Department))";
        conn.addRules(rule);

        // Run the LUBM queries with RDFS++ reasoning enabled
        for (int iter = 1; iter <= ITERATIONS; iter++) {
            System.out.println("\nIteration " + iter + ":");
            doQuery1(conn);
            doQuery2(conn);
            doQuery3(conn);
            doQuery4(conn);
            doQuery5(conn);
            doQuery6(conn);
            doQuery7(conn);
            doQuery8(conn);
            doQuery9(conn);
            doQuery10(conn);
            doQuery11(conn);
            doQuery12(conn);
            doQuery13(conn);
            doQuery14(conn);
        }

        conn.close();
        repo.shutDown();
    }

    public static void doQuery1(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:count-only t)"
                + "(q ?x !ub:takesCourse !u0d0:GraduateCourse0)"
                + "(q ?x !rdf:type !ub:GraduateStudent)" + ")";
        doQuery(ts, 1, query);
    }

    public static void doQuery2(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x ?y ?z) "
                + "(:count-only t)"
                + "(:distinct t) "
                + "(:reorder nil)"
                + "(q- ?z !rdf:type !ub:Department)"
                + "(q- ?z !ub:subOrganizationOf ?y)"
                + "(q- ?x !ub:undergraduateDegreeFrom ?y)"
                + "(q- ?x !ub:memberOf ?z)"
                + "(q- ?x !rdf:type !ub:GraduateStudent)"
                + "(q- ?y !rdf:type !ub:University)" + ")";
        doQuery(ts, 2, query);
    }

    public static void doQuery3(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:count-only t)"
                + "(q ?x !ub:publicationAuthor !u0d0:AssistantProfessor0)"
                + "(q ?x !rdf:type !ub:Publication)" + ")";
        doQuery(ts, 3, query);
    }

    public static void doQuery4(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x ?name ?email ?telephone) "
                + "(:count-only t)"
                + "(q ?x !ub:worksFor !<http://www.Department0.University0.edu>)"
                + "(q ?x !rdf:type !ub:Professor)" + "(q ?x !ub:name ?name)"
                + "(q ?x !ub:emailAddress ?email)"
                + "(q ?x !ub:telephone ?telephone)" + ")";
        doQuery(ts, 4, query);
    }

    public static void doQuery5(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:count-only t)"
                + "(q ?x !ub:memberOf !<http://www.Department0.University0.edu>)"
                + "(q ?x !rdf:type !ub:Person)" + ")";
        doQuery(ts, 5, query);
    }

    public static void doQuery6(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:distinct t) "
                + "(:count-only t)"
                + "(q ?x !rdf:type !ub:Student)" + ")";
        doQuery(ts, 6, query);
    }

    public static void doQuery7(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x ?y) "
                + "(:count-only t)"
                + "(q !u0d0:AssociateProfessor0 !ub:teacherOf ?y)"
                + "(q ?y !rdf:type !ub:Course)" + "(q ?x !ub:takesCourse ?y)"
                + "(q ?x !rdf:type !ub:Student)" + ")";
        doQuery(ts, 7, query);
    }

    public static void doQuery8(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?y ?x ?email) "
                + "(:count-only t)"
                + "(q ?y !ub:subOrganizationOf !<http://www.University0.edu>)"
                + "(q ?y !rdf:type !ub:Department)" + "(q ?x !ub:memberOf ?y)"
                + "(q ?x !rdf:type !ub:Student)"
                + "(q ?x !ub:emailAddress ?email)" + ")";
        doQuery(ts, 8, query);
    }

    public static void doQuery9(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x ?y ?z) "
                + "(:count-only t)"
                + "(q ?x !ub:advisor ?y)"
                + "(q ?x !ub:takesCourse ?z)" + "(q ?y !ub:teacherOf ?z)"
                + "(q ?x !rdf:type !ub:Student)"
                + "(q ?y !rdf:type !ub:Faculty)"
                + "(q ?z !rdf:type !ub:Course)" + ")";
        doQuery(ts, 9, query);
    }

    public static void doQuery10(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:count-only t)"
                + "(q ?x !ub:takesCourse !u0d0:GraduateCourse0)"
                + "(q ?x !rdf:type !ub:Student)" + ")";
        doQuery(ts, 10, query);
    }

    public static void doQuery11(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:count-only t)"
                + "(q ?x !ub:subOrganizationOf !<http://www.University0.edu>)"
                + "(q ?x !rdf:type !ub:ResearchGroup)" + ")";
        doQuery(ts, 11, query);
    }

    public static void doQuery12(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x ?y) "
                + "(:count-only t)"
                + "(q ?y !ub:subOrganizationOf !<http://www.University0.edu>)"
                + "(q ?y !rdf:type !ub:Department)" + "(q ?x !ub:memberOf ?y)"
                + "(Chair ?x)" + ")";
        doQuery(ts, 12, query);
    }

    public static void doQuery13(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:count-only t)"
                + "(q !<http://www.University0.edu> !ub:hasAlumnus ?x)"
                + "(q ?x !rdf:type !ub:Person)" + ")";
        doQuery(ts, 13, query);
    }

    public static void doQuery14(AGRepositoryConnection ts)
            throws OpenRDFException {
        String query = "(select0 (?x) "
                + "(:distinct t) "
                + "(:count-only t)"
                + "(q ?x !rdf:type !ub:UndergraduateStudent)" + ")";
        doQuery(ts, 14, query);
    }

    public static void doQuery(AGRepositoryConnection conn, int qi, String query)
            throws OpenRDFException {
        System.out.format("[%tT]Query %2d:", Calendar.getInstance(), qi);
        AGTupleQuery tupleQuery = conn.prepareTupleQuery(
                AGQueryLanguage.PROLOG, query);
        long begin = System.nanoTime();
        tupleQuery.setIncludeInferred(true);
        long n = tupleQuery.count();
        long delta = (System.nanoTime() - begin);
        System.out.format("%9d answers in %6d milliseconds.%n", n,
                (delta / 1000000));
    }
}
