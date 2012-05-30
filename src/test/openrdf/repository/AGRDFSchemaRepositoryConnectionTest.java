package test.openrdf.repository;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryResult;

import test.AGAbstractTest;

import com.franz.agraph.repository.AGRepository;

public class AGRDFSchemaRepositoryConnectionTest extends AGRepositoryConnectionTest {

	static AGRepository repo = null;
	
	private URI person;
	
	private URI woman;
	
	private URI man;
	
	public AGRDFSchemaRepositoryConnectionTest(String name) {
		super(name);
	}

	@Override
	protected Repository createRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

	@Override
	public void setUp()
		throws Exception
	{
		super.setUp();

		person = vf.createURI(FOAF_NS + "Person");
		woman = vf.createURI("http://example.org/Woman");
		man = vf.createURI("http://example.org/Man");
	}

	public void testDomainInference()
		throws Exception
	{
		testCon.add(name, RDFS.DOMAIN, person);
		testCon.add(bob, name, nameBob);

		assertTrue(testCon.hasStatement(bob, RDF.TYPE, person, true));
	}

	public void testSubClassInference()
		throws Exception
	{
		testCon.setAutoCommit(false);
		testCon.add(woman, RDFS.SUBCLASSOF, person);
		testCon.add(man, RDFS.SUBCLASSOF, person);
		testCon.add(alice, RDF.TYPE, woman);
		testCon.setAutoCommit(true);

		assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));
	}

	public void testMakeExplicit()
		throws Exception
	{
		testCon.setAutoCommit(false);
		testCon.add(woman, RDFS.SUBCLASSOF, person);
		testCon.add(alice, RDF.TYPE, woman);
		testCon.setAutoCommit(true);

		assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));

		testCon.add(alice, RDF.TYPE, person);

		assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));
	}

	public void testExplicitFlag()
		throws Exception
	{
		RepositoryResult<Statement> result = testCon.getStatements(RDF.TYPE, RDF.TYPE, null, true);
		try {
			assertTrue("result should not be empty", result.hasNext());
		}
		finally {
		result.close();
		}

		result = testCon.getStatements(RDF.TYPE, RDF.TYPE, null, false);
		try {
			assertFalse("result should be empty", result.hasNext());
		}
		finally {
		result.close();
		}
	}

	public void testInferencerUpdates()
		throws Exception
	{
		testCon.setAutoCommit(false);

		testCon.add(bob, name, nameBob);
		testCon.remove(bob, name, nameBob);

		testCon.setAutoCommit(true);

		assertFalse(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
	}

	public void testInferencerQueryDuringTransaction()
		throws Exception
	{
		testCon.setAutoCommit(false);

		testCon.add(bob, name, nameBob);
		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

		testCon.setAutoCommit(true);
	}

	public void testInferencerTransactionIsolation()
		throws Exception
	{
		testCon.setAutoCommit(false);
		testCon.add(bob, name, nameBob);

		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
		assertFalse(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

		testCon.setAutoCommit(true);

		assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
		assertTrue(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
	}

}
