/******************************************************************************
** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.tutorial.attributes;

import java.util.List;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGQuery;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.agraph.repository.AGRepositoryConnection.AttributeDefinition;
import com.franz.agraph.repository.UserAttributesContext;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;

/**
 * A sample class demonstrating the capabilities of AllegroGraph Triple
 * Attributes. Specifically, we demonstrate how Triple Attributes can be used as
 * a mechanism for access control.
 * 
 */
public class AttributesExample {
    private static final String HOST = getenv("AGRAPH_HOST", "localhost");
    private static final String PORT = getenv("AGRAPH_PORT", "10035");
    
    private static final String SERVER_URL = "http://" + HOST + ":" + PORT;
    private static final String CATALOG_ID = "java-catalog";
    private static final String REPOSITORY_ID = "attributesExample";
    private static final String USERNAME = getenv("AGRAPH_USER", "test");
    private static final String PASSWORD = getenv("AGRAPH_PASS", "xyzzy");

    private static AGServer server;
    private static AGCatalog catalog;
    private static AGRepository repo;
    private static AGRepositoryConnection conn;

    // Values filled in by populateRepository(), and used by main()
    private static BNode s;
    private static String ex;
    private static IRI pName;
    private static IRI pSalary;
    private static IRI pDept;
    private static IRI pInfractions;

