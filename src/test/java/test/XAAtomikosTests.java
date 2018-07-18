package test;

import com.atomikos.datasource.xa.XID;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XAAtomikosTests extends AGAbstractTest {
    public static class XAUtils {
        private static final int XA_TIMEOUT = 10;
        private static long counter = 1;

        public static String createTid() {
            return String.valueOf(++counter);
        }

        public static Xid createXid() {
            return new XID( createTid(), "test" );
        }
    }

    private static final String REPO_ID   = "test-2pc";

    private AGServer server;
    private AGRepository agrepo;

    private AGRepositoryConnection testConnection;
    private XAResource testXAResource;
    private Xid testXid;

    private AGRepositoryConnection independentConnection;
    private XAResource independentXAResource;

    @Before
    public void setUp() throws Exception {
        server = newAGServer();
        closer.closeLater(server);
        agrepo = freshRepository(CATALOG_ID, REPO_ID);
        closer.closeLater(agrepo);

        AGRepositoryConnection conn = agrepo.getConnection();
        initData(conn);

        testConnection = agrepo.getConnection();
        closer.closeLater(testConnection);
        testConnection.begin();
        testXAResource = testConnection.getXAResource();
        testXAResource.setTransactionTimeout(XAUtils.XA_TIMEOUT);
        testXid = XAUtils.createXid();

        independentConnection = agrepo.getConnection();
        closer.closeLater(independentConnection);
        independentConnection.begin();
        independentXAResource = independentConnection.getXAResource();
        independentXAResource.setTransactionTimeout(XAUtils.XA_TIMEOUT);
    }

    @After
    public void tearDown() {
        closer.close();
    }

    @Test
    public void testConnectToDatabase1() throws Exception {
        try (RepositoryResult res = testConnection.getStatements(null, null, null, false)) {
            assertTrue(res.hasNext());
        }
    }

    @Test
    public void testUpdateVisibleToIndependentTransactionAfterCommit() throws Exception {
        final long initialBalance = getBalanceInIndependentTransaction();
        updateBalanceViaTestConnection(initialBalance + 10, XAResource.TMNOFLAGS);
        performOnePhaseCommitWithXA();
        assertBalanceViaIndependentTransaction("updated balance should be visible after commit", initialBalance+10);
    }

    @Test
    public void testTwoPhaseCommitWithXA() throws Exception {
        updateBalanceViaTestConnection(10, XAResource.TMNOFLAGS);
        performPrepareWithXA();
        performCommitAfterPrepareWithXA(testXAResource);
    }

    @Test
    public void testCommitAfterPrepareIsAllowedOnDifferentXAResource() throws Exception {
        updateBalanceViaTestConnection(10, XAResource.TMNOFLAGS);
        performPrepareWithXA();
        performCommitAfterPrepareWithXA(independentXAResource);
    }

    @Test
    public void testRollbackAfterPrepareIsAllowedOnDifferentXAResource() throws Exception {
        updateBalanceViaTestConnection(10, XAResource.TMNOFLAGS);
        performPrepareWithXA();
        performRollbackWithXA(independentXAResource);
    }

    @Test
    public void testRollbackOfUnknownXidWorks() {
        try {
            testXAResource.rollback(XAUtils.createXid());
        } catch (Exception err) {
            assertTrue(err instanceof XAException);
            XAException xaerr = (XAException)err;
            assertTrue ("rollback of unknown xid gives unexpected errorCode: " + xaerr.errorCode,
                    ((XAException.XA_RBBASE <= xaerr.errorCode) && (xaerr.errorCode <= XAException.XA_RBEND )) ||
                    xaerr.errorCode == XAException.XAER_NOTA);
        }
    }


    private void performRollbackWithXA(XAResource xaResource) throws XAException {
        xaResource.rollback(testXid);
    }

    private void performCommitAfterPrepareWithXA(XAResource xaResource) throws XAException {
        xaResource.commit(testXid, false);
    }

    private void performPrepareWithXA() throws XAException {
        testXAResource.prepare(testXid);
    }

    private void performOnePhaseCommitWithXA() throws XAException {
        testXAResource.commit(testXid, true);
    }

    private void updateBalanceViaTestConnection(long balance, int xaStartFlag) throws XAException {
        testXAResource.start(testXid, xaStartFlag);
        AGValueFactory vf = testConnection.getRepository().getValueFactory();
        IRI alice = vf.createIRI("http://example.org/people/alice");
        IRI has = vf.createIRI("http://example.org/ontology/has");
        Literal alicesBalance = vf.createLiteral(balance);
        testConnection.remove(alice, has, null);
        testConnection.add(alice, has, alicesBalance);
        testXAResource.end(testXid,XAResource.TMSUCCESS);
    }

    private void assertBalanceViaIndependentTransaction(String msg, long expectedBalance) throws XAException {
        long balance = getBalanceInIndependentTransaction();
        assertEquals(msg, expectedBalance, balance);
    }

    private long getBalanceInIndependentTransaction() throws XAException {
        Xid independentXid = XAUtils.createXid();
        independentConnection.rollback();
        independentXAResource.start(independentXid, XAResource.TMNOFLAGS);
        long balance = getBalance(independentConnection);
        independentXAResource.end(independentXid,XAResource.TMSUCCESS);
        independentXAResource.commit(independentXid, true);
        return balance;
    }

    private long getBalance(AGRepositoryConnection conn) {
        AGValueFactory vf = conn.getRepository().getValueFactory();
        IRI alice = vf.createIRI("http://example.org/people/alice");
        IRI has = vf.createIRI("http://example.org/ontology/has");
        RepositoryResult<Statement> res = conn.getStatements(alice, has, null, false);
        return Long.parseLong(res.next().getObject().stringValue());
    }

    private void initData(AGRepositoryConnection conn) {
        AGValueFactory vf = conn.getRepository().getValueFactory();
        conn.begin();
        IRI alice = vf.createIRI("http://example.org/people/alice");
        IRI has = vf.createIRI("http://example.org/ontology/has");
        Literal alicesBalance = vf.createLiteral("100", XMLSchema.LONG);
        conn.add(alice, has, alicesBalance);
        conn.commit();
        conn.close();
    }

}
