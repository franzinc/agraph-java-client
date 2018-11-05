/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http;

import com.franz.agraph.http.exception.AGCustomStoredProcException;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGJSONArrayHandler;
import com.franz.agraph.http.handler.AGJSONHandler;
import com.franz.agraph.http.handler.AGLongHandler;
import com.franz.agraph.http.handler.AGRDFHandler;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.http.handler.AGStringHandler;
import com.franz.agraph.http.handler.AGBooleanHandler;
import com.franz.agraph.http.handler.AGTQRHandler;
import com.franz.agraph.http.storedproc.AGDeserializer;
import com.franz.agraph.http.storedproc.AGSerializer;
import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGMaterializer;
import com.franz.agraph.repository.AGQuery;
import com.franz.agraph.repository.AGServerVersion;
import com.franz.agraph.repository.AGSpinFunction;
import com.franz.agraph.repository.AGSpinMagicProperty;
import com.franz.agraph.repository.AGUpdate;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.agraph.repository.AGXid;
import com.franz.agraph.repository.WarmupConfig;
import com.franz.agraph.repository.repl.DurabilityLevel;
import com.franz.agraph.repository.repl.DurabilityVisitor;
import com.franz.agraph.repository.repl.TransactionSettings;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.Xid;

/**
 * The HTTP layer for interacting with AllegroGraph.
 */
public class AGHttpRepoClient implements AutoCloseable {

    /**
     * Used when constructing distributed transaction headers, to turn a durability
     * level into a string that can be included in the header.
     */
    private static final DurabilityVisitor<String> durabilityToString =
            new DurabilityVisitor<String>() {
                @Override
                public String visitInteger(final int numberOfInstances) {
                    return "durability=" + Integer.toString(numberOfInstances);
                }

                @Override
                public String visitDurabilityLevel(final DurabilityLevel level) {
                    switch (level) {
                        case MIN:
                            return "durability=min";
                        case MAX:
                            return "durability=max";
                        case QUORUM:
                            return "durability=quorum";
                        case DEFAULT:
                            return "";
                        default:
                            assert false : "Unsupported symbolic durability level.";
                            return "";
                    }
                }
            };
    private static int defaultSessionLifetimeInSeconds = 3600;
    private static NameValuePair[] emptyParams = new NameValuePair[0];
    private static AGServerVersion supportedTSVTQRVersion = new AGServerVersion("6.4.2");
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    // Used to create the pinger task
    private final ScheduledExecutorService executor;
    public ConcurrentLinkedQueue<String> savedQueryDeleteQueue;
    private int lifetimeInSeconds = defaultSessionLifetimeInSeconds;
    // delay using a dedicated session until necessary
    private boolean usingDedicatedSession = false;
    private boolean autoCommit = true;
    private int uploadCommitPeriod = 0;
    // If sessionRoot is set, this is a dedicated session client
    // repoRoot just points to the main SD port URL.
    private String sessionRoot, repoRoot;
    private boolean loadInitFile = false;
    private List<String> scripts = null;
    private TupleQueryResultFormat preferredTQRFormat;
    private BooleanQueryResultFormat preferredBQRFormat = BooleanQueryResultFormat.TEXT;
    private RDFFormat preferredRDFFormat = getDefaultRDFFormat();
    // client is inherited from the AGServer instance from which this instance was created.
    private AGHTTPClient client;
    private AGAbstractRepository repo;
    private boolean allowExternalBlankNodeIds = false;
    // Pinger task handle
    private ScheduledFuture<?> pinger;
    /**
     * When set, pass the contents of this field in the x-user-attributes Header of any request.
     * Assumes that AGHttpRepoClient only delivers requests to repo-based REST services in AG.
     * <p>
     * If this assumption is violated, then we rely on the server to ignore the header, or
     * the methods implementing these requests will need to be updated to bypass the local
     * class method executors (get/post/put/delete) and invoke the AGHTTPClient methods
     * directly as those will not insert the x-user-attributes header.
     * </p>
     */
    private String userAttributes;
    /**
     * Multi-master replication transaction config.
     */
    private TransactionSettings transactionSettings;
    // When true, any request made by this instance will include an `x-rollback' header.
    private boolean sendRollbackHeader = false;

    // Cached to avoid querying the server each time.
    private Boolean hasWarmupBug = null;

    public AGHttpRepoClient(AGAbstractRepository repo, AGHTTPClient client,
                            String repoRoot, String sessionRoot,
                            ScheduledExecutorService executor) {
        this.repo = repo;
        this.sessionRoot = sessionRoot;
        this.repoRoot = repoRoot;
        this.client = client;
        this.executor = executor;
        savedQueryDeleteQueue = new ConcurrentLinkedQueue<>();
    }

    public AGHttpRepoClient(AGAbstractRepository repo, AGHTTPClient client,
                            String repoRoot, String sessionRoot) {
        this(repo, client, repoRoot, sessionRoot, repo.getCatalog().getServer().getExecutor());
    }

    /**
     * Gets the default lifetime of sessions spawned by any instance
     * unless otherwise specified by {@link #setSessionLifetime(int)}
     *
     * @return long  defaultSessionLifetimeInSeconds
     */
    public static long getDefaultSessionLifetime() {
        return defaultSessionLifetimeInSeconds;
    }

    /**
     * Sets the default lifetime of sessions spawned by any instance
     * when none is specified by {@link #setSessionLifetime(int)}
     *
     * <p>Defaults to 3600 seconds (1 hour).</p>
     *
     * @param lifetimeInSeconds Number of seconds before the session times out
     */
    public static void setDefaultSessionLifetime(int lifetimeInSeconds) {
        defaultSessionLifetimeInSeconds = lifetimeInSeconds;
    }

    // Helper method for AGRepositoryConnection
    public static Value getStorableValue(Value v, AGValueFactory vf, boolean allowExternalBlankNodeIds) {
        Value storable = v;
        if (v instanceof BNode && !vf.isAGBlankNodeId(v.stringValue())) {
            if (allowExternalBlankNodeIds) {
                storable = vf.createIRI(vf.PREFIX_FOR_EXTERNAL_BNODES + v.stringValue());
            } else {
                throw new IllegalArgumentException("Cannot store external blank node " + v + " in AllegroGraph with the current settings. Please see javadoc for AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean) for more details and options.");
            }
        }
        return storable;
    }

    /**
     * Returns the application Resource for a given stored resource.
     * <p>
     * This method is intended for use within the AG client library,
     * not for use by applications.
     * </p>
     *
     * @param stored a stored resource
     * @param vf     a value factory that can create new store values
     * @return the application resource
     * @see #getStorableResource(Resource, AGValueFactory)
     * @since v4.4
     */
    public static Resource getApplicationResource(Resource stored, AGValueFactory vf) {
        Resource app = stored;
        if (stored instanceof IRI && vf.isURIForExternalBlankNode(stored)) {
            app = vf.createBNode(stored.stringValue().substring(vf.PREFIX_FOR_EXTERNAL_BNODES.length()));
        }
        return app;
    }

    /**
     * Returns the application Value for a given stored value.
     * <p>
     * This method is intended for use within the AG client library,
     * not for use by applications.
     * </p>
     *
     * @param stored a stored value
     * @param vf     a value factory that can create new store values
     * @return the application value
     * @see #getStorableValue(Value, AGValueFactory)
     * @since v4.4
     */
    public static Value getApplicationValue(Value stored, AGValueFactory vf) {
        Value app = stored;
        if (stored instanceof IRI && vf.isURIForExternalBlankNode(stored)) {
            app = vf.createBNode(stored.stringValue().substring(vf.PREFIX_FOR_EXTERNAL_BNODES.length()));
        }
        return app;
    }

    @Override
    public String toString() {
        return "{" + super.toString()
                + " " + client
                + " rr=" + repoRoot
                + " sr=" + sessionRoot
                + "}";
    }

    public String getRoot() throws AGHttpException {
        if (sessionRoot != null) {
            return sessionRoot;
        } else if (repoRoot != null) {
            return repoRoot;
        } else {
            throw new AGHttpException("This session-only connection has been closed. Re-open a new one to start using it again.");
        }
    }

    /**
     * Checks the System property com.franz.agraph.http.useMainPortForSessions
     * (defaults to false).
     *
     * @return boolean  true if proxying sessions through the main server port.
     */
    public boolean usingMainPortForSessions() {
        boolean b = Boolean.parseBoolean(System.getProperty("com.franz.agraph.http.useMainPortForSessions", "false"));
        logger.debug("com.franz.agraph.http.useMainPortForSessions=" + b);
        return b;
    }