    /**
     * 
     * @param name  Name of the variable
     * @param defaultValue  Value to be returned if the variable is not defined
     * @return the value of the environment variable or defaultValue if unset
     */
    private static String getenv(final String name, final String defaultValue) {
    final String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * A simple object printer.
     * 
     * @param x  The object to print
     * @return void
     */
    private static void println(Object x) {
        System.out.println(x);
    }

    /**
     * A simple row printer for outputting query results.
     * 
     * @param headerMsg  a string to print before printing rows
     * @param rows  The rows to print
     * @return void
     * @throws Exception
     */
    private static void printRows(String headerMsg, CloseableIteration<?, ?> rows)
        throws Exception {
    println(headerMsg);
        int count = 0;
        while (rows.hasNext()) {
            println(rows.next());
            count++;
        }
        println("Number of results: " + count);
        rows.close();
    }

    /**
     * Create a fresh AG repository and return a connection object to it.
     * 
     * @return AGRepositoryConnection
     * @throws Exception  if there is an error during the request
     */
    private static AGRepositoryConnection setupRepository() throws Exception {

        server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
        try {
            catalog = server.getCatalog(CATALOG_ID);
        } catch (Exception e) {
            throw new Exception("Got error when attempting to connect to server at "
                            + SERVER_URL + ": " + e);
        }

        if (catalog == null) {
            throw new Exception("Catalog " + CATALOG_ID + " does not exist. Either "
                            + "define this catalog in your agraph.cfg or modify the CATALOG_ID "
                            + "in this tutorial to name an existing catalog.");
        }

        println("Creating fresh repository " + CATALOG_ID + "/" + REPOSITORY_ID);
        catalog.deleteRepository(REPOSITORY_ID);
        repo = catalog.createRepository(REPOSITORY_ID);
        repo.initialize();

        return conn = repo.getConnection();
    }

    /**
     * Define the attributes that will be used in this example. Attributes need
     * only be defined once when setting up a repository. When importing data
     * containing triple attributes, all attributes must first be defined or the
     * import will fail.
     */
    private static void defineAttributes() throws Exception {

        try {
            conn.begin();

            /*
             * securityLevel describes the sensitivity level of the triple it is
             * attached to.
             * 
             * securityLevel must be applied to all triples, only once. The
             * acceptable values are: low, medium, and high
             * 
             * The following (ordered) relationship holds: low < medium < high
             */
            AttributeDefinition seclev = conn.new AttributeDefinition("securityLevel")
            		.ordered(true)
            		.allowedValue("low")
            		.allowedValue("medium")
            		.allowedValue("high")
            		.minimum(1)
            		.maximum(1)
            		.add();

            /*
             * department attributes indicate the department a triple is
             * applicable to. Only the below 4 values are allowed, and zero or
             * more can be assigned.
             */
            AttributeDefinition dept = conn.new AttributeDefinition("department")
            		.allowedValue("devel")
            		.allowedValue("hr")
            		.allowedValue("sales")
            		.allowedValue("accounting")
            		.add();

            /*
             * accessTokens describe a set of arbitrary values that are
             * associated with a given triple. As they are used in this example,
             * they allow for finer grained filtering along an additional
             * dimension. e.g. "Who, in a given department with the appropriate
             * securityLevel can access the triple data associated with these
             * accessTokens?"
             * 
             * Similar to department, only the below 5 values are valid, and
             * zero or more can be assigned.
             */
            AttributeDefinition access = conn.new AttributeDefinition("accessToken")
            		.allowedValue("A")
            		.allowedValue("B")
            		.allowedValue("C")
            		.allowedValue("D")
            		.allowedValue("E")
            		.add();

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    /*
     * Add a sample set of triples with various attributes based on the above
     * attribute definitions.
     * 
     * @return void
     * @throws Exception  for any unexpected errors while populating.
     */
    private static void populateRepository() throws Exception {
        AGValueFactory vf = conn.getRepository().getValueFactory();

        // subject for our set of triples.
        s = vf.createBNode();
        // our namespace
        ex = "http://example.org/ontology/";

        // a set of predicates we will use.
        pName = vf.createIRI(ex + "name");
        pSalary = vf.createIRI(ex + "salary");
        pDept = vf.createIRI(ex + "department");
        pInfractions = vf.createIRI(ex + "infractions");

        println("Adding sample triples with attributes to the repository.");

        try {
            // start a transaction so all triples are committed at once.
            conn.begin();

            // attributes for triples with general employee info
            JSONObject infoAttrs = new JSONObject()
            		.put("securityLevel", "low")
            		.put("department",
            				new JSONArray().put("devel").put("hr")
            				.put("accounting").put("sales"))
            		.put("accessToken", "A");

            // attributes for triples related to employee salary
            JSONObject salaryAttrs = new JSONObject()
            		.put("securityLevel", "medium")
            		.put("department",
            				new JSONArray().put("hr").put("accounting"))
            		.put("accessToken", "A");

            // attributes for triples related to personal/sensitive employee
            // information.
            JSONObject sensitiveAttrs = new JSONObject()
            		.put("securityLevel", "high").put("department", "hr")
            		.put("accessToken", new JSONArray().put("D").put("E"));

            // Add statements with supplied attributes.
            conn.add(s, pDept, vf.createURI(ex, "ops"), infoAttrs);
            conn.add(s, pSalary, vf.createLiteral(100000), salaryAttrs);

            // Add a statement with defaulted attributes using the defaultAttributes SPARQL prefix.
            String queryString = AGQuery.getFranzOptionPrefixString("defaultAttributes", infoAttrs.toString()) +
            		"INSERT DATA { " + s + " <" + pName + "> 'Joe Smith' . }";
            conn.prepareUpdate(QueryLanguage.SPARQL, queryString).execute();

            // Add a statement with attributes using the SPARQL ATTRIBUTES clause.
            queryString = "INSERT DATA {" +
            		" ATTRIBUTE '" + sensitiveAttrs.toString() + "' { " +
            		s + " <" + pInfractions +
            		"> <http://example.org/ontology/Infraction#ExcessiveTardiness> . } }";
            conn.prepareUpdate(QueryLanguage.SPARQL, queryString).execute();

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }

        println("Total unfiltered triple count is: " + conn.size());

    }

    /**
     * Define a static filter based on the attributes defined in
     * defineAttributes(), then commits triple store.
     * 
     * For a triple to be visible: - The user securityLevel must be >= the
     * securityLevel attribute on a triple. - The user department attributes
     * must match at least one of the department attributes on a triple. - The
     * user access-token attributes must contain all accessToken attributes on a
     * triple.
     * 
     * @throws Exception  if there is a problem setting or committing the triple-store
     * it is rolled back and the exception rethrown 
     */
    private static void defineStaticFilter() throws Exception {
        try {

            conn.begin();
            conn.setStaticAttributeFilter("(and (attribute-set>= user.securityLevel triple.securityLevel) "
            		+ "(attribute-contains-one-of user.department triple.department) "
            		+ "(attribute-contains-all-of user.accessToken triple.accessToken))");
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    /**
     * Print all triples visible at the time this function is called.
     * 
     * @param msg  an informative message printed before the statements are displayed
     * @return void
     * @throws Exception  if any problem occurs while fetching/printing triples
     */
    private static void printVisibleTriples(String msg) throws Exception {
        printRows(msg, conn.getStatements(null, null, null, false));
    }

    /**
     * Collect the values of a single variable from a SPARQL result set.
     * 
     * @param set  the result set from which the bindings will be collected
     * @param variable  the name of the binding to collect
     * @return List&lt;String&gt;  a List of the values collected, as Strings
     * @throws QueryEvaluationException  if errors are encountered while operating
     * on the result set
     */
    public static List<String> collectBindings(TupleQueryResult set, String variable)
            throws QueryEvaluationException {
        List<String> result = new ArrayList<String>(0);

        while(set.hasNext()) {
            BindingSet bindingSet = set.next();
            Value value = bindingSet.getValue(variable);
            if (value != null) {
            	result.add(value.stringValue());
            }
        }
        return result;
    }
    
    /**
     * A utility function for checking that attribute values associated with a statement
     * are valid. It queries a connection for all attribute values that match the following
     * pattern
     *
     *   (ATTRIBUTE ?value) attr:attributesNameValue (?s PREDICATE ?o)
     *
     * and checks that the number of results matches NUMEXPECTED, and that EXPECTEDVALUE
     * is found in the result set.
     *
     * @param attribute  a String naming the attribute to be checked
     * @param expectedValue  the value to search for
     * @param predicate  the predicate used to modify the statements found by the query
     * @param numExpected  the number of results expected by evaluating the query
     * @return boolean  true if the check succeeds, else throws an Exception
     * @throws Exception  if any check fails
     */
    public static boolean checkAttributeValue(String attribute, String expectedValue,
                                              IRI predicate, int numExpected)
            		throws Exception {
        String queryString = "PREFIX attr: <http://franz.com/ns/allegrograph/6.2.0/> " +
            	"select ?value where { ('" + attribute + "' ?value) attr:attributesNameValue (?s <" +
            	predicate + "> ?o) . }";
        TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate();
        List<String> bindings = collectBindings(result, "value");
        if(bindings.size() != numExpected) error("Expected " + numExpected + " result(s) from query, but got " + bindings.size());
        if(!bindings.contains(expectedValue)) error("Did not find a '" + attribute + "' attribute value of '" + expectedValue + "'.");
        return true;
    }

    /**
     * Simple error routine that throws an Exception when called, passing REASON
     * to the constructor of the Exception.
     * 
     * @param reason  an informative String explaining the exception
     * @throws Exception  the exception generated by the call
     */
    public static void error(String reason) throws Exception{
        throw new Exception(reason);
    }

    public static void main(String[] args) throws Exception {
        try (AGRepositoryConnection _conn = setupRepository()) {
            run();
        } finally {
            server.deleteRepository(REPOSITORY_ID, CATALOG_ID);
        }
    }

    private static void run() throws Exception {
        defineAttributes();
        populateRepository();

        // define some user-attributes that we will use below.
        String hrLowSecUser = "{ \"securityLevel\": [ \"low\" ], \"department\": [ \"hr\" ], \"accessToken\": [ \"A\", \"B\" ] }";
        String acctUser = "{ \"securityLevel\": [ \"medium\" ], \"department\": [ \"accounting\" ], \"accessToken\": [ \"A\", \"C\" ] }";
        String hrHighSecUser1 = "{ \"securityLevel\": [ \"high\" ], \"department\": [ \"hr\", \"devel\" ], \"accessToken\": [ \"A\", \"B\" ] }";
        String hrHighSecUser2 = "{ \"securityLevel\": [ \"high\" ], \"department\": [ \"hr\", \"accounting\" ], \"accessToken\": [ \"A\", \"C\", \"D\", \"E\" ] }";

        // No filter is set, so all triples should be visible.
        printVisibleTriples("Visible triples with no static filter set, and no user-attributes");

        /* No filter, so setting user attributes has no effect. All triples are
         * still visible.
         *
         * The UserAttributesContext establishes a scope within which the same set
         * of user attributes will be delivered with each request.
         */
        try (UserAttributesContext ctxt = new UserAttributesContext(conn,
            	hrLowSecUser)) {
            printVisibleTriples("Visible with user attributes set, but no static filter.");
        }

        defineStaticFilter();

        // Now that a filter is set, we can control the set of visible triples
        // by setting user attributes.

        // only low security triples will be visible.
        try (UserAttributesContext ctxt = new UserAttributesContext(conn,
            	hrLowSecUser)) {
            printVisibleTriples("Triples visible to hrLowSecUser.");
        }

        // low and medium security triples will be visible.
        try (UserAttributesContext ctxt = new UserAttributesContext(conn,
            	acctUser)) {
            printVisibleTriples("Triples visible to acctUser.");
        }

        // high security triples will not be visible due to accessToken
        // mismatch.
        try (UserAttributesContext ctxt = new UserAttributesContext(conn,
            	hrHighSecUser1)) {
            printVisibleTriples("Triples visible to hrHighSecUser1.");
        }

        // this user has sufficient attribute settings to see all triples.
        try (UserAttributesContext ctxt = new UserAttributesContext(conn,
            	hrHighSecUser2)) {
            printVisibleTriples("Triples visible to hrHighSecUser2.");
        }

        // Set User Attributes via SPARQL prefix and query the repository.
        // only low security triples will be visible
        String queryString = AGQuery.getFranzOptionPrefixString("userAttributes", acctUser) +
            	"select ?s ?p ?o where { ?s ?p ?o .} ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result = tupleQuery.evaluate();

        printRows("Triples visible to hrLowSecUser (via SPARQL)", result);

        // Demonstrate the binding of triple attributes as part of a SPARQL Query.
        // Only low and medium security triples will be visible.
        // 
        queryString = "PREFIX attr: <http://franz.com/ns/allegrograph/6.2.0/> " +
            	AGQuery.getFranzOptionPrefixString("userAttributes", acctUser) +
            	"select ?s ?p ?o ?attributes where { ?attributes attr:attributes (?s ?p ?o) . }";
        printRows("Triples and Attributes visible to acctUser (via SPARQL)",
            	conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());

        // Demonstrate the binding of the individual Attribute NameValues as part of a SPARQL Query.
        // Note that the predicate used in this query differs from the query above.
        // Only low and medium security triples will be visible.
        queryString = "PREFIX attr: <http://franz.com/ns/allegrograph/6.2.0/> " +
            	AGQuery.getFranzOptionPrefixString("userAttributes", acctUser) +
            	"select ?s ?name ?value where { (?name ?value) attr:attributesNameValue (?s ?p ?o) . }";
        printRows("Triples and Attributes NameValues visible to acctUser (via SPARQL)",
            	conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate());

        // Unset the static filter, so all triples are visible
        conn.setStaticAttributeFilter(null);
        conn.commit();

        // Check a cross-section of triples to see if their attributes have expected values.
        checkAttributeValue("securityLevel", "high", pInfractions, 1);
        checkAttributeValue("securityLevel", "low", pName, 1);
        checkAttributeValue("accessToken", "A", pName, 1);
        checkAttributeValue("securityLevel", "medium", pSalary, 1);
        // the ex:department triple for Joe has 4 departments. Verify one is "hr"
        checkAttributeValue("department", "hr", pDept, 4);

        // this check should fail, since there are not 3 securityLevel attributes
        // set on Joe's ex:name triple.
        boolean proceed = true;
        try {
            checkAttributeValue("securityLevel", "low", pName, 3);
            proceed = false;
            throw new Exception("checkAttributeValue did not fail as expected.");
        } catch (Exception e) {
            if (!proceed) throw e;
        }

        println("Example complete.");
    }
}
