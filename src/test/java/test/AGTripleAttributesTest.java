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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;


public class AGTripleAttributesTest extends AGAbstractTest {
    private int id = 0;
    // Connection recreated before each test
    private AGRepositoryConnection conn;

    // Utility. Generates a unique attribute name for tests.
    private String genAttributeName() {
        id = id + 1;
        return "attrdef" + id;
    }

    @Before
    public void connect() {
        conn = getConnection();
    }

    @After
    public void disconnect() {
        conn.close();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testAttributeDefinitionsBasic() throws Exception {
        JSONArray attrs;

        // test adding allowed values one at a time.
        conn.new AttributeDefinition("remoteAttr1").add();
        // implicit fetch test, to validate the add operation.
        attrs = conn.getAttributeDefinitions();
        Assert.assertEquals("Verifying 1 attribute definition", 1, attrs.length());

        conn.new AttributeDefinition("remoteAttr2")
                .allowedValue("sales")
                .allowedValue("devel")
                .allowedValue("hr")
                .add();
        attrs = conn.getAttributeDefinitions();
        Assert.assertEquals("Verifying 2 attribute definitions", 2, attrs.length());

        // test ordered
        conn.new AttributeDefinition("remoteAttr3")
                .allowedValue("low")
                .allowedValue("medium")
                .allowedValue("high")
                .ordered(true)
                .add();
        attrs = conn.getAttributeDefinitions();
        Assert.assertEquals("Verifying 3 attribute definitions", 3, attrs.length());

        // test passing allowed values as a list, include a maximum.
        List<String> values4 = new ArrayList<>(3);
        values4.add("moe");
        values4.add("larry");
        values4.add("curly");

        AttributeDefinition defn4 = conn.new AttributeDefinition("remoteAttr4");
        defn4.allowedValues(values4)
                .maximum(1)
                .add();

        attrs = conn.getAttributeDefinitions();
        Assert.assertEquals("Verifying 4 attribute definitions", 4, attrs.length());


        // TODO: Test updating an existing attribute definition, pending defined behavior when doing so.


        // test fetching attributes
        attrs = conn.getAttributeDefinition("remoteAttr3");
        Assert.assertEquals("Fetching remoteAttr3", 1, attrs.length());

        // test deleting attributes. delete remoteAttr3.
        conn.deleteAttributeDefinition(attrs.getJSONObject(0).getString("name"));
        attrs = conn.getAttributeDefinitions();
        Assert.assertEquals("Verifying one fewer defined attributes", 3, attrs.length());

        // verify attribute is gone.
        attrs = conn.getAttributeDefinition("remoteAttr3");
        Assert.assertEquals("Verifying remoteAttr3 was deleted", 0, attrs.length());

    }

    @Category(TestSuites.Prepush.class)
    @Test
    public void testInvalidNameException() throws Exception {
        try {
            conn.new AttributeDefinition("no spaces allowed").add();
            Assert.fail("no exception for invalid name.");
        } catch (AGHttpException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid name:"));
        }
    }

    @Category(TestSuites.Prepush.class)
    @Test
    public void testNegativeMinimum() throws Exception {
        try {
            conn.new AttributeDefinition(genAttributeName()).minimum(-5);
            Assert.fail("no exception for negative value.");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("minimum"));
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testNegativeMaximum() throws Exception {
        try {
            conn.new AttributeDefinition(genAttributeName()).maximum(-5);
            Assert.fail("no exception for negative value.");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("maximum"));
        }
    }

    @Test(expected = AGHttpException.class)
    @Category(TestSuites.Prepush.class)
    public void testMaximumTooBig() throws Exception {
        conn.new AttributeDefinition(genAttributeName())
                .maximum(Long.MAX_VALUE)
                .add();
    }

    @Test(expected = AGHttpException.class)
    @Category(TestSuites.Prepush.class)
    public void testMinimumTooBig() throws Exception {
        conn.new AttributeDefinition(genAttributeName())
                .minimum(Long.MAX_VALUE)
                .add();
    }

    @Test(expected = AGHttpException.class)
    @Category(TestSuites.Prepush.class)
    public void testMinGreaterThanMax() throws Exception {
        conn.new AttributeDefinition(genAttributeName())
                .maximum(2)
                .minimum(10)
                .add();
    }

    @Test(expected = AGHttpException.class)
    @Category(TestSuites.Prepush.class)
    public void testMaxOfOneWhenOrdered() throws Exception {
        List<String> values = new ArrayList<>(3);
        values.add("low");
        values.add("medium");
        values.add("high");

        // valid, maximum must be one when ordered.
        conn.new AttributeDefinition(genAttributeName())
                .ordered(true)
                .maximum(1)
                .allowedValues(values)
                .add();

        // invalid
        conn.new AttributeDefinition(genAttributeName())
                .ordered(true)
                .maximum(5)
                .allowedValues(values)
                .add();
    }

    @Test(expected = AGHttpException.class)
    @Category(TestSuites.Prepush.class)
    public void testOrderedSansValues() throws Exception {
        conn.new AttributeDefinition(genAttributeName())
                .ordered(true)
                .add();
    }

    @Test(expected = RepositoryException.class)
    @Category(TestSuites.Prepush.class)
    public void testAddTriplesWithAttributes() throws Exception {
        AGValueFactory vf = conn.getRepository().getValueFactory();

        // define an interesting couple of attributes.
        AttributeDefinition def1 = conn.new AttributeDefinition("canWork")
                .allowedValue("AR")
                .allowedValue("CH")
                .allowedValue("BR")
                .allowedValue("VZ");

        AttributeDefinition def2 = conn.new AttributeDefinition("gameImportance")
                .allowedValue("Group")
                .allowedValue("Quarter")
                .allowedValue("Semi")
                .allowedValue("Final")
                .ordered(true);

        // And one uninteresting attribute.
        AttributeDefinition def3 = conn.new AttributeDefinition("note");

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

        conn.add(ref2, RDF.TYPE, ref, badAttrs);
    }

    // TODO: Add test where maximum is exceeded.
    // TODO: Add test where minimum is not met.


    @Category(TestSuites.Prepush.class)
    @Test
    public void testStaticAttributeFiltersBasic() throws Exception {
        setupStaticAttributeFilters();

        String filter = "(and (attribute>= user.access-level triple.access-level)\n"
                + "(attribute-contains-one-of user.department triple.department)\n"
                + "(attribute-contains-all-of user.token triple.token))";

        // no filter defined yet, should get null
        String result = conn.getStaticAttributeFilter();
        Assert.assertNull("non-null result fetching StaticFilter before one defined. Got '" + result + "'", result);

        // Set a filter
        conn.setStaticAttributeFilter(filter);

        // get filter
        String returnedFilter = conn.getStaticAttributeFilter();
        Assert.assertEquals(filter, returnedFilter);

        // delete filter
        conn.deleteStaticAttributeFilter();
        result = conn.getStaticAttributeFilter();
        Assert.assertNull("non-null result after deleting staticFilter. Got '" + result + "'", result);

    }

    private void setupStaticAttributeFilters() throws Exception {
        try (AGRepositoryConnection conn = getConnection()) {

            conn.new AttributeDefinition("access-level")
                    .allowedValue("low")
                    .allowedValue("medium")
                    .allowedValue("high")
                    .ordered(true)
                    .minimum(1)
                    .add();

            conn.new AttributeDefinition("department")
                    .allowedValue("tech")
                    .allowedValue("hr")
                    .allowedValue("sales")
                    .allowedValue("accounting")
                    .add();

            conn.new AttributeDefinition("token")
                    .allowedValue("A").allowedValue("B")
                    .allowedValue("C").allowedValue("D")
                    .allowedValue("E").allowedValue("F")
                    .add();
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
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
            Assert.fail("No exception thrown when passing undefined user-attributes.");
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
