package test;

import static test.Stmt.statementSet;
import static test.Stmt.stmts;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;
import org.openrdf.rio.RDFFormat;

public class SpinTest extends AGAbstractTest {

    private final String baseURI = "http://example.org/SpinTest";
    
    private final String kennedyNamespace = "http://www.franz.com/simple#";

	private final String ageFn = baseURI + "/age";
	private final String parentsMP = baseURI + "/parents";
	private final String grandParentsMP = baseURI + "/grandParents";

	private URI context;

    @Before
    public void installScripts() throws Exception {
    	context = vf.createURI(baseURI + "#kennedy");
    	conn.setNamespace("ex", baseURI);
    	conn.setNamespace("kennedy", context.toString());
        conn.add(new File("src/tutorial/java-kennedy.ntriples"), baseURI, RDFFormat.NTRIPLES, context);
    	log.info("size " + conn.size());
    }
    
    @After
    public void cleanup0() throws Exception {
    	conn.remove((Resource)null, null, null, context);
    }
    
    @After
    public void cleanup1() throws Exception {
    	conn.getHttpRepoClient().deleteHardSpinFunction(ageFn);
    }
    
    @After
    public void cleanup2() throws Exception {
    	conn.getHttpRepoClient().deleteHardSpinMagicProperty(parentsMP);
    }
    
    @After
    public void cleanup3() throws Exception {
    	conn.getHttpRepoClient().deleteHardSpinMagicProperty(grandParentsMP);
    }

    /**
     * rfe10988
     */
    @Test
    public void spinFunction() throws Exception {
    	try {
    		Assert.fail( conn.getHttpRepoClient().getSpinFunction(ageFn) );
    	} catch (Exception e) {
    		if (e.getMessage().contains(ageFn + " is not a registered SPIN function")) {
        		// expected
    		} else {
    			throw e;
    		}
    	}
    	String ageFnSparql = "prefix kennedy: <" + kennedyNamespace+ ">\n"
    	+ "select ( (2011 - ?birthYear) as ?age ) { ?who kennedy:birth-year ?birthYear . }";
    	conn.getHttpRepoClient().putSpinFunction(ageFn, ageFnSparql, new String[] {"who"});
    	Assert.assertEquals(ageFnSparql, conn.getHttpRepoClient().getSpinFunction(ageFn));

    	String queryString = "prefix ex: <" + baseURI + ">\n"
    	+ "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "select ?first ?last ?age ?birthYear {\n"
    	+ "  ?person kennedy:first-name ?first .\n"
    	+ "  ?person kennedy:last-name ?last .\n"
    	+ "  ?person kennedy:birth-year ?birthYear .\n"
    	+ "  bind( ex:age( ?person ) as ?age ) .\n"
    	+ "} order by ?age limit 5";

        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral(39))}),
                statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate(), null, null, "age"));
    }

    /**
     * rfe10988
     */
    @Test
    public void spinMagicProperty() throws Exception {
    	try {
    		Assert.fail( conn.getHttpRepoClient().getSpinMagicProperty(parentsMP) );
    	} catch (Exception e) {
    		if (e.getMessage().contains(parentsMP + " is not a registered SPIN magic property")) {
        		// expected
    		} else {
    			throw e;
    		}
    	}
    	
    	String parentsFnSparql = "prefix kennedy: <" + kennedyNamespace+ ">\n"
    	+ "select ?parent { ?parent kennedy:has-child ?child . }";
    	conn.getHttpRepoClient().putSpinMagicProperty(parentsMP, parentsFnSparql, null);
    	Assert.assertEquals(parentsFnSparql, conn.getHttpRepoClient().getSpinMagicProperty(parentsMP));
    	
    	String queryString = "prefix ex: <" + baseURI + ">\n"
    	+ "prefix kennedy: <" + kennedyNamespace + ">\n"
    	+ "select ?person ?parentFirst {\n"
    	+ "?person kennedy:first-name 'Joseph' .\n"
    	+ "?person kennedy:birth-year '1915' .\n"
    	+ "?person kennedy:birth-year ?birthYear .\n"
    	+ "?person ex:parents ?parent .\n"
//    	+ "?parent kennedy:has-child ?person .\n"
    	+ "?parent kennedy:first-name ?parentFirst .\n"
    	+ "}";
    	
        assertSetsEqual(stmts(new Stmt[] {new Stmt(null, null, vf.createLiteral("Joseph")),
        		new Stmt(null, null, vf.createLiteral("Rose"))}),
                statementSet(conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate(),
                		null, null, "parentFirst"));
        
//    	String grandParentsFnSparql = "prefix kennedy: <" + kennedyNamespace+ ">\n"
//    	+ "select ?grand {"
//    	+ " ?parent kennedy:has-child ?child ."
//    	+ " ?grand kennedy:has-child ?parent . }";
//    	serverCode.putSpinMagicProperty(grandParentsMP, grandParentsFnSparql, new String[] {"child"});
//    	Assert.assertEquals(grandParentsFnSparql, serverCode.getSpinMagicProperty(grandParentsMP));
    	
    }

}
