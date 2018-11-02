/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.tutorial.tpc;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;

import javax.sql.XAConnection;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.ValueFactory;

import com.atomikos.datasource.xa.jdbc.JdbcTransactionalResource;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTransactionalResource;

public class TwoPhaseCommitExample {

    private static final String HOST           = getenv("AGRAPH_HOST", "localhost");
    private static final String PORT           = getenv("AGRAPH_PORT", "10035");
    // USERNAME must name an AG superuser or a regular user with 2pc permissions set
    private static final String USERNAME       = getenv("AGRAPH_USER", "test");
    private static final String PASSWORD       = getenv("AGRAPH_PASS", "xyzzy");
    
    private static final String CATALOG_ID     = "java-catalog";
    private static final String AG_REPO_NAME   = "test-2pc";
    private static final String DERBY_DB_NAME  = "test-2pc";
    
    private static final String SERVER_URL     = "http://" + HOST + ":" + PORT;

    /**
     * 
     * @param name  Name of the variable
     * @param defaultValue  Value to be returned if the variable is not defined
     * @return the value of the environment variable or defaultValue if unset
     */
    private static String getenv(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue); 
    }
    
    /**
     * A simple object printer.
     * 
     * @param x  The object to print
     */
    private static void println(Object x) {
        System.out.println(x);
    }
    
    /**
     * Create a fresh AG repository.
     * 
     * @throws Exception  if there is an error during the request
     */
    private static AGRepository setupAGRepository(AGServer server, String repoName) throws Exception {

        AGCatalog catalog;

        try {
            catalog = server.getCatalog(CATALOG_ID);
        } catch (Exception e) {
            throw new Exception("Got error when attempting to connect to server at "
                    + SERVER_URL, e);
        }

        if (catalog == null) {
            throw new Exception("Catalog " + CATALOG_ID + " does not exist. Either "
                    + "define this catalog in your agraph.cfg or modify the CATALOG_ID "
                    + "in this tutorial to name an existing catalog.");
        }

        println("Creating fresh repository " + CATALOG_ID + ":" + repoName);
        catalog.deleteRepository(repoName);
        AGRepository repo = catalog.createRepository(repoName);
        repo.initialize();

        return repo;
    }
    
    private static EmbeddedXADataSource setupDerbyDB() throws Exception {
        EmbeddedXADataSource ds = new EmbeddedXADataSource();
        
        ds.setDatabaseName(DERBY_DB_NAME);
        ds.setCreateDatabase("create");
        
        // The database will be created (if it doesn't exist) during the 
        // getConnection call.
        try (Connection conn = ds.getConnection()) {            
            try (Statement s = conn.createStatement()) {
                try {
                    s.execute("drop table orders"); 
                } catch (SQLSyntaxErrorException e) {
                    // Derby bizarrely throws an exception after successfully
                    // dropping a table.  Code 42Y55 indicates successful drop, so
                    // we check for that here. Anything else is something unexpected.
                    if (!e.getSQLState().equals("42Y55")) {
                        throw new Exception("Caught error while dropping orders table", e);
                    }
                }
                
                s.execute("create table orders (id int generated always as identity, description varchar(255))");
                println("Created orders table");
            }
        }
        
        return ds;
    }

    private static int insertOrderIntoDerby(Connection conn, String orderDescription) throws SQLException {
        String updateString = "insert into orders (description) values (?)";
        
        try (PreparedStatement s = conn.prepareStatement(updateString, PreparedStatement.RETURN_GENERATED_KEYS)) {
            s.setString(1, orderDescription);
            s.executeUpdate();
            
            try (ResultSet rs = s.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
    
    private static void shutdownDerby(EmbeddedXADataSource ds) throws Exception {
        ds.setShutdownDatabase("shutdown");
        
        try { 
            // Calling getConnection will cause the shutdown to be performed.
            ds.getConnection().close();
        } catch (SQLException e)  {
            if (e.getSQLState().equals("08006")) {
                // All is well
            } else {
                throw new Exception("Caught exception during derby shutdown", e);
            }
        }
    }


    private static void doSampleTransaction(TransactionManager tm, AGRepository agrepo, EmbeddedXADataSource derbyds)
            throws Exception {

        XAConnection derbyXAconn = derbyds.getXAConnection();

        try {
            try (Connection derbyconn = derbyXAconn.getConnection()) {
                try (AGRepositoryConnection agconn = agrepo.getConnection()) {

                    // Start a transaction for each participant
                    agconn.begin();
                    derbyconn.setAutoCommit(false);

                    // Notify the transaction manager that we're starting a transaction.
                    tm.begin();

                    try {
                        Transaction tx = tm.getTransaction();

                        // Notify the transaction manager that the connections to the AG repo and the 
                        // Derby DB are participants in the transaction.
                        tx.enlistResource(agconn.getXAResource());
                        tx.enlistResource(derbyXAconn.getXAResource());

                        String orderDesc = "Ice Cream";
                        
                        // Insert an order into the Derby DB and collect the id number of the order.
                        println("Inserting record into Derby");
                        int orderId = insertOrderIntoDerby(derbyconn, orderDesc);
                        
                        // Insert corresponding triples into the AG repo.
                        ValueFactory vf = agrepo.getValueFactory();
                        BNode bnode = vf.createBNode();
                        
                        println("Inserting triples into AG");
                        agconn.add(bnode, org.eclipse.rdf4j.model.vocabulary.RDF.TYPE, vf.createIRI("http:/.franz.com/order"));
                        agconn.add(bnode, vf.createIRI("http://franz.com/orderId"), vf.createLiteral(orderId));
                        agconn.add(bnode, vf.createIRI("http://franz.com/orderDescription"), vf.createLiteral(orderDesc));

                        // Perform 2PC.  The transaction manager will ensure the participants
                        // either both commit or both abort/rollback.
                        // Note: This also have been tm.rollback(), in which case the transaction would
                        // be automatically rolled back for all enlisted participants.
                        println("Performing two-phase commit");
                        tm.commit();
                    } catch (Exception e) {
                        // Cancel the transaction if something unexpected happened.
                        try {
                            tm.rollback();
                        } catch (Exception e2) {
                            e.addSuppressed(e2);                
                        }
                        throw new Exception("Transaction cancelled due to exception", e);
                    }                                
                }
            }
        }
        finally {
            derbyXAconn.close();
        }
    }
    
    private static void run(EmbeddedXADataSource derbyds, AGRepository agrepo) throws Exception {
        // Register the databases with the Atomikos transaction manager so that it can handle
        // transaction recovery on restart if needed.
        com.atomikos.icatch.config.Configuration.addResource(new AGTransactionalResource(agrepo));
        com.atomikos.icatch.config.Configuration.addResource(new JdbcTransactionalResource("localhost", derbyds));

        // Create a UserTransactionManager to manage multi-participant transactions.
        UserTransactionManager tm = new UserTransactionManager();
        tm.init();

        try {
            doSampleTransaction(tm, agrepo, derbyds);
        } finally {
            tm.close();
        }
    }

    public static void main(String[] args) throws Exception {
        try (AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD)) {
            EmbeddedXADataSource derbyds = setupDerbyDB();
            try {
                try (AGRepository agrepo = setupAGRepository(server, AG_REPO_NAME)) {
                    run(derbyds, agrepo);
                } finally  {
                    server.deleteRepository(AG_REPO_NAME, CATALOG_ID);
                }
            } finally {
                shutdownDerby(derbyds);
            }

        }
        println("Tutorial completed");
    }
}
