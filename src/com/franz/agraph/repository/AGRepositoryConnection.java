/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.Iteration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.OpenRDFException;
import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryConnectionBase;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.ntriples.NTriplesUtil;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGCustomStoredProcException;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.exception.AGMalformedDataException;
import com.franz.agraph.http.handler.AGRDFHandler;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.util.Closeable;
import com.franz.util.Closer;

/**
 * Implements the <a href="http://www.openrdf.org/">Sesame</a>
 * {@link RepositoryConnection} interface for AllegroGraph.
 * 
 * <p>By default, a connection is {@link #setAutoCommit(boolean) autoCommit}=true.
 * For ACID transactions, a session must be created.</p>
 * 
 * <h3><a name="sessions">Session Overview</a></h3>
 * 
 * <p>Sessions with AllegroGraph server are used for ACID transactions
 * and also for server code in InitFile and Scripts.
 * See more documentation for
 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#sessions"
 *    target="_top">Sessions in the AllegroGraph HTTP Protocol</a> and
 * <a href="http://www.franz.com/agraph/support/documentation/v4/agraph-introduction.html#ACID"
 *    target="_top">ACID transactions</a> in the AllegroGraph Server documentation.</p>
 * 
 * <p>Operations such as
 * {@link #setAutoCommit(boolean) setAutoCommit},
 * {@link #addRules(String) addRules}, and
 * {@link #registerSNAGenerator(String, List, List, List, String) registerSNAGenerator}
 * will spawn a dedicated session in order to perform their duties.
 * Adds and deletes during a session must be {@link #commit() committed}
 * or {@link #rollback() rolled back}.</p>
 * 
 * <p>A session expires when its idle {@link #setSessionLifetime(int) lifetime}
 * is exceeded, freeing up server resources.
 * A session lifetime may be extended by calling {@link #ping() ping}.
 * A session should be {@link #close() closed} when finished.
 * </p>
 * 
 * <p>{@link #setSessionLoadInitFile(boolean) InitFiles}
 * and {@link #addSessionLoadScript(String) Scripts}
 * are loaded into the server only for sessions.  See
 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#scripting"
 *    target="_top">Scripting in HTTP Protocol</a> and
 * <a href="http://www.franz.com/agraph/support/documentation/v4/agwebview.html"
 *    target="_top">search for "InitFile" in WebView</a> for how to create initFiles.
 * </p>
 * 
 * <p>Starting a session causes http requests to use a new port, which
 * may cause an exception if the client can not access it.
 * See <a href="http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport"
 * target="_top">Session Port Setup</a>.
 * </p>
 * 
 * <p>Methods that start a session if not already started:<ul>
 * <li>{@link #setAutoCommit(boolean)}</li>
 * <li>{@link #addRules(String)}</li>
 * <li>{@link #addRules(InputStream)}</li>
 * <li>{@link #registerSNAGenerator(String, List, List, List, String)}</li>
 * </ul>
 * 
 * Methods that affect a session in use:<ul>
 * <li>{@link #commit()}</li>
 * <li>{@link #rollback()}</li>
 * <li>{@link #ping()}</li>
 * <li>{@link #close()}</li>
 * </ul>
 * 
 * Methods to configure a session before it is started:<ul>
 * <li>{@link #setSessionLifetime(int)} and {@link #getSessionLifetime()}</li>
 * <li>{@link #setSessionLoadInitFile(boolean)}</li>
 * <li>{@link #addSessionLoadScript(String)}</li>
 * </ul></p>
 * 
 * <h3><a name="mapping">Data-type and Predicate Mapping</a></h3>
 * 
 * <p>For more details, see the HTTP Protocol docs for
 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#mapping"
 * target="_top">Type Mappings</a>
 * and the Lisp reference for
 * <a href="http://www.franz.com/agraph/support/documentation/v4/lisp-reference.html#ref-type-mapping"
 * target="_top">Data-type and Predicate Mapping</a>.
 * </p>
 * 
 * <p>Methods for type mappings:<ul>
 * <li>{@link #clearMappings()}</li>
 * <li>{@link #getDatatypeMappings()}</li>
 * <li>{@link #registerDatatypeMapping(URI, URI)}</li>
 * <li>{@link #deleteDatatypeMapping(URI)}</li>
 * <li>{@link #getPredicateMappings()}</li>
 * <li>{@link #registerPredicateMapping(URI, URI)}</li>
 * <li>{@link #deletePredicateMapping(URI)}</li>
 * </ul></p>
 * 
 * @since v4.0
 */
