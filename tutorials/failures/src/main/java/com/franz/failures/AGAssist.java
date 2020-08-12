package com.franz.failures;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * This class shows how one can write code to handle errors
 * in a transaction by re-setting up the connection and retrying
 * the transaction until it succeeds.
 * 
 * Some of the methods of the class combine the steps of setting up
 * a connection to a repository.  By placing this code in a few functions
 * we can then highlight the transaction restart system.
 * 
 * @author jkf
 */
interface RunAssist {

    void run() throws Exception;
}

public class AGAssist {

    public String url;
    public String username;
    public String password;
    public String catalogName;
    public String reponame;
    public AGServer server;
    public AGCatalog catalog;
    public AGRepository repository;
    public AGRepositoryConnection conn;
    public AGValueFactory vf;
    public AGConnPool pool;
    public boolean autoCommit;
    public boolean debug = false;

    public boolean sessions; // using sessions

    public static void println(Object arg) {
        System.out.println(arg);
    }

    public void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setDebug(boolean val) {
        debug = val;
    }

    public AGAssist(String reponame, String username, String password) {
        this(null, null, null, null,
                reponame, username, password);
    }

    public AGAssist(String scheme,
            String server,
            String port,
            final String catalog,
            final String reponame,
            final String username,
            final String password) {

        if (scheme == null) {
            scheme = "http";
        }

        if (server == null) {
            server = "127.0.0.1";
        }

        if (port == null) {
            port = "10035";
        }

        url = scheme + "://" + server + ":" + port;
        this.username = username;
        this.password = password;
        catalogName = catalog;
        this.reponame = reponame;
        this.server = new AGServer(this.url,
                username, password);

        if (catalog == null) {
            this.catalog = this.server.getCatalog();
        } else {
            this.catalog = this.server.getCatalog(catalog);
        }

        if (this.catalog.hasRepository(reponame)) {
            this.repository = this.catalog.openRepository(reponame);
        } else {
            this.repository = this.catalog.createRepository(reponame);
        }

        conn = this.repository.getConnection();
        vf = this.repository.getValueFactory();

    }

    public AGAssist(final AGAssist aga) {
        /* 
		 * make a copy of an existing AGAssist object but create
		 * a new AGRepositoryConnection object.  This is particuarly
		 * useful went the given aga has a connection pool and you wish
		 * to share that pool and takes connections from it.
         */
        url = aga.url;
        catalogName = aga.catalogName;
        server = aga.server;
        catalog = aga.catalog;
        repository = aga.repository;
        pool = aga.pool;
        autoCommit = aga.autoCommit;
        sessions = aga.sessions;
        reponame = aga.reponame;
        username = aga.username;
        password = aga.password;

        handleFail(() -> {
            conn = repository.getConnection();
            vf = repository.getValueFactory();
        });

    }

    public static String getenv(final String name, final String defaultValue) {
        final String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    public void close() {
        conn.close();
    }

    public void useSessions() {

        this.conn.setAutoCommit(false);
        autoCommit = false;
        sessions = true;

    }

    private Resource cvtSubject(String subject, AGValueFactory vf) throws Exception {
        Pattern r = Pattern.compile("<(.*)>");
        Matcher m = r.matcher(subject);
        if (m.find()) {
            return vf.createIRI(m.group(1));
        } else {
            throw new Exception("Resource string " + subject + " should be surrounded by <>");
        }
    }

    private IRI cvtPredicate(String pred, AGValueFactory vf) throws Exception {
        Pattern r = Pattern.compile("<(.*)>");
        Matcher m = r.matcher(pred);
        if (m.find()) {
            return vf.createIRI(m.group(1));
        } else {
            throw new Exception("Resource string " + pred + " should be surrounded by <>");
        }
    }

    private Value cvtObject(String object, AGValueFactory vf) {
        /*
        ** object can be a string like "foo" which is made into a 
        ** literal or a string like <http://foo.com> which is made
        ** into a resource
        */
        Pattern r = Pattern.compile("<(.*)>");
        Matcher m = r.matcher(object);
        if (m.find()) {
            return vf.createIRI(m.group(1));
        } else {
            return vf.createLiteral(object);
        }
    }

    public String resourceString(String local) {
        return resourceString(local, "");
    }

    public String resourceString(String local, String namespace) {
        return "<" + namespace + local + ">";
    }

    public void addTriple(String subject, String predicate, String object) throws Exception {
        AGValueFactory vf = this.vf;

        this.conn.add(cvtSubject(subject, vf),
                cvtPredicate(predicate, vf),
                cvtObject(object, vf));

    }

    // values for the sessionType argument
    // AGConnProp.Session.DEDICATED
    //    Creates a dedicated session yet it turns on AutoCommit: 
    //          Calls AGRepositoryConnection.setAutoCommit(boolean) with true.
    //    This is not very useful as you create a session in order to have transactions
    //    and if you turn on AutoCommit every operation is a committed transaction so having
    //    a session is worthless.
    // AGConnProp.Session.SHARED
    //    No dedicated session, and autoCommit is true 
    //    (that is, AGRepositoryConnection.setAutoCommit(boolean) is not called).
    //    Again this is worthless.  There's no point in pooling non-session repository connections.
    // AGConnProp.Session.TX
    //    Uses sessions and with no AutoCommit. Calls AGRepositoryConnection.setAutoCommit(boolean) with false.
    //    This is really the only value you want to use.
    public void addConnectionPool(Enum<AGConnProp.Session> sessionType, boolean lifo,
            int initialSize, int maxActive) {
        pool = AGConnPool.create(
                AGConnProp.serverUrl, url,
                AGConnProp.username, username,
                AGConnProp.password, password,
                AGConnProp.catalog, catalogName,
                AGConnProp.repository, reponame,
                AGConnProp.session, sessionType,
                AGPoolProp.shutdownHook, true,
                AGPoolProp.maxActive, maxActive,
                AGPoolProp.initialSize, initialSize,
                AGPoolProp.lifo, lifo);

        conn.close();
        repository.setConnPool(pool);
        if (sessionType == AGConnProp.Session.TX || sessionType == AGConnProp.Session.DEDICATED) {
            sessions = true;
            autoCommit = false;
        } else {
            autoCommit = true;
        }
        conn = repository.getConnection();   // get one from the pool

    }

    public void handleFail(RunAssist ra) {

        /*
         * runs a lambda expression and should it fail that expression
	 * is restarted every 5 seconds until it works.
	 * The expression should be  a complete transaction, namely
	 * a set of triple adds and deletes ending in a commit.
	 * If the connections are simple connections using autoCommit then
	 * this should be one operation and no commit is necessary.
         */
        while (true) {
            try {
                ra.run();
                return;

            } catch (Exception e) {
                if (debug) {
                    println("with sessions " + sessions + " tryHandle got exception " + e);
                }
                if (sessions) {
                    reestablishSession();
                } 
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }

    private void reestablishSession() {
        // got an error talking to the server
        // loop waiting to reconnect

        while (true) {
            try {

                // if you have asked for sessions then getConnection() will cause a http request to 
                // be sent to the server to create a session on the server.  Thus if the server
                // is down then getConnection() will throw an Exception.
                conn = repository.getConnection();
                if (debug) {
                    println("reestablish conn is  " + conn);
                }
                vf = repository.getValueFactory();
                conn.setAutoCommit(autoCommit);

                return;
            } catch (Exception e) {
                if (debug) {
                    println("trying to restablish connection, exception " + e);
                }
                // pause allowing time for the error to fix itself
                sleep(5000);

            }
        }
    }

}
