package test;

import org.openrdf.OpenRDFException;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;

/**
 * Demonstrates the LUBM benchmark Prolog queries on an AGRepository previously
 * loaded with LUBM data.
 * 
 * Note that RDFS++ reasoning must be enabled to obtain the correct answers.
 */
public class AGLubmProlog {

	public static String SERVER_URL = System.getProperty(
			"com.franz.agraph.test.serverURL", "http://localhost:8080");
	public static String CATALOG_ID = System.getProperty(
			"com.franz.agraph.test.catalogID", "/");
	public static String REPOSITORY_ID = System.getProperty(
			"com.franz.agraph.test.repositoryID", "LUBM-50");
	public static String USERNAME = System.getProperty(
			"com.franz.agraph.test.username", "test");
	public static String PASSWORD = System.getProperty(
			"com.franz.agraph.test.password", "xyzzy");

	public static String ubnamespace = "http://www.lehigh.edu/%7Ezhp2/2004/0401/univ-bench.owl#";

	public static void main(String[] args) throws OpenRDFException {

		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getRootCatalog();
		AGRepository repo = catalog.createRepository(REPOSITORY_ID);
		repo.initialize();
		AGRepositoryConnection conn = repo.getConnection();
		System.out.println(repo.getRepositoryURL() + ": " + conn.size()
				+ " triples.");

		// Register namespaces used in queries
		conn.setNamespace("ub", ubnamespace);
		conn.setNamespace("u0d0", "http://www.Department0.University0.edu/");

		// Add Prolog rule needed for query 12.
		String rule = "(<-- (Chair ?x1)" + "(q ?x1 !ub:headOf ?y)"
				+ "(q ?x1 !rdf:type !ub:Person)"
				+ "(q ?y !rdf:type !ub:Department))";
		conn.addRules(rule);

		// Run the LUBM queries with RDFS++ reasoning enabled
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

		conn.close();
		repo.shutDown();
	}

	public static void doQuery1(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q ?x !ub:takesCourse !u0d0:GraduateCourse0)"
				+ "(q ?x !rdf:type !ub:GraduateStudent)" + ")";
		doQuery(ts, 1, query);
	}

	public static void doQuery2(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x ?y ?z) "
				+ "(q ?z !rdf:type !ub:Department)"
				+ "(q ?z !ub:subOrganizationOf ?y)"
				+ "(q ?x !ub:undergraduateDegreeFrom ?y)"
				+ "(q ?x !ub:memberOf ?z)"
				+ "(q ?x !rdf:type !ub:GraduateStudent)"
				+ "(q ?y !rdf:type !ub:University)" + ")";
		doQuery(ts, 2, query);
	}

	public static void doQuery3(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q ?x !ub:publicationAuthor !u0d0:AssistantProfessor0)"
				+ "(q ?x !rdf:type !ub:Publication)" + ")";
		doQuery(ts, 3, query);
	}

	public static void doQuery4(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x ?name ?email ?telephone) "
				+ "(q ?x !ub:worksFor !<http://www.Department0.University0.edu>)"
				+ "(q ?x !rdf:type !ub:Professor)" + "(q ?x !ub:name ?name)"
				+ "(q ?x !ub:emailAddress ?email)"
				+ "(q ?x !ub:telephone ?telephone)" + ")";
		doQuery(ts, 4, query);
	}

	public static void doQuery5(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q ?x !ub:memberOf !<http://www.Department0.University0.edu>)"
				+ "(q ?x !rdf:type !ub:Person)" + ")";
		doQuery(ts, 5, query);
	}

	public static void doQuery6(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) " + "(q ?x !rdf:type !ub:Student)" + ")";
		doQuery(ts, 6, query);
	}

	public static void doQuery7(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x ?y) "
				+ "(q !u0d0:AssociateProfessor0 !ub:teacherOf ?y)"
				+ "(q ?y !rdf:type !ub:Course)" + "(q ?x !ub:takesCourse ?y)"
				+ "(q ?x !rdf:type !ub:Student)" + ")";
		doQuery(ts, 7, query);
	}

	public static void doQuery8(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?y ?x ?email) "
				+ "(q ?y !ub:subOrganizationOf !<http://www.University0.edu>)"
				+ "(q ?y !rdf:type !ub:Department)" + "(q ?x !ub:memberOf ?y)"
				+ "(q ?x !rdf:type !ub:Student)"
				+ "(q ?x !ub:emailAddress ?email)" + ")";
		doQuery(ts, 8, query);
	}

	public static void doQuery9(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x ?y ?z) " + "(q ?x !ub:advisor ?y)"
				+ "(q ?x !ub:takesCourse ?z)" + "(q ?y !ub:teacherOf ?z)"
				+ "(q ?x !rdf:type !ub:Student)"
				+ "(q ?y !rdf:type !ub:Faculty)"
				+ "(q ?z !rdf:type !ub:Course)" + ")";
		doQuery(ts, 9, query);
	}

	public static void doQuery10(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q ?x !ub:takesCourse !u0d0:GraduateCourse0)"
				+ "(q ?x !rdf:type !ub:Student)" + ")";
		doQuery(ts, 10, query);
	}

	public static void doQuery11(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q ?x !ub:subOrganizationOf !<http://www.University0.edu>)"
				+ "(q ?x !rdf:type !ub:ResearchGroup)" + ")";
		doQuery(ts, 11, query);
	}

	public static void doQuery12(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x ?y) "
				+ "(q ?y !ub:subOrganizationOf !<http://www.University0.edu>)"
				+ "(q ?y !rdf:type !ub:Department)" + "(q ?x !ub:memberOf ?y)"
				+ "(Chair ?x)" + ")";
		doQuery(ts, 12, query);
	}

	public static void doQuery13(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q !<http://www.University0.edu> !ub:hasAlumnus ?x)"
				+ "(q ?x !rdf:type !ub:Person)" + ")";
		doQuery(ts, 13, query);
	}

	public static void doQuery14(AGRepositoryConnection ts)
			throws OpenRDFException {
		String query = "(select0 (?x) "
				+ "(q ?x !rdf:type !ub:UndergraduateStudent)" + ")";
		doQuery(ts, 14, query);
	}

	public static void doQuery(AGRepositoryConnection conn, int qi, String query) throws OpenRDFException {
		System.out.format("Query %2d:", qi);
		AGTupleQuery tupleQuery = conn.prepareTupleQuery(
				AGQueryLanguage.PROLOG, query);
		tupleQuery.setIncludeInferred(true);
		long begin = System.nanoTime();
		long n = tupleQuery.count();
		long delta = (System.nanoTime() - begin);
		System.out.format("%7d answers in %6d milliseconds.%n", n, (delta / 1000000));
	}
}