    private boolean overrideServerUseMainPortForSessions() {
        boolean b = Boolean.parseBoolean(System.getProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions", "false"));
        logger.debug("com.franz.agraph.http.overrideServerUseMainPortForSessions=" + b);
        return b;
    }

    public AGValueFactory getValueFactory() {
        return repo.getValueFactory();
    }

    /**
     * Returns the 'lifetime' for a dedicated session spawned by this instance.
     *
     * @return lifetime in seconds
     */
    public int getSessionLifetime() {
        return lifetimeInSeconds;
    }

    /**
     * Sets the 'lifetime' for a dedicated session spawned by this instance.
     *
     * @param lifetimeInSeconds an integer number of seconds
     */
    public void setSessionLifetime(int lifetimeInSeconds) {
        this.lifetimeInSeconds = lifetimeInSeconds;
    }

    /**
     * Returns true if the initfile (if any) will be loaded by dedicated
     * sessions spawned by this instance.
     *
     * @return boolean  the current setting of 'loadInitFile'
     */
    public boolean getSessionLoadInitFile() {
        return loadInitFile;
    }

    /**
     * When true, dedicated sessions will load the initfile upon creation.
     *
     * @param loadInitFile boolean indicating the new value to set
     */
    public void setSessionLoadInitFile(boolean loadInitFile) {
        this.loadInitFile = loadInitFile;
    }

    /**
     * Adds a 'script' for a dedicated session spawned by this instance.
     *
     * @param scriptName the name of the script to be added to the list of scripts
     *                   loaded for this session
     */
    public void addSessionLoadScript(String scriptName) {
        if (this.scripts == null) {
            this.scripts = new ArrayList<>();
        }
        this.scripts.add(scriptName);
    }

    /**
     * Fetch the preferred {@link TupleQueryResultFormat}.
     *
     * @return TupleQueryResultFormat  the current preferred format
     */
    public TupleQueryResultFormat getPreferredTQRFormat() {
        if (preferredTQRFormat == null) {
            if (repo.getServer().getComparableVersion().compareTo(supportedTSVTQRVersion) >= 0) {
                preferredTQRFormat = TupleQueryResultFormat.TSV;
            } else {
                preferredTQRFormat = TupleQueryResultFormat.SPARQL;
            }
        }
        return preferredTQRFormat;
    }

    /**
     * Set the preferred {@link TupleQueryResultFormat}
     *
     * @param preferredTQRFormat the new value to set as preferred
     */
    public void setPreferredTQRFormat(TupleQueryResultFormat preferredTQRFormat) {
        this.preferredTQRFormat = preferredTQRFormat;
    }

    public BooleanQueryResultFormat getPreferredBQRFormat() {
        return preferredBQRFormat;
    }

    public void setPreferredBQRFormat(
            BooleanQueryResultFormat preferredBQRFormat) {
        this.preferredBQRFormat = preferredBQRFormat;
    }

    /**
     * Gets the default RDFFormat to use in making requests that
     * return RDF statements; the format should support contexts.
     * <p>
     * Gets System property com.franz.agraph.http.defaultRDFFormat
     * (NQUADS and TRIX are currently supported), defaults to TRIX
     * if the property is not present, and returns the corresponding
     * RDFFormat.
     * </p>
     *
     * @return an RDFFormat, either NQUADS or TRIX
     */
    public RDFFormat getDefaultRDFFormat() {
        RDFFormat format = System.getProperty("com.franz.agraph.http.defaultRDFFormat", "TRIX").equalsIgnoreCase("NQUADS") ? RDFFormat.NQUADS : RDFFormat.TRIX;
        logger.debug("Defaulting to " + format.getDefaultMIMEType() + " for requests that return RDF statements.");
        return format;
    }

    /**
     * Gets the RDFFormat to use in making requests that return
     * RDF statements.
     * <p>
     * Defaults to the format returned by {@link #getDefaultRDFFormat()}
     * </p>
     *
     * @return an RDFFormat, either NQUADS or TRIX
     */
    public RDFFormat getPreferredRDFFormat() {
        return preferredRDFFormat;
    }

    /**
     * Sets the RDFFormat to use in making requests that return
     * RDF statements; the format must support contexts.
     * <p>
     * AGRDFFormat.NQUADS and RDFFormat.TRIX are currently supported.
     * Defaults to the format returned by {@link #getDefaultRDFFormat()}
     * </p>
     *
     * @param preferredRDFFormat The new {@link RDFFormat} to set as preferred
     */
    public void setPreferredRDFFormat(RDFFormat preferredRDFFormat) {
        logger.debug("Defaulting to " + preferredRDFFormat.getDefaultMIMEType() + " for requests that return RDF statements.");
        this.preferredRDFFormat = preferredRDFFormat;
    }

    public String getServerURL() {
        return client.getServerURL();
    }

    public AGHTTPClient getHTTPClient() {
        return client;
    }

    /**
     * Takes a List of existing Headers or null, adds the x-user-attribute header
     * if userAttributes is set, and returns an array representation of the List.
     * <p>
     * This method is written to be last operation performed on a set of headers before invoking
     * an HTTP request method on the current connection's AGHTTPClient, as it converts the
     * headers into the type accepted by the AGHTTPClient method executors.
     * </p>
     *
     * @param headers, a List<Header> of headers
     * @return headers[], the headers converted to an array
     */
    private Header[] prepareHeaders(List<Header> headers) {

        if (headers == null) {
            headers = new ArrayList<>(0);
        }

        if (userAttributes != null) {
            headers.add(new Header(AGProtocol.USER_ATTRIBUTE_HEADER, userAttributes));
        }
        if (sendRollbackHeader) {
            headers.add(new Header(AGProtocol.X_ROLLBACK_HEADER, "yes"));
        }
        addReplHeader(headers);
        return headers.toArray(new Header[headers.size()]);
    }

    private void addReplHeader(List<Header> headers) {
        // Do not add any headers if there is no transaction config.
        if (transactionSettings == null) {
            return;
        }

        // Accumulate the header value here
        final StringBuilder value = new StringBuilder();

        value.append(transactionSettings.visitDurability(durabilityToString));

        final Integer distributedTransactionTimeout =
                transactionSettings.getDistributedTransactionTimeout();
        if (distributedTransactionTimeout != null) {
            if (value.length() > 0) {
                value.append(' ');
            }
            value.append("distributedTransactionTimeout=");
            value.append(distributedTransactionTimeout.toString());
        }

        final Integer transactionLatencyCount =
                transactionSettings.getTransactionLatencyCount();
        if (transactionLatencyCount != null) {
            if (value.length() > 0) {
                value.append(' ');
            }
            value.append("transactionLatencyCount=");
            value.append(transactionLatencyCount.toString());
        }

        final Integer transactionLatencyTimeout =
                transactionSettings.getTransactionLatencyTimeout();
        if (transactionLatencyTimeout != null) {
            if (value.length() > 0) {
                value.append(' ');
            }
            value.append("transactionLatencyTimeout=");
            value.append(transactionLatencyTimeout.toString());
        }

        if (value.length() > 0) {
            headers.add(new Header(AGProtocol.X_REPL_SETTINGS, value.toString()));
        }
    }

    private NameValuePair[] prepareParams(Collection<? extends NameValuePair> params) {
        if (params != null) {
            return params.toArray(new NameValuePair[params.size()]);
        }
        return emptyParams;
    }

    protected void get(String url, List<Header> headers,
                       Collection<? extends NameValuePair> params,
                       AGResponseHandler handler) throws AGHttpException {

        getHTTPClient().get(url, prepareHeaders(headers), prepareParams(params),
                handler);
    }

    protected void post(String url, List<Header> headers,
                        Collection<? extends NameValuePair> params,
                        RequestEntity requestEntity, AGResponseHandler handler) throws AGHttpException {

        getHTTPClient().post(url, prepareHeaders(headers), prepareParams(params),
                requestEntity, handler);
    }

    protected void put(String url, List<Header> headers,
                       Collection<? extends NameValuePair> params,
                       RequestEntity requestEntity, AGResponseHandler handler) throws AGHttpException {

        getHTTPClient().put(url, prepareHeaders(headers), prepareParams(params),
                requestEntity, handler);
    }

    protected void delete(String url, List<Header> headers,
                          Collection<? extends NameValuePair> params,
                          AGResponseHandler handler) throws AGHttpException {

        getHTTPClient().delete(url, prepareHeaders(headers), prepareParams(params),
                handler);
    }

    private void useDedicatedSession(boolean autoCommit)
            throws AGHttpException {
        if (sessionRoot == null) {
            String url = AGProtocol.getSessionURL(getRoot());
            List<NameValuePair> params = new ArrayList<>(3);
            params.add(new NameValuePair(AGProtocol.LIFETIME_PARAM_NAME,
                    Integer.toString(lifetimeInSeconds)));
            params.add(new NameValuePair(AGProtocol.AUTOCOMMIT_PARAM_NAME,
                    Boolean.toString(autoCommit)));
            params.add(new NameValuePair(AGProtocol.LOAD_INIT_FILE_PARAM_NAME,
                    Boolean.toString(loadInitFile)));
            if (scripts != null) {
                for (String script : scripts) {
                    params.add(new NameValuePair("script", script));
                }
            }
            AGStringHandler handler = new AGStringHandler();
            post(url, null, params, null, handler);
            usingDedicatedSession = true;
            sessionRoot = adjustSessionUrlIfUsingMainPort(handler.getResult());
            startPinger();
            if (logger.isDebugEnabled()) {
                logger.debug("openSession: {}", sessionRoot);
            }
        }
    }

    /**
     * Starts the periodic pinger task.
     * This must be invoked at most once.
     */
    private void startPinger() {
        assert pinger == null;

        // Exit if disabled.
        if (executor == null) {
            return;
        }

        // Convert to ms, cut in half to avoid timeouts if the task
        // is executed a bit later than expected.
        final long delay = getSessionLifetime() * 1000 / 2;
        // Randomize the initial delay
        final long initialDelay = (long) (Math.random() * delay);
        pinger = executor.scheduleWithFixedDelay(() -> {
            try {
                // Note - this will execute concurrently with other
                // connection activity. This is ok, since
                // AGHttpClient uses a connection pool to allow
                // concurrent access.
                ping();
            } catch (final AGHttpException e) {
                // Pinger errors are normal when shutting down...
                logger.debug("Pinger exception", e);
            }
        }, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancel the periodic pinger task if it exists.
     */
    private void stopPinger() {
        if (pinger != null) {
            pinger.cancel(false);
        }
    }

    /**
     * Returns an appropriate session url to use.
     * <p>
     * If the UseMainPortForSessions server configuration option
     * is false (the default), the server provides a session url
     * seizing a dedicated port to communicate with the
     * appropriate server backend for this session. Sometimes
     * clients may prefer to use the main port for all
     * communication, as dictated by the system property
     * com.franz.agraph.http.usingMainPortForSessions=true. When
     * this flag is set, this method returns a modified session
     * url that allows the client to communicate via the main port
     * for this session.
     * </p>
     * <p>
     * If the UseMainPortForSessions server configuration option
     * is true, then this does nothing as the session url provided
     * by the server already points to the main port.
     * </p>
     *
     * @param dedicatedSessionUrl the dedicated session url from the server
     * @return an appropriate session url, possibly using main port instead
     * @throws AGHttpException
     */
    private String adjustSessionUrlIfUsingMainPort(String dedicatedSessionUrl) throws AGHttpException {
        boolean usesMainPort = dedicatedSessionUrl.contains("/session/");
        if (usingMainPortForSessions() && !usesMainPort) {
                        /* Turn the url that looks like
                         * http://localhost:<session-port>/...
                         * into
                         * http://localhost:<frontend-port>/session/<session-port>/... */
            String tail = dedicatedSessionUrl.substring(dedicatedSessionUrl.lastIndexOf(':') + 1);
            String port = tail.substring(0, tail.indexOf('/'));
            try {
                Integer.parseInt(port);
            } catch (NumberFormatException e) {
                throw new AGHttpException(
                        "problem finding port in session url: " + tail);
            }
            return repoRoot + "/session/" + tail;
        } else if (!usingMainPortForSessions() && usesMainPort
                && overrideServerUseMainPortForSessions()) {
                /* Turn the url that looks like
                 * http://localhost:<frontend-port>/session/<session-port>/
                 * into
                 * http://localhost:<session-port> */
            int mainPortStart = dedicatedSessionUrl.lastIndexOf(":") + 1;
            int mainPortEnd = dedicatedSessionUrl.indexOf("/", mainPortStart);
            int sessionPortStart = dedicatedSessionUrl.indexOf("/session/") + 9;
            int sessionPortEnd = dedicatedSessionUrl.indexOf("/", sessionPortStart);
            return (dedicatedSessionUrl.substring(0, mainPortStart)
                    + dedicatedSessionUrl.substring(sessionPortStart, sessionPortEnd)
                    + dedicatedSessionUrl.substring(sessionPortEnd));
        } else {
            return dedicatedSessionUrl;
        }
    }

    private void closeSession() throws
            AGHttpException {
        if (sessionRoot != null && !getHTTPClient().isClosed()) {
            stopPinger();
            String url = AGProtocol.getSessionCloseLocation(sessionRoot);
            try {
                post(url, null, null, null, null);
                if (logger.isDebugEnabled()) {
                    logger.debug("closeSession: {}", url);
                }
            } catch (AGHttpException c) {
                // Assume that the session was already closed.
            } finally {
                sessionRoot = null;
            }
        }
    }

    public void getStatements(Resource subj, IRI pred, Value obj,
                              String includeInferred, RDFHandler handler, Resource... contexts)
            throws RDFHandlerException, AGHttpException {
        getStatements(subj, pred, obj, includeInferred,
                new AGRDFHandler(getPreferredRDFFormat(), handler,
                        getValueFactory(), getAllowExternalBlankNodeIds()),
                contexts);
    }

    public void getStatements(Resource subj, IRI pred, Value obj,
                              String includeInferred,
                              AGResponseHandler handler, Resource... contexts)
            throws AGHttpException {
        String uri = Protocol.getStatementsLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredRDFFormat().getDefaultMIMEType()));


        AGValueFactory vf = getValueFactory();
        List<NameValuePair> params = new ArrayList<>(5);
        if (subj != null) {
            subj = getStorableResource(subj, vf);
            params.add(new NameValuePair(Protocol.SUBJECT_PARAM_NAME, Protocol
                    .encodeValue(subj)));
        }
        if (pred != null) {
            params.add(new NameValuePair(Protocol.PREDICATE_PARAM_NAME,
                    Protocol.encodeValue(pred)));
        }
        if (obj != null) {
            obj = getStorableValue(obj, vf);
            params.add(new NameValuePair(Protocol.OBJECT_PARAM_NAME, Protocol
                    .encodeValue(obj)));
        }
        for (Resource ctx : contexts) {
            ctx = getStorableResource(ctx, vf);
            params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
                    Protocol.encodeContext(ctx)));
        }
        params.add(new NameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME,
                includeInferred));

        get(uri, headers, params, handler);
    }

    public void getStatements(RDFHandler handler, String... ids) throws AGHttpException {
        getStatements(
                new AGRDFHandler(
                        getPreferredRDFFormat(),
                        handler,
                        getValueFactory(),
                        getAllowExternalBlankNodeIds()),
                ids);
    }

    public void getStatements(AGResponseHandler handler, String... ids) throws AGHttpException {
        String uri = Protocol.getStatementsLocation(getRoot()) + "/id";
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredRDFFormat().getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(5);
        for (String id : ids) {
            params.add(new NameValuePair("id", id));
        }

        get(uri, headers, params, handler);
    }

    public void addStatements(Resource subj, IRI pred, Value obj,
                              Resource... contexts) throws AGHttpException {
        String uri = Protocol.getStatementsLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header("Content-Type",
                RDFFormat.NTRIPLES.getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(5);
        if (subj != null) {
            params.add(new NameValuePair(Protocol.SUBJECT_PARAM_NAME, Protocol
                    .encodeValue(subj)));
        }
        if (pred != null) {
            params.add(new NameValuePair(Protocol.PREDICATE_PARAM_NAME,
                    Protocol.encodeValue(pred)));
        }
        if (obj != null) {
            params.add(new NameValuePair(Protocol.OBJECT_PARAM_NAME, Protocol
                    .encodeValue(obj)));
        }
        for (String encodedContext : Protocol.encodeContexts(contexts)) {
            params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
                    encodedContext));
        }
        post(uri, headers, params, null, null);
    }

    public void deleteStatements(Resource subj, IRI pred, Value obj,
                                 Resource... contexts) throws AGHttpException {
        String url = Protocol.getStatementsLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(5);
        if (subj != null) {
            params.add(new NameValuePair(Protocol.SUBJECT_PARAM_NAME, Protocol
                    .encodeValue(subj)));
        }
        if (pred != null) {
            params.add(new NameValuePair(Protocol.PREDICATE_PARAM_NAME,
                    Protocol.encodeValue(pred)));
        }
        if (obj != null) {
            params.add(new NameValuePair(Protocol.OBJECT_PARAM_NAME, Protocol
                    .encodeValue(obj)));
        }
        for (String encodedContext : Protocol.encodeContexts(contexts)) {
            params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
                    encodedContext));
        }
        delete(url, null, params, null);
    }

    public boolean isDedicatedSession() {
        return usingDedicatedSession;
    }

    public boolean isAutoCommit() throws AGHttpException {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) throws AGHttpException {
        useDedicatedSession(autoCommit);
        String url = AGProtocol.getAutoCommitLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(1);

        params.add(new NameValuePair(AGProtocol.ON_PARAM_NAME,
                Boolean.toString(autoCommit)));
        post(url, null, params, null, null);
        this.autoCommit = autoCommit;
    }

    /**
     * Gets the commit period used when uploading statements.
     *
     * @return int  The value of the current commit period in triples
     * @see #setUploadCommitPeriod(int)
     */
    public int getUploadCommitPeriod() {
        return uploadCommitPeriod;
    }

    /**
     * Sets the commit period to use when uploading statements.
     * <p>
     * Causes a commit to happen after every period=N added statements
     * inside a call to
     * </p>
     * <p>
     * {@link #upload(String, RequestEntity, String, boolean, String, IRI, RDFFormat, Resource...)}
     * </p>
     * <p>
     * Defaults to period=0, meaning that no commits are done in an upload.
     * </p>
     * <p>
     * Setting period &gt; 0 can be used to work around the fact that
     * uploading a huge amount of statements in a single transaction
     * will require excessive amounts of memory.
     * </p>
     *
     * @param period A non-negative integer
     * @see #getUploadCommitPeriod()
     */
    public void setUploadCommitPeriod(int period) {
        if (period < 0) {
            throw new IllegalArgumentException("upload commit period must be non-negative.");
        }
        uploadCommitPeriod = period;
    }

    public enum CommitPhase {
        PREPARE("prepare"), COMMIT("commit");

        private String string;

        CommitPhase(String string) {
            this.string = string;
        }

        public String toString() {
            return string;
        }

    }

    /**
     * Prepares or finalizes a previous prepared commit depending on the value of
     * phase.
     *
     * @param phase CommitPhase.PREPARE or CommitPhase.COMMIT
     * @param xid The transaction xid
     * @throws AGHttpException if an error occurs.
     */
    public void commit(CommitPhase phase, Xid xid) throws AGHttpException {
        String url = getRoot() + "/" + AGProtocol.COMMIT;

        List<NameValuePair> params = new ArrayList<NameValuePair>(2);

        params.add(new NameValuePair(AGProtocol.COMMIT_PHASE, phase.toString()));
        params.add(new NameValuePair(AGProtocol.XID, new AGXid(xid).toString()));

        post(url, null, params, (RequestEntity) null, null);
    }

    public void commit() throws AGHttpException {
        String url = getRoot() + "/" + AGProtocol.COMMIT;

        post(url, null, null, null, null);
    }

    public void rollback() throws AGHttpException {
        String url = getRoot() + "/" + AGProtocol.ROLLBACK;

        post(url, null, null, null, null);
    }

    /**
     * Aborts a previously prepared commit
     * @param xid The transaction id of the prepared commit to abort.
     * @throws AGHttpException if an error occurs.
     */

    public void rollback(Xid xid) throws AGHttpException {
        String url = getRoot() + "/" + AGProtocol.ROLLBACK;

        List<NameValuePair> params = new ArrayList<NameValuePair>(2);

        params.add(new NameValuePair(AGProtocol.XID, new AGXid(xid).toString()));

        post(url, null, params, (RequestEntity) null, null);
    }

    /**
     * @return an array of Xids of transactions that are in the prepared state
     * on the server.
     * @throws DecoderException if the response from the server is invalid.
     */
    public Xid[] getPreparedTransactions() throws DecoderException {
        String url = getRoot() + "/" + AGProtocol.GET_PREPARED_TRANSACTIONS;

        List<Header> headers = new ArrayList<Header>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME, "application/json"));

        AGJSONArrayHandler handler = new AGJSONArrayHandler();

        get(url, headers, null, handler);

        List<Xid> res = new ArrayList<Xid>();

        for (Object entry : handler.getResult().toList()) {
            res.add(AGXid.AGXidFromString((String) entry));
        }

        return res.toArray(new Xid[0]);
    }

    /**
     * When set to true, an x-rollback header will be passed with each request
     * made by this AGHttpRepoClient instance.
     *
     * @param sendRollbackHeader boolean indicating whether each request
     *                           should include a rollback header.
     */
    public void setSendRollbackHeader(boolean sendRollbackHeader) {
        this.sendRollbackHeader = sendRollbackHeader;
    }

    public void clearNamespaces() throws AGHttpException {
        String url = Protocol.getNamespacesLocation(getRoot());

        delete(url, null, null, null);
    }

    public void upload(final Reader contents, String baseURI,
                       final RDFFormat dataFormat, boolean overwrite,
                       JSONObject attributes, Resource... contexts)
            throws RDFParseException, AGHttpException {
        final Charset charset = dataFormat.hasCharset() ? dataFormat
                .getCharset() : Charset.forName("UTF-8");

        RequestEntity entity = new RequestEntity() {

            public long getContentLength() {
                return -1; // don't know
            }

            public String getContentType() {
                String format = dataFormat.getDefaultMIMEType();
                return format + "; charset="
                        + charset.name();
            }

            public boolean isRepeatable() {
                return false;
            }

            public void writeRequest(OutputStream out) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(out, charset);
                IOUtil.transfer(contents, writer);
                writer.flush();
            }
        };

        upload(entity, baseURI, overwrite, null, null, null, attributes, contexts);
    }

    public void upload(InputStream contents, String baseURI,
                       RDFFormat dataFormat, boolean overwrite,
                       JSONObject attributes, Resource... contexts)
            throws RDFParseException, AGHttpException {
        upload(contents, baseURI, dataFormat, overwrite, -1, null, attributes, contexts);
    }

    public void upload(InputStream contents, String baseURI,
                       RDFFormat dataFormat, boolean overwrite,
                       long size, String contentEncoding,
                       JSONObject attributes, Resource... contexts)
            throws RDFParseException, AGHttpException {
        String format = dataFormat.getDefaultMIMEType();
        RequestEntity entity = new InputStreamRequestEntity(contents, size, format);
        upload(entity, baseURI, overwrite, null, null, null, attributes, contentEncoding, contexts);
    }

    public void sendRDFTransaction(InputStream rdftransaction) throws AGHttpException {
        RequestEntity entity = new InputStreamRequestEntity(rdftransaction, -1,
                "application/x-rdftransaction");
        upload(entity, null, false, null, null, null, null);
    }

    public void sendRDFTransaction(InputStream rdftransaction, JSONObject attributes)
            throws AGHttpException {
        RequestEntity entity = new InputStreamRequestEntity(rdftransaction, -1,
                "application/x-rdftransaction");
        upload(entity, null, false, null, null, null, attributes);
    }

    public void uploadJSON(JSONArray rows, Resource... contexts)
            throws AGHttpException {
        String url = Protocol.getStatementsLocation(getRoot());
        uploadJSON(url, rows, contexts);
    }

    public void uploadJSON(String url, JSONArray rows, Resource... contexts)
            throws AGHttpException {
        if (rows == null) {
            return;
        }

        try {
            final String data = rows.toString();
            RequestEntity entity = new StringRequestEntity(
                    data, "application/json", "UTF-8");
            upload(url, entity, null, false, null, null, null, contexts);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteJSON(JSONArray rows, Resource... contexts)
            throws AGHttpException {
        uploadJSON(AGProtocol.getStatementsDeleteLocation(getRoot()), rows, contexts);
    }

    public void load(IRI source, String baseURI, RDFFormat dataFormat,
                     Resource... contexts) throws AGHttpException {
        upload(null, baseURI, false, null, source, dataFormat, null, contexts);
    }

    public void load(IRI source, String baseURI, RDFFormat dataFormat,
                     JSONObject attributes, Resource... contexts) throws AGHttpException {
        upload(null, baseURI, false, null, source, dataFormat,
                attributes, contexts);
    }

    public void load(String serverAbsolutePath, String baseURI,
                     RDFFormat dataFormat, Resource... contexts) throws
            AGHttpException {
        upload(null, baseURI, false, serverAbsolutePath, null, dataFormat,
                null, contexts);
    }

    public void load(String serverAbsolutePath, String baseURI,
                     RDFFormat dataFormat, JSONObject attributes,
                     Resource... contexts) throws AGHttpException {
        upload(null, baseURI, false, serverAbsolutePath, null, dataFormat,
                attributes, contexts);
    }

    public void upload(RequestEntity reqEntity, String baseURI,
                       boolean overwrite, String serverSideFile, IRI serverSideURL,
                       RDFFormat dataFormat, JSONObject attributes,
                       Resource... contexts) throws AGHttpException {
        String url = Protocol.getStatementsLocation(getRoot());
        upload(url, reqEntity, baseURI, overwrite, serverSideFile, serverSideURL,
                dataFormat, attributes, contexts);
    }

    public void upload(RequestEntity reqEntity, String baseURI,
                       boolean overwrite, String serverSideFile, IRI serverSideURL,
                       RDFFormat dataFormat, JSONObject attributes, String contentEncoding,
                       Resource... contexts) throws AGHttpException {
        String url = Protocol.getStatementsLocation(getRoot());
        upload(url, reqEntity, baseURI, overwrite, serverSideFile, serverSideURL,
                dataFormat, attributes, contentEncoding, contexts);
    }

    /*
     * This method is called by the uploadJSON methods, which already have attributes
     * built into the input stream passed as part of the reqEntity.
     */
    public void upload(String url, RequestEntity reqEntity, String baseURI,
                       boolean overwrite, String serverSideFile, IRI serverSideURL,
                       RDFFormat dataFormat, Resource... contexts) throws AGHttpException {
        upload(url, reqEntity, baseURI, overwrite, serverSideFile, serverSideURL,
                dataFormat, null, contexts);
    }

    public void upload(String url, RequestEntity reqEntity, String baseURI,
                       boolean overwrite, String serverSideFile, IRI serverSideURL,
                       RDFFormat dataFormat, JSONObject attributes,
                       Resource... contexts) throws AGHttpException {
        upload(url, reqEntity, baseURI, overwrite, serverSideFile, serverSideURL,
                dataFormat, attributes, null, contexts);
    }

    public void upload(String url, RequestEntity reqEntity, String baseURI,
                       boolean overwrite, String serverSideFile, IRI serverSideURL,
                       RDFFormat dataFormat, JSONObject attributes, String contentEncoding,
                       Resource... contexts) throws AGHttpException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        List<Header> headers = new ArrayList<>(1);
        if (dataFormat != null) {
            String format = dataFormat.getDefaultMIMEType();
            headers.add(new Header("Content-Type", format));
        }
        if (contentEncoding != null) {
            headers.add(new Header("Content-Encoding", contentEncoding));
        }
        List<NameValuePair> params = new ArrayList<>(5);
        for (String encodedContext : Protocol.encodeContexts(contexts)) {
            params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
                    encodedContext));
        }
        if (baseURI != null && baseURI.trim().length() != 0) {
            String encodedBaseURI = Protocol.encodeValue(getValueFactory().createIRI(baseURI));
            params.add(new NameValuePair(Protocol.BASEURI_PARAM_NAME,
                    encodedBaseURI));
        }
        if (uploadCommitPeriod > 0) {
            params.add(new NameValuePair("commit", Integer.toString(uploadCommitPeriod)));
        }
        if (serverSideFile != null && serverSideFile.trim().length() != 0) {
            params.add(new NameValuePair(AGProtocol.FILE_PARAM_NAME,
                    serverSideFile));
        }
        if (serverSideURL != null) {
            params.add(new NameValuePair(AGProtocol.URL_PARAM_NAME,
                    serverSideURL.stringValue()));
        }
        if (attributes != null) {
            params.add(new NameValuePair(AGProtocol.ATTRIBUTES_PARAM_NAME, attributes.toString()));
        }
        if (!overwrite) {
            post(url, headers, params,
                    reqEntity, null);
        } else {
            // TODO: overwrite
            throw new UnsupportedOperationException();
        }
    }

    public TupleQueryResult getContextIDs() throws
            AGHttpException {
        try {
            TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
            getContextIDs(builder);
            return builder.getQueryResult();
        } catch (TupleQueryResultHandlerException e) {
            // Found a bug in TupleQueryResultBuilder?
            throw new RuntimeException(e);
        }
    }

    public void getContextIDs(TupleQueryResultHandler handler)
            throws TupleQueryResultHandlerException,
            AGHttpException {
        String url = Protocol.getContextsLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredTQRFormat().getDefaultMIMEType()));

        get(url, headers, null,
                new AGTQRHandler(getPreferredTQRFormat(), handler, getValueFactory(), getAllowExternalBlankNodeIds()));
    }

    public long size(Resource... contexts) throws
            AGHttpException {
        String url = Protocol.getSizeLocation(getRoot());

        String[] encodedContexts = Protocol.encodeContexts(contexts);
        List<NameValuePair> contextParams = new ArrayList<>(encodedContexts.length);
        for (String encodedContext : encodedContexts) {
            contextParams.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
                    encodedContext));
        }
        AGLongHandler handler = new AGLongHandler();
        get(url, null, contextParams, handler);
        return handler.getResult();
    }

    public TupleQueryResult getNamespaces() throws
            AGHttpException {
        try {
            TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
            getNamespaces(builder);
            return builder.getQueryResult();
        } catch (TupleQueryResultHandlerException e) {
            // Found a bug in TupleQueryResultBuilder?
            throw new RuntimeException(e);
        }
    }

    public void getNamespaces(TupleQueryResultHandler handler)
            throws TupleQueryResultHandlerException,
            AGHttpException {
        String url = Protocol.getNamespacesLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredTQRFormat().getDefaultMIMEType()));

        get(url, headers, null,
                new AGTQRHandler(getPreferredTQRFormat(), handler, getValueFactory(), getAllowExternalBlankNodeIds()));
    }

    public String getNamespace(String prefix) throws
            AGHttpException {
        String url = Protocol.getNamespacePrefixLocation(getRoot(),
                prefix);

        AGStringHandler handler = new AGStringHandler();
        try {
            get(url, null, null, handler);
        } catch (AGHttpException e) {
            if (!e.getMessage().equals("Not found.")) {
                throw e;
            }
        }
        return handler.getResult();
    }

    public void setNamespacePrefix(String prefix, String name)
            throws AGHttpException {
        String url = Protocol.getNamespacePrefixLocation(getRoot(),
                prefix);

        try {
            put(url, null, null,
                    new StringRequestEntity(name, "text/plain", "UTF-8"), null);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeNamespacePrefix(String prefix) throws AGHttpException {
        String url = Protocol.getNamespacePrefixLocation(getRoot(),
                prefix);

        delete(url, null, null, null);
    }

    public void query(AGQuery q, boolean analyzeOnly, AGResponseHandler handler) throws
            AGHttpException {

        String url = getRoot();
        if (q.isPrepared()) {
            processSavedQueryDeleteQueue();
            url = AGProtocol.getSavedQueryLocation(url, q.getName());
        }
        List<Header> headers = new ArrayList<>(5);
        headers.add(new Header("Content-Type", Protocol.FORM_MIME_TYPE
                + "; charset=utf-8"));
        if (handler.getRequestMIMEType() != null) {
            headers.add(new Header(Protocol.ACCEPT_PARAM_NAME, handler
                    .getRequestMIMEType()));
        }
        List<NameValuePair> queryParams = getQueryMethodParameters(q);
        if (analyzeOnly) {
            queryParams.add(new NameValuePair("analyzeIndicesUsed", "true"));
        }
        post(url, headers, queryParams, null, handler);
        if (sessionRoot != null && q.getName() != null) {
            q.setPrepared(true);
        }
    }

    protected List<NameValuePair> getQueryMethodParameters(AGQuery q) {
        QueryLanguage ql = q.getLanguage();
        Dataset dataset = q.getDataset();
        boolean includeInferred = q.getIncludeInferred();
        String planner = q.getPlanner();
        Binding[] bindings = q.getBindingsArray();
        String save = q.getName();

        List<NameValuePair> queryParams = new ArrayList<>(
                bindings.length + 10);

        if (!q.isPrepared()) {
            queryParams.add(new NameValuePair(Protocol.QUERY_LANGUAGE_PARAM_NAME,
                    ql.getName()));
            if (q instanceof AGUpdate) {
                queryParams.add(new NameValuePair(Protocol.UPDATE_PARAM_NAME, q.getQueryString()));
            } else {
                queryParams.add(new NameValuePair(Protocol.QUERY_PARAM_NAME, q.getQueryString()));
            }
            if (q.getBaseURI() != null) {
                queryParams.add(new NameValuePair(Protocol.BASEURI_PARAM_NAME, q.getBaseURI()));
            }
            if (q.getMaxExecutionTime() > 0) {
                queryParams.add(new NameValuePair(Protocol.TIMEOUT_PARAM_NAME, Integer.toString(q.getMaxExecutionTime())));
            }
            queryParams.add(new NameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME,
                    Boolean.toString(includeInferred)));
            if (q.isCheckVariables()) {
                queryParams.add(new NameValuePair(AGProtocol.CHECK_VARIABLES,
                        Boolean.toString(q.isCheckVariables())));
            }
            if (q.getLimit() >= 0) {
                queryParams.add(new NameValuePair(AGProtocol.LIMIT_PARAM_NAME,
                        Integer.toString(q.getLimit())));
            }
            if (q.getOffset() >= 0) {
                queryParams.add(new NameValuePair("offset",
                        Integer.toString(q.getOffset())));
            }
            if (q.isLoggingEnabled()) {
                queryParams.add(new NameValuePair("logQuery", "true"));
            }
            if (planner != null) {
                queryParams.add(new NameValuePair(AGProtocol.PLANNER_PARAM_NAME,
                        planner));
            }
            if (q.getEngine() != null) {
                queryParams.add(new NameValuePair("engine", q.getEngine()));
            }

            if (sessionRoot != null && save != null) {
                queryParams.add(new NameValuePair(AGProtocol.SAVE_PARAM_NAME,
                        save));
            }

            if (ql == QueryLanguage.SPARQL && dataset != null) {
                if (q instanceof AGUpdate) {
                    for (IRI graphURI : dataset.getDefaultRemoveGraphs()) {
                        queryParams.add(new NameValuePair(Protocol.REMOVE_GRAPH_PARAM_NAME, String.valueOf(graphURI)));
                    }
                    if (dataset.getDefaultInsertGraph() != null) {
                        queryParams.add(new NameValuePair(Protocol.INSERT_GRAPH_PARAM_NAME, String.valueOf(dataset.getDefaultInsertGraph())));
                    }
                    for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
                        queryParams.add(new NameValuePair(Protocol.USING_GRAPH_PARAM_NAME, String.valueOf(defaultGraphURI)));
                    }
                    for (IRI namedGraphURI : dataset.getNamedGraphs()) {
                        queryParams.add(new NameValuePair(Protocol.USING_NAMED_GRAPH_PARAM_NAME, String.valueOf(namedGraphURI)));
                    }
                } else {
                    for (IRI defaultGraphURI : dataset.getDefaultGraphs()) {
                        if (defaultGraphURI == null) {
                            queryParams.add(new NameValuePair(
                                    Protocol.CONTEXT_PARAM_NAME, Protocol.NULL_PARAM_VALUE));
                        } else {
                            queryParams.add(new NameValuePair(
                                    Protocol.DEFAULT_GRAPH_PARAM_NAME, defaultGraphURI.toString()));
                        }
                    }
                    for (IRI namedGraphURI : dataset.getNamedGraphs()) {
                        queryParams.add(new NameValuePair(
                                Protocol.NAMED_GRAPH_PARAM_NAME, namedGraphURI.toString()));
                    }
                }
            } // TODO: no else clause here assumes AG's default dataset matches
            // Sesame's, confirm this.
            // TODO: deal with prolog queries scoped to a graph for Jena
        }

        for (Binding binding : bindings) {
            String paramName = Protocol.BINDING_PREFIX + binding.getName();
            String paramValue = Protocol.encodeValue(getStorableValue(binding.getValue(), getValueFactory()));
            queryParams.add(new NameValuePair(paramName, paramValue));
        }

        return queryParams;
    }

    /**
     * Free up any no-longer-needed saved queries as reported
     * by AGQuery finalizer.
     *
     * @throws AGHttpException if there is an error during the request
     */
    private void processSavedQueryDeleteQueue() throws AGHttpException {
        String queryName;
        while ((queryName = savedQueryDeleteQueue.poll()) != null) {
            deleteSavedQuery(queryName);
        }
    }

    public void deleteSavedQuery(String queryName) throws AGHttpException {
        String url = AGProtocol.getSavedQueryLocation(getRoot(), queryName);

        delete(url, null, null, null);
    }

    public synchronized void close() throws AGHttpException {
        if (sessionRoot != null) {
            closeSession();
        }
    }

    /**
     * Creates a new freetext index with the given parameters.
     *
     * @param name              the name of the new index
     * @param predicates        the predicates that will cause triples to be indexed
     * @param indexLiterals     true if literals should be indexed
     * @param indexLiteralTypes the datatypes of Literals that should be indexed.
     * @param indexResources    true if resources should be indexed
     * @param indexFields       the fields (s, p, o, g) to index.
     * @param minimumWordSize   the smallest word that will be indexed.
     * @param stopWords         list of words that will not be indexed
     * @param wordFilters       filters to apply to words before they are indexed.
     * @param innerChars        list of characters that constitute a word
     * @param borderChars       list of characters that can begin/terminate a word
     * @param tokenizer         the name of a supported tokenizer
     * @throws AGHttpException if there's a problem with the index parameters
     *                         or handling the request.
     *                         <p>
     *                         See also the protocol documentation for
     *                         <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-freetext-index">freetext index parameters</a>
     *                         </p>.
     */
    public void createFreetextIndex(String name, List<String> predicates, boolean indexLiterals,
                                    List<String> indexLiteralTypes, String indexResources, List<String> indexFields,
                                    int minimumWordSize, List<String> stopWords, List<String> wordFilters, List<String> innerChars,
                                    List<String> borderChars, String tokenizer)
            throws AGHttpException {
        String url = AGProtocol.getFreetextIndexLocation(getRoot(), name);
        List<NameValuePair> params = new ArrayList<>();
        if (predicates != null) {
            for (String pred : predicates) {
                params.add(new NameValuePair("predicate", pred));
            }
        }
        if (indexLiterals) {
            if (indexLiteralTypes != null) {
                for (String type : indexLiteralTypes) {
                    params.add(new NameValuePair("indexLiteralType", type));
                }
            }
        } else {
            // only need to send this if it's false
            params.add(new NameValuePair("indexLiterals", "false"));
        }
        if (!indexResources.equals("true")) {
            // only need to send this if it's not "true"
            params.add(new NameValuePair("indexResources", indexResources));

        }
        if (indexFields != null) {
            for (String field : indexFields) {
                params.add(new NameValuePair("indexField", field));
            }
        }
        if (minimumWordSize != 3) {
            params.add(new NameValuePair("minimumWordSize", Integer.toString(minimumWordSize)));
        }
        if (stopWords != null) {
            for (String word : stopWords) {
                params.add(new NameValuePair("stopWord", word));
            }
        }
        if (wordFilters != null) {
            for (String filter : wordFilters) {
                params.add(new NameValuePair("wordFilter", filter));
            }
        }
        if (innerChars != null) {
            for (String inner : innerChars) {
                params.add(new NameValuePair("innerChars", inner));
            }
        }
        if (borderChars != null) {
            for (String border : borderChars) {
                params.add(new NameValuePair("borderChars", border));
            }
        }
        if (!tokenizer.equals("default")) {
            params.add(new NameValuePair("tokenizer", tokenizer));
        }
        put(url, null, params, null, null);
    }

    /**
     * Delete the freetext index of the given name.
     *
     * @param index the name of the index to delete
     * @throws AGHttpException if an error occurs while deleting
     */
    public void deleteFreetextIndex(String index) throws AGHttpException {
        String url = AGProtocol.getFreetextIndexLocation(getRoot(), index);

        delete(url, null, null, null);
    }

    /**
     * Lists the free text indices defined on the repository.
     *
     * @return a list of index names
     * @throws AGHttpException if an error occurs with the request
     */
    public List<String> listFreetextIndices()
            throws AGHttpException {
        return Arrays.asList(getFreetextIndices());
    }

    /**
     * @return String[]  the list of defined freetext indices
     * @throws AGHttpException if there is a problem with the request
     * @see #listFreetextIndices()
     * @deprecated
     */
    public String[] getFreetextIndices() throws AGHttpException {
        String url = AGProtocol.getFreetextIndexLocation(getRoot());
        AGStringHandler handler = new AGStringHandler();
        getHTTPClient().get(url, new Header[0], new NameValuePair[0], handler);
        return handler.getResult().split("\n");
    }

    /**
     * Gets the configuration of the given index.
     *
     * @param index the name of the index to lookup
     * @return JSONObject  a description of the index configuration
     * @throws AGHttpException if there is an error delivering the request
     * @throws JSONException   if there is an error parsing the request into JSON
     */
    public JSONObject getFreetextIndexConfiguration(String index)
            throws AGHttpException, JSONException {
        String url = AGProtocol.getFreetextIndexLocation(getRoot(), index);
        AGJSONHandler handler = new AGJSONHandler();
        get(url, null, null, handler);
        return handler.getResult();
    }

    public String[] getFreetextPredicates(String index)
            throws AGHttpException {
        String url = AGProtocol.getFreetextIndexLocation(getRoot(), index) + "/predicates";
        AGStringHandler handler = new AGStringHandler();
        get(url, null, null, handler);
        return handler.getResult().split("\n");
    }

    public void evalFreetextQuery(String pattern, String expression, String index, boolean sorted, int limit, int offset, AGResponseHandler handler)
            throws AGHttpException {
        String url = AGProtocol.getFreetextLocation(getRoot());
        List<Header> headers = new ArrayList<>(5);
        headers.add(new Header("Content-Type", Protocol.FORM_MIME_TYPE
                + "; charset=utf-8"));
        if (handler.getRequestMIMEType() != null) {
            headers.add(new Header(Protocol.ACCEPT_PARAM_NAME, handler
                    .getRequestMIMEType()));
        }
        List<NameValuePair> queryParams = new ArrayList<>(6);
        if (pattern != null) {
            queryParams.add(new NameValuePair("pattern", pattern));
        }
        if (expression != null) {
            queryParams.add(new NameValuePair("expression", expression));
        }
        if (index != null) {
            queryParams.add(new NameValuePair("index", index));
        }
        if (sorted) {
            queryParams.add(new NameValuePair("sorted", "true"));
        }
        if (limit > 0) {
            queryParams.add(new NameValuePair("limit", Integer.toString(limit)));
        }
        if (offset > 0) {
            queryParams.add(new NameValuePair("offset", Integer.toString(offset)));
        }
        post(url, headers, queryParams, null, handler);
    }

    public void registerPredicateMapping(IRI predicate, IRI primitiveType)
            throws AGHttpException {
        String url = AGProtocol.getPredicateMappingLocation(getRoot());
        String pred_nt = NTriplesUtil.toNTriplesString(predicate);
        String primtype_nt = NTriplesUtil.toNTriplesString(primitiveType);

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new NameValuePair(AGProtocol.FTI_PREDICATE_PARAM_NAME, pred_nt));
        params.add(new NameValuePair(AGProtocol.ENCODED_TYPE_PARAM_NAME,
                primtype_nt));
        post(url, null, params, null, null);
    }

    public void deletePredicateMapping(IRI predicate)
            throws AGHttpException {
        String url = AGProtocol.getPredicateMappingLocation(getRoot());
        String pred_nt = NTriplesUtil.toNTriplesString(predicate);

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair(AGProtocol.FTI_PREDICATE_PARAM_NAME, pred_nt));
        delete(url, null, params, null);
    }

    public String[] getPredicateMappings() throws AGHttpException {
        String url = AGProtocol.getPredicateMappingLocation(getRoot());

        AGStringHandler handler = new AGStringHandler();
        get(url, null, null, handler);
        String result = handler.getResult();
        if (result.equals("")) {
            return new String[0];
        } else {
            return result.split("\n");
        }
    }

    public void registerDatatypeMapping(IRI datatype, IRI primitiveType)
            throws AGHttpException {
        String url = AGProtocol.getDatatypeMappingLocation(getRoot());
        String datatype_nt = NTriplesUtil.toNTriplesString(datatype);
        String primtype_nt = NTriplesUtil.toNTriplesString(primitiveType);

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new NameValuePair(AGProtocol.TYPE_PARAM_NAME, datatype_nt));
        params.add(new NameValuePair(AGProtocol.ENCODED_TYPE_PARAM_NAME,
                primtype_nt));
        post(url, null, params, null, null);
    }

    public void deleteDatatypeMapping(IRI datatype) throws AGHttpException {
        String url = AGProtocol.getDatatypeMappingLocation(getRoot());
        String datatype_nt = NTriplesUtil.toNTriplesString(datatype);

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair(AGProtocol.TYPE_PARAM_NAME, datatype_nt));
        delete(url, null, params, null);
    }

    public String[] getDatatypeMappings() throws AGHttpException {
        String url = AGProtocol.getDatatypeMappingLocation(getRoot());

        AGStringHandler handler = new AGStringHandler();
        get(url, null, null, handler);
        String result = handler.getResult();
        if (result.equals("")) {
            return new String[0];
        } else {
            return result.split("\n");
        }
    }

    public void clearMappings() throws AGHttpException {
        clearMappings(false);
    }

    public void clearMappings(boolean includeAutoEncodedPrimitiveTypes) throws AGHttpException {
        String url = AGProtocol.getMappingLocation(getRoot());
        if (includeAutoEncodedPrimitiveTypes) {
            url = url + "/all";
        }

        delete(url, null, null, null);
    }

    public void addRules(String rules) throws AGHttpException {
        InputStream rulestream;
        try {
            rulestream = new ByteArrayInputStream(rules
                    .getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        addRules(rulestream);
    }

    public void addRules(InputStream rulestream) throws AGHttpException {
        useDedicatedSession(isAutoCommit());
        String url = AGProtocol.getFunctorLocation(getRoot());

        RequestEntity entity = new InputStreamRequestEntity(rulestream, -1,
                null);
        post(url, null, null, entity, null);
    }

    public String evalInServer(String lispForm) throws AGHttpException {
        String result;
        InputStream stream;
        stream = new ByteArrayInputStream(lispForm.getBytes(StandardCharsets.UTF_8));
        result = evalInServer(stream);
        return result;
    }

    public String evalInServer(InputStream stream) throws AGHttpException {
        String url = AGProtocol.getEvalLocation(getRoot());
        List<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/plain; charset=utf-8"));

        AGStringHandler handler = new AGStringHandler();
        RequestEntity entity = new InputStreamRequestEntity(stream, -1,
                null);
        post(url, headers, null, entity, handler);
        return handler.getResult();
    }

    public void ping() throws AGHttpException {
        if (usingDedicatedSession) {
            String url = AGProtocol.getSessionPingLocation(getRoot());

            get(url, null, null, null);
        }
    }

    public String[] getGeoTypes() throws AGHttpException {
        String url = AGProtocol.getGeoTypesLocation(getRoot());

        AGStringHandler handler = new AGStringHandler();
        get(url, null, null, handler);
        return handler.getResult().split("\n");
    }

    public String registerCartesianType(float stripWidth, float xmin, float xmax,
                                        float ymin, float ymax) throws AGHttpException {
        String url = AGProtocol.getGeoTypesCartesianLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(5);
        params.add(new NameValuePair(AGProtocol.STRIP_WIDTH_PARAM_NAME,
                Float.toString(stripWidth)));
        params.add(new NameValuePair(AGProtocol.XMIN_PARAM_NAME, Float.toString(xmin)));
        params.add(new NameValuePair(AGProtocol.XMAX_PARAM_NAME, Float.toString(xmax)));
        params.add(new NameValuePair(AGProtocol.YMIN_PARAM_NAME, Float.toString(ymin)));
        params.add(new NameValuePair(AGProtocol.YMAX_PARAM_NAME, Float.toString(ymax)));

        AGStringHandler handler = new AGStringHandler();
        post(url, null, params, null, handler);
        return handler.getResult();
    }

    public String registerSphericalType(float stripWidth, String unit, float latmin, float longmin,
                                        float latmax, float longmax) throws AGHttpException {
        String url = AGProtocol.getGeoTypesSphericalLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(5);
        params.add(new NameValuePair(AGProtocol.STRIP_WIDTH_PARAM_NAME,
                Float.toString(stripWidth)));
        params.add(new NameValuePair(AGProtocol.UNIT_PARAM_NAME, unit));
        params.add(new NameValuePair(AGProtocol.LATMIN_PARAM_NAME, Float.toString(latmin)));
        params.add(new NameValuePair(AGProtocol.LONGMIN_PARAM_NAME, Float.toString(longmin)));
        params.add(new NameValuePair(AGProtocol.LATMAX_PARAM_NAME, Float.toString(latmax)));
        params.add(new NameValuePair(AGProtocol.LONGMAX_PARAM_NAME, Float.toString(longmax)));

        AGStringHandler handler = new AGStringHandler();
        post(url, null, params, null, handler);
        return handler.getResult();
    }

    public void registerPolygon(String polygon, List<String> points)
            throws AGHttpException {
        if (points.size() < 3) {
            throw new IllegalArgumentException("A minimum of three points are required to register a polygon.");
        }
        String url = AGProtocol.getGeoPolygonLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(7);
        params.add(new NameValuePair(AGProtocol.RESOURCE_PARAM_NAME, polygon));
        for (String point : points) {
            params.add(new NameValuePair(AGProtocol.POINT_PARAM_NAME, point));
        }
        put(url, null, params, null, null);
    }

    public void getGeoBox(String type_uri, String predicate_uri, float xmin, float xmax,
                          float ymin, float ymax, int limit, boolean infer, AGResponseHandler handler)
            throws AGHttpException {
        String url = AGProtocol.getGeoBoxLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredRDFFormat().getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(7);
        params.add(new NameValuePair(AGProtocol.TYPE_PARAM_NAME, type_uri));
        params.add(new NameValuePair(AGProtocol.GEO_PREDICATE_PARAM_NAME, predicate_uri));
        params.add(new NameValuePair(AGProtocol.XMIN_PARAM_NAME, Float.toString(xmin)));
        params.add(new NameValuePair(AGProtocol.XMAX_PARAM_NAME, Float.toString(xmax)));
        params.add(new NameValuePair(AGProtocol.YMIN_PARAM_NAME, Float.toString(ymin)));
        params.add(new NameValuePair(AGProtocol.YMAX_PARAM_NAME, Float.toString(ymax)));
        params.add(new NameValuePair(AGProtocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(infer)));
        if (0 != limit) {
            params.add(new NameValuePair(AGProtocol.LIMIT_PARAM_NAME, Integer.toString(limit)));
        }
        get(url, headers, params, handler);
    }

    public void getGeoCircle(String type_uri, String predicate_uri, float x, float y,
                             float radius, int limit, boolean infer, AGResponseHandler handler)
            throws AGHttpException {
        String url = AGProtocol.getGeoCircleLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredRDFFormat().getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(7);
        params.add(new NameValuePair(AGProtocol.TYPE_PARAM_NAME, type_uri));
        params.add(new NameValuePair(AGProtocol.GEO_PREDICATE_PARAM_NAME, predicate_uri));
        params.add(new NameValuePair(AGProtocol.X_PARAM_NAME, Float.toString(x)));
        params.add(new NameValuePair(AGProtocol.Y_PARAM_NAME, Float.toString(y)));
        params.add(new NameValuePair(AGProtocol.RADIUS_PARAM_NAME, Float.toString(radius)));
        params.add(new NameValuePair(AGProtocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(infer)));
        if (0 != limit) {
            params.add(new NameValuePair(AGProtocol.LIMIT_PARAM_NAME, Integer.toString(limit)));
        }
        get(url, headers, params, handler);
    }

    public void getGeoHaversine(String type_uri, String predicate_uri, float lat, float lon, float radius, String unit, int limit, boolean infer, AGResponseHandler handler)
            throws AGHttpException {
        String url = AGProtocol.getGeoHaversineLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredRDFFormat().getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(7);
        params.add(new NameValuePair(AGProtocol.TYPE_PARAM_NAME, type_uri));
        params.add(new NameValuePair(AGProtocol.GEO_PREDICATE_PARAM_NAME, predicate_uri));
        params.add(new NameValuePair(AGProtocol.LAT_PARAM_NAME, Float.toString(lat)));
        params.add(new NameValuePair(AGProtocol.LON_PARAM_NAME, Float.toString(lon)));
        params.add(new NameValuePair(AGProtocol.RADIUS_PARAM_NAME, Float.toString(radius)));
        params.add(new NameValuePair(AGProtocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(infer)));
        if (0 != limit) {
            params.add(new NameValuePair(AGProtocol.LIMIT_PARAM_NAME, Integer.toString(limit)));
        }
        get(url, headers, params, handler);
    }

    public void getGeoPolygon(String type_uri, String predicate_uri, String polygon, int limit, boolean infer, AGResponseHandler handler)
            throws AGHttpException {
        String url = AGProtocol.getGeoPolygonLocation(getRoot());
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME,
                getPreferredRDFFormat().getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(7);
        params.add(new NameValuePair(AGProtocol.TYPE_PARAM_NAME, type_uri));
        params.add(new NameValuePair(AGProtocol.GEO_PREDICATE_PARAM_NAME, predicate_uri));
        params.add(new NameValuePair(AGProtocol.POLYGON_PARAM_NAME, polygon));
        params.add(new NameValuePair(AGProtocol.INCLUDE_INFERRED_PARAM_NAME, Boolean.toString(infer)));
        if (0 != limit) {
            params.add(new NameValuePair(AGProtocol.LIMIT_PARAM_NAME, Integer.toString(limit)));
        }
        get(url, headers, params, handler);
    }

    public void registerSNAGenerator(String generator, List<String> objectOfs, List<String> subjectOfs, List<String> undirecteds, String query)
            throws AGHttpException {
        useDedicatedSession(autoCommit);
        String url = AGProtocol.getSNAGeneratorLocation(getRoot(), generator);

        List<NameValuePair> params = new ArrayList<>(7);
        for (String objectOf : objectOfs) {
            params.add(new NameValuePair(AGProtocol.OBJECTOF_PARAM_NAME, objectOf));
        }
        for (String subjectOf : subjectOfs) {
            params.add(new NameValuePair(AGProtocol.SUBJECTOF_PARAM_NAME, subjectOf));
        }
        for (String undirected : undirecteds) {
            params.add(new NameValuePair(AGProtocol.UNDIRECTED_PARAM_NAME, undirected));
        }
        if (query != null) {
            params.add(new NameValuePair(AGProtocol.QUERY_PARAM_NAME, query));
        }
        put(url, null, params, null, null);
    }

    public void registerSNANeighborMatrix(String matrix, String generator, List<String> group, int depth)
            throws AGHttpException {
        String url = AGProtocol.getSNANeighborMatrixLocation(getRoot(), matrix);

        List<NameValuePair> params = new ArrayList<>(7);
        params.add(new NameValuePair(AGProtocol.GENERATOR_PARAM_NAME, generator));
        for (String node : group) {
            params.add(new NameValuePair(AGProtocol.GROUP_PARAM_NAME, node));
        }
        params.add(new NameValuePair(AGProtocol.DEPTH_PARAM_NAME, Integer.toString(depth)));
        put(url, null, params, null, null);
    }

    /**
     * Returns a list of indices for this repository.  When listValid is true,
     * return all possible valid index types for this store; when listValid is
     * false, return only the current actively managed index types.
     *
     * @param listValid true yields all valid types, false yields active types
     * @return list of indices, never null
     * @throws AGHttpException if there is a problem with the request
     */
    public List<String> listIndices(boolean listValid) throws AGHttpException {
        String url = AGProtocol.getIndicesURL(getRoot());

        List<NameValuePair> data = new ArrayList<>(1);
        data.add(new NameValuePair("listValid", Boolean.toString(listValid)));

        AGStringHandler handler = new AGStringHandler();
        get(url, null, data, handler);
        return Arrays.asList(handler.getResult().split("\n"));
    }

    /**
     * Adds the given index to the list of actively managed indices.
     * This will take affect on the next commit.
     *
     * @param index a valid index type
     * @throws AGHttpException if there is a problem with the request
     * @see #listIndices(boolean)
     */
    public void addIndex(String index) throws AGHttpException {
        String url = AGProtocol.getIndicesURL(getRoot()) + "/" + index;

        put(url, null, null, null, null);
    }

    /**
     * Drops the given index from the list of actively managed indices.
     * This will take affect on the next commit.
     *
     * @param index a valid index type
     * @throws AGHttpException if there is a problem with the request
     * @see #listIndices(boolean)
     */
    public void dropIndex(String index) throws AGHttpException {
        String url = AGProtocol.getIndicesURL(getRoot()) + "/" + index;

        delete(url, null, null, null);
    }

    public void registerEncodableNamespace(String namespace, String format)
            throws AGHttpException {
        String url = getRoot() + "/encodedIds/prefixes";

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new NameValuePair("prefix", namespace));
        params.add(new NameValuePair("format", format));
        post(url, null, params, null, null);
    }

    public void unregisterEncodableNamespace(String namespace)
            throws AGHttpException {
        String url = getRoot() + "/encodedIds/prefixes";

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new NameValuePair("prefix", namespace));
        delete(url, null, params, null);
    }

    /**
     * Enables the spogi cache in this repository.
     *
     * @param size the size of the cache, in triples
     * @throws AGHttpException if there is a problem with the request
     */
    public void enableTripleCache(long size) throws AGHttpException {
        String url = getRoot() + "/tripleCache";

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair("size", Long.toString(size)));
        put(url, null, params, null, null);
    }

    public void registerEncodableNamespaces(JSONArray formattedNamespaces)
            throws AGHttpException {
        String url = getRoot() + "/encodedIds/prefixes";
        uploadJSON(url, formattedNamespaces);
    }

    public TupleQueryResult getEncodableNamespaces() throws AGHttpException {
        String url = getRoot() + "/encodedIds/prefixes";
        return getHTTPClient().getTupleQueryResult(url);
    }

    /**
     * Invoke a stored procedure on the AllegroGraph server.
     * The args must already be encoded, and the response is encoded.
     *
     * <p>Low-level access to the data sent to the server can be done with:</p>
     * <pre>{@code
     * {@link AGDeserializer#decodeAndDeserialize(String) AGDeserializer.decodeAndDeserialize}(
     *     callStoredProcEncoded(functionName, moduleName,
     *         {@link AGSerializer#serializeAndEncode(Object[]) AGSerializer.serializeAndEncode}(args)));
     * }</pre>
     *
     * <p>If an error occurs in the stored procedure then result will
     * be a two element vector  with the first element being the string "_fail_"
     * and the second element being the error message (also a string).</p>
     *
     * <p>{@link #callStoredProc(String, String, Object...)}
     * does this encoding, decoding, and throws exceptions for error result.
     * </p>
     *
     * @param functionName stored proc lisp function, for example "addTwo"
     * @param moduleName   lisp FASL file name, for example "example.fasl"
     * @param argsEncoded  byte-encoded arguments to the stored procedure
     * @return String  byte-encoded response from stored proc
     * @throws AGHttpException if there is a problem with the request
     * @see #callStoredProc(String, String, Object...)
     * @since v4.2
     * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
     */
    public String callStoredProcEncoded(String functionName, String moduleName, String argsEncoded)
            throws AGHttpException {
        String url = AGProtocol.getStoredProcLocation(getRoot()) + "/" + functionName;
        List<Header> headers = new ArrayList<>(1);
        headers.add(new Header("x-scripts", moduleName));

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair("spargstr", argsEncoded));
        AGStringHandler handler = new AGStringHandler();
        post(url, headers, params, null, handler);
        return handler.getResult();
    }

    /**
     * Invoke a stored procedure on the AllegroGraph server.
     *
     * <p>The input arguments and the return value can be:
     * {@link String}, {@link Integer}, null, byte[],
     * or Object[] or {@link List} of these (can be nested).</p>
     *
     * @param functionName stored proc lisp function, for example "addTwo"
     * @param moduleName   lisp FASL file name, for example "example.fasl"
     * @param args         arguments to the stored proc
     * @return Object  return value of stored proc
     * @throws AGCustomStoredProcException for errors from stored proc
     * @see AGSerializer#serializeAndEncode(Object[])
     * @see AGDeserializer#decodeAndDeserialize(String)
     * @since v4.2
     * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
     */
    public Object callStoredProc(String functionName, String moduleName, Object... args)
            throws AGHttpException {
        Object o = AGDeserializer.decodeAndDeserialize(
                callStoredProcEncoded(functionName, moduleName,
                        AGSerializer.serializeAndEncode(args)));
        if (o instanceof Object[]) {
            Object[] a = (Object[]) o;
            if (a.length == 2 && "_fail_".equals(a[0])) {
                throw new AGCustomStoredProcException(a[1] == null ? null : a[1].toString());
            }
        }
        return o;
    }

    /**
     * Returns the storeID as a long
     *
     * @return The store ID
     * @throws AGHttpException if there is a problem with the request
     */
    public long getStoreID() throws AGHttpException {
        String url = getRoot() + "/storeID";

        // The response is an 8 character (4 hex bytes) string.
        AGStringHandler handler = new AGStringHandler();
        get(url, null, null, handler);
        return Long.parseLong(handler.getResult(), 16);
    }

    /**
     * Returns the size of the spogi cache.
     *
     * @return long  the size of the spogi cache, in triples
     * @throws AGHttpException if there is a problem with the request
     */
    public long getTripleCacheSize() throws AGHttpException {
        String url = getRoot() + "/tripleCache";

        AGLongHandler handler = new AGLongHandler();
        get(url, null, null, handler);
        return handler.getResult();
    }

    /**
     * Disables the spogi triple cache.
     *
     * @throws AGHttpException if there is a problem with the request
     */
    public void disableTripleCache() throws AGHttpException {
        String url = getRoot() + "/tripleCache";

        delete(url, null, null, null);
    }

    public String[] getBlankNodes(int blankNodeAmount) throws AGHttpException {
        return getHTTPClient().getBlankNodes(getRoot(), blankNodeAmount);
    }

    /**
     * Deletes all duplicates from the store.
     * <p>
     * The comparisonMode determines what will be deemed a "duplicate".
     * <p>
     * If comparisonMode is "spog", quad parts (s,p,o,g) will all be
     * compared when looking for duplicates.
     * <p>
     * If comparisonMode is "spo", only the (s,p,o) parts will be
     * compared; the same triple in different graphs will thus be deemed
     * duplicates.
     * <p>
     * See also the protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#delete-statements-duplicates">deleting duplicates</a>
     *
     * @param comparisonMode determines what is a duplicate
     * @throws AGHttpException if there is a problem with the request
     */
    public void deleteDuplicates(String comparisonMode) throws AGHttpException {
        String url = Protocol.getStatementsLocation(getRoot()) + "/duplicates";

        List<NameValuePair> params = new ArrayList<>(2);
        if (comparisonMode != null) {
            params.add(new NameValuePair("mode", comparisonMode));
        }
        delete(url, null, params, null);
    }

    public void getDuplicateStatements(String comparisonMode, RDFHandler handler) throws AGHttpException, RDFHandlerException {
        String url = Protocol.getStatementsLocation(getRoot()) + "/duplicates";
        List<Header> headers = new ArrayList<>(1);

        headers.add(new Header(Protocol.ACCEPT_PARAM_NAME, getPreferredRDFFormat().getDefaultMIMEType()));

        List<NameValuePair> params = new ArrayList<>(2);
        if (comparisonMode != null) {
            params.add(new NameValuePair("mode", comparisonMode));
        }

        get(url, headers, params,
                new AGRDFHandler(getPreferredRDFFormat(), handler, getValueFactory(), getAllowExternalBlankNodeIds()));
    }

    /**
     * Materializes inferred statements (generates and adds them to the store).
     * <p>
     * The materializer's configuration determines how statements are materialized.
     *
     * @param materializer the materializer to use
     * @return long  the number of statements added
     * @throws AGHttpException if there is a problem with the request
     * @see AGMaterializer#newInstance()
     */
    public long materialize(AGMaterializer materializer) throws AGHttpException {
        String url = getRoot() + "/materializeEntailed";

        List<NameValuePair> params = new ArrayList<>(5);
        if (materializer != null) {
            for (String with : materializer.getWithRulesets()) {
                params.add(new NameValuePair("with", with));
            }
            for (String without : materializer.getWithoutRulesets()) {
                params.add(new NameValuePair("without", without));
            }
            Integer period = materializer.getCommitPeriod();
            if (period != null) {
                params.add(new NameValuePair("commit", period.toString()));
            }
            Boolean useTypeSubproperty = materializer.getUseTypeSubproperty();
            if (useTypeSubproperty != null) {
                params.add(new NameValuePair("useTypeSubproperty", useTypeSubproperty.toString()));
            }
            Resource inferredGraph = materializer.getInferredGraph();
            if (inferredGraph != null) {
                params.add(new NameValuePair("inferredGraph", Protocol.encodeContext(inferredGraph)));
            }
        } // else, using the server's default materializer
        AGLongHandler handler = new AGLongHandler();
        put(url, null, params, null, handler);
        return handler.getResult();
    }

    /**
     * Deletes materialized statements.
     *
     * @param inferredGraph Graph to delete the triples from. If null the default graph will be used.
     * @return long  the number of statements deleted
     * @throws AGHttpException if there is a problem with the request
     * @see #materialize(AGMaterializer)
     */
    public long deleteMaterialized(final Resource inferredGraph) throws RepositoryException {
        String url = getRoot() + "/materializeEntailed";

        List<NameValuePair> params = new ArrayList<>(2);
        if (inferredGraph != null) {
            params.add(new NameValuePair("inferredGraph", Protocol.encodeContext(inferredGraph)));
        }
        AGLongHandler handler = new AGLongHandler();
        delete(url, null, params, handler);
        return handler.getResult();
    }

    public void optimizeIndices(Boolean wait, int level) throws AGHttpException {
        String url = repoRoot + "/indices/optimize";

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new NameValuePair("wait", Boolean.toString(wait)));
        params.add(new NameValuePair("level", Integer.toString(level)));

        post(url, null, params, null, null);
    }

    public void optimizeIndices(Boolean wait) throws AGHttpException {
        String url = repoRoot + "/indices/optimize";

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair("wait", Boolean.toString(wait)));
        post(url, null, params, null, null);
    }

    /**
     * @since v4.4
     */
    private void putSpinX(String x, AGSpinFunction fn) throws AGHttpException {
        List<NameValuePair> params = new ArrayList<>(
                (fn.getArguments() == null ? 0 : fn.getArguments().length) + 1);

        params.add(new NameValuePair(AGProtocol.SPIN_QUERY, fn.getQuery()));
        for (int i = 0; fn.getArguments() != null && i < fn.getArguments().length; i++) {
            params.add(new NameValuePair(AGProtocol.SPIN_ARGUMENTS, fn.getArguments()[i]));
        }
        put(AGProtocol.spinURL(getRoot(), x, fn.getUri()), null, params, null, null);
    }

    /**
     * @param uri name of the SPIN magic property to retrieve
     * @return String  a string representation of the property definition
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public String getSpinFunction(String uri) throws AGHttpException {
        return getHTTPClient().getString(AGProtocol.spinURL(getRoot(), AGProtocol.SPIN_FUNCTION, uri));
    }

    /**
     * @return {@link TupleQueryResult}  a list of defined SPIN magic properties
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public TupleQueryResult listSpinFunctions() throws AGHttpException {
        return getHTTPClient().getTupleQueryResult(AGProtocol.spinURL(getRoot(), AGProtocol.SPIN_FUNCTION, null));
    }

    /**
     * @param fn the SPIN magic property to add
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public void putSpinFunction(AGSpinFunction fn) throws AGHttpException {
        putSpinX(AGProtocol.SPIN_FUNCTION, fn);
    }

    /**
     * @param uri name of the SPIN magic property to delete
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public void deleteSpinFunction(String uri) throws AGHttpException {
        delete(AGProtocol.spinURL(getRoot(), AGProtocol.SPIN_FUNCTION, uri), null, null, null);
    }

    /**
     * @param uri name of the SPIN magic property to delete
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public void deleteHardSpinFunction(String uri) throws AGHttpException {
        try {
            deleteSpinFunction(uri);
        } catch (AGHttpException e) {
            if (!e.getMessage().contains(uri + " is not a registered SPIN function")) {
                throw e;
            }
        }
    }

    /**
     * @param uri name of the SPIN magic property to retrieve
     * @return String  a string representation of the property definition
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public String getSpinMagicProperty(String uri) throws AGHttpException {
        return getHTTPClient().getString(AGProtocol.spinURL(getRoot(), AGProtocol.SPIN_MAGICPROPERTY, uri));
    }

    /**
     * @return {@link TupleQueryResult}  a list of defined SPIN magic properties
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public TupleQueryResult listSpinMagicProperties() throws AGHttpException {
        return getHTTPClient().getTupleQueryResult(AGProtocol.spinURL(getRoot(), AGProtocol.SPIN_MAGICPROPERTY, null));
    }

    /**
     * @param fn the SPIN magic property to add
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public void putSpinMagicProperty(AGSpinMagicProperty fn) throws AGHttpException {
        putSpinX(AGProtocol.SPIN_MAGICPROPERTY, fn);
    }

    /**
     * @param uri The uri naming the SPIN magic property to delete
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public void deleteSpinMagicProperty(String uri) throws AGHttpException {
        delete(AGProtocol.spinURL(getRoot(), AGProtocol.SPIN_MAGICPROPERTY, uri), null, null, null);
    }

    /**
     * @param uri The uri naming the SPIN magic property to delete
     * @throws AGHttpException if there is a problem with the request
     * @since v4.4
     */
    public void deleteHardSpinMagicProperty(String uri) throws AGHttpException {
        try {
            deleteSpinMagicProperty(uri);
        } catch (AGHttpException e) {
            if (!e.getMessage().contains(uri + " is not a registered SPIN magic property")) {
                throw e;
            }
        }
    }

    /**
     * @return true iff this HTTP/Storage layer allows external blank node ids.
     * @see #setAllowExternalBlankNodeIds(boolean)
     * @see AGValueFactory#isAGBlankNodeId(String)
     * @since v4.4
     */
    public boolean getAllowExternalBlankNodeIds() {
        return allowExternalBlankNodeIds;
    }

    /**
     * Enables/Disables an option to support external blank nodes (experimental).
     * <p>
     * Disabled by default.  Enable it from an AGRepositoryConnection using:
     * <p>
     * conn.prepareHttpRepoClient().setAllowExternalBlankNodeIds(true);
     * <p>
     * An external blank node is a blank node whose id is not generated
     * by AllegroGraph.  Applications should normally request new blank
     * nodes from AllegroGraph [via the AGValueFactory#createBNode()
     * method in the Sesame api, or via the AGModel#createResource()
     * method in the Jena adapter]; this helps to avoid unintended
     * blank node id collisions (particularly in a multi-user setting)
     * and enables blank nodes to be stored more efficiently, etc.
     * <p>
     * Previously, for applications that did use an external blank node id
     * [such as via AGValueFactory#createBNode("ex") in Sesame, or in Jena via
     * AGModel#createResource(AnonId.create("ex"))] or an external blank
     * node factory [such as ValueFactoryImpl#createBNode() in Sesame or
     * ResourceFactory#createResource() in Jena], there were some issues
     * that arose.  When the application tried to add a statement such
     * as [_:ex p a] over HTTP, the AllegroGraph server allocates a new
     * blank node id to use in place of the external blank node id _:ex,
     * so that the actual statement stored is something like
     * [_:bF010696Fx1 p a].
     * <p>
     * There are 2 issues with this:
     * <p>
     * 1) adding a second statement such as [_:ex q b] in a subsequent
     * request will result in another new bnode id being allocated,
     * so that the statement stored might be [_:bF010696Fx2 q b],
     * and the link between the two statements (that exists client
     * side via blank node _:ex) is now lost.
     * <p>
     * 2) trying to get the statement [_:ex p a] will fail, because the
     * actual statement stored is [_:bF010696Fx1 p a].
     * <p>
     * Note that these issues arise with external blank node ids because
     * the server does not know their scope, and by default assumes that
     * the scope is limited to the HTTP request in order to avoid blank
     * node conflicts.  When AG-allocated blank node ids are used instead,
     * the two issues above do not arise (they are stored as is, and they
     * will be found as expected with a get).  In short, AG-allocated blank
     * nodes round-trip, external blank nodes do not.
     * <p>
     * One workaround for this is to have applications declare that they
     * want external blank nodes to round-trip.  An external blank node
     * can exist for the life of an application, and that life can exceed
     * the life of an AllegroGraph server instance, so adding a statement
     * with an external blank node will need to persist the external blank
     * node's id somehow.  AllegroGraph doesn't currently allow storing blank
     * nodes with an arbitrary external id, so this workaround converts them
     * to URI's (of the form "urn:x-bnode:id" by default) for storage, and
     * recovers the blank nodes with correct ids when the values are retrieved
     * in queries, so that applications continue to transparently deal with
     * BNodes and see the expected behavior w.r.t. issues 1) and 2) above.
     * <p>
     * When enabling the workaround described here, the application must
     * take responsibility for using an external blank node factory that
     * avoids blank node conflicts, and must also be willing to incur the
     * additional costs of storing external blank nodes as URIs.  When the
     * workaround is disabled (allow=false, the default), the application
     * must avoid using external blank nodes with AG (using AG-allocated
     * blank nodes instead); using external blank node ids when they are
     * not allowed will result in an explanatory exception (as opposed to
     * quietly exhibiting the two problems described above).
     * <p>
     * This method and approach are experimental and subject to change.
     * <p>
     *
     * @param allow true enables a workaround to support external bnodes
     * @see #getAllowExternalBlankNodeIds()
     * @see AGValueFactory#isAGBlankNodeId(String)
     * @since v4.4
     */
    public void setAllowExternalBlankNodeIds(boolean allow) {
        allowExternalBlankNodeIds = allow;
    }

    /**
     * Returns a storable Resource for the given Resource.
     * <p>
     * The intent is that the returned resource can be stored in an
     * AG repository, will round-trip if added to and retrieved from
     * the repository, and that the given Resource can be cheaply
     * recreated from the returned resource.
     * <p>
     * This method simply returns the original resource when it is a
     * URI or a BNode with an AG generated blank node id (these can be
     * stored and will round-trip).  For external BNodes (with an id
     * that does not look like it was generated by AG), a URI of the
     * form "urn:x-bnode:id" is returned when the connection allows
     * external blank nodes; otherwise, an IllegalArgumentException is
     * thrown explaining the need to either avoid external blank nodes
     * or enable the workaround for external blank nodes.
     * <p>
     * This method is intended for use within the AG client library,
     * not for use by applications.
     *
     * @param r  a resource
     * @param vf a value factory that can create new store values
     * @return a storable resource for the given resource
     * @see #getApplicationResource(Resource, AGValueFactory)
     * @see #setAllowExternalBlankNodeIds(boolean)
     * @since v4.4
     */
    public Resource getStorableResource(Resource r, AGValueFactory vf) {
        Resource storable = r;
        if (r instanceof BNode && !vf.isAGBlankNodeId(r.stringValue())) {
            if (allowExternalBlankNodeIds) {
                storable = vf.createIRI(vf.PREFIX_FOR_EXTERNAL_BNODES + r.stringValue());
            } else {
                throw new IllegalArgumentException("Cannot store external blank node "
                        + r + " in AllegroGraph with the current settings. "
                        + "Please see javadoc for "
                        + "AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)"
                        + "for more details and options.");
            }
        }
        return storable;
    }

    /**
     * Returns a storable Value for the given Value.
     * <p>
     * The intent is that the returned value can be stored in an
     * AG repository, will round-trip if added to and retrieved from
     * the repository, and that the given Value can be cheaply
     * recreated from the returned value.
     * <p>
     * This method simply returns the original value when it is a URI,
     * Literal, or BNode with AG generated blank node id (these can be
     * stored and will round-trip).  For external BNodes (with an id
     * that does not look like it was generated by AG), a URI of the
     * form "urn:x-bnode:id" is returned when the connection allows
     * external blank nodes; otherwise, an IllegalArgumentException is
     * thrown explaining the need to either avoid external blank nodes
     * or enable the workaround for external blank nodes.
     * <p>
     * This method is intended for use within the AG client library,
     * not for use by applications.
     *
     * @param v  a value
     * @param vf a value factory that can create new store values
     * @return a storable value for the given value
     * @see #getApplicationValue(Value, AGValueFactory)
     * @see #setAllowExternalBlankNodeIds(boolean)
     * @since v4.4
     */
    public Value getStorableValue(Value v, AGValueFactory vf) {
        return getStorableValue(v, vf, getAllowExternalBlankNodeIds());
    }

    /**
     * Sets the AG user for X-Masquerade-As-User requests.
     * <p>
     * For AG superusers only.  This allows AG superusers to run requests as
     * another user in a dedicated session.
     *
     * @param user the user for X-Masquerade-As-User requests
     * @throws RepositoryException if an error occurs
     */
    public void setMasqueradeAsUser(String user) throws RepositoryException {
        useDedicatedSession(autoCommit);
        getHTTPClient().setMasqueradeAsUser(user);
    }

    /**
     * HTTP client layer function for requesting that AG define a new attribute. Only parameters
     * explicitly set by the client will be passed to the request.
     *
     * @param name          the name of the attribute being defined
     * @param allowedValues list of allowed values (null means all values allowed)
     * @param ordered       true if <code>allowedValues</code> are ordered
     * @param minimum       the minimum number of times this attribute can be specified
     * @param maximum       the maximum number of times this attribute can be specified
     * @throws AGHttpException if there's a problem with the request
     */
    public void addAttributeDefinition(String name, List<String> allowedValues, boolean ordered, long minimum, long maximum)
            throws AGHttpException {
        String url = AGProtocol.getAttributeDefinitionLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(5);

        params.add(new NameValuePair(AGProtocol.NAME_PARAM_NAME, name));
        params.add(new NameValuePair(AGProtocol.ORDERED_PARAM_NAME, Boolean.toString(ordered)));

        if (minimum >= 0) {
            params.add(new NameValuePair(AGProtocol.MINIMUM_PARAM_NAME, Long.toString(minimum)));
        }

        if (maximum >= 0) {
            params.add(new NameValuePair(AGProtocol.MAXIMUM_PARAM_NAME, Long.toString(maximum)));
        }

        if (allowedValues != null) {
            for (String value : allowedValues) {
                params.add(new NameValuePair(AGProtocol.ALLOWED_VALUE_PARAM_NAME, value));
            }
        }

        post(url, null, params, null, null);
    }

    /**
     * HTTP client layer function for requesting that AG deletes an existing attribute.
     *
     * @param name the attribute to delete
     * @throws AGHttpException if there's a problem with the request
     */
    public void deleteAttributeDefinition(String name) throws AGHttpException {
        String url = AGProtocol.getAttributeDefinitionLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair(AGProtocol.NAME_PARAM_NAME, name));

        delete(url, null, params, null);
    }

    /**
     * Fetch all attribute definitions.
     *
     * @return JSONArray containing a JSONObject for each attribute
     * @throws AGHttpException if there's a problem with the request
     * @throws JSONException   if there's a problem parsing the request into JSON
     */
    public JSONArray getAttributeDefinition()
            throws AGHttpException, JSONException {
        return getAttributeDefinition(null);
    }

    /**
     * Fetch the attribute definition named by NAME. Return all definitions if NAME is null.
     *
     * @param name of the attribute
     * @return a JSONArray containing a JSONObject for the attribute found, or
     * all attributes if NAME is null
     * @throws AGHttpException if there's a problem with the request
     * @throws JSONException   if there's a problem parsing the request into JSON
     */
    public JSONArray getAttributeDefinition(String name)
            throws AGHttpException, JSONException {
        String url = AGProtocol.getAttributeDefinitionLocation(getRoot());

        List<NameValuePair> params = null;
        AGJSONHandler handler = new AGJSONHandler();

        if (name != null) {
            params = new ArrayList<>(1);
            params.add(new NameValuePair(AGProtocol.NAME_PARAM_NAME, name));
        }

        get(url, null, params, handler);

        return handler.getArrayResult();
    }

    public String getStaticAttributeFilter() throws AGHttpException {
        String url = AGProtocol.getStaticFilterLocation(getRoot());

        AGStringHandler handler = new AGStringHandler();
        String result;

        get(url, null, null, handler);

        result = handler.getResult();

        if (result != null && result.trim().length() == 0) {
            result = null;
        }
        return result;
    }

    public void setStaticAttributeFilter(String filter) throws AGHttpException {
        String url = AGProtocol.getStaticFilterLocation(getRoot());

        List<NameValuePair> params = new ArrayList<>(1);
        params.add(new NameValuePair(AGProtocol.FILTER_PARAM_NAME, filter));

        post(url, null, params, null, null);
    }

    public void deleteStaticAttributeFilter() throws AGHttpException {
        String url = AGProtocol.getStaticFilterLocation(getRoot());

        delete(url, null, null, null);
    }

    public boolean getNDGeospatialDatatypeAutomation() throws AGHttpException {
        String url = AGProtocol.getNDGeospatialDatatypeAutomation(getRoot());

        AGBooleanHandler handler = new AGBooleanHandler();

        get(url, null, null, handler);

        return handler.getResult();
    }

    public void enableNDGeospatialDatatypeAutomation() throws AGHttpException {
        String url = AGProtocol.getNDGeospatialDatatypeAutomation(getRoot());

        put(url, null, null, null, null);
    }

    public void disableNDGeospatialDatatypeAutomation() throws AGHttpException {
        String url = AGProtocol.getNDGeospatialDatatypeAutomation(getRoot());

        delete(url, null, null, null);
    }

    /**
     * Return the current userAttributes setting for this connection.
     *
     * @return String containing a json representation of the attributes
     */
    public String getUserAttributes() {
        return userAttributes;
    }

    /**
     * Set the user attributes for this connection.
     *
     * @param value, a String containing a serialized JSON object of the attributes
     */
    public void setUserAttributes(String value) {
        if (value != null && value.trim().length() == 0) {
            userAttributes = null;
        } else {
            userAttributes = value;
        }
    }

    /**
     * Gets the distributed transaction settings.
     *
     * @return Settings (possibly {@code null}).
     */
    public TransactionSettings getTransactionSettings() {
        return transactionSettings;
    }

    /**
     * Change the transaction settings related to multi-master replication.
     *
     * @param transactionSettings New settings or {@code null}, meaning
     *                            "use server-chosen defaults for all settings".
     */
    public void setTransactionSettings(final TransactionSettings transactionSettings) {
        this.transactionSettings = transactionSettings;
    }

    /**
     * Checks the server version to see if a commit is attempted after a warmup.
     *
     * @return True if the bug is present, false otherwise.
     */
    private boolean hasWarmupBug() {
        if (hasWarmupBug == null) {
            final AGServerVersion version = repo.getServer().getComparableVersion();
            hasWarmupBug = version.compareTo(new AGServerVersion("6.3.0")) < 0;
        }
        return hasWarmupBug;
    }

    /**
     * Asks the server to read store's internal data structures into memory.
     *
     * Use {@link #warmup(WarmupConfig)} to specify which structures should be read.
     */
    public void warmup() {
        warmup(null);
    }

    /**
     * Asks the server to read store's internal data structures into memory.
     *
     * @param config Config object that can be used to specify which structures
     *               should be read into memory. If this parameter is {@code null}
     *               then the choice will be left to the server.
     */
    public void warmup(final WarmupConfig config) {
        final String url = getRoot() + "/" + AGProtocol.WARMUP;
        List<NameValuePair> params = new ArrayList<>(2);
        if (config != null) {
            params.add(new NameValuePair(
                    AGProtocol.INCLUDE_STRINGS,
                    Boolean.toString(config.getIncludeStrings())));
            params.add(new NameValuePair(
                    AGProtocol.INCLUDE_TRIPLES,
                    Boolean.toString(config.getIncludeTriples())));
        }
        try {
            put(url, null, params, null, null);
        } catch (AGHttpException e) {
            // Older versions of AG tried to do a commit after warmup.
            // In a read-only store the commit would fail, but since
            // the warmup has been executed by then we can safely ignore
            // such errors.
            if (!hasWarmupBug() || !e.getMessage().contains("commit operation is not allowed")) {
                throw e;
            }
        }
    }
}
