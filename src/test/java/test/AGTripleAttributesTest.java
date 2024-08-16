/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGRepositoryConnection.AttributeDefinition;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class AGTripleAttributesTest extends AGAbstractTest {
    private int id = 0;
    // Connection recreated before each test
    private AGRepositoryConnection conn;

    // Utility. Generates a unique attribute name for tests.
    private String genAttributeName() {
        id = id + 1;
        return "attrdef" + id;
    }

    @BeforeEach
    public void connect() {
        conn = getConnection();
    }

    @AfterEach
    public void disconnect() {
        conn.close();
    }

    @Test
    public void testAttributeDefinitionsBasic() throws Exception {
        JSONArray attrs;

        // test adding allowed values one at a time.
        conn.defineAttribute("remoteAttr1").add();
        // implicit fetch test, to validate the add operation.
        attrs = conn.getAttributeDefinitions();
        assertEquals(1, attrs.length());

        conn.defineAttribute("remoteAttr2")
                .allowedValue("sales")
                .allowedValue("devel")
                .allowedValue("hr")
                .add();
        attrs = conn.getAttributeDefinitions();
        assertEquals(2, attrs.length());

        // test ordered
        conn.defineAttribute("remoteAttr3")
                .allowedValue("low")
                .allowedValue("medium")
                .allowedValue("high")
                .ordered(true)
                .add();
        attrs = conn.getAttributeDefinitions();
        assertEquals(3, attrs.length());

        // test passing allowed values as a list, include a maximum.
        List<String> values4 = new ArrayList<>(3);
        values4.add("moe");
        values4.add("larry");
        values4.add("curly");

        conn.defineAttribute("remoteAttr4")
                .allowedValues(values4)
                .maximum(1)
                .add();

        attrs = conn.getAttributeDefinitions();
        assertEquals(4, attrs.length());


        // TODO: Test updating an existing attribute definition, pending defined behavior when doing so.


        // test fetching attributes
        attrs = conn.getAttributeDefinition("remoteAttr3");
        assertEquals(1, attrs.length());

        // test deleting attributes. delete remoteAttr3.
        conn.deleteAttributeDefinition(attrs.getJSONObject(0).getString("name"));
        attrs = conn.getAttributeDefinitions();
        assertEquals(3, attrs.length());

        // verify attribute is gone.
        attrs = conn.getAttributeDefinition("remoteAttr3");
        assertEquals(0, attrs.length());

    }

    @Test
    public void testInvalidNameException() throws Exception {
        try {
            conn.defineAttribute("no spaces allowed").add();
            fail("no exception for invalid name.");
        } catch (AGHttpException e) {
            assertTrue(e.getMessage().contains("Invalid name:"));
        }
    }

    @Test
    public void testNegativeMinimum() throws Exception {
        try {
            conn.defineAttribute(genAttributeName()).minimum(-5);
            fail("no exception for negative value.");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("minimum"));
        }
    }

    @Test
    public void testNegativeMaximum() throws Exception {
        try {
            conn.defineAttribute(genAttributeName()).maximum(-5);
            fail("no exception for negative value.");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("maximum"));
        }
    }

    @Test
    public void testMaximumTooBig() {
        assertThrows(AGHttpException.class, () ->
                conn.defineAttribute(genAttributeName())
                        .maximum(Long.MAX_VALUE)
                        .add()
        );
    }

    @Test
    public void testMinimumTooBig() throws Exception {
        assertThrows(AGHttpException.class, () ->
                conn.defineAttribute(genAttributeName())
                        .minimum(Long.MAX_VALUE)
                        .add()
        );
    }

    @Test
    public void testMinGreaterThanMax() throws Exception {
        assertThrows(AGHttpException.class, () ->
                conn.defineAttribute(genAttributeName())
                        .maximum(2)
                        .minimum(10)
                        .add()
        );
    }

    @Test
    public void testMaxOfOneWhenOrdered() throws Exception {
        List<String> values = new ArrayList<>(3);
        values.add("low");
        values.add("medium");
        values.add("high");

        // valid, maximum must be one when ordered.
        conn.defineAttribute(genAttributeName())
                .ordered(true)
                .maximum(1)
                .allowedValues(values)
                .add();

        // invalid
        assertThrows(AGHttpException.class, () ->
                conn.defineAttribute(genAttributeName())
                        .ordered(true)
                        .maximum(5)
                        .allowedValues(values)
                        .add()
        );
    }

    @Test
    public void testOrderedSansValues() throws Exception {
        assertThrows(AGHttpException.class, () ->
                conn.defineAttribute(genAttributeName())
                        .ordered(true)
                        .add()
        );
    }

    @Test
    public void testAddTriplesWithAttributes() {
        AGValueFactory vf = conn.getRepository().getValueFactory();

        // define an interesting couple of attributes.
        AttributeDefinition def1 = conn.defineAttribute("canWork")
                .allowedValue("AR")
                .allowedValue("CH")
                .allowedValue("BR")
                .allowedValue("VZ");

        AttributeDefinition def2 = conn.defineAttribute("gameImportance")
                .allowedValue("Group")
                .allowedValue("Quarter")
                .allowedValue("Semi")
                .allowedValue("Final")
                .ordered(true);

        // And one uninteresting attribute.
        AttributeDefinition def3 = conn.defineAttribute("note");

        def1.add();
        def2.add();
        def3.add();

        // create some resources...
        IRI ref1 = vf.createIRI("http://example.org/fifa/refs/CrookedReferee");
        IRI ref2 = vf.createIRI("http://example.org/fifa/refs/HonestReferee");
        IRI ref = vf.createIRI("http://example.org/ontology/HeadReferee");
        IRI c1 = vf.createIRI("http://example.org/fifa/copa_america");
        IRI c2 = vf.createIRI("http://example.org/fifa/copa_mundial");

        // and some attributes, including some which are null
        JSONObject attr1 = new JSONObject("{ canWork: [ AR, BR, null ], gameImportance: Semi, note: null }");
        JSONObject attr2 = new JSONObject("{ canWork: [ AR, CH, BR, VZ ], gameImportance: Final }");

        // Triple construction API
        conn.add(ref1, RDF.TYPE, ref, attr1, c1);
        conn.add(ref2, RDF.TYPE, ref, attr2, c1, c2);

        // Statement API
        Statement stmt1 = vf.createStatement(ref1, vf.createIRI(RDFS.label.getURI()), vf.createLiteral("Don't trust this ref."));
        conn.add(stmt1);

        // add a triple with an undefined attribute
        JSONObject badAttrs = new JSONObject("{ canWork: [ AR, CH ], nationality: AR }");


        assertThrows(RepositoryException.class, () ->
                conn.add(ref2, RDF.TYPE, ref, badAttrs)
        );
    }

    // TODO: Add test where maximum is exceeded.
    // TODO: Add test where minimum is not met.


    @Test
    public void testStaticAttributeFiltersBasic() throws Exception {
        setupStaticAttributeFilters();

        String filter = "(and (attribute>= user.access-level triple.access-level)\n"
                + "(attribute-contains-one-of user.department triple.department)\n"
                + "(attribute-contains-all-of user.token triple.token))";

        // no filter defined yet, should get null
        String result = conn.getStaticAttributeFilter();
        assertNull(result);

        // Set a filter
        conn.setStaticAttributeFilter(filter);

        // get filter
        String returnedFilter = conn.getStaticAttributeFilter();
        assertEquals(filter, returnedFilter);

        // delete filter
        conn.deleteStaticAttributeFilter();
        result = conn.getStaticAttributeFilter();
        assertNull(result);
    }

    private void setupStaticAttributeFilters() throws Exception {
        try (AGRepositoryConnection conn = getConnection()) {

            conn.defineAttribute("access-level")
                    .allowedValue("low")
                    .allowedValue("medium")
                    .allowedValue("high")
                    .ordered(true)
                    .minimum(1)
                    .add();

            conn.defineAttribute("department")
                    .allowedValue("tech")
                    .allowedValue("hr")
                    .allowedValue("sales")
                    .allowedValue("accounting")
                    .add();

            conn.defineAttribute("token")
                    .allowedValue("A").allowedValue("B")
                    .allowedValue("C").allowedValue("D")
                    .allowedValue("E").allowedValue("F")
                    .add();
        }
    }

    @Test
    public void testPassingUserAttributesHeader() throws Exception {
        // make sure no old attribute definitions are lurking.
        conn.close();
        cat.deleteRepository(REPO_ID);
        cat.createRepository(REPO_ID);
        conn = getConnection();

        JSONObject userAttrs = new JSONObject("{ access-level: medium, token: [ A, B, F ], department: sales }");

        conn.setUserAttributes(userAttrs);

        // attributes aren't defined yet. should error.
        // this test is sufficient to verify that the header is being passed correctly.
        try {
            conn.size();
            fail("No exception thrown when passing undefined user-attributes.");
        } catch (AGHttpException e) {
            // as expected
        }

        // clear userAttributes
        conn.setUserAttributes((String) null);
        // if header is sent despite clearing userAttributes, an exception should be thrown.
        conn.size();

        conn.setUserAttributes(userAttrs);

        setupStaticAttributeFilters();
        // should not error.
        conn.size();
    }
}
