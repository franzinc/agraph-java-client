/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.AGHttpRepoClient.CommitPhase;
import com.franz.agraph.http.exception.AGCustomStoredProcException;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.exception.AGMalformedDataException;
import com.franz.agraph.http.handler.AGDownloadHandler;
import com.franz.agraph.http.handler.AGRDFHandler;
import com.franz.agraph.http.handler.AGRawStreamer;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.repository.repl.TransactionSettings;
import com.franz.util.Ctx;
import org.apache.commons.codec.DecoderException;
import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.io.GZipUtil;
import org.eclipse.rdf4j.common.io.ZipUtil;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.StatementImpl;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.base.AbstractRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Implements the <a href="http://www.openrdf.org/">Sesame</a>
 * {@link RepositoryConnection} interface for AllegroGraph.
 * <p>By default, a connection is in {@link #setAutoCommit(boolean)
 * autoCommit} mode. Connections in this mode are said to be
 * <i>shared</i>. Multiple shared connections may be serviced by the
 * same server back-end process and they have no associated state,
 * thus they do not support some functionality such as multi-step
 * transactions and datatype mappings.</p>
 * <p>Full functionality is offered by <i>dedicated</i> sessions at
 * the cost of higher server resource requirements.</p>
 * <p>Note that concurrent access to the same connection object of
 * either kind is explicitly forbidden. The client must perform its
 * own synchronization to ensure non-concurrent access. This is
 * typically achieved by employing connection pooling (see {@link
 * com.franz.agraph.pool.AGConnPool}) and having exactly one thread
 * that uses each connection.</p>
 *
 * <h3><a id="sessions">Dedicated Session Overview</a></h3>
 * <p>Sessions with AllegroGraph server are used for ACID transactions
 * and also for server code in InitFile and Scripts.
 * See more documentation for
 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#sessions"
 * target="_top">Sessions in the AllegroGraph HTTP Protocol</a> and
 * <a href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html#ACID"
 * target="_top">ACID transactions</a> in the AllegroGraph Server documentation.</p>
 * <p>Operations such as
 * {@link #setAutoCommit(boolean) setAutoCommit},
 * {@link #addRules(String) addRules}, and
 * {@link #registerSNAGenerator(String, List, List, List, String) registerSNAGenerator}
 * will spawn a dedicated session (simply session, from now on) in
 * order to perform their duties. Adds and deletes during a session
 * must be {@link #commit() committed} or {@link #rollback() rolled
 * back}.</p>
 * <p>To conserve resources, the server will drop a session when
 * its idle {@link #setSessionLifetime(int) lifetime} is exceeded.
 * To avoid this, the client periodically sends a {@link #ping() ping}
 * message to the server. This automated behavior can be controlled
 * by changing the executor used to schedule maintenance tasks.
 * This can be done either in the
 * {@link AGServer#setExecutor(ScheduledExecutorService)} server}
 * object or when creating a new connection with
 * {@link AGRepository#getConnection(ScheduledExecutorService)}.</p>
 * <p>
 * A session should be {@link #close() closed} when finished.
 * </p>
 * <p>{@link #setSessionLoadInitFile(boolean) InitFiles}
 * and {@link #addSessionLoadScript(String) Scripts}
 * are loaded into the server only for sessions.  See
 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#scripting"
 * target="_top">Scripting in HTTP Protocol</a> and
 * <a href="http://www.franz.com/agraph/support/documentation/current/agwebview.html"
 * target="_top">search for "InitFile" in WebView</a> for how to create initFiles.
 * </p>
 * <p>Starting a session causes http requests to use a new port, which
 * may cause an exception if the client can not access it.
 * See <a href="http://www.franz.com/agraph/support/documentation/current/server-installation.html#sessionport"
 * target="_top">Session Port Setup</a>.
 * </p>
 * <p>Methods that start a session if not already started:</p>
 * <ul>
 * <li>{@link #setAutoCommit(boolean)}</li>
 * <li>{@link #addRules(String)}</li>
 * <li>{@link #addRules(InputStream)}</li>
 * <li>{@link #registerSNAGenerator(String, List, List, List, String)}</li>
 * </ul>
 * <p>Methods that affect a session in use:</p>
 * <ul>
 * <li>{@link #commit()}</li>
 * <li>{@link #rollback()}</li>
 * <li>{@link #ping()}</li>
 * <li>{@link #close()}</li>
 * </ul>
 * <p>Methods to configure a session before it is started:</p>
 * <ul>
 * <li>{@link #setSessionLifetime(int)} and {@link #getSessionLifetime()}</li>
 * <li>{@link #setSessionLoadInitFile(boolean)}</li>
 * <li>{@link #addSessionLoadScript(String)}</li>
 * </ul>
 *
 * <h3><a id="mapping">Data-type and Predicate Mapping</a></h3>
 * <p>For more details, see the HTTP Protocol docs for
 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#mapping"
 * target="_top">Type Mappings</a>
 * and the Lisp reference for
 * <a href="http://www.franz.com/agraph/support/documentation/current/lisp-reference.html#ref-type-mapping"
 * target="_top">Data-type and Predicate Mapping</a>.
 * </p>
 * <p>Methods for type mappings:</p>
 * <ul>
 * <li>{@link #clearMappings()}</li>
 * <li>{@link #getDatatypeMappings()}</li>
 * <li>{@link #registerDatatypeMapping(IRI, IRI)}</li>
 * <li>{@link #deleteDatatypeMapping(IRI)}</li>
 * <li>{@link #getPredicateMappings()}</li>
 * <li>{@link #registerPredicateMapping(IRI, IRI)}</li>
 * <li>{@link #deletePredicateMapping(IRI)}</li>
 * </ul>
 *
 * @since v4.0
 */
public class AGRepositoryConnection
        extends AbstractRepositoryConnection
        implements RepositoryConnection, AutoCloseable {

    public static final String PROP_STREAM_RESULTS = "com.franz.agraph.repository.AGRepositoryConnection.streamResults";

    public static final String PROP_USE_ADD_STATEMENT_BUFFER = "com.franz.agraph.repository.AGRepositoryConnection.useAddStatementBuffer";
    public static final String PROP_ADD_STATEMENT_BUFFER_MAX_SIZE = "com.franz.agraph.repository.AGRepositoryConnection.addStatementBufferMaxSize";
    public static final int DEFAULT_ADD_STATEMENT_BUFFER_SIZE = 10000;

    private final AGAbstractRepository repository;
    private final AGHttpRepoClient repoclient;
    private final AGValueFactory vf;
    private final List<JSONArray> addStatementBuffer; // never null
    private boolean streamResults;
    // If not null close will return the connection to this pool instead of closing.
    private AGConnPool pool;
    /**
     * Whether buffering of "Add" statements is enabled. (Not activated in autocommit mode).
     */
    private boolean addStatementBufferEnabled;
    private int addStatementBufferMaxSize;

    /**
     * @param repository a repository name
     * @param client     the HTTP client through which requests on this connection will be made
     * @see AGRepository#getConnection()
     * @see AGVirtualRepository#getConnection()
     */
    public AGRepositoryConnection(AGRepository repository, AGHttpRepoClient client) {
        this(repository, client, repository);
    }

    public AGRepositoryConnection(AGVirtualRepository repository, AGHttpRepoClient client) {
        this(repository, client, repository.wrapped);
    }

    private AGRepositoryConnection(AGAbstractRepository repository, AGHttpRepoClient client, AGRepository realRepo) {
        super(repository);
        this.repository = repository;
        this.repoclient = client;
        // use system property so this can be tested from build.xml
        setStreamResults(Boolean.parseBoolean(System.getProperty(PROP_STREAM_RESULTS)));
        vf = new AGValueFactory(realRepo, this);

        addStatementBufferEnabled = Boolean.parseBoolean(System.getProperty(PROP_USE_ADD_STATEMENT_BUFFER));
        addStatementBufferMaxSize = Integer.parseInt(System.getProperty(PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + DEFAULT_ADD_STATEMENT_BUFFER_SIZE));
        addStatementBuffer = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "{" + super.toString()
                + " " + getHttpRepoClientInternal()
                + "}";
    }

    /*
     * @Override protected void finalize() throws Throwable { try { if
     * (isOpen()) { close(); } } finally { super.finalize(); } }
     */

    @Override
    public AGAbstractRepository getRepository() {
        return repository;
    }

    /**
     * @return the AGServer object associated with this connection
     */
    public AGServer getServer() {
        return getRepository().getCatalog().getServer();
    }

    /**
     * This is the method to call, in this or other classes, if you want to do HTTP interaction with the repository.
     * As a side effect (what the "prepare" stands for) any buffered statements are sent over.
     *
     * @return the AGHttpRepoclient used by this connection (after the addStatementBuffer is cleared)
     */
    // Methods in this class should use the private method "getHttpRepoClientInternal()"
    // for side effect free access.
    public AGHttpRepoClient prepareHttpRepoClient() {
        return getHttpRepoClientHandlingBuffer(true);
    }

    /**
     * This is the method to call inside this class, where you need to do HTTP interaction with the repository
     * but don't want to trigger sending buffered statements (as happens in {@link #prepareHttpRepoClient()}).
     *
     * @return the AGHttpRepoclient used by this connection (without influencing the addStatementBuffer)
     */
    private AGHttpRepoClient getHttpRepoClientInternal() {
        return getHttpRepoClientHandlingBuffer(false);
    }

    private AGHttpRepoClient getHttpRepoClientHandlingBuffer(boolean handleBuffer) {
        if (handleBuffer && addStatementBufferEnabled) {
            forwardBufferedAddStatements();
        }
        return repoclient;
    }

    /**
     * @return whether the addStatementBuffer is in principle enabled (by the {@link #PROP_USE_ADD_STATEMENT_BUFFER} property
     * or a call to {@link #setAddStatementBufferEnabled(boolean) setAddStatementBufferEnabled}).
     */
    public boolean isAddStatementBufferEnabled() {
        return addStatementBufferEnabled;
    }

    /**
     * Enable or disable the use of the addStatementBuffer. This can be called within an active transaction and will affect
     * (speed up) the rest of the transaction. When called in autocommit mode, it has no immediate effect, but in a subsequent
     * transaction the buffer functionality will be used.
     * <p>
     * The buffer can also be enabled by setting property {@link #PROP_USE_ADD_STATEMENT_BUFFER}.
     *
     * @param enabled whether to enable buffering
     */
    public void setAddStatementBufferEnabled(boolean enabled) {
        addStatementBufferEnabled = enabled;

        if (!addStatementBufferEnabled) {
            forwardBufferedAddStatements();
        }
    }

    /**
     * @return whether the addStatementBuffer is actually used right now for the connection.
     */
    public boolean isUseAddStatementBuffer() {
        return addStatementBufferEnabled && !getHttpRepoClientInternal().isAutoCommit();
    }

    /**
     * Arrange for the JSON to be buffered and then later sent in a batch.
     */
    private void bufferAddStatement(JSONArray rows) {
        addStatementBuffer.add(rows);
        if (addStatementBuffer.size() >= addStatementBufferMaxSize) {
            forwardBufferedAddStatements();
        }
    }

    public int getAddStatementBufferMaxSize() {
        return addStatementBufferMaxSize;
    }

    /**
     * Set the maximum size of the addStatementBuffer.
     * <p>
     * This size can also be set by using property {@link #PROP_ADD_STATEMENT_BUFFER_MAX_SIZE}.
     *
     * @param size new maximum buffer size
     */
    public void setAddStatementBufferMaxSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Buffer maxSize must be positive integer");
        }
        addStatementBufferMaxSize = size;
        if (addStatementBuffer.size() >= addStatementBufferMaxSize) {
            forwardBufferedAddStatements(); // simply send them all
        }
    }

    /**
     * Forward the buffered statements to the repo, by sending the JSON over the HTTP connection.
     * Afterwards the buffer will be empty (even if the upload failed: those statements are lost).
     */
    private void forwardBufferedAddStatements() throws RepositoryException {
        if (!addStatementBuffer.isEmpty()) {
            // System.out.println("Now uploading " + addStatementBuffer.size()+ " pending \"Add Statement\"");
            JSONArray totalArray = new JSONArray();
            for (JSONArray addStmtJson : addStatementBuffer) {
                append(totalArray, addStmtJson);
            }
            try {
                getHttpRepoClientInternal().uploadJSON(totalArray);
            } catch (AGHttpException e) {
                throw new RepositoryException(e);
            } finally {
                addStatementBuffer.clear();
            }
        }
    }

    /**
     * @return the number of buffered statements to be added
     */
    public int getNumBufferedAddStatements() {
        return addStatementBuffer.size();
    }

    @Override
    public AGValueFactory getValueFactory() {
        return vf;
    }

    @Override
    protected void addWithoutCommit(Resource subject, IRI predicate,
                                    Value object, Resource... contexts) throws RepositoryException {
        Statement st = new StatementImpl(subject, predicate, object);
        JSONArray rows = encodeJSON(st, null, contexts);

        if (isUseAddStatementBuffer()) {
            bufferAddStatement(rows);
        } else {
            try {
                prepareHttpRepoClient().uploadJSON(rows);
            } catch (AGHttpException e) {
                throw new RepositoryException(e);
            }
        }
    }

    protected void addWithoutCommit(Resource subject, IRI predicate,
                                    Value object, JSONObject attributes, Resource... contexts) throws RepositoryException {
        Statement st = new StatementImpl(subject, predicate, object);
        JSONArray rows = encodeJSON(st, attributes, contexts);

        if (isUseAddStatementBuffer()) {
            bufferAddStatement(rows);
        } else {
            try {
                prepareHttpRepoClient().uploadJSON(rows);
            } catch (AGHttpException e) {
                throw new RepositoryException(e);
            }
        }
    }

    @Override
    protected void removeWithoutCommit(Resource subject, IRI predicate,
                                       Value object, Resource... contexts) throws RepositoryException {
        prepareHttpRepoClient().deleteStatements(subject, predicate, object,
                contexts);
    }

    public void add(Iterable<? extends Statement> statements,
                    Resource... contexts) throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        JSONArray rows = new JSONArray();
        for (Statement st : statements) {
            JSONArray rows_st = encodeJSON(st, null, contexts);
            append(rows, rows_st);
        }
        try {
            prepareHttpRepoClient().uploadJSON(rows, contexts);
        } catch (AGHttpException e) {
            throw new RepositoryException(e);
        }
    }

    public void add(Iterable<? extends Statement> statements,
                    JSONObject attributes,
                    Resource... contexts) throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        JSONArray rows = new JSONArray();
        for (Statement st : statements) {
            JSONArray rows_st = encodeJSON(st, attributes, contexts);
            append(rows, rows_st);
        }
        try {
            prepareHttpRepoClient().uploadJSON(rows, contexts);
        } catch (AGHttpException e) {
            throw new RepositoryException(e);
        }
    }

    private void append(JSONArray rows, JSONArray rowsSt) {
        for (int i = 0; i < rowsSt.length(); i++) {
            try {
                rows.put(rowsSt.get(i));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <E extends Exception> void add(
            Iteration<? extends Statement, E> statementIter,
            Resource... contexts) throws RepositoryException, E {
        OpenRDFUtil.verifyContextNotNull(contexts);
        JSONArray rows = new JSONArray();
        while (statementIter.hasNext()) {
            append(rows, encodeJSON(statementIter.next(), null, contexts));
        }
        try {
            prepareHttpRepoClient().uploadJSON(rows);
        } catch (AGHttpException e) {
            throw new RepositoryException(e);
        }
    }

    public <E extends Exception> void add(
            Iteration<? extends Statement, E> statementIter,
            JSONObject attributes,
            Resource... contexts) throws RepositoryException, E {
        OpenRDFUtil.verifyContextNotNull(contexts);
        JSONArray rows = new JSONArray();
        while (statementIter.hasNext()) {
            append(rows, encodeJSON(statementIter.next(), attributes, contexts));
        }
        try {
            prepareHttpRepoClient().uploadJSON(rows);
        } catch (AGHttpException e) {
            throw new RepositoryException(e);
        }
    }

    private JSONArray encodeJSON(Statement st, JSONObject attributes, Resource... contexts) {
        JSONArray rows = new JSONArray();
        String attrs = null;

        if (attributes != null) {
            attrs = attributes.toString();
        }

        if (contexts.length == 0) {
            JSONArray row = new JSONArray().put(encodeValueForStorageJSON(st.getSubject()))
                    .put(encodeValueForStorageJSON(st.getPredicate()))
                    .put(encodeValueForStorageJSON(st.getObject()));
            // there are no contexts passed in, but a context may be encoded in the row.
            if (st.getContext() != null) {
                row.put(encodeValueForStorageJSON(st.getContext()));
            } else {
                row.put((Object) null);
            }
            if (attrs != null) {
                row.put(attrs);
            }
            rows.put(row);
        } else {
            for (Resource c : contexts) {
                JSONArray row = new JSONArray().put(encodeValueForStorageJSON(st.getSubject()))
                        .put(encodeValueForStorageJSON(st.getPredicate()))
                        .put(encodeValueForStorageJSON(st.getObject()));
                // contexts passed in as argument to encodeJSON supersede any context that may
                // be specified in the row itself. A context of null refers to the default Graph.
                if (c != null) {
                    row.put(encodeValueForStorageJSON(c));
                } else {
                    row.put((Object) null);
                }
                if (attrs != null) {
                    row.put(attrs);
                }
                rows.put(row);
            }
        }
        return rows;
    }

    private String encodeValueForStorageJSON(Value v) {
        Value storableValue = AGHttpRepoClient.getStorableValue(v, vf, getHttpRepoClientInternal().getAllowExternalBlankNodeIds());
        return NTriplesUtil.toNTriplesString(storableValue);
    }

    /**
     * Adds RDF data from the specified file to a specific contexts in the
     * repository.
     *
     * @param file       a file containing RDF data
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved. This defaults to the value of
     *                   {@link java.io.File#toURI() file.toURI()} if the value is set to
     *                   <code>null</code>
     * @param dataFormat the serialization format of the data
     * @param contexts   the contexts to add the data to. Note that this parameter is a
     *                   vararg and as such is optional. If no contexts are specified, the
     *                   data is added to any context specified in the actual data file, or
     *                   if the data contains no context, it is added to the default context.
     *                   If one or more contexts are specified the data is added to these
     *                   contexts, ignoring any context information in the data itself
     * @throws IOException                  if an I/O error occurred while reading from the file
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        add(file, baseURI, dataFormat, null, contexts);
    }

    /**
     * Adds RDF data from the specified file to a specific contexts in the
     * repository.
     *
     * @param file       a file containing RDF data
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved. This defaults to the value of
     *                   {@link java.io.File#toURI() file.toURI()} if the value is set to
     *                   <code>null</code>
     * @param dataFormat the serialization format of the data
     * @param attributes a JSONObject of attribute bindings that will be added to each statement
     *                   imported from `file'. For RDFFormats that support the specification of
     *                   attributes (like NQX) these attributes will be applied to statements
     *                   that do not already specify attributes
     * @param contexts   the contexts to add the data to. Note that this parameter is a
     *                   vararg and as such is optional. If no contexts are specified, the
     *                   data is added to any context specified in the actual data file, or
     *                   if the data contains no context, it is added to the default context.
     *                   If one or more contexts are specified the data is added to these
     *                   contexts, ignoring any context information in the data itself
     * @throws IOException                  if an I/O error occurred while reading from the file
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(File file, String baseURI, RDFFormat dataFormat,
                    JSONObject attributes, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        if (baseURI == null) {
            // default baseURI to file
            baseURI = file.toURI().toString();
        }
        if (dataFormat == null) {
            dataFormat = Rio.getParserFormatForFileName(file.getName()).orElse(null);
        }

        try (final FileInputStream in = new FileInputStream(file)) {
            add(in, baseURI, dataFormat, file.length(), attributes, contexts);
        }
    }

    /**
     * Adds the RDF data that can be found at the specified URL to the
     * repository, optionally to one or more named contexts.
     *
     * @param url        the URL of the RDF data
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved. This defaults to the value of {@link
     *                   java.net.URL#toExternalForm() url.toExternalForm()} if the value is
     *                   set to <code>null</code>
     * @param dataFormat the serialization format of the data
     * @param contexts   the contexts to add the data to. If one or more contexts are
     *                   specified the data is added to these contexts, ignoring any context
     *                   information in the data itself
     * @throws IOException                  if an I/O error occurred while reading from the URL
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        add(url, baseURI, dataFormat, null, contexts);
    }

    /**
     * Adds the RDF data that can be found at the specified URL to the
     * repository, optionally to one or more named contexts.
     *
     * @param url        the URL of the RDF data
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved. This defaults to the value of {@link
     *                   java.net.URL#toExternalForm() url.toExternalForm()} if the value is
     *                   set to <code>null</code>
     * @param dataFormat the serialization format of the data
     * @param attributes a JSONObject of attribute bindings that will be added to each statement
     *                   imported from `url'. For RDFFormats that support the specification of
     *                   attributes (like NQX) these attributes will be applied to statements
     *                   that do not already specify attributes
     * @param contexts   the contexts to add the data to. If one or more contexts are
     *                   specified the data is added to these contexts, ignoring any context
     *                   information in the data itself
     * @throws IOException                  if an I/O error occurred while reading from the URL
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(URL url, String baseURI, RDFFormat dataFormat,
                    JSONObject attributes, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        if (baseURI == null) {
            baseURI = url.toExternalForm();
        }

        URLConnection con = url.openConnection();

        // Set appropriate Accept headers
        if (dataFormat != null) {
            for (String mimeType : dataFormat.getMIMETypes()) {
                con.addRequestProperty("Accept", mimeType);
            }
        } else {
            Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
            List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, true, null);
            for (String acceptParam : acceptParams) {
                con.addRequestProperty("Accept", acceptParam);
            }
        }

        if (dataFormat == null) {
            // Try to determine the data's MIME type
            String mimeType = con.getContentType();
            int semiColonIdx = mimeType.indexOf(';');
            if (semiColonIdx >= 0) {
                mimeType = mimeType.substring(0, semiColonIdx);
            }
            dataFormat = Rio.getParserFormatForMIMEType(mimeType).orElse(null);

            // Fall back to using file name extensions
            if (dataFormat == null) {
                dataFormat = Rio.getParserFormatForFileName(url.getPath()).orElse(null);
            }
        }

        try (final InputStream in = con.getInputStream()) {
            add(in, baseURI, dataFormat, attributes, contexts);
        }
    }

    /**
     * Adds RDF data from an InputStream to the repository, optionally to one or
     * more named contexts.
     *
     * @param in         an InputStream from which RDF data can be read
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved
     * @param dataFormat the serialization format of the data
     * @param contexts   the contexts to add the data to. If one or more contexts are
     *                   supplied the method ignores contextual information in the actual
     *                   data. If no contexts are supplied the contextual information in the
     *                   input stream is used, if no context information is available the
     *                   data is added without any context
     * @throws IOException                  if an I/O error occurred while reading from the input stream
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        add(in, baseURI, dataFormat, null, contexts);
    }

    /**
     * Adds RDF data from an InputStream to the repository, optionally to one or
     * more named contexts.
     *
     * @param in         an InputStream from which RDF data can be read
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved
     * @param dataFormat the serialization format of the data
     * @param attributes a JSONObject of attribute bindings that will be added to each statement
     *                   imported from `in'. For RDFFormats that support the specification of
     *                   attributes (like NQX) these attributes will be applied to statements
     *                   that do not already specify attributes
     * @param contexts   the contexts to add the data to. If one or more contexts are
     *                   supplied the method ignores contextual information in the actual
     *                   data. If no contexts are supplied the contextual information in the
     *                   input stream is used, if no context information is available the
     *                   data is added without any context
     * @throws IOException                  if an I/O error occurred while reading from the input stream
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(InputStream in, String baseURI, RDFFormat dataFormat,
                    JSONObject attributes, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        add(in, baseURI, dataFormat, -1, attributes, contexts);
    }

    // Same as the other add(InputStream, ...), but makes it possible to pass
    // the size of the stream if it happens to be known (used when uploading files).
    // Without knowing the size the HTTP request will have to use chunked encoding
    // and that is less efficient (due mostly to a suboptimal implementation of
    // unchunking in aserve).
    // Pass -1 if the size is not known.
    private void add(InputStream in, String baseURI, RDFFormat dataFormat,
                     long size, JSONObject attributes, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        // We need to look at the magic number at the start of the stream.
        // That means the stream needs to be buffered - if it is not, we
        // add the buffer here.
        if (!in.markSupported()) {
            in = new BufferedInputStream(in, 1024);
        }

        if (ZipUtil.isZipStream(in)) {
            addZip(in, baseURI, dataFormat, contexts);
        } else {
            prepareHttpRepoClient().upload(in, baseURI, dataFormat,
                    false,    // overwrite = false
                    size,
                    GZipUtil.isGZipStream(in) ? "gzip" : null,
                    attributes,
                    contexts);
        }
    }

    /**
     * Adds all files stored in a ZIP archive to the repository.
     *
     * @param in         input stream containing the ZIP file
     * @param baseURI    used to resolve relative URIs
     * @param dataFormat default RDF format used when it is not possible to
     *                   determine the format from file's extension
     * @param contexts   named graphs to add the data to
     * @throws IOException         .
     * @throws RDFParseException   .
     * @throws RepositoryException .
     */
    private void addZip(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        boolean autoCommit = !isActive();
        begin();

        try {
            try (ZipInputStream zipIn = new ZipInputStream(in)) {
                for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    RDFFormat format = Rio.getParserFormatForFileName(entry.getName()).orElse(dataFormat);

                    try {
                        // Prevent parser (Xerces) from closing the input stream
                        FilterInputStream wrapper = new FilterInputStream(zipIn) {

                            public void close() {
                            }
                        };
                        add(wrapper, baseURI, format, contexts);
                    } catch (RDFParseException e) {
                        if (autoCommit) {
                            rollback();
                        }

                        String msg = e.getMessage() + " in " + entry.getName();
                        RDFParseException pe = new RDFParseException(msg, e.getLineNumber(), e.getColumnNumber());
                        pe.initCause(e);
                        throw pe;
                    } finally {
                        zipIn.closeEntry();
                    }
                }
            }
        } catch (IOException | RepositoryException e) {
            if (autoCommit) {
                rollback();
            }
            throw e;
        } finally {
            if (autoCommit) {
                commit();
            }
        }
    }

    /**
     * Adds RDF data from a Reader to the repository, optionally to one or more
     * named contexts. <b>Note: using a Reader to upload byte-based data means
     * that you have to be careful not to destroy the data's character encoding
     * by enforcing a default character encoding upon the bytes. If possible,
     * adding such data using an InputStream is to be preferred.</b>
     *
     * @param reader     a Reader from which RDF data can be read
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved
     * @param dataFormat the serialization format of the data
     * @param contexts   he contexts to add the data to. If one or more contexts are
     *                   specified the data is added to these contexts, ignoring any context
     *                   information in the data itself
     * @throws IOException                  if an I/O error occurred while reading from the reader
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        add(reader, baseURI, dataFormat, null, contexts);
    }

    /**
     * Adds RDF data from a Reader to the repository, optionally to one or more
     * named contexts. <b>Note: using a Reader to upload byte-based data means
     * that you have to be careful not to destroy the data's character encoding
     * by enforcing a default character encoding upon the bytes. If possible,
     * adding such data using an InputStream is to be preferred.</b>
     *
     * @param reader     a Reader from which RDF data can be read
     * @param baseURI    the base URI against which any relative URIs in the data are
     *                   resolved
     * @param dataFormat the serialization format of the data
     * @param attributes a JSONObject of attribute bindings that will be added to each statement
     *                   imported from `reader'. For RDFFormats that support the specification of
     *                   attributes (like NQX) these attributes will be applied to statements
     *                   that do not already specify attributes
     * @param contexts   the contexts to add the data to. If one or more contexts are
     *                   specified the data is added to these contexts, ignoring any context
     *                   information in the data itself
     * @throws IOException                  if an I/O error occurred while reading from the reader
     * @throws UnsupportedRDFormatException if no parser is available for the specified RDF format
     * @throws RDFParseException            if an error occurred while parsing the RDF data
     * @throws RepositoryException          if the data could not be added to the repository, for example
     *                                      because the repository is not writable
     */
    public void add(Reader reader, String baseURI, RDFFormat dataFormat,
                    JSONObject attributes, Resource... contexts)
            throws IOException, RDFParseException, RepositoryException {
        try {
            prepareHttpRepoClient().upload(reader, baseURI, dataFormat, false, attributes, contexts);
        } catch (AGMalformedDataException e) {
            throw new RDFParseException(e);
        }
    }

    /**
     * Adds a statement with the specified subject, predicate and object to this
     * repository, optionally to one or more named contexts.
     *
     * @param subject   the statement's subject
     * @param predicate the statement's predicate
     * @param object    the statement's object
     * @param contexts  the contexts to add the data to. Note that this parameter is a
     *                  vararg and as such is optional. If the data contains no context,
     *                  it is added to the default context. If one or more contexts are
     *                  specified the data is added to these contexts, ignoring any context
     *                  information in the data itself
     * @throws RepositoryException if the data could not be added to the repository, for example
     *                             because the repository is not writable
     */
    public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        addWithoutCommit(subject, predicate, object, contexts);
    }

    public void add(Resource subject, IRI predicate, Value object, JSONObject attributes, Resource... contexts)
            throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        addWithoutCommit(subject, predicate, object, attributes, contexts);
    }

    /**
     * Removes the statement(s) with the specified subject, predicate and object
     * from the repository, optionally restricted to the specified contexts.
     *
     * @param subject   the statement's subject, or <code>null</code> for a wildcard
     * @param predicate the statement's predicate, or <code>null</code> for a wildcard
     * @param object    the statement's object, or <code>null</code> for a wildcard
     * @param contexts  the context(s) to remove the data from. Note that this parameter is
     *                  a vararg and as such is optional. If no contexts are supplied the
     *                  method operates on the entire repository
     * @throws RepositoryException if the statement(s) could not be removed from the repository, for
     *                             example because the repository is not writable
     */
    public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        removeWithoutCommit(subject, predicate, object, contexts);
    }

    public void remove(Statement st, Resource... contexts)
            throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        removeWithoutCommit(st, contexts);
    }

    public void add(Statement st, Resource... contexts)
            throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        addWithoutCommit(st, contexts);
    }

    public void add(Statement st, JSONObject attributes, Resource... contexts)
            throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        addWithoutCommit(st, attributes, contexts);
    }

    protected void addWithoutCommit(Statement st, Resource... contexts)
            throws RepositoryException {
        if (contexts.length == 0 && st.getContext() != null) {
            contexts = new Resource[] {st.getContext()};
        }

        addWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
    }

    protected void addWithoutCommit(Statement st, JSONObject attributes, Resource... contexts)
            throws RepositoryException {
        if (contexts.length == 0 && st.getContext() != null) {
            contexts = new Resource[] {st.getContext()};
        }

        addWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), attributes, contexts);
    }

    protected void removeWithoutCommit(Statement st, Resource... contexts)
            throws RepositoryException {
        if (contexts.length == 0 && st.getContext() != null) {
            contexts = new Resource[] {st.getContext()};
        }

        removeWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
    }

    public void remove(Iterable<? extends Statement> statements,
                       Resource... contexts) throws RepositoryException {
        OpenRDFUtil.verifyContextNotNull(contexts);
        JSONArray rows = new JSONArray();
        for (Statement st : statements) {
            append(rows, encodeJSON(st, null, contexts));
        }
        prepareHttpRepoClient().deleteJSON(rows);
    }

    public <E extends Exception> void remove(
            Iteration<? extends Statement, E> statements, Resource... contexts)
            throws RepositoryException, E {
        OpenRDFUtil.verifyContextNotNull(contexts);
        JSONArray rows = new JSONArray();
        while (statements.hasNext()) {
            append(rows, encodeJSON(statements.next(), null, contexts));
        }
        prepareHttpRepoClient().deleteJSON(rows);
    }

    /**
     * @see #setAutoCommit(boolean)
     * @deprecated since release 2.7.0. Use isActive() instead.
     */
    @Override
    public boolean isAutoCommit() throws RepositoryException {
        return prepareHttpRepoClient().isAutoCommit();
    }

    /**
     * Setting autoCommit to false creates a dedicated server session
     * which supports ACID transactions.
     * Setting to true will create a dedicated server session.
     * <p>
     * See <a href="#sessions">session overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-session"
     * target="_top">POST session</a> for more details.
     * <p>Starting a session causes http requests to use a new port, which
     * may cause an exception if the client can not access it.
     * See <a href="http://www.franz.com/agraph/support/documentation/current/server-installation.html#sessionport"
     * target="_top">Session Port Setup</a>.
     * </p>
     *
     * @deprecated As of release 2.7.0, use begin() instead.
     */
    @Override
    public void setAutoCommit(boolean autoCommit) throws RepositoryException {
        prepareHttpRepoClient().setAutoCommit(autoCommit);
    }

    /**
     *
     * @return an XAResource suitable for passing to a transaction manager to allow
     *         this connection to participate in an XA/2PC distributed transaction.
     */
    public XAResource getXAResource() {
        return new AGXAResource(this);
    }

    /**
     * Asks the server to prepare a commit for later finalization/rollback.
     *
     * @param xid The transaction id to assign to the prepared commit
     */
    public void prepareCommit(Xid xid) {
        prepareHttpRepoClient().commit(CommitPhase.PREPARE, xid);

    }

    /**
     * Commit the current transaction.
     * See <a href="#sessions">session overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-commit"
     * target="_top">POST commit</a> for more details.
     */
    public void commit() throws RepositoryException {
        prepareHttpRepoClient().commit();
    }


    /**
     * Finalize a previously prepared two phase commit (2PC).
     *
     * @param xid The XID of the prepared transaction to be finalized
     */
    public void commit(Xid xid) {
        prepareHttpRepoClient().commit(CommitPhase.COMMIT, xid);
    }

    /**
     * Commit the current transaction.
     * <p>
     * See <a href="#sessions">session overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-commit"
     * target="_top">POST commit</a> for more details.
     *
     * @param transactionSettings Distributed transaction settings to be used by this commit.
     */
    public void commit(final TransactionSettings transactionSettings) throws RepositoryException {
        try (Ctx ignored = transactionSettingsCtx(transactionSettings)) {
            prepareHttpRepoClient().commit();
        }
    }

    /**
     * Roll back the current transaction (discard all changes made since last commit).
     * See <a href="#sessions">session overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-rollback"
     * target="_top">POST rollback</a> for more details.
     */
    public void rollback() throws RepositoryException {
        // This could first clear addStatementBuffer in order to avoid sending it over needlessly.
        // To keep the buffering logic simple and without further side effects, we don't for now.
        prepareHttpRepoClient().rollback();
    }

    /**
     * Aborts a previously prepared commit.
     *
     * @param xid The transaction id of the prepared commit to abort.
     */
    public void rollback(Xid xid) {
        prepareHttpRepoClient().rollback(xid);
    }

    /**
     * @return an array of Xids of commits that have been prepared on the server.
     * @throws DecoderException if the response from the server is invalid.
     */
    public Xid[] getPreparedTransactions() throws DecoderException {
        return prepareHttpRepoClient().getPreparedTransactions();
    }

    /**
     * If true, automatically use {@link AGStreamTupleQuery}.
     * Default is false.
     *
     * @return boolean the value of the <code>streamResults</code> parameter
     * @see #setStreamResults(boolean)
     */
    public boolean isStreamResults() {
        return streamResults;
    }

    /**
     * Set to true to automatically use {@link AGStreamTupleQuery}
     * for {@link #prepareTupleQuery(QueryLanguage, String, String)}.
     *
     * @param streamResults new setting for the streamResults parameter
     * @see #isStreamResults()
     */
    public void setStreamResults(boolean streamResults) {
        this.streamResults = streamResults;
    }

    @Override
    public void clearNamespaces() throws RepositoryException {
        prepareHttpRepoClient().clearNamespaces();
    }

    /**
     * Closes the session if there is one started.
     * See <a href="#sessions">session overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-close-session"
     * target="_top">POST close</a> for more details.
     */
    @Override
    public void close() throws RepositoryException {
        if (pool != null) {
            try {
                pool.returnObject(this);
            } catch (final Exception e) {
                throw new RepositoryException(e);
            }
        } else {
            if (isOpen()) {
                prepareHttpRepoClient().close();
                super.close();
            }
        }
    }

    public void exportStatements(Resource subj, IRI pred, Value obj,
                                 boolean includeInferred, RDFHandler handler, Resource... contexts)
            throws RDFHandlerException, RepositoryException {
        prepareHttpRepoClient().getStatements(subj, pred, obj, Boolean.toString(includeInferred),
                handler, contexts);
    }

    public void exportStatements(RDFHandler handler, String... ids)
            throws RDFHandlerException, RepositoryException {
        prepareHttpRepoClient().getStatements(handler, ids);
    }

    public RepositoryResult<Resource> getContextIDs()
            throws RepositoryException {
        try {
            List<Resource> contextList = new ArrayList<>();

            try (TupleQueryResult contextIDs = prepareHttpRepoClient().getContextIDs()) {
                while (contextIDs.hasNext()) {
                    BindingSet bindingSet = contextIDs.next();
                    Value context = bindingSet.getValue("contextID");

                    if (context instanceof Resource) {
                        contextList.add((Resource) context);
                    }
                }
            }

            return createRepositoryResult(contextList);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Creates a RepositoryResult for the supplied element set.
     *
     * @param <E>      the class of elements in the set
     * @param elements the set of elements
     * @return RepositoryResult  the set of elements as a RepositoryResult
     */
    public <E> RepositoryResult<E> createRepositoryResult(
            Iterable<? extends E> elements) {
        return new RepositoryResult<>(
                new CloseableIteratorIteration<E, RepositoryException>(elements
                        .iterator()));
    }

    public String getNamespace(String prefix) throws RepositoryException {
        return prepareHttpRepoClient().getNamespace(prefix);
    }

    public RepositoryResult<Namespace> getNamespaces()
            throws RepositoryException {
        try {
            List<Namespace> namespaceList = new ArrayList<>();

            try (TupleQueryResult namespaces = prepareHttpRepoClient().getNamespaces()) {
                while (namespaces.hasNext()) {
                    BindingSet bindingSet = namespaces.next();
                    Value prefix = bindingSet.getValue("prefix");
                    Value namespace = bindingSet.getValue("namespace");

                    if (prefix instanceof Literal
                            && namespace instanceof Literal) {
                        String prefixStr = ((Literal) prefix).getLabel();
                        String namespaceStr = ((Literal) namespace).getLabel();
                        namespaceList.add(new SimpleNamespace(prefixStr,
                                namespaceStr));
                    }
                }
            }

            return createRepositoryResult(namespaceList);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }

    public RepositoryResult<Statement> getStatements(Resource subj, IRI pred,
                                                     Value obj, boolean includeInferred, Resource... contexts)
            throws RepositoryException {
        try {
            StatementCollector collector = new StatementCollector();
            exportStatements(subj, pred, obj, includeInferred, collector,
                    contexts);
            return createRepositoryResult(collector.getStatements());
        } catch (RDFHandlerException e) {
            // found a bug in StatementCollector?
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasStatement(Resource subj, IRI pred, Value obj,
                                boolean includeInferred, Resource... contexts)
            throws RepositoryException {

        final AtomicBoolean hasItems = new AtomicBoolean();
        AGHttpRepoClient client = prepareHttpRepoClient();
        StatementCollector collector = new StatementCollector() {
            public void handleStatement(Statement s) {
                hasItems.set(true);
            }
        };
        AGResponseHandler handler = new AGRDFHandler(
                client.getPreferredRDFFormat(),
                collector,
                getValueFactory(),
                client.getAllowExternalBlankNodeIds());
        // only a single statement will be requested
        client.getStatementsLimit(1, subj, pred, obj,
                Boolean.toString(includeInferred), handler, contexts);

        return hasItems.get();
    }

    /**
     * Downloads statements matching given pattern to a file.
     * <p>
     * The output format is determined by the server.
     *
     * @param file            Output path.
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @throws AGHttpException .
     */
    public void downloadStatements(final File file,
                                   final Resource subj, final IRI pred, final Value obj,
                                   final boolean includeInferred,
                                   final Resource... contexts)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                new AGDownloadHandler(file), contexts);
    }

    /**
     * Downloads statements matching given pattern to a file.
     * <p>
     * The output format is determined by the server.
     *
     * @param file            Output path.
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @throws AGHttpException .
     */
    public void downloadStatements(final String file,
                                   final Resource subj, final IRI pred, final Value obj,
                                   final boolean includeInferred,
                                   final Resource... contexts)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                new AGDownloadHandler(file), contexts);
    }

    /**
     * Downloads statements matching given pattern to a file.
     *
     * @param file            Output path.
     * @param format          Format to export the data in.
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @throws AGHttpException .
     */
    public void downloadStatements(final File file, final RDFFormat format,
                                   final Resource subj, final IRI pred, final Value obj,
                                   final boolean includeInferred,
                                   final Resource... contexts)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                new AGDownloadHandler(file, format), contexts);
    }

    /**
     * Downloads statements matching given pattern to a file.
     *
     * @param file            Output path.
     * @param format          Format to export the data in.
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @throws AGHttpException .
     */
    public void downloadStatements(final String file, final RDFFormat format,
                                   final Resource subj, final IRI pred, final Value obj,
                                   final boolean includeInferred,
                                   final Resource... contexts)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                new AGDownloadHandler(file, format), contexts);
    }

    /**
     * Downloads statements matching given pattern to a file.
     *
     * @param file            Output path.
     * @param mimeType        MIME type that will be requested from the server (output format).
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @throws AGHttpException .
     */
    public void downloadStatements(final File file, final String mimeType,
                                   final Resource subj, final IRI pred, final Value obj,
                                   final boolean includeInferred,
                                   final Resource... contexts)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                new AGDownloadHandler(file, mimeType), contexts);
    }

    /**
     * Downloads statements matching given pattern to a file.
     *
     * @param file            Output path.
     * @param mimeType        MIME type that will be requested from the server (output format).
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @throws AGHttpException .
     */
    public void downloadStatements(final String file, final String mimeType,
                                   final Resource subj, final IRI pred, final Value obj,
                                   final boolean includeInferred,
                                   final Resource... contexts)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                new AGDownloadHandler(file, mimeType), contexts);
    }

    /**
     * Returns statements matching given pattern as an InputStream.
     * <p>
     * The output format will be chosen by the server.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws AGHttpException .
     */
    public InputStream streamStatements(final Resource subj, final IRI pred, final Value obj,
                                        final boolean includeInferred,
                                        final Resource... contexts)
            throws AGHttpException {
        final AGRawStreamer handler = new AGRawStreamer();
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                handler, contexts);
        return handler.getStream();
    }

    /**
     * Returns statements matching given pattern as an InputStream.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param mimeType        MIME type that will be requested from the server (output format).
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws AGHttpException .
     */
    public InputStream streamStatements(final String mimeType,
                                        final Resource subj, final IRI pred, final Value obj,
                                        final boolean includeInferred,
                                        final Resource... contexts)
            throws AGHttpException {
        final AGRawStreamer handler = new AGRawStreamer(mimeType);
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                handler, contexts);
        return handler.getStream();
    }

    /**
     * Returns statements matching given pattern as an InputStream.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param format          Format to export the data in.
     * @param subj            Subject filter.
     * @param pred            Predicate filter.
     * @param obj             Object filter.
     * @param includeInferred If true, inferred triples will be included in the result.
     * @param contexts        Optional list of graphs to export.
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws AGHttpException .
     */
    public InputStream streamStatements(final RDFFormat format,
                                        final Resource subj, final IRI pred, final Value obj,
                                        final boolean includeInferred,
                                        final Resource... contexts)
            throws AGHttpException {
        final AGRawStreamer handler = new AGRawStreamer(format);
        prepareHttpRepoClient().getStatements(
                subj, pred, obj, Boolean.toString(includeInferred),
                handler, contexts);
        return handler.getStream();
    }

    /**
     * Returns statements having the specified ids.
     * <p>
     * This api is subject to change.  There is currently no
     * natural way to obtain a statement's triple id from the
     * java client; when that is possible, this api may change.
     *
     * @param ids Strings representing statement ids
     * @return the statements having the specified ids. The result object
     * is a {@link RepositoryResult} object, a lazy Iterator-like object
     * containing {@link Statement}s and optionally throwing a
     * {@link RepositoryException} when an error when a problem occurs
     * during retrieval
     * @throws RepositoryException if there is an error with this request
     */
    public RepositoryResult<Statement> getStatements(String... ids)
            throws RepositoryException {
        try {
            StatementCollector collector = new StatementCollector();
            exportStatements(collector, ids);
            return createRepositoryResult(collector.getStatements());
        } catch (RDFHandlerException e) {
            // found a bug in StatementCollector?
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads statements with given ids to a file.
     *
     * @param file   Output path.
     * @param format Format to export the data in.
     * @param ids    Strings representing statement ids
     * @throws AGHttpException .
     */
    public void downloadStatements(final File file, final RDFFormat format,
                                   final String ids)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(new AGDownloadHandler(file, format), ids);
    }

    /**
     * Downloads statements with given ids to a file.
     *
     * @param file   Output path.
     * @param format Format to export the data in.
     * @param ids    Strings representing statement ids
     * @throws AGHttpException .
     */
    public void downloadStatements(final String file, final RDFFormat format,
                                   final String ids)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(new AGDownloadHandler(file, format), ids);
    }

    /**
     * Downloads statements with given ids to a file.
     *
     * @param file     Output path.
     * @param mimeType MIME type to be requested from the server.
     *                 Use {@code "*&#47;*"} to let the server choose
     *                 the output format.
     * @param ids      Strings representing statement ids
     * @throws AGHttpException .
     */
    public void downloadStatements(final File file, final String mimeType,
                                   final String ids)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                new AGDownloadHandler(file, mimeType), ids);
    }

    /**
     * Downloads statements with given ids to a file.
     *
     * @param file     Output path.
     * @param mimeType MIME type to be requested from the server.
     *                 Use {@code "*&#47;*"} to let the server choose
     *                 the output format.
     * @param ids      Strings representing statement ids
     * @throws AGHttpException .
     */
    public void downloadStatements(final String file, final String mimeType,
                                   final String ids)
            throws AGHttpException {
        prepareHttpRepoClient().getStatements(
                new AGDownloadHandler(file, mimeType), ids);
    }

    /**
     * Returns statements with given ids as an InputStream.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param format Format to export the data in.
     * @param ids    Strings representing statement ids
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws AGHttpException .
     */
    public InputStream streamStatements(final RDFFormat format,
                                        final String... ids)
            throws AGHttpException {
        final AGRawStreamer handler = new AGRawStreamer(format);
        prepareHttpRepoClient().getStatements(handler, ids);
        return handler.getStream();
    }

    /**
     * Returns statements with given ids as an InputStream.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param mimeType MIME type to be requested from the server.
     *                 Use {@code "*&#47;*"} to let the server choose
     *                 the output format.
     * @param ids      Strings representing statement ids
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws AGHttpException .
     */
    public InputStream streamStatements(final String mimeType,
                                        final String... ids)
            throws AGHttpException {
        final AGRawStreamer handler = new AGRawStreamer(mimeType);
        prepareHttpRepoClient().getStatements(handler, ids);
        return handler.getStream();
    }

    /**
     * Prepares a {@link AGQuery} for evaluation on this repository. Note
     * that the preferred way of preparing queries is to use the more specific
     * {@link #prepareTupleQuery(QueryLanguage, String, String)},
     * {@link #prepareBooleanQuery(QueryLanguage, String, String)}, or
     * {@link #prepareGraphQuery(QueryLanguage, String, String)} methods instead.
     *
     * @throws UnsupportedOperationException if the method is not supported for the supplied query language
     * @throws IllegalArgumentException      if the query type (Tuple, Graph, Boolean) cannot be determined
     */
    public AGQuery prepareQuery(QueryLanguage ql, String queryString, String baseURI) {
        if (QueryLanguage.SPARQL.equals(ql)) {
            String strippedQuery = stripSparqlQueryString(queryString).toUpperCase();
            if (strippedQuery.startsWith("SELECT")) {
                return prepareTupleQuery(ql, queryString, baseURI);
            } else if (strippedQuery.startsWith("ASK")) {
                return prepareBooleanQuery(ql, queryString, baseURI);
            } else if (strippedQuery.startsWith("CONSTRUCT") || strippedQuery.startsWith("DESCRIBE")) {
                return prepareGraphQuery(ql, queryString, baseURI);
            } else {
                throw new IllegalArgumentException("Unable to determine a query type (Tuple, Graph, Boolean) for the query:\n" + queryString);
            }
        } else if (AGQueryLanguage.PROLOG.equals(ql)) {
            return prepareTupleQuery(ql, queryString, baseURI);
        } else {
            throw new UnsupportedOperationException("Operation not supported for query language " + ql);
        }
    }

    /**
     * Removes any SPARQL prefix and base declarations and comments from
     * the supplied SPARQL query string.
     *
     * @param queryString a SPARQL query string
     * @return a substring of queryString, with prefix and base declarations removed
     */
    private String stripSparqlQueryString(String queryString) {
        String normalizedQuery = queryString;

        // strip all prefix declarations
        Pattern pattern = Pattern.compile("prefix[^:]+:\\s*<[^>]*>\\s*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(queryString);
        int startIndexCorrection = 0;
        while (matcher.find()) {
            normalizedQuery = normalizedQuery.substring(matcher.end() - startIndexCorrection,
                    normalizedQuery.length());
            startIndexCorrection += (matcher.end() - startIndexCorrection);
        }

        // strip base declaration (if present)
        pattern = Pattern.compile("base\\s+<[^>]*>\\s*", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(normalizedQuery);
        if (matcher.find()) {
            normalizedQuery = normalizedQuery.substring(matcher.end(), normalizedQuery.length());
        }

        // strip any comments
        pattern = Pattern.compile("\\s*#.*", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(normalizedQuery);
        normalizedQuery = matcher.replaceAll("");

        return normalizedQuery.trim();
    }


    @Override
    public AGTupleQuery prepareTupleQuery(QueryLanguage ql, String queryString) {
        return prepareTupleQuery(ql, queryString, null);
    }

    @Override
    public AGTupleQuery prepareTupleQuery(QueryLanguage ql, String queryString,
                                          String baseURI) {
        // TODO: consider having the server parse and process the query,
        // throw MalformedQueryException, etc.
        AGTupleQuery q = new AGTupleQuery(this, ql, queryString, baseURI);
        q.prepare();
        if (streamResults) {
            q = new AGStreamTupleQuery(q);
        }
        return q;
    }

    @Override
    public AGGraphQuery prepareGraphQuery(QueryLanguage ql, String queryString) {
        return prepareGraphQuery(ql, queryString, null);
    }

    @Override
    public AGGraphQuery prepareGraphQuery(QueryLanguage ql, String queryString,
                                          String baseURI) {
        // TODO: consider having the server parse and process the query,
        // throw MalformedQueryException, etc.
        AGGraphQuery q = new AGGraphQuery(this, ql, queryString, baseURI);
        q.prepare();
        return q;
    }

    @Override
    public AGBooleanQuery prepareBooleanQuery(QueryLanguage ql,
                                              String queryString) {
        return prepareBooleanQuery(ql, queryString, null);
    }

    @Override
    public AGBooleanQuery prepareBooleanQuery(QueryLanguage ql,
                                              String queryString, String baseURI) {
        // TODO: consider having the server parse and process the query,
        // throw MalformedQueryException, etc.
        AGBooleanQuery q = new AGBooleanQuery(this, ql, queryString, baseURI);
        q.prepare();
        return q;
    }

    @Override
    public AGUpdate prepareUpdate(QueryLanguage ql,
                                  String queryString, String baseURI) {
        // TODO: consider having the server parse and process the query,
        // throw MalformedQueryException, etc.
        AGUpdate u = new AGUpdate(this, ql, queryString, baseURI);
        u.prepare();
        return u;
    }

    @Override
    public AGUpdate prepareUpdate(QueryLanguage ql, String queryString) {
        // TODO: consider having the server parse and process the query,
        // throw MalformedQueryException, etc.
        AGUpdate u = new AGUpdate(this, ql, queryString, null);
        u.prepare();
        return u;
    }

    @Override
    public void removeNamespace(String prefix) throws RepositoryException {
        prepareHttpRepoClient().removeNamespacePrefix(prefix);
    }

    @Override
    public void setNamespace(String prefix, String name)
            throws RepositoryException {
        prepareHttpRepoClient().setNamespacePrefix(prefix, name);
    }

    public long size(Resource... contexts) throws RepositoryException {
        return prepareHttpRepoClient().size(contexts);
    }

    /************************************
     * AllegroGraph Extensions hereafter
     */

    /**
     * Creates a freetext index with the given name and configuration.
     * <p>
     * See documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-freetext-index">freetext index parameters</a>.
     *
     * @param indexName the index name to create
     * @param config    the index configuration
     * @throws RepositoryException if there is an error with this request
     * @see AGFreetextIndexConfig#newInstance()
     */
    public void createFreetextIndex(String indexName, AGFreetextIndexConfig config)
            throws RepositoryException {
        List<String> predicates = new ArrayList<>();
        for (IRI uri : config.getPredicates()) {
            predicates.add(NTriplesUtil.toNTriplesString(uri));
        }
        prepareHttpRepoClient().createFreetextIndex(indexName, predicates, config.getIndexLiterals(), config.getIndexLiteralTypes(), config.getIndexResources(), config.getIndexFields(), config.getMinimumWordSize(), config.getStopWords(), config.getWordFilters(), config.getInnerChars(), config.getBorderChars(), config.getTokenizer());
    }

    /**
     * Deletes the freetext index of the specified name.
     *
     * @param indexName the index to be deleted
     * @throws RepositoryException if there is an error with this request
     * @see #createFreetextIndex(String, AGFreetextIndexConfig)
     */
    public void deleteFreetextIndex(String indexName) throws RepositoryException {
        prepareHttpRepoClient().deleteFreetextIndex(indexName);
    }

    /**
     * Registers a predicate for free text indexing. Once registered, the
     * objects of data added to the repository having this predicate will be
     * text indexed and searchable.
     *
     * @param name       of the index to create
     * @param predicates the predicates this index will operate on
     * @throws RepositoryException if there is an error with this request
     * @see #createFreetextIndex(String, AGFreetextIndexConfig)
     * @deprecated
     */
    public void createFreetextIndex(String name, IRI[] predicates)
            throws RepositoryException {
        AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
        config.getPredicates().addAll(Arrays.asList(predicates));
        createFreetextIndex(name, config);
    }

    /**
     * Gets the predicates that have been registered for text indexing.
     *
     * @param index name of the index to lookup
     * @return String[]  the predicates this index operates on
     * @throws RepositoryException if there is an error during the request
     * @see #getFreetextIndexConfig(String)
     * @deprecated
     */
    public String[] getFreetextPredicates(String index) throws RepositoryException {
        return prepareHttpRepoClient().getFreetextPredicates(index);
    }

    /**
     * Gets the configuration of the specified free text index.
     *
     * @param indexName name of the index
     * @return {@link AGFreetextIndexConfig}  the configuration of the specified index
     * @throws RepositoryException if there is an error during the request
     * @throws JSONException       if there is a problem parsing the response to JSON
     */
    public AGFreetextIndexConfig getFreetextIndexConfig(String indexName)
            throws RepositoryException, JSONException {
        return new AGFreetextIndexConfig(prepareHttpRepoClient().getFreetextIndexConfiguration(indexName));
    }

    /**
     * Gets freetext indexes that have been created
     *
     * @return String[]  a list of freetext index names
     * @throws RepositoryException if there is an error during the request
     * @see #listFreetextIndices()
     * @deprecated
     */
    public String[] getFreetextIndices() throws RepositoryException {
        return prepareHttpRepoClient().getFreetextIndices();
    }

    /**
     * Lists the freetext indices that have been defined for this repository.
     *
     * @return List  a list of freetext index names
     * @throws RepositoryException if there is an error during the request
     */
    public List<String> listFreetextIndices() throws RepositoryException {
        return prepareHttpRepoClient().listFreetextIndices();
    }

    /**
     * Registers a predicate mapping from the predicate to a primitive datatype.
     * This can be useful in speeding up query performance and enabling range
     * queries over datatypes.
     * <p>Once registered, the objects of any data added via this connection that
     * have this predicate will be mapped to the primitive datatype.</p>
     * <p>For example, registering that predicate {@code <http://example.org/age>}
     * is mapped to {@link XMLSchema#INT} and adding the triple:
     * {@code <http://example.org/Fred> <http://example.org/age> "24"}
     * will result in the object being treated as {@code "24"^^xsd:int}.</p>
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-put-predmapping"
     * target="_top">POST predicate mapping</a>.</p>
     *
     * @param predicate the predicate URI
     * @param primtype  the datatype URI
     * @throws RepositoryException if there is an error during the request
     * @see #getPredicateMappings()
     */
    public void registerPredicateMapping(IRI predicate, IRI primtype)
            throws RepositoryException {
        prepareHttpRepoClient().registerPredicateMapping(predicate, primtype);
    }

    /**
     * Deletes any predicate mapping associated with the given predicate.
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#delete-predmapping"
     * target="_top">DELETE predicate mapping</a>.</p>
     *
     * @param predicate the predicate mapping to delete
     * @throws RepositoryException if there is an error during the request
     * @see #getPredicateMappings()
     */
    public void deletePredicateMapping(IRI predicate)
            throws RepositoryException {
        prepareHttpRepoClient().deletePredicateMapping(predicate);
    }

    // TODO: return RepositoryResult<Mapping>?

    /**
     * Gets the predicate mappings defined for this connection.
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-predmapping"
     * target="_top">GET predicate mapping</a>
     * and the Lisp reference for the
     * <a href="http://www.franz.com/agraph/support/documentation/current/lisp-reference.html#function.predicate-mapping"
     * target="_top">predicate-mapping function</a>.
     * </p>
     *
     * @return String[]  the predicate mappings that have been registered
     * @throws RepositoryException if there is an error during the request
     * @see #registerPredicateMapping(IRI, IRI)
     * @see #deletePredicateMapping(IRI)
     * @see #getDatatypeMappings()
     */
    public String[] getPredicateMappings() throws RepositoryException {
        return prepareHttpRepoClient().getPredicateMappings();
    }

    // TODO: are all primtypes available as URI constants?

    /**
     * Registers a datatype mapping from the datatype to a primitive datatype.
     * This can be useful in speeding up query performance and enabling range
     * queries over user datatypes.
     * <p>Once registered, the objects of any data added via this connection that
     * have this datatype will be mapped to the primitive datatype.</p>
     * <p>For example, registering that datatype {@code <http://example.org/usertype>}
     * is mapped to {@link XMLSchema#INT} and adding the triple:
     * {@code <http://example.org/Fred> <http://example.org/age> "24"^^<http://example.org/usertype>}
     * will result in the object being treated as {@code "24"^^xsd:int}.</p>
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-put-typemapping"
     * target="_top">POST type mapping</a>.</p>
     *
     * @param datatype the name of the datatype mapping to register
     * @param primtype the primitive type of the new mapping
     * @throws RepositoryException if there is an error during the request
     * @see #getDatatypeMappings()
     */
    public void registerDatatypeMapping(IRI datatype, IRI primtype)
            throws RepositoryException {
        prepareHttpRepoClient().registerDatatypeMapping(datatype, primtype);
    }

    /**
     * Deletes any datatype mapping associated with the given datatype.
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#delete-typemapping"
     * target="_top">DELETE type mapping</a>.</p>
     *
     * @param datatype the datatype mapping to delete
     * @throws RepositoryException if there is an error during the request
     * @see #getDatatypeMappings()
     * @see #clearMappings()
     */
    public void deleteDatatypeMapping(IRI datatype) throws RepositoryException {
        prepareHttpRepoClient().deleteDatatypeMapping(datatype);
    }

    // TODO: return RepositoryResult<Mapping>?

    /**
     * Gets the datatype mappings defined for this connection.
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-typemapping"
     * target="_top">GET type mapping</a>
     * and the Lisp reference for the
     * <a href="http://www.franz.com/agraph/support/documentation/current/lisp-reference.html#function.datatype-mapping"
     * target="_top">datatype-mapping function</a>.
     *
     * @return String[]  the datatype mappings that have been registered
     * @throws RepositoryException if there is an error during the request
     * @see #deleteDatatypeMapping(IRI)
     * @see #clearMappings()
     * @see #registerDatatypeMapping(IRI, IRI)
     * @see #getPredicateMappings()
     */
    public String[] getDatatypeMappings() throws RepositoryException {
        return prepareHttpRepoClient().getDatatypeMappings();
    }

    /**
     * Deletes all user-defined predicate and datatype mappings for this connection,
     * and reestablishes the automatic mappings for primitive datatypes.
     * <p>
     * This is equivalent to clearMappings(false).
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#delete-all-mapping"
     * target="_top">DELETE all mapping</a> for more details.</p>
     *
     * @throws RepositoryException if there is an error during the request
     * @see #getDatatypeMappings()
     * @see #getPredicateMappings()
     */
    public void clearMappings() throws RepositoryException {
        prepareHttpRepoClient().clearMappings();
    }

    /**
     * Deletes all predicate and user-defined datatype mappings for this connection.
     * <p>
     * When includeAutoEncodedPrimitiveTypes is true, also deletes the automatic
     * mappings for primitive datatypes; this is rarely what you want to do, as it
     * will cause range queries to perform much less efficiently than when encodings
     * are used; this option can be useful for ensuring literal forms are preserved
     * in the store (there can be precision loss when encoding some literals).
     * <p>See <a href="#mapping">mapping overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#delete-all-mapping"
     * target="_top">DELETE all mapping</a> for more details.</p>
     *
     * @param includeAutoEncodedPrimitiveTypes true if auto-encoded primitive types should be cleared
     * @throws RepositoryException if there is an error during the request
     * @see #getDatatypeMappings()
     * @see #getPredicateMappings()
     */
    public void clearMappings(boolean includeAutoEncodedPrimitiveTypes) throws RepositoryException {
        prepareHttpRepoClient().clearMappings(includeAutoEncodedPrimitiveTypes);
    }

    /**
     * Deletes all attribute definitions from the repository.
     *
     * @throws RepositoryException if there is an error during the request
     */
    public void clearAttributes() throws RepositoryException {
        try {
            final JSONArray attributes = getAttributeDefinitions();
            for (int i = 0; i < attributes.length(); i++) {
                final JSONObject attribute = attributes.getJSONObject(i);
                deleteAttributeDefinition(attribute.getString("name"));
            }
        } catch (JSONException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Adds Prolog rules to be used on this connection.
     * <p>
     * See <a href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html#prolog"
     * target="_top">Prolog Lisp documentation</a>
     * and <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-functor"
     * target="_top">Prolog functor registration</a>.
     * <p>Starts a session if one is not already started.
     * See <a href="#sessions">session overview</a> for more details.
     * Starting a session causes http requests to use a new port, which
     * may cause an exception if the client can not access it.
     * See <a href="http://www.franz.com/agraph/support/documentation/current/server-installation.html#sessionport"
     * target="_top">Session Port Setup</a>.
     * </p>
     *
     * @param rules a string of rule text
     * @throws RepositoryException if there is an error during the request
     * @see #addRules(InputStream)
     */
    public void addRules(String rules) throws RepositoryException {
        prepareHttpRepoClient().addRules(rules);
    }

    // TODO: specify RuleLanguage

    /**
     * Adds Prolog rules to be used on this connection.
     * <p>
     * See <a href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html#prolog"
     * target="_top">Prolog Lisp documentation</a>
     * and <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-functor"
     * target="_top">Prolog functor registration</a>.
     * <p>Starts a session if one is not already started.
     * See <a href="#sessions">session overview</a> for more details.
     * Starting a session causes http requests to use a new port, which
     * may cause an exception if the client can not access it.
     * See <a href="http://www.franz.com/agraph/support/documentation/current/server-installation.html#sessionport"
     * target="_top">Session Port Setup</a>.
     * </p>
     *
     * @param rulestream a stream of rule text
     * @throws RepositoryException if there is an error during the request
     * @see #addRules(String)
     */
    public void addRules(InputStream rulestream) throws RepositoryException {
        prepareHttpRepoClient().addRules(rulestream);
    }

    /**
     * Evaluates a Lisp form on the server, and returns the result as a String.
     * <p>
     * See <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-eval"
     * target="_top">HTTP POST eval</a>.
     *
     * @param lispForm the Lisp form to evaluate
     * @return String  the result of the evaluation
     * @throws RepositoryException if there is an error during the request
     * @see #evalInServer(String)
     */
    public String evalInServer(String lispForm) throws RepositoryException {
        return prepareHttpRepoClient().evalInServer(lispForm);
    }

    /**
     * Evaluates a Lisp form on the server, and returns the result as a String.
     * <p>
     * See <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-eval"
     * target="_top">HTTP POST eval</a>.
     *
     * @param stream the Lisp form to evaluate
     * @return the result in a String
     * @throws RepositoryException if there is an error during the request
     * @see #evalInServer(String)
     */
    public String evalInServer(InputStream stream) throws RepositoryException {
        return prepareHttpRepoClient().evalInServer(stream);
    }

    /**
     * Instructs the server to fetch and load data from the specified URI.
     *
     * @param source     the URI to fetch and load
     * @param baseURI    the base URI for the source document
     * @param dataFormat the RDF data format for the source document
     * @param contexts   zero or more contexts into which data will be loaded
     * @throws RepositoryException if there is an error during the request
     */
    public void load(IRI source, String baseURI, RDFFormat dataFormat,
                     Resource... contexts) throws RepositoryException {
        prepareHttpRepoClient().load(source, baseURI, dataFormat, contexts);
    }

    /**
     * Instructs the server to fetch and load data from the specified URI.
     *
     * @param source     the URI to fetch and load
     * @param baseURI    the base URI for the source document
     * @param dataFormat the RDF data format for the source document
     * @param attributes a JSONObject of attribute bindings that will be added to each statement
     *                   imported from `source'. For RDFFormats that support the specification of
     *                   attributes (like NQX) these attributes will be applied to statements
     *                   that do not already specify attributes
     * @param contexts   zero or more contexts into which data will be loaded
     * @throws RepositoryException if there is an error during the request
     */
    public void load(IRI source, String baseURI, RDFFormat dataFormat,
                     JSONObject attributes, Resource... contexts)
            throws RepositoryException {
        prepareHttpRepoClient().load(source, baseURI, dataFormat, attributes, contexts);
    }

    /**
     * Instructs the server to load data from the specified server-side path.
     *
     * @param absoluteServerPath the path to the server-side source file
     * @param baseURI            the base URI for the source document
     * @param dataFormat         the RDF data format for the source document
     * @param contexts           zero or more contexts into which data will be loaded
     * @throws RepositoryException if there is an error during the request
     */
    public void load(String absoluteServerPath, String baseURI,
                     RDFFormat dataFormat, Resource... contexts)
            throws RepositoryException {
        prepareHttpRepoClient().load(absoluteServerPath, baseURI, dataFormat,
                contexts);
    }

    /**
     * Instructs the server to load data from the specified server-side path.
     *
     * @param absoluteServerPath the path to the server-side source file
     * @param baseURI            the base URI for the source document
     * @param dataFormat         the RDF data format for the source document
     * @param attributes         a JSONObject of attribute bindings that will be added to each statement
     *                           imported from `absoluteServerPath'. For RDFFormats that support the
     *                           specification of attributes (like NQX) these attributes will be applied
     *                           to statements that do not already specify attributes
     * @param contexts           zero or more contexts into which data will be loaded
     * @throws RepositoryException if there is an error during the request
     */
    public void load(String absoluteServerPath, String baseURI,
                     RDFFormat dataFormat, JSONObject attributes,
                     Resource... contexts) throws RepositoryException {
        prepareHttpRepoClient().load(absoluteServerPath, baseURI, dataFormat,
                attributes, contexts);
    }

    /**
     * Instructs the server to extend the life of this connection's dedicated
     * session, if it is using one.  Sessions that are idle for more than the session
     * lifetime will be terminated by the server.
     * <p>Note that this method is called automatically before the timeout expires.</p>
     * <p>See <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#sessions">session overview</a>
     * and <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-ping"
     * target="_top">GET ping</a> for more details.</p>
     *
     * @throws RepositoryException if there is an error during the request
     * @see #setSessionLifetime(int)
     */
    public void ping() throws RepositoryException {
        prepareHttpRepoClient().ping();
    }

    // TODO: return RepositoryResult<URI>?

    /**
     * @return String[]  the geospatial types that have been registered
     * @throws RepositoryException if there is an error during the request
     */
    public String[] getGeoTypes() throws RepositoryException {
        return prepareHttpRepoClient().getGeoTypes();
    }

    /**
     * Registers a cartesian geospatial subtype.
     *
     * @param stripWidth the strip width in some units
     * @param xmin       the minimum x range
     * @param ymin       the minimum y range
     * @param xmax       the maximum x range
     * @param ymax       the maximum y range
     * @return URI  the datatype encoding of this subtype
     * @throws RepositoryException if there is an error during the request
     */
    public IRI registerCartesianType(float stripWidth, float xmin, float xmax,
                                     float ymin, float ymax) throws RepositoryException {
        String nTriplesURI = prepareHttpRepoClient().registerCartesianType(stripWidth,
                xmin, xmax, ymin, ymax);
        return NTriplesUtil.parseURI(nTriplesURI, getValueFactory());
    }

    /**
     * Registers a spherical geospatial subtype.
     *
     * @param stripWidth the strip width in some units
     * @param unit       the distance unit <code>stripWidth</code> is specified in
     * @param latmin     the minimum latitude range
     * @param lonmin     the minimum longitude range
     * @param latmax     the maximum latitude range
     * @param lonmax     the maximum longitude range
     * @return URI  the datatype encoding of this subtype
     * @throws RepositoryException if there is an error during the request
     */
    public IRI registerSphericalType(float stripWidth, String unit,
                                     float latmin, float lonmin, float latmax, float lonmax)
            throws RepositoryException {
        String nTriplesURI = prepareHttpRepoClient().registerSphericalType(stripWidth,
                unit, latmin, lonmin, latmax, lonmax);
        return NTriplesUtil.parseURI(nTriplesURI, getValueFactory());
    }

    public IRI registerSphericalType(float stripWidth, String unit) throws RepositoryException {
        return registerSphericalType(stripWidth, unit, -90, -180, 90, 180);
    }

    /**
     * Registers a polygon.
     *
     * @param polygon the name of this polygon
     * @param points  a List of points describing the polygon
     * @throws RepositoryException if there is an error during the request
     */
    public void registerPolygon(IRI polygon, List<Literal> points)
            throws RepositoryException {
        List<String> nTriplesPoints = new ArrayList<>(points.size());
        for (Literal point : points) {
            nTriplesPoints.add(NTriplesUtil.toNTriplesString(point));
        }
        prepareHttpRepoClient().registerPolygon(NTriplesUtil.toNTriplesString(polygon), nTriplesPoints);
    }

    public RepositoryResult<Statement> getStatementsInBox(IRI type,
                                                          IRI predicate, float xmin, float xmax, float ymin,
                                                          float ymax, int limit, boolean infer) throws RepositoryException {
        StatementCollector collector = new StatementCollector();
        AGResponseHandler handler = new AGRDFHandler(prepareHttpRepoClient().getPreferredRDFFormat(),
                collector, getValueFactory(), prepareHttpRepoClient().getAllowExternalBlankNodeIds());
        prepareHttpRepoClient().getGeoBox(NTriplesUtil.toNTriplesString(type),
                NTriplesUtil.toNTriplesString(predicate),
                xmin, xmax, ymin, ymax, limit, infer, handler);
        return createRepositoryResult(collector.getStatements());
    }

    public RepositoryResult<Statement> getStatementsInCircle(IRI type,
                                                             IRI predicate, float x, float y, float radius,
                                                             int limit, boolean infer) throws RepositoryException {
        StatementCollector collector = new StatementCollector();
        AGResponseHandler handler = new AGRDFHandler(prepareHttpRepoClient().getPreferredRDFFormat(),
                collector, getValueFactory(), prepareHttpRepoClient().getAllowExternalBlankNodeIds());
        prepareHttpRepoClient().getGeoCircle(NTriplesUtil.toNTriplesString(type),
                NTriplesUtil.toNTriplesString(predicate),
                x, y, radius, limit, infer, handler);
        return createRepositoryResult(collector.getStatements());
    }

    public RepositoryResult<Statement> getGeoHaversine(IRI type,
                                                       IRI predicate, float lat, float lon, float radius,
                                                       String unit, int limit, boolean infer) throws RepositoryException {
        StatementCollector collector = new StatementCollector();
        AGResponseHandler handler = new AGRDFHandler(prepareHttpRepoClient().getPreferredRDFFormat(),
                collector, getValueFactory(), prepareHttpRepoClient().getAllowExternalBlankNodeIds());
        prepareHttpRepoClient().getGeoHaversine(NTriplesUtil.toNTriplesString(type),
                NTriplesUtil.toNTriplesString(predicate),
                lat, lon, radius, unit, limit, infer, handler);
        return createRepositoryResult(collector.getStatements());
    }

    public RepositoryResult<Statement> getStatementsInPolygon(IRI type,
                                                              IRI predicate, IRI polygon, int limit, boolean infer) throws RepositoryException {
        StatementCollector collector = new StatementCollector();
        AGResponseHandler handler = new AGRDFHandler(prepareHttpRepoClient().getPreferredRDFFormat(),
                collector, getValueFactory(), prepareHttpRepoClient().getAllowExternalBlankNodeIds());
        prepareHttpRepoClient().getGeoPolygon(NTriplesUtil.toNTriplesString(type), NTriplesUtil.toNTriplesString(predicate), NTriplesUtil.toNTriplesString(polygon), limit, infer, handler);
        return createRepositoryResult(collector.getStatements());
    }

    /**
     * See <a href="http://www.franz.com/agraph/support/documentation/current/lisp-reference.html#sna"
     * target="_top">Social network analysis Lisp documentation</a>
     * and <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#sna"
     * target="_top">SNA generator registration</a>.
     * <p>Starts a session if one is not already started.
     * See <a href="#sessions">session overview</a> for more details.
     * Starting a session causes http requests to use a new port, which
     * may cause an exception if the client can not access it.
     * See <a href="http://www.franz.com/agraph/support/documentation/current/server-installation.html#sessionport"
     * target="_top">Session Port Setup</a>.
     * </p>
     *
     * @param generator   Generator name
     * @param objectOfs   a list of predicates
     * @param subjectOfs  a list of predicates
     * @param undirecteds a list of predicates
     * @param query       a string representation of a select clause
     * @throws RepositoryException if there is an error during the request
     */
    public void registerSNAGenerator(String generator, List<IRI> objectOfs, List<IRI> subjectOfs, List<IRI> undirecteds, String query) throws RepositoryException {
        List<String> objOfs = new ArrayList<>();
        if (objectOfs != null) {
            for (IRI objectOf : objectOfs) {
                objOfs.add(NTriplesUtil.toNTriplesString(objectOf));
            }
        }
        List<String> subjOfs = new ArrayList<>();
        if (subjectOfs != null) {
            for (IRI subjectOf : subjectOfs) {
                subjOfs.add(NTriplesUtil.toNTriplesString(subjectOf));
            }
        }
        List<String> undirs = new ArrayList<>();
        if (undirecteds != null) {
            for (IRI undirected : undirecteds) {
                undirs.add(NTriplesUtil.toNTriplesString(undirected));
            }
        }
        prepareHttpRepoClient().registerSNAGenerator(generator, objOfs, subjOfs, undirs, query);
    }

    public void registerSNANeighborMatrix(String matrix, String generator, List<IRI> group, int depth) throws RepositoryException {
        if (group == null || group.size() == 0) {
            throw new IllegalArgumentException("group must be non-empty.");
        }
        List<String> grp = new ArrayList<>(3);
        for (IRI node : group) {
            grp.add(NTriplesUtil.toNTriplesString(node));
        }
        prepareHttpRepoClient().registerSNANeighborMatrix(matrix, generator, grp, depth);
    }

    /**
     * Returns a list of actively managed indices for this repository.
     *
     * @return a list of actively managed indices for this repository
     * @throws RDF4JException if there is an error during the request
     */
    public List<String> listIndices() throws RDF4JException {
        return prepareHttpRepoClient().listIndices(false);
    }

    /**
     * Returns a list of all possible index types for this repository.
     *
     * @return a list of valid index types
     * @throws RDF4JException if there is an error during the request
     */
    public List<String> listValidIndices() throws RDF4JException {
        return prepareHttpRepoClient().listIndices(true);
    }

    /**
     * Adds the given index to the list of actively managed indices.
     * This will take affect on the next commit.
     *
     * @param type a valid index type
     * @throws RepositoryException if there is an error during the request
     * @see #listValidIndices()
     */
    public void addIndex(String type) throws RepositoryException {
        prepareHttpRepoClient().addIndex(type);
    }

    /**
     * Drops the given index from the list of actively managed indices.
     * This will take affect on the next commit.
     *
     * @param type an actively managed index type
     * @throws RepositoryException if there is an error during the request
     * @see #listValidIndices()
     */
    public void dropIndex(String type) throws RepositoryException {
        prepareHttpRepoClient().dropIndex(type);
    }

    /**
     * Executes an application/x-rdftransaction.
     * <p>
     * This method is useful for bundling add/remove operations into a
     * single request and minimizing round trips to the server.
     * <p>
     * Changes are committed iff the connection is in autoCommit mode.
     * For increased throughput when sending multiple rdftransaction
     * requests, consider using autoCommit=false and committing less
     * frequently .
     *
     * @param rdftransaction a stream in application/x-rdftransaction format
     * @throws RepositoryException if there is an error during the request
     * @throws RDFParseException   if malformed data is encountered
     * @throws IOException         on errors reading from the <code>rdftransaction</code> stream
     */
    public void sendRDFTransaction(InputStream rdftransaction) throws RepositoryException,
            RDFParseException, IOException {
        try {
            prepareHttpRepoClient().sendRDFTransaction(rdftransaction);
        } catch (AGMalformedDataException e) {
            throw new RDFParseException(e);
        }
    }

    /**
     * send an RDFTransaction, including attributes.
     *
     * @param rdftransaction a stream in application/x-rdftransaction format
     * @param attributes     a JSONObject of attribute bindings that will be added
     *                       to each triple imported from `rdftransaction'.
     * @throws RepositoryException if there is an error during the request
     * @throws RDFParseException   if malformed data is encountered
     * @throws IOException         on errors reading from the <code>rdftransaction</code> stream
     */
    public void sendRDFTransaction(InputStream rdftransaction, JSONObject attributes)
            throws RepositoryException, RDFParseException, IOException {
        try {
            prepareHttpRepoClient().sendRDFTransaction(rdftransaction, attributes);
        } catch (AGMalformedDataException e) {
            throw new RDFParseException(e);
        }
    }

    /**
     * Registers an encodable namespace having the specified format.
     * <p>Registering an encodable namespace enables a more efficient
     * encoding of URIs in a namespace, and generation of unique
     * URIs for that namespace, because its URIs are declared to
     * conform to a specified format; the namespace is thereby
     * bounded in size, and encodable.</p>
     * <p>The namespace is any valid URIref, e.g.:
     * <code>http://franz.com/ns0</code>
     * </p>
     * <p>The format is a string using a simplified regular expression
     * syntax supporting character ranges and counts specifying the
     * suffix portion of the URIs in the namespace, e.g:
     * <code>[a-z][0-9]-[a-f]{3}</code>
     * </p>
     * <p>Generation of unique URIs {@link AGValueFactory#generateURI(String)}
     * for the above namespace and format might yield an ID such as:
     * <p>
     * <code>http://franz.com/ns0@@a0-aaa</code>
     * </p>
     * <p>Note: "@@" is used to concatenate the namespace and id suffix
     * to facilitate efficient recognition/encoding during parsing.</p>
     * <p>The format can be ambiguous (e.g., "[A-Z]{1,2}[B-C}{0,1}").
     * We will not check for ambiguity in this first version but can
     * add this checking at a later time.</p>
     * <p>If the format corresponds to a namespace that is not encodable
     * (it may be malformed, or perhaps it's too large to encode), an
     * exception is thrown.</p>
     * <p>For more details, see
     * <a href="http://www.franz.com/agraph/support/documentation/current/encoded-ids.html"
     * target="_top">Encoded IDs</a>.</p>
     *
     * @param namespace a valid namespace, a URI ref
     * @param format    a valid format for an encodable namespace
     * @throws RepositoryException if there is an error during the request
     * @see #registerEncodableNamespaces(Iterable)
     * @see #listEncodableNamespaces()
     * @see #unregisterEncodableNamespace(String)
     * @see AGValueFactory#generateURI(String)
     * @see AGValueFactory#generateURIs(String, int)
     */
    public void registerEncodableNamespace(String namespace, String format) throws RepositoryException {
        prepareHttpRepoClient().registerEncodableNamespace(namespace, format);
    }

    /**
     * Registers multiple formatted namespaces in a single request.
     *
     * @param formattedNamespaces an iterable collection of formatted namespaces
     * @throws RepositoryException if there is an error during the request
     * @see #registerEncodableNamespace(String, String)
     */
    public void registerEncodableNamespaces(Iterable<? extends AGFormattedNamespace> formattedNamespaces) throws RepositoryException {
        JSONArray rows = new JSONArray();
        for (AGFormattedNamespace ns : formattedNamespaces) {
            JSONObject row = new JSONObject();
            try {
                row.put("prefix", ns.getNamespace());
                row.put("format", ns.getFormat());
            } catch (JSONException e) {
                throw new RepositoryException(e);
            }
            rows.put(row);
        }
        prepareHttpRepoClient().registerEncodableNamespaces(rows);
    }

    /**
     * Returns a list of the registered encodable namespaces.
     *
     * @return List  a list of the registered encodable namespaces
     * @throws RDF4JException if there is a problem parsing the request
     * @see #registerEncodableNamespace(String, String)
     */
    public List<AGFormattedNamespace> listEncodableNamespaces()
            throws RDF4JException {
        List<AGFormattedNamespace> result = new ArrayList<>();
        try (TupleQueryResult tqresult = prepareHttpRepoClient()
                .getEncodableNamespaces()) {
            while (tqresult.hasNext()) {
                BindingSet bindingSet = tqresult.next();
                Value prefix = bindingSet.getValue("prefix");
                Value format = bindingSet.getValue("format");
                result.add(new AGFormattedNamespace(prefix.stringValue(),
                        format.stringValue()));
            }
        }
        return result;
    }

    /**
     * Unregisters the specified encodable namespace.
     *
     * @param namespace the namespace to unregister
     * @throws RepositoryException if there is an error during the request
     * @see #registerEncodableNamespace(String, String)
     */
    public void unregisterEncodableNamespace(String namespace) throws RepositoryException {
        prepareHttpRepoClient().unregisterEncodableNamespace(namespace);
    }

    /**
     * Invoke a stored procedure on the AllegroGraph server.
     * <p>The input arguments and the return value can be:
     * {@link String}, {@link Integer}, null, byte[],
     * or Object[] or {@link List} of these (can be nested).</p>
     * <p>See also
     * {@link #prepareHttpRepoClient()}.{@link AGHttpRepoClient#callStoredProc(String, String, Object...)
     * callStoredProc}<code>(functionName, moduleName, args)</code>
     * </p>
     *
     * @param functionName stored proc lisp function, for example "addTwo"
     * @param moduleName   lisp FASL file name, for example "example.fasl"
     * @param args         arguments to the stored proc
     * @return return value of stored proc
     * @throws AGCustomStoredProcException for errors from stored proc
     * @since v4.2
     * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
     */
    public Object callStoredProc(String functionName, String moduleName, Object... args)
            throws RepositoryException {
        return prepareHttpRepoClient().callStoredProc(functionName, moduleName, args);
    }

    /**
     * Returns the lifetime for a dedicated session spawned by this connection.
     * <p>See also: <a href="#sessions">Session overview</a>.</p>
     *
     * @return the session lifetime, in seconds
     * @see #setSessionLifetime(int)
     * @see #ping()
     */
    public int getSessionLifetime() {
        return prepareHttpRepoClient().getSessionLifetime();
    }

    /**
     * Sets the 'lifetime' for a dedicated session spawned by this connection.
     * Seconds a session can be idle before being collected.
     * If unset, the lifetime defaults to the value of
     * <a href="../../../../../daemon-config.html#DefaultSessionTimeout">DefaultSessionTimeout</a>.
     * If set, the value must not be larger than
     * <a href="../../../../../daemon-config.html#MaximumSessionTimeout">MaximumSessionTimeout</a>.
     * See <a href="../../../../../daemon-config.html#session-values">Session directives</a>
     * in <a href="../../../../../daemon-config.html">Server Configuration and Control</a> for more information.
     * <p>Also see <a href="#sessions">session overview</a> and
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-session"
     * target="_top">POST session</a> for more details.</p>
     *
     * @param lifetimeInSeconds the session lifetime, in seconds
     * @see #getSessionLifetime()
     * @see #ping()
     */
    public void setSessionLifetime(int lifetimeInSeconds) {
        prepareHttpRepoClient().setSessionLifetime(lifetimeInSeconds);
    }

    /**
     * Sets the 'loadInitFile' flag for a dedicated session spawned by
     * this connection. This method does not create a session.
     * <p>See also: <a href="#sessions">Session overview</a>.</p>
     *
     * @param loadInitFile boolean determining if the init file is loaded
     *                     into this session
     * @see #addSessionLoadScript(String)
     */
    public void setSessionLoadInitFile(boolean loadInitFile) {
        prepareHttpRepoClient().setSessionLoadInitFile(loadInitFile);
    }

    /**
     * Adds a 'script' for a dedicated session spawned by this connection.
     * This method does not create a session.
     * May be called multiple times for different scripts.
     * The script text must already be uploaded to the user.
     * <p>Scripts are server code that may be loaded during a session.</p>
     * <p>See also: <a href="#sessions">Session overview</a>.</p>
     *
     * @param scriptName name of the script to be loaded
     * @see #setSessionLoadInitFile(boolean)
     */
    public void addSessionLoadScript(String scriptName) {
        prepareHttpRepoClient().addSessionLoadScript(scriptName);
    }

    long cachedStoreID = -1;

    /**
     * Returns the store ID
     *
     * @return The store ID
     * @throws RepositoryException if there is an error during the request
     */
    public long getStoreID() throws RepositoryException {
        if (cachedStoreID == -1) {
            cachedStoreID = prepareHttpRepoClient().getStoreID();
        }
        return cachedStoreID;
    }

    /**
     * Enables the spogi cache in this repository.
     * <p>
     * Takes a size argument to set the size of the cache.
     *
     * @param size the size of the cache, in triples
     * @throws RepositoryException if there is an error during the request
     */
    public void enableTripleCache(long size) throws RepositoryException {
        prepareHttpRepoClient().enableTripleCache(size);
    }

    /**
     * Returns the size of the spogi cache.
     *
     * @return the size of the spogi cache, in triples
     * @throws RepositoryException if there is an error during the request
     */
    public long getTripleCacheSize() throws RepositoryException {
        return prepareHttpRepoClient().getTripleCacheSize();
    }

    /**
     * Disables the spogi triple cache.
     *
     * @throws RepositoryException if there is an error during the request
     */
    public void disableTripleCache() throws RepositoryException {
        prepareHttpRepoClient().disableTripleCache();
    }

    /**
     * Gets the commit period used within large add/load operations.
     *
     * @return int  the current upload commit period
     * @throws RepositoryException if there is an error with this request
     * @see AGHttpRepoClient#getUploadCommitPeriod()
     */
    public int getUploadCommitPeriod() throws RepositoryException {
        return prepareHttpRepoClient().getUploadCommitPeriod();

    }

    /**
     * Sets the commit period to use within large add/load operations.
     *
     * @param period commit after this many statements
     * @throws RepositoryException if there is an error with this request
     * @see AGHttpRepoClient#setUploadCommitPeriod(int)
     */
    public void setUploadCommitPeriod(int period) throws RepositoryException {
        prepareHttpRepoClient().setUploadCommitPeriod(period);

    }

    /**
     * Instruct the server to optimize the indices for this store.
     *
     * @param wait  a boolean, false for request to return immediately
     * @param level determines the work to be done. See the index documentation
     *              for an explanation of the different levels
     * @throws RepositoryException if there is an error with this request
     *                             See the <a href="http://franz.com/agraph/support/documentation/current/triple-index.html#optimize">Index documentation</a>
     *                             for more details.
     */
    public void optimizeIndices(Boolean wait, int level) throws RepositoryException {
        prepareHttpRepoClient().optimizeIndices(wait, level);
    }

    public void optimizeIndices(Boolean wait) throws RepositoryException {
        prepareHttpRepoClient().optimizeIndices(wait);
    }

    /**
     * @param uri SPIN function identifier
     * @return String  the SPIN function query text
     * @throws RDF4JException if there is an error with this request
     * @see #putSpinFunction(AGSpinFunction)
     * @see #deleteSpinFunction(String)
     * @see #listSpinFunctions()
     * @see #getSpinMagicProperty(String)
     * @since v4.4
     */
    public String getSpinFunction(String uri) throws RDF4JException {
        return prepareHttpRepoClient().getSpinFunction(uri);
    }

    /**
     * @return List  currently defined SPIN functions
     * @throws RDF4JException if there is an error with this request
     * @see #getSpinFunction(String)
     * @see #putSpinFunction(AGSpinFunction)
     * @see #deleteSpinFunction(String)
     * @see #listSpinMagicProperties()
     * @since v4.4
     */
    public List<AGSpinFunction> listSpinFunctions() throws RDF4JException {
        try (TupleQueryResult list = prepareHttpRepoClient().listSpinFunctions()) {
            List<AGSpinFunction> result = new ArrayList<>();
            while (list.hasNext()) {
                result.add(new AGSpinFunction(list.next()));
            }
            return result;
        }
    }

    /**
     * @param fn the SPIN function to add
     * @throws RDF4JException if there is an error with this request
     * @see #getSpinFunction(String)
     * @see #deleteSpinFunction(String)
     * @see #putSpinMagicProperty(AGSpinMagicProperty)
     * @since v4.4
     */
    public void putSpinFunction(AGSpinFunction fn) throws RDF4JException {
        prepareHttpRepoClient().putSpinFunction(fn);
    }

    /**
     * @param uri SPIN function identifier
     * @throws RDF4JException if there is an error with this request
     * @see #putSpinFunction(AGSpinFunction)
     * @see #getSpinFunction(String)
     * @since v4.4
     */
    public void deleteSpinFunction(String uri) throws RDF4JException {
        prepareHttpRepoClient().deleteSpinFunction(uri);
    }

    /**
     * @param uri SPIN magic property identifier
     * @return String  describing the SPIN magic property
     * @throws RDF4JException if there is an error with this request
     * @see #putSpinMagicProperty(AGSpinMagicProperty)
     * @see #deleteSpinMagicProperty(String)
     * @since v4.4
     */
    public String getSpinMagicProperty(String uri) throws RDF4JException {
        return prepareHttpRepoClient().getSpinMagicProperty(uri);
    }

    /**
     * @return List  all defined SPIN magic properties
     * @throws RDF4JException if there is an error with this request
     * @see #getSpinMagicProperty(String)
     * @see #putSpinMagicProperty(AGSpinMagicProperty)
     * @see #deleteSpinMagicProperty(String)
     * @see #listSpinFunctions()
     * @since v4.4
     */
    public List<AGSpinMagicProperty> listSpinMagicProperties() throws RDF4JException {
        try (TupleQueryResult list = prepareHttpRepoClient().listSpinMagicProperties()) {
            List<AGSpinMagicProperty> result = new ArrayList<>();
            while (list.hasNext()) {
                result.add(new AGSpinMagicProperty(list.next()));
            }
            return result;
        }
    }

    /**
     * @param uri SPIN magic property identifier
     * @throws RDF4JException if there is an error with this request
     * @see #putSpinMagicProperty(AGSpinMagicProperty)
     * @see #getSpinMagicProperty(String)
     * @since v4.4
     */
    public void deleteSpinMagicProperty(String uri) throws RDF4JException {
        prepareHttpRepoClient().deleteSpinMagicProperty(uri);
    }

    /**
     * @param fn the SPIN magic property to add
     * @throws RDF4JException if there is an error with this request
     * @see #getSpinMagicProperty(String)
     * @see #deleteSpinMagicProperty(String)
     * @see #putSpinFunction(AGSpinFunction)
     * @since v4.4
     */
    public void putSpinMagicProperty(AGSpinMagicProperty fn) throws RDF4JException {
        prepareHttpRepoClient().putSpinMagicProperty(fn);
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
     * @param comparisonMode method to determine what is a duplicate
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteDuplicates(String comparisonMode) throws RepositoryException {
        prepareHttpRepoClient().deleteDuplicates(comparisonMode);
    }

    /**
     * Returns all duplicates from the store.
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
     * @param comparisonMode method to determine what is a duplicate
     * @return RepositoryResult  the duplicates that exist in the store
     * @throws AGHttpException if there is an error with this request
     */
    public RepositoryResult<Statement> getDuplicateStatements(String comparisonMode)
            throws RepositoryException {
        try {
            StatementCollector collector = new StatementCollector();
            prepareHttpRepoClient().getDuplicateStatements(comparisonMode, collector);

            return createRepositoryResult(collector.getStatements());
        } catch (RDFHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Materializes inferred statements (generates and adds them to the store).
     * <p>
     * The materializer's configuration determines how statements are materialized.
     *
     * @param materializer the materializer to use
     * @return long  the number of statements added
     * @throws AGHttpException if there is an error with this request
     * @see AGMaterializer#newInstance()
     */
    public long materialize(AGMaterializer materializer) throws RepositoryException {
        return prepareHttpRepoClient().materialize(materializer);
    }

    /**
     * Deletes materialized statements from the default graph.
     *
     * @return the number of statements deleted
     * @throws AGHttpException if there is an error with this request
     * @see #materialize(AGMaterializer)
     */
    public long deleteMaterialized() throws RepositoryException {
        return deleteMaterialized((Resource) null);
    }

    /**
     * Deletes materialized statements.
     *
     * @param materializer Materializer parameters used to create the triples.
     * @return the number of statements deleted
     * @throws AGHttpException if there is an error with this request
     * @see #materialize(AGMaterializer)
     */
    public long deleteMaterialized(final AGMaterializer materializer) throws RepositoryException {
        return deleteMaterialized(materializer.getInferredGraph());
    }

    /**
     * Deletes materialized statements from given graph.
     *
     * @param inferredGraph Graph to delete the statements from.
     *                      If null the default graph will be used.
     * @return the number of statements deleted
     * @throws AGHttpException if there is an error with this request
     * @see #materialize(AGMaterializer)
     */
    public long deleteMaterialized(final Resource inferredGraph) throws RepositoryException {
        return prepareHttpRepoClient().deleteMaterialized(inferredGraph);
    }

    /**
     * Sets the AG user for X-Masquerade-As-User requests.
     * <p>
     * For AG superusers only.  This allows AG superusers to run requests as
     * another user in a dedicated session.
     *
     * @param user the user for X-Masquerade-As-User requests
     * @throws RepositoryException if there is an error with this request
     */
    public void setMasqueradeAsUser(String user) throws RepositoryException {
        prepareHttpRepoClient().setMasqueradeAsUser(user);
    }

    /**
     * Begins a transaction requiring {@link #commit()} or {@link #rollback()} to
     * be called to end the transaction.
     *
     * @throws RepositoryException if there is an error with this request
     * @see #isActive()
     * @see #commit()
     * @see #rollback()
     * @since 2.7.0
     */
    public void begin() throws RepositoryException {
        prepareHttpRepoClient().setAutoCommit(false);
    }

    /**
     * Indicates if a transaction is currently active on the connection. A
     * transaction is active if {@link #begin()} has been called, and becomes
     * inactive after {@link #commit()} or {@link #rollback()} has been called.
     *
     * @return <code>true</code> iff a transaction is active, <code>false</code>
     * iff no transaction is active.
     * @throws UnknownTransactionStateException if the transaction state can not be determined. This can happen
     *                                          for instance when communication with a repository fails or times
     *                                          out.
     * @throws RepositoryException              if there is an error with this request
     * @since 2.7.0
     */
    public boolean isActive() throws UnknownTransactionStateException,
            RepositoryException {
        return !prepareHttpRepoClient().isAutoCommit();
    }

    /**
     * Removes all statement(s) within the specified contexts.
     *
     * @param contexts the context(s) to remove the data from. Note that this parameter is
     *                 a vararg and as such is optional. If no contexts are supplied the
     *                 method operates on the entire repository
     * @throws RepositoryException if the statement(s) could not be removed from the repository, for
     *                             example because the repository is not writable
     */
    public void clear(Resource... contexts)
            throws RepositoryException {
        remove(null, null, null, contexts);
    }

    /**
     * Delete an existing triple attribute definition.
     *
     * @param name The name of the defined attribute to delete
     * @throws RepositoryException if there is an error with this request
     */
    public void deleteAttributeDefinition(String name) throws RepositoryException {
        prepareHttpRepoClient().deleteAttributeDefinition(name);
    }

    /**
     * Return a list of all attributes defined for the current connection.
     *
     * @return JSONArray of Triple Attribute definitions
     * @throws RepositoryException if there is an error with this request
     * @throws JSONException       if there is an error parsing the response to JSON
     */
    public JSONArray getAttributeDefinitions()
            throws RepositoryException, JSONException {
        return prepareHttpRepoClient().getAttributeDefinition();
    }

    /**
     * Return the definition of the attribute named by NAME.
     *
     * @param name the attribute definition to lookup
     * @return JSONArray of all found definitions
     * @throws RepositoryException if there is an error with this request
     * @throws JSONException       if there is an error parsing the response to JSON
     */
    public JSONArray getAttributeDefinition(String name)
            throws RepositoryException, JSONException {
        return prepareHttpRepoClient().getAttributeDefinition(name);
    }

    /**
     * Fetch the string representation of the static attribute filter defined on this
     * repository
     *
     * @return String, or null if no static filter is defined
     * @throws RepositoryException if there is an error with this request
     */
    public String getStaticAttributeFilter() throws RepositoryException {
        return prepareHttpRepoClient().getStaticAttributeFilter();
    }

    /**
     * Establish a static attribute filter on the current repository.
     *
     * @param filter a String representing a static attribute filter definition
     * @throws RepositoryException if there is an error with this request
     */
    public void setStaticAttributeFilter(String filter) throws RepositoryException {
        prepareHttpRepoClient().setStaticAttributeFilter(filter);
    }

    /**
     * Delete the static attribute filter defined on this repository.
     *
     * @throws RepositoryException if there is an error with this request
     */
    public void deleteStaticAttributeFilter() throws RepositoryException {
        prepareHttpRepoClient().deleteStaticAttributeFilter();
    }

    /**
     * Fetch the status of nD Geospatical Datatype Automation on this repository.
     *
     * @return boolean, true if enabled, false if disabled
     * @throws RepositoryException if there is an error with this request
     */
    public boolean getNDGeospatialDatatypeAutomation() throws RepositoryException {
        return prepareHttpRepoClient().getNDGeospatialDatatypeAutomation();
    }

    /**
     * Enable nD Geospatical Datatype Automation on this repository.
     *
     * @throws RepositoryException if there is an error with this request
     */
    public void enableNDGeospatialDatatypeAutomation() throws RepositoryException {
        prepareHttpRepoClient().enableNDGeospatialDatatypeAutomation();
    }

    /**
     * Disable nD Geospatical Datatype Automation on this repository.
     *
     * @throws RepositoryException if there is an error with this request
     */
    public void disableNDGeospatialDatatypeAutomation() throws RepositoryException {
        prepareHttpRepoClient().disableNDGeospatialDatatypeAutomation();
    }

    public String getUserAttributes() {
        return prepareHttpRepoClient().getUserAttributes();
    }

    public void setUserAttributes(String value) {
        prepareHttpRepoClient().setUserAttributes(value);
    }

    public void setUserAttributes(JSONObject value) {
        setUserAttributes(value.toString());
    }

    /**
     * Configure distributed transaction behavior.
     *
     * @param transactionSettings Distributed transaction settings.
     */
    public void setTransactionSettings(final TransactionSettings transactionSettings) {
        prepareHttpRepoClient().setTransactionSettings(transactionSettings);
    }

    /**
     * A 'context manager' for temporarily changing transaction settings.
     * <p>
     * Use it in a try-with-resources block, like this:
     *
     * <pre>{@code
     * try (Ctx ignored = conn.transactionSettingsCtx(newSettings)) {
     *     // Any explicit or implicit commits here will use new settings
     * }
     * }</pre>
     *
     * @param transactionSettings New distributed transaction settings.
     * @return A closeable object that can be used in try-with-resources statement.
     * When closed the object will restore old transaction settings.
     */
    public Ctx transactionSettingsCtx(final TransactionSettings transactionSettings) {
        final TransactionSettings oldSettings = prepareHttpRepoClient().getTransactionSettings();
        setTransactionSettings(transactionSettings);
        return () -> setTransactionSettings(oldSettings);
    }

    /**
     * Sets the connection pool this object will be returned to on close.
     * <p>
     * Set to {@code null} to make close really shutdown the connection.
     *
     * @param pool Connection pool.
     */
    public void setPool(final AGConnPool pool) {
        this.pool = pool;
    }

    /**
     * Builder class for defining a new attribute definition. After instantiation,
     * use the setter methods to build up the attribute definition. The {@code add}
     * method will submit the definition to AG.
     * <p>
     * The object can be discarded once {@code add} is called.
     */

    public class AttributeDefinition {
        // required
        private String name;

        // optional
        private List<String> allowedValues; // empty list means any string is acceptable
        private boolean ordered = false;
        private long minimum = -1;
        private long maximum = -1;

        /**
         * Constructor
         *
         * @param name of the attribute
         */
        public AttributeDefinition(String name) {
            this.name = name;
        }

        /**
         * Overwrite current setting of allowedValues with the argument List.
         *
         * @param values, a {@code List<String>} of allowed values
         * @return this
         */
        public AttributeDefinition allowedValues(List<String> values) {
            allowedValues = values;
            return this;
        }

        /**
         * Add an allowed value to the current list of allowed values for this attribute definition
         *
         * @param value an allowed value for this attribute
         * @return this
         */
        public AttributeDefinition allowedValue(String value) {
            if (allowedValues == null) {
                allowedValues = new ArrayList<>(5);
                allowedValues.add(value);
            } else {
                allowedValues.add(value);
            }

            return this;
        }

        /**
         * Specifies whether the values allowed by this attribute definition are ordered.
         *
         * @param value boolean, if this attribute is ordered
         * @return this
         */
        public AttributeDefinition ordered(boolean value) {
            ordered = value;
            return this;
        }

        /**
         * The minimum number of times this attribute must be provided for a triple.
         *
         * @param value, the minimum number of times this attribute can be specified
         * @return this
         * @throws Exception if <code>value</code> is invalid
         */
        public AttributeDefinition minimum(long value) throws Exception {
            if (value < 0) {
                throw new Exception("minimum must be a non-negative integer.");
            }
            minimum = value;
            return this;
        }

        /**
         * The maximum number of times this attribute can be provided with a triple.
         *
         * @param value the maximum number of times this attribute can be specified
         * @return this
         * @throws Exception if <code>value</code> is invalid
         */
        public AttributeDefinition maximum(long value) throws Exception {
            if (value < 0) {
                throw new Exception("maximum must be greater than 0.");
            }
            maximum = value;
            return this;
        }

        /**
         * Pass the current attribute definition to AllegroGraph for defining.
         *
         * @return this
         * @throws AGHttpException if there is an error with the request
         */
        public AttributeDefinition add() throws AGHttpException {
            AGRepositoryConnection.this.prepareHttpRepoClient().addAttributeDefinition(name, allowedValues, ordered, minimum, maximum);
            return this;
        }
    }
    /**
     * Asks the server to read store's internal data structures into memory.
     *
     * Use {@link #warmup(WarmupConfig)} to specify which structures should be read.
     */
    public void warmup() {
        getHttpRepoClientInternal().warmup();
    }

    /**
     * Asks the server to read store's internal data structures into memory.
     *
     * @param config Config object that can be used to specify which structures
     *               should be read into memory. If this parameter is {@code null}
     *               then the choice will be left to the server.
     */
    public void warmup(final WarmupConfig config) {
        getHttpRepoClientInternal().warmup(config);
    }
}
