/******************************************************************************
** Copyright (c) 2008-2016 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
// import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
// import org.junit.rules.ExpectedException;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.jena.AGStatement;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.agraph.repository.AGRepositoryConnection.AttributeDefinition;
import com.hp.hpl.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.List;


public class AGTripleAttributesTest extends AGAbstractTest {

	// Leaving as a reminder that there are better annotations available
	// for testing exceptions if we use the JUnit test Runner class.
	// @Rule
    // private ExpectedException thrown = ExpectedException.none();
	
	private int id = 0;
	
	// Utility. Generates a unique attribute name for tests.
	private String genAttributeName() {
		id = id + 1;
		return "attrdef" + id;
	}
		
	@Test
	@Category(TestSuites.Prepush.class)
    public void testAttributeDefinitionsBasic() throws Exception
    {
    	AGRepositoryConnection conn = getConnection();
    	
    	JSONArray attrs;
    	
    	// test adding allowed values one at a time.
    	AttributeDefinition defn1 = conn.new AttributeDefinition("remoteAttr1").add();
    	// implicit fetch test, to validate the add operation.
    	attrs = conn.getAttributeDefinitions();
    	Assert.assertEquals("Verifying 1 attribute definition", 1, attrs.length());
    	
    	AttributeDefinition defn2 = conn.new AttributeDefinition("remoteAttr2")
    		.allowedValue("sales")
    		.allowedValue("devel")
    		.allowedValue("hr")
    		.add();
    	attrs = conn.getAttributeDefinitions();
    	Assert.assertEquals("Verifying 2 attribute definitions", 2, attrs.length());
    	
    	// test ordered
    	AttributeDefinition defn3 = conn.new AttributeDefinition("remoteAttr3")
    		.allowedValue("low")
    		.allowedValue("medium")
    		.allowedValue("high")
    		.ordered(true)
    		.add();
    	attrs = conn.getAttributeDefinitions();
    	Assert.assertEquals("Verifying 3 attribute definitions", 3, attrs.length());
    	
    	// test passing allowed values as a list, include a maximum.
    	List<String> values4 = new ArrayList<String>(3);
    	values4.add("moe"); values4.add("larry"); values4.add("curly");
    	
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
	@Test//(expected = AGHttpException.class)
	public void testInvalidNameException() throws Exception {
    	AGRepositoryConnection conn = getConnection();

    	try {
    		conn.new AttributeDefinition("no spaces allowed").add();
    		throw new Exception("no exception for invalid name.");
    	} catch (AGHttpException e) {
    		if (! e.getMessage().contains("Invalid name:")) {
    			throw e;
    		}
    	}
	}
	
	@Category(TestSuites.Prepush.class)
	@Test
	public void testNegativeMinimum() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		// thrown.expect(Exception.class);
		// thrown.expectMessage("minimum must be a non-negative integer.");
		
		try {
			conn.new AttributeDefinition(genAttributeName()).minimum(-5);
			throw new Exception("no exception for negative value.");
		} catch (Exception e) {
			if (! e.getMessage().contains("minimum")) {
				throw e;
			}
		}
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void testNegativeMaximum() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		// thrown.expect(Exception.class);
		// thrown.expectMessage("maximum must be greater than 0.");
		
		try {
			conn.new AttributeDefinition(genAttributeName()).maximum(-5);
			throw new Exception("no exception for negative value.");
		} catch (Exception e) {
			if (! e.getMessage().contains("maximum")) {
				throw e;
			}
		}
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void testMaximumTooBig() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		try {
			conn.new AttributeDefinition(genAttributeName())
				.maximum(Long.MAX_VALUE)
				.add();
			throw new Exception("no exception when maximum too large");
		} catch (AGHttpException e) {
		}
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void testMinimumTooBig() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		try {
			conn.new AttributeDefinition(genAttributeName())
				.minimum(Long.MAX_VALUE)
				.add();
			throw new Exception("no exception when minimum too large");
		} catch (AGHttpException e) {
		}
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void testMinGreaterThanMax() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		try {
			conn.new AttributeDefinition(genAttributeName())
				.maximum(2)
				.minimum(10)
				.add();
			throw new Exception("no exception when minimum > maximum");
		} catch (AGHttpException e) {
		}
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void testMaxOfOneWhenOrdered() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		List<String> values = new ArrayList<String>(3);
    	values.add("low"); values.add("medium"); values.add("high");
    	
    	// valid, maximum must be one when ordered.
    	conn.new AttributeDefinition(genAttributeName())
    		.ordered(true)
    		.maximum(1)
    		.allowedValues(values)
    		.add();
    	
    	// invalid
		try {
			conn.new AttributeDefinition(genAttributeName())
				.ordered(true)
				.maximum(5)
				.allowedValues(values)
				.add();
			throw new Exception("no exception when ordered, and maximum != 1");
		} catch (AGHttpException e) {
		}
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void testOrderedSansValues() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		try {
			conn.new AttributeDefinition(genAttributeName())
				.ordered(true)
				.add();
			throw new Exception("no exception when ordered, and no allowedValues specified");
		} catch (AGHttpException e) {
		}
	}
	
	@Category(TestSuites.Temp.class)
	@Test
	public void testAddTriplesWithAttributes() throws Exception {
		AGRepositoryConnection conn = getConnection();
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
		URI ref1 = vf.createURI("http://example.org/fifa/refs/CrookedReferee");
        URI ref2 = vf.createURI("http://example.org/fifa/refs/HonestReferee");
        URI ref = vf.createURI("http://example.org/ontology/HeadReferee");
        URI c1 = vf.createURI("http://example.org/fifa/copa_america");
        URI c2 = vf.createURI("http://example.org/fifa/copa_mundial");
        
	// and some attributes, including some which are null
        JSONObject attr1 = new JSONObject("{ canWork: [ AR, BR, null ], gameImportance: Semi, note: null }");
        JSONObject attr2 = new JSONObject("{ canWork: [ AR, CH, BR, VZ ], gameImportance: Final }");
        
        // Triple construction API
        conn.add(ref1, RDF.TYPE, ref, attr1, c1);
        conn.add(ref2, RDF.TYPE, ref, attr2, c1, c2);
        
        // Statement API
        Statement stmt1 = vf.createStatement(ref1, vf.createURI(RDFS.label.getURI()), vf.createLiteral("Don't trust this ref."));
        conn.add(stmt1);
        
        // add a triple with an undefined attribute
        JSONObject badAttrs = new JSONObject("{ canWork: [ AR, CH ], nationality: AR }");
        
        try {
        	conn.add(ref2, RDF.TYPE, ref, badAttrs);
        	throw new Exception("no exception when adding an undefined attribute");
        } catch (RepositoryException e) {
        }
	}
	
	// TODO: Add test where maximum is exceeded.
	// TODO: Add test where minimum is not met.
	
	
	@Category(TestSuites.Prepush.class)
	@Test
	public void testStaticAttributeFiltersBasic() throws Exception {
		AGRepositoryConnection conn = getConnection();
		
		setupStaticAttributeFilters();
		
		String filter = new String("(and (attribute>= user.access-level triple.access-level)\n"
                + "(attribute-contains-one-of user.department triple.department)\n"
                + "(attribute-contains-all-of user.token triple.token))");
		
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
		
		AGRepositoryConnection conn = getConnection();
		
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

	@Category(TestSuites.Prepush.class)
	@Test
	public void testPassingUserAttributesHeader() throws Exception {
		// make sure no old attribute definitions are lurking.
		cat.deleteRepository(REPO_ID);
		cat.createRepository(REPO_ID);
		AGRepositoryConnection conn = getConnection();
		
		JSONObject userAttrs = new JSONObject("{ access-level: medium, token: [ A, B, F ], department: sales }");
		
		conn.setUserAttributes(userAttrs);
		
		// attributes aren't defined yet. should error.
		// this test is sufficient to verify that the header is being passed correctly.
		try {
			conn.size();
			throw new Exception("No exception thrown when passing undefined user-attributes.");
		} catch (AGHttpException e) {
		}
		
		// clear userAttributes
		conn.setUserAttributes((String)null);
		// if header is sent despite clearing userAttributes, an exception should be thrown.
		conn.size();
		
		conn.setUserAttributes(userAttrs);

		setupStaticAttributeFilters();
		// should not error.
		conn.size();
		
	}
}
