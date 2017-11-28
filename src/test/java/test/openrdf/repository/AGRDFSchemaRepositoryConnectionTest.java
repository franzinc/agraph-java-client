package test.openrdf.repository;

import com.franz.agraph.repository.AGRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Test;
import test.AGAbstractTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AGRDFSchemaRepositoryConnectionTest extends AGRepositoryConnectionTest {

    static AGRepository repo = null;

    private IRI person;

    private IRI woman;

    private IRI man;

    @Override
    protected Repository createRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

    @Override
    public void setUp()
            throws Exception {
        super.setUp();

        person = vf.createIRI(FOAF_NS + "Person");
        woman = vf.createIRI("http://example.org/Woman");
        man = vf.createIRI("http://example.org/Man");
    }

    @Test
    public void testDomainInference()
            throws Exception {
        testCon.add(name, RDFS.DOMAIN, person);
        testCon.add(bob, name, nameBob);

        assertTrue(testCon.hasStatement(bob, RDF.TYPE, person, true));
    }

    @Test
    public void testSubClassInference()
            throws Exception {
        testCon.setAutoCommit(false);
        testCon.add(woman, RDFS.SUBCLASSOF, person);
        testCon.add(man, RDFS.SUBCLASSOF, person);
        testCon.add(alice, RDF.TYPE, woman);
        testCon.setAutoCommit(true);

        assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));
    }

    @Test
    public void testMakeExplicit()
            throws Exception {
        testCon.setAutoCommit(false);
        testCon.add(woman, RDFS.SUBCLASSOF, person);
        testCon.add(alice, RDF.TYPE, woman);
        testCon.setAutoCommit(true);

        assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));

        testCon.add(alice, RDF.TYPE, person);

        assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));
    }

    @Test
    public void testExplicitFlag()
            throws Exception {
        RepositoryResult<Statement> result = testCon.getStatements(RDF.TYPE, RDF.TYPE, null, true);
        while (result.hasNext()) {
            try {
                assertTrue("result should not be empty", result.hasNext());
            } finally {
                result.close();
            }
        }

        result = testCon.getStatements(RDF.TYPE, RDF.TYPE, null, false);
        try {
            assertFalse("result should be empty", result.hasNext());
        } finally {
            result.close();
        }
    }

    @Test
    public void testInferencerUpdates()
            throws Exception {
        testCon.setAutoCommit(false);

        testCon.add(bob, name, nameBob);
        testCon.remove(bob, name, nameBob);

        testCon.setAutoCommit(true);

        assertFalse(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
    }

    @Test
    public void testInferencerQueryDuringTransaction()
            throws Exception {
        testCon.setAutoCommit(false);
        testCon.add(bob, name, nameBob);
        assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

        testCon.setAutoCommit(true);
    }

    @Test
    public void testInferencerTransactionIsolation()
            throws Exception {
        testCon.setAutoCommit(false);
        testCon.add(bob, name, nameBob);

        assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
        assertFalse(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

        testCon.setAutoCommit(true);

        assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
        assertTrue(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
    }

}