public class AGRepositoryConnection
extends RepositoryConnectionBase
implements RepositoryConnection, Closeable {

	private final AGAbstractRepository repository;
	private final AGHttpRepoClient repoclient;
	private boolean streamResults;
	private final AGValueFactory vf;

	/**
	 * @see AGRepository#getConnection()
	 * @see AGVirtualRepository#getConnection()
	 */
	public AGRepositoryConnection(AGRepository repository, AGHttpRepoClient client) {
		super(repository);
		this.repository = repository;
		this.repoclient = client;
		// use system property so this can be tested from build.xml
		setStreamResults("true".equals(System.getProperty("com.franz.agraph.repository.AGRepositoryConnection.streamResults")));
		vf = new AGValueFactory(repository, this);
	}

	public AGRepositoryConnection(AGVirtualRepository repository, AGHttpRepoClient client) {
		super(repository);
		this.repository = repository;
		this.repoclient = client;
		// use system property so this can be tested from build.xml
		setStreamResults("true".equals(System.getProperty("com.franz.agraph.repository.AGRepositoryConnection.streamResults")));
		vf = new AGValueFactory(repository.wrapped, this);
	}
	
	@Override
	public String toString() {
		return "{" + super.toString()
		+ " " + repoclient
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
	 * Returns the lower level HTTP/Storage layer for this connection.
	 * 
	 */
	public AGHttpRepoClient getHttpRepoClient() {
		return repoclient;
	}

	@Override
	public AGValueFactory getValueFactory() {
		return vf;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		Statement st = new StatementImpl(subject, predicate, object);
		JSONArray rows = encodeJSON(st,contexts);
		try {
			getHttpRepoClient().uploadJSON(rows);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		for (Statement st : statements) {
			JSONArray rows_st = encodeJSON(st);
			append(rows, rows_st);
		}
		try {
			getHttpRepoClient().uploadJSON(rows, contexts);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	private void append(JSONArray rows, JSONArray rowsSt) {
		for (int i=0;i<rowsSt.length();i++) {
			try {
				rows.put(rowsSt.get(i));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		while (statementIter.hasNext()) {
			append(rows, encodeJSON(statementIter.next(),contexts));
		}
		try {
			getHttpRepoClient().uploadJSON(rows);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	private JSONArray encodeJSON(Statement st, Resource... contexts) {
		JSONArray rows = new JSONArray();
		if (contexts.length==0) {
			JSONArray row = new JSONArray().put(
					encodeValueForStorageJSON(st.getSubject())).put(
							encodeValueForStorageJSON(st.getPredicate())).put(
									encodeValueForStorageJSON(st.getObject()));
			if (st.getContext() != null) {
				row.put(encodeValueForStorageJSON(st.getContext()));
			}
			rows.put(row);
		} else {
			for (Resource c: contexts) {
				JSONArray row = new JSONArray().put(
						encodeValueForStorageJSON(st.getSubject())).put(
								encodeValueForStorageJSON(st.getPredicate())).put(
										encodeValueForStorageJSON(st.getObject()));
				if (c != null) {
					row.put(encodeValueForStorageJSON(c));
				}
				rows.put(row);
			}
		}
		return rows;
	}

	private String encodeValueForStorageJSON(Value v) {
		return NTriplesUtil.toNTriplesString(repoclient.getStorableValue(v,vf));
	}
	
	@Override
	protected void addInputStreamOrReader(Object inputStreamOrReader,
			String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {

		if (inputStreamOrReader instanceof InputStream) {
			try {
				getHttpRepoClient().upload(((InputStream) inputStreamOrReader),
					baseURI, dataFormat, false, contexts);
			} catch (AGMalformedDataException e) {
				throw new RDFParseException(e);
			}
		} else if (inputStreamOrReader instanceof Reader) {
			try {
				getHttpRepoClient().upload(((Reader) inputStreamOrReader), baseURI,
					dataFormat, false, contexts);
			} catch (AGMalformedDataException e) {
				throw new RDFParseException(e);
			}
		} else {
			throw new IllegalArgumentException(
					"inputStreamOrReader must be an InputStream or a Reader, is a: "
							+ inputStreamOrReader.getClass());
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		getHttpRepoClient().deleteStatements(subject, predicate, object,
				contexts);
	}

	@Override
	public void remove(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		for (Statement st : statements) {
			append(rows, encodeJSON(st,contexts));
		}
		getHttpRepoClient().deleteJSON(rows);
	}
    
	@Override
	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statements, Resource... contexts)
	throws RepositoryException, E {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		while (statements.hasNext()) {
			append(rows, encodeJSON(statements.next(),contexts));
		}
		getHttpRepoClient().deleteJSON(rows);
	}

	/**
	 * Setting autoCommit to false creates a dedicated server session
	 * which supports ACID transactions.
	 * Setting to true will create a dedicated server session.
	 * 
	 * See <a href="#sessions">session overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-session"
	 * target="_top">POST session</a> for more details.
	 * 
	 * <p>Starting a session causes http requests to use a new port, which
	 * may cause an exception if the client can not access it.
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport"
	 * target="_top">Session Port Setup</a>.
	 * </p>
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		getHttpRepoClient().setAutoCommit(autoCommit);
	}

	/**
	 * @see #setAutoCommit(boolean)
	 */
	@Override
	public boolean isAutoCommit() throws RepositoryException {
		return getHttpRepoClient().isAutoCommit();
	}

	/**
	 * Commit the current transaction.
	 * See <a href="#sessions">session overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-commit"
	 * target="_top">POST commit</a> for more details.
	 */
	public void commit() throws RepositoryException {
		getHttpRepoClient().commit();
	}

	/**
	 * Roll back the current transaction (discard all changes made since last commit).
	 * See <a href="#sessions">session overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-rollback"
	 * target="_top">POST rollback</a> for more details.
	 */
	public void rollback() throws RepositoryException {
		getHttpRepoClient().rollback();
	}

	/**
	 * Set to true to automatically use {@link AGStreamTupleQuery}
	 * for {@link #prepareTupleQuery(QueryLanguage, String, String)}.
	 * @see #isStreamResults()
	 */
	public void setStreamResults(boolean streamResults) {
		this.streamResults = streamResults;
	}

	/**
	 * If true, automatically use {@link AGStreamTupleQuery}.
	 * Default is false.
	 * @see #setStreamResults(boolean)
	 */
	public boolean isStreamResults() {
		return streamResults;
	}
	
	/**
	 * This method should do nothing, as the AllegroGraph server manages
	 * autoCommit.
	 */
	@Override
	protected void autoCommit() throws RepositoryException {
		/*
		 * if (isAutoCommit()) { commit(); }
		 */
	}

	public void clearNamespaces() throws RepositoryException {
		getHttpRepoClient().clearNamespaces();
	}

	/**
	 * Closes the session if there is one started.
	 * See <a href="#sessions">session overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-close-session"
	 * target="_top">POST close</a> for more details.
	 */
	@Override
	public void close() throws RepositoryException {
		if (isOpen()) {
			getHttpRepoClient().close();
			super.close();
		}
	}

	public void exportStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, RDFHandler handler, Resource... contexts)
			throws RDFHandlerException, RepositoryException {
		getHttpRepoClient().getStatements(subj, pred, obj, Boolean.toString(includeInferred),
					handler, contexts);
	}

	public void exportStatements(RDFHandler handler, String... ids)
	throws RDFHandlerException, RepositoryException {
		getHttpRepoClient().getStatements(handler, ids);
	}
	
	public RepositoryResult<Resource> getContextIDs()
			throws RepositoryException {
		try {
			List<Resource> contextList = new ArrayList<Resource>();

			TupleQueryResult contextIDs = getHttpRepoClient().getContextIDs();
			try {
				while (contextIDs.hasNext()) {
					BindingSet bindingSet = contextIDs.next();
					Value context = bindingSet.getValue("contextID");

					if (context instanceof Resource) {
						contextList.add((Resource) context);
					}
				}
			} finally {
				contextIDs.close();
			}

			return createRepositoryResult(contextList);
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Creates a RepositoryResult for the supplied element set.
	 */
	public <E> RepositoryResult<E> createRepositoryResult(
			Iterable<? extends E> elements) {
		return new RepositoryResult<E>(
				new CloseableIteratorIteration<E, RepositoryException>(elements
						.iterator()));
	}

	public String getNamespace(String prefix) throws RepositoryException {
		return getHttpRepoClient().getNamespace(prefix);
	}

	public RepositoryResult<Namespace> getNamespaces()
			throws RepositoryException {
		try {
			List<Namespace> namespaceList = new ArrayList<Namespace>();

			TupleQueryResult namespaces = getHttpRepoClient().getNamespaces();
			try {
				while (namespaces.hasNext()) {
					BindingSet bindingSet = namespaces.next();
					Value prefix = bindingSet.getValue("prefix");
					Value namespace = bindingSet.getValue("namespace");

					if (prefix instanceof Literal
							&& namespace instanceof Literal) {
						String prefixStr = ((Literal) prefix).getLabel();
						String namespaceStr = ((Literal) namespace).getLabel();
						namespaceList.add(new NamespaceImpl(prefixStr,
								namespaceStr));
					}
				}
			} finally {
				namespaces.close();
			}

			return createRepositoryResult(namespaceList);
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
	}

	public RepositoryResult<Statement> getStatements(Resource subj, URI pred,
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

	/**
	 * Returns statements having the specified ids.
	 *  
	 * This api is subject to change.  There is currently no 
	 * natural way to obtain a statement's triple id from the 
	 * java client; when that is possible, this api may change.
	 * 
	 * @param ids Strings representing statement ids.
	 * @return The statements having the specified ids. The result object
	 *         is a {@link RepositoryResult} object, a lazy Iterator-like object
	 *         containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs
	 *         during retrieval.
	 * @throws RepositoryException
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
	 * Prepares a {@link AGQuery} for evaluation on this repository. Note
	 * that the preferred way of preparing queries is to use the more specific
	 * {@link #prepareTupleQuery(QueryLanguage, String, String)},
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)}, or
	 * {@link #prepareGraphQuery(QueryLanguage, String, String)} methods instead.
	 * 
	 * @throws UnsupportedOperationException
	 *         if the method is not supported for the supplied query language.
	 * @throws IllegalArgumentException
	 *         if the query type (Tuple, Graph, Boolean) cannot be determined.
	 */
	public AGQuery prepareQuery(QueryLanguage ql, String queryString, String baseURI) {
		if (QueryLanguage.SPARQL.equals(ql)) {
			String strippedQuery = stripSparqlQueryString(queryString).toUpperCase();
			if (strippedQuery.startsWith("SELECT")) {
				return prepareTupleQuery(ql, queryString, baseURI);
			}
			else if (strippedQuery.startsWith("ASK")) {
				return prepareBooleanQuery(ql, queryString, baseURI);
			}
			else if (strippedQuery.startsWith("CONSTRUCT") || strippedQuery.startsWith("DESCRIBE")) {
				return prepareGraphQuery(ql, queryString, baseURI);
			} else {
				throw new IllegalArgumentException("Unable to determine a query type (Tuple, Graph, Boolean) for the query:\n" + queryString);
			}
		}
		else if (AGQueryLanguage.PROLOG.equals(ql)) {
			return prepareTupleQuery(ql, queryString, baseURI);
		}
		else {
			throw new UnsupportedOperationException("Operation not supported for query language " + ql);
		}
	}

	/**
	 * Removes any SPARQL prefix and base declarations and comments from 
	 * the supplied SPARQL query string.
	 * 
	 * @param queryString
	 *        a SPARQL query string
	 * @return a substring of queryString, with prefix and base declarations
	 *         removed.
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
	
	public void removeNamespace(String prefix) throws RepositoryException {
		getHttpRepoClient().removeNamespacePrefix(prefix);
	}

	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		getHttpRepoClient().setNamespacePrefix(prefix, name);
	}

	public long size(Resource... contexts) throws RepositoryException {
		return getHttpRepoClient().size(contexts);
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
	 * @see AGFreetextIndexConfig#newInstance()
	 * @param indexName the index name to create
	 * @param config the index configuration
	 */
	public void createFreetextIndex(String indexName, AGFreetextIndexConfig config)
	throws RepositoryException {
		List<String> predicates = new ArrayList<String>();
		for (URI uri: config.getPredicates()) {
			predicates.add(NTriplesUtil.toNTriplesString(uri));
		}
		getHttpRepoClient().createFreetextIndex(indexName, predicates, config.getIndexLiterals(), config.getIndexLiteralTypes(), config.getIndexResources(), config.getIndexFields(), config.getMinimumWordSize(), config.getStopWords(), config.getWordFilters(), config.getInnerChars(), config.getBorderChars(), config.getTokenizer());
	}
	
	/**
	 * Deletes the freetext index of the specified name.
	 * 
	 * @see #createFreetextIndex(String, AGFreetextIndexConfig)
	 */
	public void deleteFreetextIndex(String indexName) throws RepositoryException {
		getHttpRepoClient().deleteFreetextIndex(indexName);
	}
	
	/**
	 * Registers a predicate for free text indexing. Once registered, the
	 * objects of data added to the repository having this predicate will be
	 * text indexed and searchable.
	 * 
	 * @deprecated
	 * @see #createFreetextIndex(String, AGFreetextIndexConfig)
	 */
	public void createFreetextIndex(String name, URI[] predicates)
			throws RepositoryException {
		AGFreetextIndexConfig config = AGFreetextIndexConfig.newInstance();
		config.getPredicates().addAll(Arrays.asList(predicates));
		createFreetextIndex(name,config);
	}

	/**
	 * Gets the predicates that have been registered for text indexing.
	 * 
	 * @deprecated
	 * @see #getFreetextIndexConfig(String)
	 */
	public String[] getFreetextPredicates(String index) throws RepositoryException {
		return getHttpRepoClient().getFreetextPredicates(index);
	}

	/**
	 * Gets the configuration of the specified free text index.
	 */
	public AGFreetextIndexConfig getFreetextIndexConfig(String indexName) throws RepositoryException {
		return new AGFreetextIndexConfig(getHttpRepoClient().getFreetextIndexConfiguration(indexName));
	}
	
	/**
	 * Gets freetext indexes that have been created
	 * @deprecated
	 * @see #listFreetextIndices()
	 */
	public String[] getFreetextIndices() throws RepositoryException {
		return getHttpRepoClient().getFreetextIndices();
	}

	/**
	 * Lists the freetext indices that have been defined for this repository.
	 * 
	 * @return a list of freetext index names
	 */
	public List<String> listFreetextIndices() throws RepositoryException {
		return getHttpRepoClient().listFreetextIndices();
	}
	
	/**
	 * Registers a predicate mapping from the predicate to a primitive datatype.
	 * This can be useful in speeding up query performance and enabling range
	 * queries over datatypes.
	 * 
	 * <p>Once registered, the objects of any data added via this connection that
	 * have this predicate will be mapped to the primitive datatype.</p>
	 * 
	 * <p>For example, registering that predicate {@code <http://example.org/age>}
	 * is mapped to {@link XMLSchema#INT} and adding the triple:
	 * {@code <http://example.org/Fred> <http://example.org/age> "24"}
	 * will result in the object being treated as {@code "24"^^xsd:int}.</p>
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-put-predmapping"
	 * target="_top">POST predicate mapping</a>.</p>
	 * 
	 * @param predicate the predicate URI
	 * @param primtype datatype URI
	 * @see #getPredicateMappings()
	 */
	public void registerPredicateMapping(URI predicate, URI primtype)
			throws RepositoryException {
		getHttpRepoClient().registerPredicateMapping(predicate, primtype);
	}

	/**
	 * Deletes any predicate mapping associated with the given predicate.
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#delete-predmapping"
	 * target="_top">DELETE predicate mapping</a>.</p>
	 * 
	 * @param predicate the predicate
	 * @see #getPredicateMappings()
	 */
	public void deletePredicateMapping(URI predicate)
			throws RepositoryException {
		getHttpRepoClient().deletePredicateMapping(predicate);
	}

	// TODO: return RepositoryResult<Mapping>?
	/**
	 * Gets the predicate mappings defined for this connection.
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#get-predmapping"
	 * target="_top">GET predicate mapping</a>
	 * and the Lisp reference for the
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/lisp-reference.html#function.predicate-mapping"
	 * target="_top">predicate-mapping function</a>.
	 * </p>
	 * 
	 * @see #registerPredicateMapping(URI, URI)
	 * @see #deletePredicateMapping(URI)
	 * @see #getDatatypeMappings()
	 */
	public String[] getPredicateMappings() throws RepositoryException {
		return getHttpRepoClient().getPredicateMappings();
	}

	// TODO: are all primtypes available as URI constants?
	/**
	 * Registers a datatype mapping from the datatype to a primitive datatype.
	 * This can be useful in speeding up query performance and enabling range
	 * queries over user datatypes.
	 * 
	 * <p>Once registered, the objects of any data added via this connection that
	 * have this datatype will be mapped to the primitive datatype.</p>
	 * 
	 * <p>For example, registering that datatype {@code <http://example.org/usertype>}
	 * is mapped to {@link XMLSchema#INT} and adding the triple:
	 * {@code <http://example.org/Fred> <http://example.org/age> "24"^^<http://example.org/usertype>}
	 * will result in the object being treated as {@code "24"^^xsd:int}.</p>
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-put-typemapping"
	 * target="_top">POST type mapping</a>.</p>
	 * 
	 * @param datatype the user datatype
	 * @param primtype the primitive type
	 * @see #getDatatypeMappings()
	 */
	public void registerDatatypeMapping(URI datatype, URI primtype)
			throws RepositoryException {
		getHttpRepoClient().registerDatatypeMapping(datatype, primtype);
	}

	/**
	 * Deletes any datatype mapping associated with the given datatype.
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#delete-typemapping"
	 * target="_top">DELETE type mapping</a>.</p>
	 * 
	 * @param datatype the user datatype
	 * @see #getDatatypeMappings()
	 * @see #clearMappings()
	 */
	public void deleteDatatypeMapping(URI datatype) throws RepositoryException {
		getHttpRepoClient().deleteDatatypeMapping(datatype);
	}

	// TODO: return RepositoryResult<Mapping>?
	/**
	 * Gets the datatype mappings defined for this connection.
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#get-typemapping"
	 * target="_top">GET type mapping</a>
	 * and the Lisp reference for the
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/lisp-reference.html#function.datatype-mapping"
	 * target="_top">datatype-mapping function</a>.
	 * 
	 * @see #deleteDatatypeMapping(URI)
	 * @see #clearMappings()
	 * @see #registerDatatypeMapping(URI, URI)
	 * @see #getPredicateMappings()
	 */
	public String[] getDatatypeMappings() throws RepositoryException {
		return getHttpRepoClient().getDatatypeMappings();
	}

	/**
	 * Deletes all user-defined predicate and datatype mappings for this connection, 
	 * and reestablishes the automatic mappings for primitive datatypes.  
	 * 
	 * This is equivalent to clearMappings(false).
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#delete-all-mapping"
	 * target="_top">DELETE all mapping</a> for more details.</p>
	 * 
	 * @see #getDatatypeMappings()
	 * @see #getPredicateMappings()
	 */
	public void clearMappings() throws RepositoryException {
		getHttpRepoClient().clearMappings();
	}

	/**
	 * Deletes all predicate and user-defined datatype mappings for this connection.
	 * <p>  
	 * When includeAutoEncodedPrimitiveTypes is true, also deletes the automatic 
	 * mappings for primitive datatypes; this is rarely what you want to do, as it
	 * will cause range queries to perform much less efficiently than when encodings 
	 * are used; this option can be useful for ensuring literal forms are preserved 
	 * in the store (there can be precision loss when encoding some literals).
	 * 
	 * <p>See <a href="#mapping">mapping overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#delete-all-mapping"
	 * target="_top">DELETE all mapping</a> for more details.</p>
	 * 
	 * @see #getDatatypeMappings()
	 * @see #getPredicateMappings()
	 */
	public void clearMappings(boolean includeAutoEncodedPrimitiveTypes) throws RepositoryException {
		getHttpRepoClient().clearMappings(includeAutoEncodedPrimitiveTypes);
	}
	
	/**
	 * Adds Prolog rules to be used on this connection.
	 * 
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/agraph-introduction.html#prolog"
	 * target="_top">Prolog Lisp documentation</a>
	 * and <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-functor"
	 * target="_top">Prolog functor registration</a>.
	 * 
	 * <p>Starts a session if one is not already started.
	 * See <a href="#sessions">session overview</a> for more details.
	 * Starting a session causes http requests to use a new port, which
	 * may cause an exception if the client can not access it.
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport"
	 * target="_top">Session Port Setup</a>.
	 * </p>
	 * 
	 * @param rules a string of rule text
	 * @see #addRules(InputStream)
	 */
	public void addRules(String rules) throws RepositoryException {
		getHttpRepoClient().addRules(rules);
	}

	// TODO: specify RuleLanguage
	/**
	 * Adds Prolog rules to be used on this connection.
	 * 
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/agraph-introduction.html#prolog"
	 * target="_top">Prolog Lisp documentation</a>
	 * and <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-functor"
	 * target="_top">Prolog functor registration</a>.
	 * 
	 * <p>Starts a session if one is not already started.
	 * See <a href="#sessions">session overview</a> for more details.
	 * Starting a session causes http requests to use a new port, which
	 * may cause an exception if the client can not access it.
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport"
	 * target="_top">Session Port Setup</a>.
	 * </p>
	 * 
	 * @param rulestream a stream of rule text
	 * @see #addRules(String)
	 */
	public void addRules(InputStream rulestream) throws RepositoryException {
		getHttpRepoClient().addRules(rulestream);
	}

	/**
	 * Evaluates a Lisp form on the server, and returns the result as a String.
	 * 
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-eval"
	 * target="_top">HTTP POST eval</a>.
	 * 
	 * @param lispForm the Lisp form to evaluate
	 * @return the result in a String
	 * @see #evalInServer(String)
	 */
	public String evalInServer(String lispForm) throws RepositoryException {
		return getHttpRepoClient().evalInServer(lispForm);
	}

	/**
	 * Evaluates a Lisp form on the server, and returns the result as a String.
	 * 
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-eval"
	 * target="_top">HTTP POST eval</a>.
	 * 
	 * @param stream the Lisp form to evaluate
	 * @return the result in a String
	 * @see #evalInServer(String)
	 */
	public String evalInServer(InputStream stream) throws RepositoryException {
		return getHttpRepoClient().evalInServer(stream);
	}

	/**
	 * Instructs the server to fetch and load data from the specified URI.
	 * 
	 * @param source
	 *            the URI to fetch and load.
	 * @param baseURI
	 *            the base URI for the source document.
	 * @param dataFormat
	 *            the RDF data format for the source document.
	 * @param contexts
	 *            zero or more contexts into which data will be loaded.
	 * @throws RepositoryException
	 */
	public void load(URI source, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws RepositoryException {
		getHttpRepoClient().load(source, baseURI, dataFormat, contexts);
	}

	/**
	 * Instructs the server to load data from the specified server-side path.
	 * 
	 * @param absoluteServerPath
	 *            the path to the server-side source file.
	 * @param baseURI
	 *            the base URI for the source document.
	 * @param dataFormat
	 *            the RDF data format for the source document.
	 * @param contexts
	 *            zero or more contexts into which data will be loaded.
	 * @throws RepositoryException
	 */
	public void load(String absoluteServerPath, String baseURI,
			RDFFormat dataFormat, Resource... contexts)
			throws RepositoryException {
		getHttpRepoClient().load(absoluteServerPath, baseURI, dataFormat,
					contexts);
	}

	/**
	 * Instructs the server to extend the life of this connection's dedicated
	 * session, if it is using one.  Such connections that are idle for its
	 * session lifetime will be closed by the server.
	 * 
	 * <p>See <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#sessions">session overview</a> 
	 * and <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#get-ping"
	 * target="_top">GET ping</a> for more details.</p>
	 * 
	 * @throws RepositoryException
	 * @see #setSessionLifetime(int)
	 */
	public void ping() throws RepositoryException {
		getHttpRepoClient().ping();
	}

	// TODO: return RepositoryResult<URI>?
	/**
	 * Gets the Geospatial types that have been registered.
	 */
	public String[] getGeoTypes() throws RepositoryException {
		return getHttpRepoClient().getGeoTypes();
	}

	/**
	 * Registers a cartesian type.
	 */
	public URI registerCartesianType(float stripWidth, float xmin, float xmax,
			float ymin, float ymax) throws RepositoryException {
		String nTriplesURI = getHttpRepoClient().registerCartesianType(stripWidth,
				xmin, xmax, ymin, ymax);
		return NTriplesUtil.parseURI(nTriplesURI, getValueFactory());
	}

	/**
	 * Registers a spherical type.
	 */
	public URI registerSphericalType(float stripWidth, String unit,
			float latmin, float lonmin, float latmax, float lonmax)
			throws RepositoryException {
		String nTriplesURI = getHttpRepoClient().registerSphericalType(stripWidth,
				unit, latmin, lonmin, latmax, lonmax);
		return NTriplesUtil.parseURI(nTriplesURI, getValueFactory());
	}

	public URI registerSphericalType(float stripWidth, String unit) throws RepositoryException {
		return registerSphericalType(stripWidth,unit,-90,-180,90,180);
	}
	
	/**
	 * Registers a polygon.
	 */
	public void registerPolygon(URI polygon, List<Literal> points)
	throws RepositoryException {
		List<String> nTriplesPoints = new ArrayList<String>(points.size());
		for (Literal point: points) {
			nTriplesPoints.add(NTriplesUtil.toNTriplesString(point));
		}
		getHttpRepoClient().registerPolygon(NTriplesUtil.toNTriplesString(polygon), nTriplesPoints);
	}
	
	public RepositoryResult<Statement> getStatementsInBox(URI type,
			URI predicate, float xmin, float xmax, float ymin,
			float ymax, int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGRDFHandler(getHttpRepoClient().getPreferredRDFFormat(),
				collector, getValueFactory(),getHttpRepoClient().getAllowExternalBlankNodeIds());
		getHttpRepoClient().getGeoBox(NTriplesUtil.toNTriplesString(type),
		                              NTriplesUtil.toNTriplesString(predicate),
		                              xmin, xmax, ymin, ymax, limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}

	public RepositoryResult<Statement> getStatementsInCircle(URI type,
			URI predicate, float x, float y, float radius,
			int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGRDFHandler(getHttpRepoClient().getPreferredRDFFormat(),
				collector, getValueFactory(),getHttpRepoClient().getAllowExternalBlankNodeIds());
		getHttpRepoClient().getGeoCircle(NTriplesUtil.toNTriplesString(type),
		                                 NTriplesUtil.toNTriplesString(predicate),
		                                 x, y, radius, limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}
	
	public RepositoryResult<Statement> getGeoHaversine(URI type,
			URI predicate, float lat, float lon, float radius,
			String unit, int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGRDFHandler(getHttpRepoClient().getPreferredRDFFormat(),
				collector, getValueFactory(),getHttpRepoClient().getAllowExternalBlankNodeIds());
		getHttpRepoClient().getGeoHaversine(NTriplesUtil.toNTriplesString(type),
		                                    NTriplesUtil.toNTriplesString(predicate),
		                                    lat, lon, radius, unit, limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}
	
	public RepositoryResult<Statement> getStatementsInPolygon(URI type,
			URI predicate, URI polygon, int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGRDFHandler(getHttpRepoClient().getPreferredRDFFormat(),
				collector, getValueFactory(),getHttpRepoClient().getAllowExternalBlankNodeIds());
		getHttpRepoClient().getGeoPolygon(NTriplesUtil.toNTriplesString(type), NTriplesUtil.toNTriplesString(predicate), NTriplesUtil.toNTriplesString(polygon), limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}
	
	/**
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/lisp-reference.html#sna"
	 * target="_top">Social network analysis Lisp documentation</a>
	 * and <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#sna"
	 * target="_top">SNA generator registration</a>.
	 * 
	 * <p>Starts a session if one is not already started.
	 * See <a href="#sessions">session overview</a> for more details.
	 * Starting a session causes http requests to use a new port, which
	 * may cause an exception if the client can not access it.
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport"
	 * target="_top">Session Port Setup</a>.
	 * </p>
	 * 
	 * @param generator
	 * @param objectOfs
	 * @param subjectOfs
	 * @param undirecteds
	 * @param query
	 */
	public void registerSNAGenerator(String generator, List<URI> objectOfs, List<URI> subjectOfs, List<URI> undirecteds, String query) throws RepositoryException {
		List<String> objOfs = new ArrayList<String>();
		if (objectOfs!=null) {
			for (URI objectOf: objectOfs) {
				objOfs.add(NTriplesUtil.toNTriplesString(objectOf));
			}
		}
		List<String> subjOfs = new ArrayList<String>();
		if (subjectOfs!=null) {
			for (URI subjectOf: subjectOfs) {
				subjOfs.add(NTriplesUtil.toNTriplesString(subjectOf));
			}
		}
		List<String> undirs = new ArrayList<String>();
		if (undirecteds!=null) {
			for (URI undirected: undirecteds) {
				undirs.add(NTriplesUtil.toNTriplesString(undirected));
			}
		}
		getHttpRepoClient().registerSNAGenerator(generator, objOfs, subjOfs, undirs, query);
	}
	
	public void registerSNANeighborMatrix(String matrix, String generator, List<URI> group, int depth) throws RepositoryException {
		if (group==null || group.size()==0) {
			throw new IllegalArgumentException("group must be non-empty.");
		}
		List<String> grp = new ArrayList<String>(3);
		for (URI node: group) {
			grp.add(NTriplesUtil.toNTriplesString(node));
		}
		getHttpRepoClient().registerSNANeighborMatrix(matrix, generator, grp, depth);
	}
	
	/**
	 * Returns a list of actively managed indices for this repository.
	 * 
	 * @return a list of actively managed indices for this repository.
	 * @throws OpenRDFException
	 */
    public List<String> listIndices() throws OpenRDFException {
    	return getHttpRepoClient().listIndices(false);
    }
    
    /**
     * Returns a list of all possible index types for this repository.
     * 
     * @return a list of valid index types
     * @throws OpenRDFException
     */
    public List<String> listValidIndices() throws OpenRDFException {
    	return getHttpRepoClient().listIndices(true);
    }

    /**
     * Adds the given index to the list of actively managed indices.
     * This will take affect on the next commit.
     * 
     * @param type a valid index type
     * @throws RepositoryException
   	 * @see #listValidIndices()
   	 */
    public void addIndex(String type) throws RepositoryException {
    	getHttpRepoClient().addIndex(type);
    }

    /**
     * Drops the given index from the list of actively managed indices.
     * This will take affect on the next commit.
     * 
     * @param type an actively managed index type.
     * @throws RepositoryException
     * @see #listValidIndices()
     */
    public void dropIndex(String type) throws RepositoryException {
    	getHttpRepoClient().dropIndex(type);
    }
    
	/** 
	 * Executes an application/x-rdftransaction.
	 * 
	 * This method is useful for bundling add/remove operations into a 
	 * single request and minimizing round trips to the server.
	 * 
	 * Changes are committed iff the connection is in autoCommit mode.
	 * For increased throughput when sending multiple rdftransaction 
	 * requests, consider using autoCommit=false and committing less 
	 * frequently . 
	 * 
	 * @param rdftransaction a stream in application/x-rdftransaction format
	 * @throws RepositoryException
	 * @throws RDFParseException
	 * @throws IOException
	 */
	public void sendRDFTransaction(InputStream rdftransaction) throws RepositoryException,
			RDFParseException, IOException {
		try {
			getHttpRepoClient().sendRDFTransaction(rdftransaction);
		} catch (AGMalformedDataException e) {
			throw new RDFParseException(e);
		}
	}

	/**
	 * Registers an encodable namespace having the specified format.
	 * 
	 * <p>Registering an encodable namespace enables a more efficient 
	 * encoding of URIs in a namespace, and generation of unique 
	 * URIs for that namespace, because its URIs are declared to 
	 * conform to a specified format; the namespace is thereby 
	 * bounded in size, and encodable.</p>
	 * 
	 * <p>The namespace is any valid URIref, e.g.: 
	 * <code>http://franz.com/ns0</code>
	 * </p>
	 * 
	 * <p>The format is a string using a simplified regular expression
	 * syntax supporting character ranges and counts specifying the
	 * suffix portion of the URIs in the namespace, e.g:
	 * <code>[a-z][0-9]-[a-f]{3}</code>
	 * </p>
	 * 
	 * <p>Generation of unique URIs {@link AGValueFactory#generateURI(String)}
	 * for the above namespace and format might yield an ID such as:
	 *  
	 * <code>http://franz.com/ns0@@a0-aaa</code>
	 * </p>
	 * 
	 * <p>Note: "@@" is used to concatenate the namespace and id suffix
	 * to facilitate efficient recognition/encoding during parsing.</p>
	 *    
	 * <p>The format can be ambiguous (e.g., "[A-Z]{1,2}[B-C}{0,1}").
	 * We will not check for ambiguity in this first version but can
	 * add this checking at a later time.</p>
	 * 
	 * <p>If the format corresponds to a namespace that is not encodable
	 * (it may be malformed, or perhaps it's too large to encode), an 
	 * exception is thrown.</p>
	 * 
	 * <p>For more details, see
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/encoded-ids.html"
	 * target="_top">Encoded IDs</a>.</p>
	 *  
	 * @param namespace a valid namespace, a URI ref
	 * @param format a valid format for an encodable namespace
	 * @see #registerEncodableNamespaces(Iterable)
	 * @see #listEncodableNamespaces()
	 * @see #unregisterEncodableNamespace(String)
	 * @see AGValueFactory#generateURI(String)
	 * @see AGValueFactory#generateURIs(String, int)
	 */
	public void registerEncodableNamespace(String namespace, String format) throws RepositoryException {
		getHttpRepoClient().registerEncodableNamespace(namespace, format);
	}
	
	/**
	 * Registers multiple formatted namespaces in a single request.
	 * 
	 * @see #registerEncodableNamespace(String, String)
	 */
	public void registerEncodableNamespaces(Iterable <? extends AGFormattedNamespace> formattedNamespaces) throws RepositoryException {
		JSONArray rows = new JSONArray();
		for (AGFormattedNamespace ns: formattedNamespaces) {
			JSONObject row  = new JSONObject();
			try {
				row.put("prefix",ns.getNamespace());
				row.put("format", ns.getFormat());
			} catch (JSONException e) {
				throw new RepositoryException(e);
			}
			rows.put(row);
		}
		getHttpRepoClient().registerEncodableNamespaces(rows);
	}
	
	/**
	 * Returns a list of the registered encodable namespaces.
	 *  
	 * @return a list of the registered encodable namespaces
	 * @see #registerEncodableNamespace(String, String)
	 */
	public List<AGFormattedNamespace> listEncodableNamespaces()
			throws OpenRDFException {
		TupleQueryResult tqresult = getHttpRepoClient()
				.getEncodableNamespaces();
		List<AGFormattedNamespace> result = new ArrayList<AGFormattedNamespace>();
		try {
			while (tqresult.hasNext()) {
				BindingSet bindingSet = tqresult.next();
				Value prefix = bindingSet.getValue("prefix");
				Value format = bindingSet.getValue("format");
				result.add(new AGFormattedNamespace(prefix.stringValue(),
						format.stringValue()));
			}
		} finally {
			tqresult.close();
		}
		return result;
	}
	
	/**
	 * Unregisters the specified encodable namespace.
	 * 
	 * @param namespace the namespace to unregister
	 * @see #registerEncodableNamespace(String, String)
	 */
	public void unregisterEncodableNamespace(String namespace) throws RepositoryException {
		getHttpRepoClient().unregisterEncodableNamespace(namespace);
	}
	
	/**
	 * Invoke a stored procedure on the AllegroGraph server.
	 * 
	 * <p>The input arguments and the return value can be:
	 * {@link String}, {@link Integer}, null, byte[],
	 * or Object[] or {@link List} of these (can be nested).</p>
	 * 
	 * <p>See also
	 * {@link #getHttpRepoClient()}.{@link AGHttpRepoClient#callStoredProc(String, String, Object...)
	 * callStoredProc}<code>(functionName, moduleName, args)</code>
	 * </p>
	 * 
	 * @param functionName stored proc lisp function, for example "addTwo"
	 * @param moduleName lisp FASL file name, for example "example.fasl"
	 * @param args arguments to the stored proc
	 * @return return value of stored proc
	 * @throws AGCustomStoredProcException for errors from stored proc
	 * 
	 * @since v4.2
	 * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
	 */
	public Object callStoredProc(String functionName, String moduleName, Object...args)
	throws RepositoryException {
		return getHttpRepoClient().callStoredProc(functionName, moduleName, args);
	}

	/**
	 * Sets the 'lifetime' for a dedicated session spawned by this connection.
	 * Seconds a session can be idle before being collected.
	 * This method does not create a session.
	 * 
	 * <p>See <a href="#sessions">session overview</a> and
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-session"
	 * target="_top">POST session</a> for more details.</p>
	 * 
	 * @param lifetimeInSeconds the session lifetime, in seconds.
	 * @see #getSessionLifetime()
	 * @see #ping()
	 */
	public void setSessionLifetime(int lifetimeInSeconds) {
		getHttpRepoClient().setSessionLifetime(lifetimeInSeconds);
	}
	
	/**
	 * Returns the lifetime for a dedicated session spawned by this connection.
	 * 
	 * <p>See also: <a href="#sessions">Session overview</a>.</p>
	 * 
	 * @see #setSessionLifetime(int)
	 * @see #ping()
	 * @return the session lifetime, in seconds.
	 */
	public int getSessionLifetime() {
		return getHttpRepoClient().getSessionLifetime();
	}
	
	/**
	 * Sets the 'loadInitFile' for a dedicated session spawned by this connection.
	 * This method does not create a session.
	 * 
	 * <p>loadInitFile is a boolean, defaulting to false, which determines
	 * whether the initfile is loaded into this session.</p>
	 * 
	 * <p>See also: <a href="#sessions">Session overview</a>.</p>
	 * 
	 * @see #addSessionLoadScript(String)
	 */
	public void setSessionLoadInitFile(boolean loadInitFile) {
		getHttpRepoClient().setSessionLoadInitFile(loadInitFile);
	}
	
	/**
	 * Adds a 'script' for a dedicated session spawned by this connection.
	 * This method does not create a session.
	 * May be called multiple times for different scripts.
	 * The script text must already be uploaded to the user.
	 * 
	 * <p>Scripts are server code that may be loaded during a session.</p>
	 * 
	 * <p>See also: <a href="#sessions">Session overview</a>.</p>
	 * 
	 * @see #setSessionLoadInitFile(boolean)
	 */
	public void addSessionLoadScript(String scriptName) {
		getHttpRepoClient().addSessionLoadScript(scriptName);
	}
	
	/**
	 * Enables the spogi cache in this repository.
	 * 
	 * Takes a size argument to set the size of the cache.
	 * 
	 * @param size the size of the cache, in triples.
	 * @throws RepositoryException
	 */
	public void enableTripleCache(long size) throws RepositoryException {
		getHttpRepoClient().enableTripleCache(size);
	}
	
	/**
	 * Returns the size of the spogi cache.
	 * 
	 * @return the size of the spogi cache, in triples.
	 * @throws RepositoryException
	 */
	public long getTripleCacheSize() throws RepositoryException {
		return getHttpRepoClient().getTripleCacheSize();
	}
	
	/**
	 * Disables the spogi triple cache.
	 * 
	 * @throws RepositoryException
	 */
 	public void disableTripleCache() throws RepositoryException {
 		getHttpRepoClient().disableTripleCache();
 	}
 	
 	/**
 	 * Sets the commit period to use within large add/load operations.
 	 * 
 	 * @param period commit after this many statements
 	 * @throws RepositoryException
 	 * @see AGHttpRepoClient#setUploadCommitPeriod(int)
 	 */
 	public void setUploadCommitPeriod(int period) throws RepositoryException {
 		getHttpRepoClient().setUploadCommitPeriod(period);
 		
 	}
 	
 	/**
	 * Gets the commit period used within large add/load operations.
	 * 
 	 * @throws RepositoryException
 	 * @see AGHttpRepoClient#getUploadCommitPeriod()
 	 */
 	public int getUploadCommitPeriod() throws RepositoryException {
 		return getHttpRepoClient().getUploadCommitPeriod();
 		
 	}

	/**
	 * tells the server to try and optimize the indices for this
	 * store.
	 *
	 * @param wait is a boolean, false for request to return immediately
	 * @param level determines the work to be done. See the index documentation
	 *        for an explanation of the different levels.
	 * @throws RepositoryException
	 */
	public void optimizeIndices(Boolean wait, int level) throws RepositoryException {
		getHttpRepoClient().optimizeIndices(wait, level);
	}

	public void optimizeIndices(Boolean wait) throws RepositoryException {
		getHttpRepoClient().optimizeIndices(wait);
	}

	/**
	 * 
	 * @param uri spin function identifier
	 * @return spin function query text
	 * @see #putSpinFunction(AGSpinFunction)
	 * @see #deleteSpinFunction(String)
	 * @see #listSpinFunctions()
	 * @see #getSpinMagicProperty(String)
	 * @since v4.4
	 */
	public String getSpinFunction(String uri) throws OpenRDFException {
		return getHttpRepoClient().getSpinFunction(uri);
	}

	/**
	 * @see #getSpinFunction(String)
	 * @see #putSpinFunction(AGSpinFunction)
	 * @see #deleteSpinFunction(String)
	 * @see #listSpinMagicProperties()
	 * @since v4.4
	 */
	public List<AGSpinFunction> listSpinFunctions() throws OpenRDFException {
        TupleQueryResult list = getHttpRepoClient().listSpinFunctions();
		try {
			List<AGSpinFunction> result = new ArrayList<AGSpinFunction>();
			while (list.hasNext()) {
				result.add(new AGSpinFunction(list.next()));
			}
			return result;
		} finally {
			Closer.Close(list);
		}
	}
	
	/**
	 * 
	 * @see #getSpinFunction(String)
	 * @see #deleteSpinFunction(String)
	 * @see #putSpinMagicProperty(AGSpinMagicProperty)
	 * @since v4.4
	 */
	public void putSpinFunction(AGSpinFunction fn) throws OpenRDFException {
		getHttpRepoClient().putSpinFunction(fn);
	}

	/**
	 * 
	 * @param uri spin function identifier
	 * @see #putSpinFunction(AGSpinFunction)
	 * @see #getSpinFunction(String)
	 * @since v4.4
	 */
	public void deleteSpinFunction(String uri) throws OpenRDFException {
		getHttpRepoClient().deleteSpinFunction(uri);
	}

	/**
	 * 
	 * @param uri spin magic property identifier
	 * @return sparqlQuery
	 * @see #putSpinMagicProperty(AGSpinMagicProperty)
	 * @see #deleteSpinMagicProperty(String)
	 * @since v4.4
	 */
	public String getSpinMagicProperty(String uri) throws OpenRDFException {
		return getHttpRepoClient().getSpinMagicProperty(uri);
	}

	/**
	 * @see #getSpinMagicProperty(String)
	 * @see #putSpinMagicProperty(AGSpinMagicProperty)
	 * @see #deleteSpinMagicProperty(String)
	 * @see #listSpinFunctions()
	 * @since v4.4
	 */
	public List<AGSpinMagicProperty> listSpinMagicProperties() throws OpenRDFException {
        TupleQueryResult list = getHttpRepoClient().listSpinMagicProperties();
		try {
			List<AGSpinMagicProperty> result = new ArrayList<AGSpinMagicProperty>();
			while (list.hasNext()) {
				result.add(new AGSpinMagicProperty(list.next()));
			}
			return result;
		} finally {
			Closer.Close(list);
		}
	}
	
	/**
	 * 
	 * @param uri spin magic property identifier
	 * @see #putSpinMagicProperty(AGSpinMagicProperty)
	 * @see #getSpinMagicProperty(String)
	 * @since v4.4
	 */
	public void deleteSpinMagicProperty(String uri) throws OpenRDFException {
		getHttpRepoClient().deleteSpinMagicProperty(uri);
	}

	/**
	 * 
	 * @see #getSpinMagicProperty(String)
	 * @see #deleteSpinMagicProperty(String)
	 * @see #putSpinFunction(AGSpinFunction)
	 * @since v4.4
	 */
	public void putSpinMagicProperty(AGSpinMagicProperty fn) throws OpenRDFException {
		getHttpRepoClient().putSpinMagicProperty(fn);
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
	 * @param comparisonMode determines what is a duplicate 
	 * @throws AGHttpException
	 */
	public void deleteDuplicates(String comparisonMode) throws RepositoryException {
		getHttpRepoClient().deleteDuplicates(comparisonMode);
	}
	
	/**
	 * Materializes inferred statements (generates and adds them to the store).
	 * <p>
	 * The materializer's configuration determines how statements are materialized.
	 * 
	 * @param materializer the materializer to use. 
	 * @return the number of statements added.
	 * @throws AGHttpException
	 * @see AGMaterializer#newInstance()
	 */
	public long materialize(AGMaterializer materializer) throws RepositoryException {
		return getHttpRepoClient().materialize(materializer);
	}
	
	/**
	 * Deletes materialized statements.
	 * 
	 * @return the number of statements deleted.
	 * @throws AGHttpException
	 * @see #materialize(AGMaterializer)
	 */
	public long deleteMaterialized() throws RepositoryException {
		return getHttpRepoClient().deleteMaterialized();
	}
	
	/**
	 * Sets the AG user for X-Masquerade-As-User requests.
	 * 
	 * For AG superusers only.  This allows AG superusers to run requests as
	 * another user in a dedicated session.
	 *  
	 * @param user the user for X-Masquerade-As-User requests.
	 */
	public void setMasqueradeAsUser(String user) throws RepositoryException {
		repoclient.setMasqueradeAsUser(user);
	}
	
}
