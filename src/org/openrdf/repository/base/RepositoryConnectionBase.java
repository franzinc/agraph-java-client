/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.iteration.Iteration;

import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;

/**
 * Abstract class implementing most 'convenience' methods in the
 * RepositoryConnection interface by transforming parameters and mapping the
 * methods to the basic (abstractly declared) methods.
 * <p>
 * Open connections are automatically closed when being garbage collected. A
 * warning message will be logged when the system property
 * <tt>org.openrdf.repository.debug</tt> has been set to a non-<tt>null</tt>
 * value.
 * 
 * @author jeen
 * @author Arjohn Kampman
 */
public abstract class RepositoryConnectionBase implements RepositoryConnection {

	/*
	 * Note: the following debugEnabled method are private so that they can be
	 * removed when open connections no longer block other connections and they
	 * can be closed silently (just like in JDBC).
	 */
	private static boolean debugEnabled() {
		try {
			return System.getProperty("org.openrdf.repository.debug") != null;
		}
		catch (SecurityException e) {
			// Thrown when not allowed to read system properties, for example
			// when running in applets
			return false;
		}
	}

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final Repository repository;

	private boolean isOpen;

	private boolean autoCommit;

	/*
	 * Stores a stack trace that indicates where this connection as created if
	 * debugging is enabled.
	 */
	private Throwable creatorTrace;

	protected RepositoryConnectionBase(Repository repository) {
		this.repository = repository;
		this.isOpen = true;
		this.autoCommit = true;

		if (debugEnabled()) {
			creatorTrace = new Throwable();
		}
	}

	public Repository getRepository() {
		return repository;
	}

	public boolean isOpen()
		throws RepositoryException
	{
		return isOpen;
	}

	public void close()
		throws RepositoryException
	{
		isOpen = false;
	}

	@Override
	protected void finalize()
		throws Throwable
	{
		try {
			if (isOpen()) {
				if (creatorTrace != null) {
					logger.warn("Closing connection due to garbage collection, connection was create in:",
							creatorTrace);
				}

				close();
			}
		}
		finally {
			super.finalize();
		}
	}

