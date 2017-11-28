/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.lubm;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQuery;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import org.eclipse.rdf4j.OpenRDFException;

import java.util.Calendar;

/**
 * Demonstrates the LUBM benchmark SPARQL queries.
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
public class AGLubmSparql {

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
    static final String LUBMprefix =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "PREFIX ub: <" + UBNAMESPACE + "> " +
                    "PREFIX u0d0: <http://www.Department0.University0.edu/> ";
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

    public static void doQuery1(AGRepositoryConnection conn) throws OpenRDFException {
        String q1 = LUBMprefix
                + "SELECT DISTINCT ?X "
                + "WHERE {"
                + "?X ub:takesCourse u0d0:GraduateCourse0 . "
                + "?X rdf:type ub:GraduateStudent . "
                + "}";
        doQuery(conn, 1, q1);
    }

    public static void doQuery2(AGRepositoryConnection conn) throws OpenRDFException {
        String q2 = LUBMprefix
                + "SELECT DISTINCT ?X ?Y ?Z "
                + "WHERE { "
                + "?Z rdf:type ub:Department . "
                + "?Z ub:subOrganizationOf ?Y . "
                + "?X ub:undergraduateDegreeFrom ?Y . "
                + "?X ub:memberOf ?Z . "
                + "?X rdf:type ub:GraduateStudent . "
                + "?Y rdf:type ub:University . "
                + "}";
        doQuery(conn, 2, q2);
    }

    public static void doQuery3(AGRepositoryConnection conn) throws OpenRDFException {
        String q3 = LUBMprefix
                + "SELECT DISTINCT ?X "
                + "WHERE { "
                + "?X ub:publicationAuthor u0d0:AssistantProfessor0 . "
                + "?X rdf:type ub:Publication . "
                + "}";
        doQuery(conn, 3, q3);
    }

    public static void doQuery4(AGRepositoryConnection conn) throws OpenRDFException {
        String q4 = LUBMprefix
                + "SELECT DISTINCT ?X ?Y1 ?Y2 ?Y3 "
                + "WHERE { "
                + "?X ub:worksFor <http://www.Department0.University0.edu> . "
                + "?X rdf:type ub:Professor . "
                + "?X ub:name ?Y1 . "
                + "?X ub:emailAddress ?Y2 . "
                + "?X ub:telephone ?Y3 . "
                + "}";
        doQuery(conn, 4, q4);
    }

    public static void doQuery5(AGRepositoryConnection conn) throws OpenRDFException {
        String q5 = LUBMprefix
                + "SELECT DISTINCT ?X "
                + "WHERE { "
                + "?X ub:memberOf <http://www.Department0.University0.edu> . "
                + "?X rdf:type ub:Person . "
                + "}";
        doQuery(conn, 5, q5);
    }

    public static void doQuery6(AGRepositoryConnection conn) throws OpenRDFException {
        String q6 = LUBMprefix
                + "SELECT ?X "
                + "WHERE { "
                + "?X rdf:type ub:Student . "
                + "}";
        doQuery(conn, 6, q6);
    }

    public static void doQuery7(AGRepositoryConnection conn) throws OpenRDFException {
        String q7 = LUBMprefix
                + "SELECT DISTINCT ?X ?Y "
                + "WHERE { "
                + "u0d0:AssociateProfessor0    ub:teacherOf ?Y . "
                + "?Y rdf:type ub:Course . "
                + "?X ub:takesCourse ?Y . "
                + "?X rdf:type ub:Student . "
                + "}";
        doQuery(conn, 7, q7);
    }

    public static void doQuery8(AGRepositoryConnection conn) throws OpenRDFException {
        String q8 = LUBMprefix
                + "SELECT DISTINCT ?X ?Y ?Z "
                + "WHERE { "
                + "?Y ub:subOrganizationOf <http://www.University0.edu> . "
                + "?Y rdf:type ub:Department . "
                + "?X ub:memberOf ?Y . "
                + "?X rdf:type ub:Student . "
                + "?X ub:emailAddress ?Z . "
                + "}";
        doQuery(conn, 8, q8);
    }

    public static void doQuery9(AGRepositoryConnection conn) throws OpenRDFException {
        String q9 = LUBMprefix
                + "SELECT DISTINCT ?X ?Y ?Z "
                + "WHERE { "
                + "?Y rdf:type ub:Faculty . "
                + "?Y ub:teacherOf ?Z . "
                + "?X ub:advisor ?Y . "
                + "?X ub:takesCourse ?Z . "
                + "?X rdf:type ub:Student . "
                + "?Z rdf:type ub:Course . "
                + "}";
        doQuery(conn, 9, q9);
    }

    public static void doQuery10(AGRepositoryConnection conn) throws OpenRDFException {
        String q10 = LUBMprefix
                + "SELECT DISTINCT ?X "
                + "WHERE { "
                + "?X ub:takesCourse u0d0:GraduateCourse0 . "
                + "?X rdf:type ub:Student . "
                + "}";
        doQuery(conn, 10, q10);
    }

    public static void doQuery11(AGRepositoryConnection conn) throws OpenRDFException {
        String q11 = LUBMprefix
                + "SELECT DISTINCT ?X "
                + "WHERE { "
                + "?X ub:subOrganizationOf <http://www.University0.edu> . "
                + "?X rdf:type ub:ResearchGroup . "
                + "}";
        doQuery(conn, 11, q11);
    }

    public static void doQuery12(AGRepositoryConnection conn) throws OpenRDFException {
        String q12 = LUBMprefix
                + "SELECT DISTINCT ?X ?Y "
                + "WHERE { "
                + "?Y ub:subOrganizationOf <http://www.University0.edu> . "
                + "?Y rdf:type ub:Department . "
                + "?X rdf:type ub:Chair . "
                + "?X ub:worksFor ?Y . "
                + "}";
        doQuery(conn, 12, q12);
    }

    public static void doQuery13(AGRepositoryConnection conn) throws OpenRDFException {
        String q13 = LUBMprefix
                + "SELECT DISTINCT ?X "
                + "WHERE { "
                + "<http://www.University0.edu> ub:hasAlumnus ?X . "
                + "?X rdf:type ub:Person . "
                + "}";
        doQuery(conn, 13, q13);
    }

    public static void doQuery14(AGRepositoryConnection conn) throws OpenRDFException {
        String q14 = LUBMprefix
                + "SELECT ?X "
                + "WHERE { "
                + "?X rdf:type ub:UndergraduateStudent . "
                + "}";
        doQuery(conn, 14, q14);
    }

    public static void doQuery(AGRepositoryConnection conn, int qi, String query) throws OpenRDFException {
        System.out.format("[%tT]Query %2d:", Calendar.getInstance(), qi);
        AGTupleQuery tupleQuery = conn.prepareTupleQuery(
                AGQueryLanguage.SPARQL, query);
        tupleQuery.setIncludeInferred(true);
        tupleQuery.setPlanner(AGQuery.SPARQL_IDENTITY_PLANNER);
        long begin = System.nanoTime();
        long n = tupleQuery.count();
        long delta = (System.nanoTime() - begin);
        System.out.format("%9d answers in %6d milliseconds.%n", n, (delta / 1000000));
    }
}
