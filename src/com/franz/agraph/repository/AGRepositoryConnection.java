/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
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
import java.util.List;

import org.json.JSONArray;
import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
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
import com.franz.agraph.http.AGResponseHandler;
import com.franz.util.Closeable;

/**
 * Implements the Sesame RepositoryConnection interface for AllegroGraph.
 */
public class AGRepositoryConnection extends RepositoryConnectionBase implements
		RepositoryConnection, Closeable {

	private final AGRepository repository;
	private final AGHttpRepoClient repoclient;

	/**
	 * @param repository
	 * @throws RepositoryException
	 */
	public AGRepositoryConnection(AGRepository repository)
			throws RepositoryException {
		super(repository);
		this.repository = repository;
		repoclient = new AGHttpRepoClient(this);
	}

	/*
	 * @Override protected void finalize() throws Throwable { try { if
	 * (isOpen()) { close(); } } finally { super.finalize(); } }
	 */

	@Override
	public AGRepository getRepository() {
		return repository;
	}

	public AGHttpRepoClient getHttpRepoClient() {
		return repoclient;
	}

	@Override
	public AGValueFactory getValueFactory() {
		return getRepository().getValueFactory();
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		Statement st = new StatementImpl(subject, predicate, object);
		JSONArray rows = new JSONArray().put(encodeJSON(st, contexts));
		getHttpRepoClient().uploadJSON(rows, contexts);
	}

	@Override
	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		for (Statement st : statements) {
			JSONArray row = encodeJSON(st, contexts);
			rows.put(row);
		}
		getHttpRepoClient().uploadJSON(rows, contexts);
	}

	@Override
	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		while (statementIter.hasNext()) {
			rows.put(encodeJSON(statementIter.next(), contexts));
		}
		getHttpRepoClient().uploadJSON(rows, contexts);
	}

	private JSONArray encodeJSON(Statement st, Resource... contexts) {
		JSONArray row = new JSONArray().put(
				NTriplesUtil.toNTriplesString(st.getSubject())).put(
				NTriplesUtil.toNTriplesString(st.getPredicate())).put(
				NTriplesUtil.toNTriplesString(st.getObject()));
		if (contexts.length == 0 && st.getContext() != null) {
			row.put(NTriplesUtil.toNTriplesString(st.getContext()));
		}
		return row;
	}

	@Override
	protected void addInputStreamOrReader(Object inputStreamOrReader,
			String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {

		if (inputStreamOrReader instanceof InputStream) {
			getHttpRepoClient().upload(((InputStream) inputStreamOrReader),
					baseURI, dataFormat, false, contexts);
		} else if (inputStreamOrReader instanceof Reader) {
			getHttpRepoClient().upload(((Reader) inputStreamOrReader), baseURI,
					dataFormat, false, contexts);
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
			JSONArray row = encodeJSON(st, contexts);
			rows.put(row);
		}
		getHttpRepoClient().deleteJSON(rows, contexts);
	}
    
	@Override
	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statements, Resource... contexts)
	throws RepositoryException, E {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows = new JSONArray();
		while (statements.hasNext()) {
			JSONArray row = encodeJSON(statements.next(), contexts);
			rows.put(row);
		}
		getHttpRepoClient().deleteJSON(rows, contexts);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		getHttpRepoClient().setAutoCommit(autoCommit);
	}

	@Override
	public boolean isAutoCommit() throws RepositoryException {
		return getHttpRepoClient().isAutoCommit();
	}

	public void commit() throws RepositoryException {
		getHttpRepoClient().commit();
	}

	public void rollback() throws RepositoryException {
		getHttpRepoClient().rollback();
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
		try {
			getHttpRepoClient().getStatements(subj, pred, obj, includeInferred,
					handler, contexts);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
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
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Creates a RepositoryResult for the supplied element set.
	 */
	protected <E> RepositoryResult<E> createRepositoryResult(
			Iterable<? extends E> elements) {
		return new RepositoryResult<E>(
				new CloseableIteratorIteration<E, RepositoryException>(elements
						.iterator()));
	}

	public String getNamespace(String prefix) throws RepositoryException {
		try {
			return getHttpRepoClient().getNamespace(prefix);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
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
		} catch (IOException e) {
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
	 * Unsupported method, throws an {@link UnsupportedOperationException}.
	 */
	public AGQuery prepareQuery(QueryLanguage ql, String queryString,
			String baseURI) {
		// TODO: consider supporting this
		throw new UnsupportedOperationException();
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
		return new AGTupleQuery(this, ql, queryString, baseURI);
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
		return new AGGraphQuery(this, ql, queryString, baseURI);
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
		return new AGBooleanQuery(this, ql, queryString, baseURI);
	}

	public void removeNamespace(String prefix) throws RepositoryException {
		getHttpRepoClient().removeNamespacePrefix(prefix);
	}

	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		getHttpRepoClient().setNamespacePrefix(prefix, name);
	}

	public long size(Resource... contexts) throws RepositoryException {
		try {
			return getHttpRepoClient().size(contexts);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	/************************************
	 * AllegroGraph Extensions hereafter
	 */

	/**
	 * Registers a predicate for free text indexing. Once registered, the
	 * objects of data added to the repository having this predicate will be
	 * text indexed and searchable.
	 */
	public void registerFreetextPredicate(URI predicate)
			throws RepositoryException {
		getHttpRepoClient().registerFreetextPredicate(predicate);
	}

	// TODO: return RepositoryResult<URI>?
	/**
	 * Gets the predicates that have been registered for text indexing.
	 */
	public String[] getFreetextPredicates() throws RepositoryException {
		return getHttpRepoClient().getFreetextPredicates();
	}

	/**
	 * Registers a predicate mapping from the predicate to a primitive datatype.
	 * This can be useful in speeding up query performance and enabling range
	 * queries over datatypes.
	 * 
	 * Once registered, the objects of any data added via this connection that
	 * have this predicate will be mapped to the primitive datatype.
	 * 
	 * For example, registering that predicate <http://example.org/age> is
	 * mapped to XMLSchema.INT and adding the triple:
	 * 
	 * <http://example.org/Fred> <http://example.org/age> "24"
	 * 
	 * will result in the object being treated as having datatype "24"^^xsd:int.
	 * 
	 * @param predicate
	 *            the predicate
	 * @param primtype
	 * @throws RepositoryException
	 */
	public void registerPredicateMapping(URI predicate, URI primtype)
			throws RepositoryException {
		getHttpRepoClient().registerPredicateMapping(predicate, primtype);
	}

	/**
	 * Deletes any predicate mapping associated with the given predicate.
	 * 
	 * @param predicate
	 *            the predicate
	 * @throws RepositoryException
	 */
	public void deletePredicateMapping(URI predicate)
			throws RepositoryException {
		getHttpRepoClient().deletePredicateMapping(predicate);
	}

	// TODO: return RepositoryResult<Mapping>?
	/**
	 * Gets the predicate mappings defined for this connection.
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
	 * Once registered, the objects of any data added via this connection that
	 * have this datatype will be mapped to the primitive datatype.
	 * 
	 * For example, registering that datatype <http://example.org/usertype> is
	 * mapped to XMLSchema.INT and adding the triple:
	 * 
	 * <http://example.org/Fred> <http://example.org/age>
	 * "24"^^<http://example.org/usertype>
	 * 
	 * will result in the object being treated as having datatype "24"^^xsd:int.
	 * 
	 * @param datatype
	 *            the user datatype
	 * @param primtype
	 *            the primitive type
	 * @throws RepositoryException
	 */
	public void registerDatatypeMapping(URI datatype, URI primtype)
			throws RepositoryException {
		getHttpRepoClient().registerDatatypeMapping(datatype, primtype);
	}

	/**
	 * Deletes any datatype mapping associated with the given datatype.
	 * 
	 * @param datatype
	 *            the user datatype
	 * @throws RepositoryException
	 */
	public void deleteDatatypeMapping(URI datatype) throws RepositoryException {
		getHttpRepoClient().deleteDatatypeMapping(datatype);
	}

	// TODO: return RepositoryResult<Mapping>?
	/**
	 * Gets the datatype mappings defined for this connection.
	 */
	public String[] getDatatypeMappings() throws RepositoryException {
		return getHttpRepoClient().getDatatypeMappings();
	}

	/**
	 * Deletes all predicate and datatype mappings for this connection.
	 * 
	 * @throws RepositoryException
	 */
	public void clearMappings() throws RepositoryException {
		getHttpRepoClient().clearMappings();
	}

	/**
	 * Adds Prolog rules to be used on this connection.
	 * 
	 * @param rules
	 *            a string of rules.
	 * @throws RepositoryException
	 */
	public void addRules(String rules) throws RepositoryException {
		getHttpRepoClient().addRules(rules);
	}

	// TODO: specify RuleLanguage
	/**
	 * Adds Prolog rules to be used on this connection.
	 * 
	 * @param rulestream
	 *            a stream of rules.
	 * @throws RepositoryException
	 */
	public void addRules(InputStream rulestream) throws RepositoryException {
		getHttpRepoClient().addRules(rulestream);
	}

	/**
	 * Evaluates a Lisp form on the server, and returns the result as a String.
	 * 
	 * @param lispForm
	 *            the Lisp form to evaluate, in a String.
	 * @return the result in a String.
	 * @throws RepositoryException
	 */
	public String evalInServer(String lispForm) throws RepositoryException {
		return getHttpRepoClient().evalInServer(lispForm);
	}

	/**
	 * Evaluates a Lisp form on the server, and returns the result as a String.
	 * 
	 * @param stream
	 *            the Lisp form to evaluate, in a stream.
	 * @return the result in a String.
	 * @throws RepositoryException
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
		try {
			getHttpRepoClient().load(source, baseURI, dataFormat, contexts);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
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
		try {
			getHttpRepoClient().load(absoluteServerPath, baseURI, dataFormat,
					contexts);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Instructs the server to extend the life of this connection's dedicated
	 * session, if it is using one. Such connections that are idle for 3600
	 * seconds will be closed by the server.
	 * 
	 * @throws RepositoryException
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
		AGResponseHandler handler = new AGResponseHandler(getRepository(),
				collector, getHttpRepoClient().getPreferredRDFFormat());
		getHttpRepoClient().getGeoBox(NTriplesUtil.toNTriplesString(type),
		                              NTriplesUtil.toNTriplesString(predicate),
		                              xmin, xmax, ymin, ymax, limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}

	public RepositoryResult<Statement> getStatementsInCircle(URI type,
			URI predicate, float x, float y, float radius,
			int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGResponseHandler(getRepository(),
				collector, getHttpRepoClient().getPreferredRDFFormat());
		getHttpRepoClient().getGeoCircle(NTriplesUtil.toNTriplesString(type),
		                                 NTriplesUtil.toNTriplesString(predicate),
		                                 x, y, radius, limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}
	
	public RepositoryResult<Statement> getGeoHaversine(URI type,
			URI predicate, float lat, float lon, float radius,
			String unit, int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGResponseHandler(getRepository(),
				collector, getHttpRepoClient().getPreferredRDFFormat());
		getHttpRepoClient().getGeoHaversine(NTriplesUtil.toNTriplesString(type),
		                                    NTriplesUtil.toNTriplesString(predicate),
		                                    lat, lon, radius, unit, limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}
	
	public RepositoryResult<Statement> getStatementsInPolygon(URI type,
			URI predicate, URI polygon, int limit, boolean infer) throws RepositoryException {
		StatementCollector collector = new StatementCollector();
		AGResponseHandler handler = new AGResponseHandler(getRepository(),
				collector, getHttpRepoClient().getPreferredRDFFormat());
		getHttpRepoClient().getGeoPolygon(NTriplesUtil.toNTriplesString(type), NTriplesUtil.toNTriplesString(predicate), NTriplesUtil.toNTriplesString(polygon), limit, infer, handler);
		return createRepositoryResult(collector.getStatements());
	}
	
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
}