	public Query prepareQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareQuery(ql, query, null);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareTupleQuery(ql, query, null);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareGraphQuery(ql, query, null);
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareBooleanQuery(ql, query, null);
	}

	public boolean hasStatement(Resource subj, URI pred, Value obj, boolean includeInferred,
			Resource... contexts)
		throws RepositoryException
	{
		RepositoryResult<Statement> stIter = getStatements(subj, pred, obj, includeInferred, contexts);
		try {
			return stIter.hasNext();
		}
		finally {
			stIter.close();
		}
	}

	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
		throws RepositoryException
	{
		return hasStatement(st.getSubject(), st.getPredicate(), st.getObject(), includeInferred, contexts);
	}

	public boolean isEmpty()
		throws RepositoryException
	{
		return size() == 0;
	}

	public void export(RDFHandler handler, Resource... contexts)
		throws RepositoryException, RDFHandlerException
	{
		exportStatements(null, null, null, false, handler, contexts);
	}

	public void setAutoCommit(boolean autoCommit)
		throws RepositoryException
	{
		if (autoCommit == this.autoCommit) {
			return;
		}

		this.autoCommit = autoCommit;

		// if we are switching from non-autocommit to autocommit mode, commit any
		// pending updates
		if (autoCommit) {
			commit();
		}
	}

	public boolean isAutoCommit()
		throws RepositoryException
	{
		return autoCommit;
	}

	/**
	 * Calls {@link #commit} when in auto-commit mode.
	 */
	protected void autoCommit()
		throws RepositoryException
	{
		if (isAutoCommit()) {
			commit();
		}
	}

	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (baseURI == null) {
			// default baseURI to file
			baseURI = file.toURI().toString();
		}

		InputStream in = new FileInputStream(file);

		try {
			add(in, baseURI, dataFormat, contexts);
		}
		finally {
			in.close();
		}
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		InputStream in = url.openStream();

		try {
			add(in, baseURI, dataFormat, contexts);
		}
		finally {
			in.close();
		}
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		addInputStreamOrReader(in, baseURI, dataFormat, contexts);
	}

	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		addInputStreamOrReader(reader, baseURI, dataFormat, contexts);
	}

	/**
	 * Adds the data that can be read from the supplied InputStream or Reader to
	 * this repository.
	 * 
	 * @param inputStreamOrReader
	 *        An {@link InputStream} or {@link Reader} containing RDF data that
	 *        must be added to the repository.
	 * @param baseURI
	 *        The base URI for the data.
	 * @param dataFormat
	 *        The file format of the data.
	 * @param context
	 *        The context to which the data should be added in case
	 *        <tt>enforceContext</tt> is <tt>true</tt>. The value
	 *        <tt>null</tt> indicates the null context.
	 * @throws IOException
	 * @throws UnsupportedRDFormatException
	 * @throws RDFParseException
	 * @throws RepositoryException
	 */
	protected void addInputStreamOrReader(Object inputStreamOrReader, String baseURI, RDFFormat dataFormat,
			Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		RDFParser rdfParser = Rio.createParser(dataFormat, getRepository().getValueFactory());

		rdfParser.setVerifyData(true);
		rdfParser.setStopAtFirstError(true);
		rdfParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);
		rdfParser.setRDFHandler(rdfInserter);

		boolean autoCommit = isAutoCommit();
		setAutoCommit(false);

		try {
			if (inputStreamOrReader instanceof InputStream) {
				rdfParser.parse((InputStream)inputStreamOrReader, baseURI);
			}
			else if (inputStreamOrReader instanceof Reader) {
				rdfParser.parse((Reader)inputStreamOrReader, baseURI);
			}
			else {
				throw new IllegalArgumentException(
						"inputStreamOrReader must be an InputStream or a Reader, is a: "
								+ inputStreamOrReader.getClass());
			}
		}
		catch (RDFHandlerException e) {
			if (autoCommit) {
				rollback();
			}
			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException)e.getCause();
		}
		catch (RuntimeException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		finally {
			setAutoCommit(autoCommit);
		}
	}

	public void add(Iterable<? extends Statement> statements, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean autoCommit = isAutoCommit();
		setAutoCommit(false);

		try {
			for (Statement st : statements) {
				addWithoutCommit(st, contexts);
			}
		}
		catch (RepositoryException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		catch (RuntimeException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		finally {
			setAutoCommit(autoCommit);
		}
	}

	public <E extends Exception> void add(Iteration<? extends Statement, E> statementIter,
			Resource... contexts)
		throws RepositoryException, E
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean autoCommit = isAutoCommit();
		setAutoCommit(false);

		try {
			while (statementIter.hasNext()) {
				addWithoutCommit(statementIter.next(), contexts);
			}
		}
		catch (RepositoryException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		catch (RuntimeException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		finally {
			setAutoCommit(autoCommit);
		}
	}

	public void add(Statement st, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);
		addWithoutCommit(st, contexts);
		autoCommit();
	}

	public void add(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);
		addWithoutCommit(subject, predicate, object, contexts);
		autoCommit();
	}

	public void remove(Iterable<? extends Statement> statements, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean autoCommit = isAutoCommit();
		setAutoCommit(false);

		try {
			for (Statement st : statements) {
				remove(st, contexts);
			}
		}
		catch (RepositoryException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		catch (RuntimeException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		finally {
			setAutoCommit(autoCommit);
		}
	}

	public <E extends Exception> void remove(Iteration<? extends Statement, E> statementIter,
			Resource... contexts)
		throws RepositoryException, E
	{
		boolean autoCommit = isAutoCommit();
		setAutoCommit(false);

		try {
			while (statementIter.hasNext()) {
				remove(statementIter.next(), contexts);
			}
		}
		catch (RepositoryException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		catch (RuntimeException e) {
			if (autoCommit) {
				rollback();
			}
			throw e;
		}
		finally {
			setAutoCommit(autoCommit);
		}
	}

	public void remove(Statement st, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);
		removeWithoutCommit(st, contexts);
		autoCommit();
	}

	public void remove(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);
		removeWithoutCommit(subject, predicate, object, contexts);
		autoCommit();
	}

	public void clear(Resource... contexts)
		throws RepositoryException
	{
		remove(null, null, null, contexts);
	}

	protected void addWithoutCommit(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts.length == 0 && st.getContext() != null) {
			contexts = new Resource[] { st.getContext() };
		}

		addWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	protected abstract void addWithoutCommit(Resource subject, URI predicate, Value object,
			Resource... contexts)
		throws RepositoryException;

	protected void removeWithoutCommit(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts.length == 0 && st.getContext() != null) {
			contexts = new Resource[] { st.getContext() };
		}

		removeWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	protected abstract void removeWithoutCommit(Resource subject, URI predicate, Value object,
			Resource... contexts)
		throws RepositoryException;
}
