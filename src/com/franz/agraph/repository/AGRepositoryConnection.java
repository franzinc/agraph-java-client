/**
 * 
 */
package com.franz.agraph.repository;

import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.Iteration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.json.JSONArray;
import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.NamespaceImpl;
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

/**
 * Implements the RepositoryConnection interface for AllegroGraph.
 */
public class AGRepositoryConnection extends RepositoryConnectionBase implements
		RepositoryConnection {

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
	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows=new JSONArray();
		for (Statement st : statements) {
				JSONArray row = new JSONArray().put(
					NTriplesUtil.toNTriplesString(st.getSubject())).put(
					NTriplesUtil.toNTriplesString(st.getPredicate())).put(
					NTriplesUtil.toNTriplesString(st.getObject()));
			rows.put(row);
		}
		if (rows==null) return;
		InputStream in;
		try {
			in = new ByteArrayInputStream(rows.toString().getBytes("UTF-8"));
			RequestEntity entity = new InputStreamRequestEntity(in, -1,
					"application/json");
			getHttpRepoClient().upload(entity, null, false, null, null, null,
					contexts);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (RDFParseException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public <E extends Exception> void add(Iteration<? extends Statement, E> statementIter,
			Resource... contexts)
		throws RepositoryException, E
	{
		OpenRDFUtil.verifyContextNotNull(contexts);
		JSONArray rows=new JSONArray();
		while (statementIter.hasNext()) {
				Statement st = statementIter.next();
				JSONArray row = new JSONArray().put(
					NTriplesUtil.toNTriplesString(st.getSubject())).put(
					NTriplesUtil.toNTriplesString(st.getPredicate())).put(
					NTriplesUtil.toNTriplesString(st.getObject()));
			rows.put(row);
		}
		if (rows==null) return;
		InputStream in;
		try {
			in = new ByteArrayInputStream(rows.toString().getBytes("UTF-8"));
			RequestEntity entity = new InputStreamRequestEntity(in, -1,
					"application/json");
			getHttpRepoClient().upload(entity, null, false, null, null, null,
					contexts);
		} catch (RepositoryException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			// TODO: determine what is proper to throw here.
			throw new RepositoryException(e);
		}
	}
	
	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		JSONArray row = new JSONArray().put(
				NTriplesUtil.toNTriplesString(subject)).put(
				NTriplesUtil.toNTriplesString(predicate)).put(
				NTriplesUtil.toNTriplesString(object));
		JSONArray rows = new JSONArray().put(row);
		InputStream in;
		try {
			in = new ByteArrayInputStream(rows.toString().getBytes("UTF-8"));
			RequestEntity entity = new InputStreamRequestEntity(in, -1,
					"application/json");
			getHttpRepoClient().upload(entity, null, false, null, null, null,
					contexts);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (RDFParseException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
		/*
		 * try { getHttpRepoClient().addStatements(subject, predicate, object,
		 * contexts); } catch (IOException e) { throw new
		 * RepositoryException(e); } ByteArrayOutputStream out = new
		 * ByteArrayOutputStream(); NTriplesWriter writer = new
		 * NTriplesWriter(out); try { writer.startRDF(); writer
		 * .handleStatement(new StatementImpl(subject, predicate, object));
		 * writer.endRDF(); InputStream in = new
		 * ByteArrayInputStream(out.toByteArray()); // TODO: check whether using
		 * NTriples is lossy addInputStreamOrReader(in, null,
		 * RDFFormat.NTRIPLES, contexts); } catch (RDFHandlerException e) {
		 * throw new RepositoryException(e); } catch (RDFParseException e) { //
		 * found a bug? throw new RuntimeException(e); } catch (IOException e) {
		 * throw new RepositoryException(e); }
		 */
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

	public void registerFreetextPredicate(URI predicate)
			throws RepositoryException {
		getHttpRepoClient().registerFreetextPredicate(predicate);
	}

	// TODO: return RepositoryResult<URI>?
	public String[] getFreetextPredicates() throws RepositoryException {
		return getHttpRepoClient().getFreetextPredicates();
	}

	public void registerPredicateMapping(URI predicate, URI primtype)
			throws RepositoryException {
		getHttpRepoClient().registerPredicateMapping(predicate, primtype);
	}

	public void deletePredicateMapping(URI predicate)
			throws RepositoryException {
		getHttpRepoClient().deletePredicateMapping(predicate);
	}

	// TODO: return RepositoryResult<Mapping>?
	public String[] getPredicateMappings() throws RepositoryException {
		return getHttpRepoClient().getPredicateMappings();
	}

	// TODO: are all primtypes available as URI constants?
	public void registerDatatypeMapping(URI datatype, URI primtype)
			throws RepositoryException {
		getHttpRepoClient().registerDatatypeMapping(datatype, primtype);
	}

	public void deleteDatatypeMapping(URI datatype) throws RepositoryException {
		getHttpRepoClient().deleteDatatypeMapping(datatype);
	}

	// TODO: return RepositoryResult<Mapping>?
	public String[] getDatatypeMappings() throws RepositoryException {
		return getHttpRepoClient().getDatatypeMappings();
	}

	public void clearMappings() throws RepositoryException {
		getHttpRepoClient().clearMappings();
	}

	public void addRules(String rules) throws RepositoryException {
		getHttpRepoClient().addRules(rules);
	}

	// TODO: specify RuleLanguage
	public void addRules(InputStream rulestream) throws RepositoryException {
		getHttpRepoClient().addRules(rulestream);
	}

	public String evalInServer(String lispForm) throws RepositoryException {
		return getHttpRepoClient().evalInServer(lispForm);
	}

	public String evalInServer(InputStream stream) throws RepositoryException {
		return getHttpRepoClient().evalInServer(stream);
	}

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

	public void ping() throws RepositoryException {
		getHttpRepoClient().ping();
	}

}
