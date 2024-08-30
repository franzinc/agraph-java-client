package test.rdf4j.repository;

import com.franz.agraph.http.exception.AGMalformedDataException;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.testsuite.repository.RDFSchemaRepositoryConnectionTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.rdf4j.RDF4JTestsHelper;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AGRDFSchemaRepositoryConnectionTest extends RDFSchemaRepositoryConnectionTest {

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @Override
    protected Repository createRepository(File dataDir) throws Exception {
        return RDF4JTestsHelper.getTestRepository();
    }

    private IRI person;
    private IRI woman;
    private IRI man;

    @BeforeEach
    public void setUp() throws Exception {
        vf = testRepository.getValueFactory();
        person = vf.createIRI(FOAF_NS + "Person");
        woman = vf.createIRI("http://example.org/Woman");
        man = vf.createIRI("http://example.org/Man");
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testAddMalformedLiteralsDefaultConfig(IsolationLevel level) {
        assertThrows(AGMalformedDataException.class, () ->
                super.testAddMalformedLiteralsStrictConfig(level)
        );
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testAddMalformedLiteralsStrictConfig(IsolationLevel level) throws Exception {
        assertThrows(AGMalformedDataException.class, () ->
                super.testAddMalformedLiteralsStrictConfig(level)
        );
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testGetStatementsMalformedTypedLiteral(IsolationLevel level) {
        // TODO: RDF4J expects the repo to handle malformed literal, but AG does not accept them
        assertThrows(AGMalformedDataException.class, () ->
                super.testAddMalformedLiteralsStrictConfig(level)
        );
    }

    @Disabled
    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testSizeDuplicateStatement(IsolationLevel level) {
        super.testSizeDuplicateStatement(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testImportNamespacesFromIterable(IsolationLevel level) {
        super.testImportNamespacesFromIterable(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testContextStatementsNotDuplicated(IsolationLevel level) {
        super.testContextStatementsNotDuplicated(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testInferencerTransactionIsolation(IsolationLevel level) {
        super.testInferencerTransactionIsolation(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testContextStatementsNotDuplicated2(IsolationLevel level) {
        super.testContextStatementsNotDuplicated2(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testInferencerQueryDuringTransaction(IsolationLevel level) {
        super.testInferencerQueryDuringTransaction(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testExplicitFlag(IsolationLevel level) {
        super.testExplicitFlag(level);
    }

    @Test
    public void testDomainInference() {
        super.setupTest(IsolationLevels.NONE);
        testCon.add(name, RDFS.DOMAIN, person);
        testCon.add(bob, name, nameBob);
        assertTrue(testCon.hasStatement(bob, RDF.TYPE, person, true));
    }

    @Test
    public void testSubClassInference() {
        super.setupTest(IsolationLevels.NONE);
        testCon.begin();
        testCon.add(woman, RDFS.SUBCLASSOF, person);
        testCon.add(man, RDFS.SUBCLASSOF, person);
        testCon.add(alice, RDF.TYPE, woman);
        testCon.commit();
        assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));
    }

    @Test
    public void testMakeExplicit() {
        super.setupTest(IsolationLevels.NONE);

        testCon.setAutoCommit(false);
        testCon.add(woman, RDFS.SUBCLASSOF, person);
        testCon.add(alice, RDF.TYPE, woman);
        testCon.setAutoCommit(true);

        assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));

        testCon.add(alice, RDF.TYPE, person);

        assertTrue(testCon.hasStatement(alice, RDF.TYPE, person, true));
    }

    @Test
    public void testInferencerUpdates() {
        super.setupTest(IsolationLevels.NONE);
        testCon.setAutoCommit(false);

        testCon.add(bob, name, nameBob);
        testCon.remove(bob, name, nameBob);

        testCon.setAutoCommit(true);

        assertFalse(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
    }

    @Disabled
    @Test
    public void testInferencerQueryDuringTransaction() {
        testCon.setAutoCommit(false);
        testCon.add(bob, name, nameBob);
        assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

        testCon.setAutoCommit(true);
    }

    @Disabled
    @Test
    public void testInferencerTransactionIsolation() {
        testCon.setAutoCommit(false);
        testCon.add(bob, name, nameBob);

        assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
        assertFalse(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));

        testCon.setAutoCommit(true);

        assertTrue(testCon.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
        assertTrue(testCon2.hasStatement(bob, RDF.TYPE, RDFS.RESOURCE, true));
    }


}
