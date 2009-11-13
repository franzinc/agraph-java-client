package com.franz.ag.repository;

import info.aduna.iteration.Iteration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.sail.SailException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.SPARQLQuery;
import com.knowledgereefsystems.agsail.AllegroSail;
import com.knowledgereefsystems.agsail.AllegroSailConnection;

public class AGRepositoryConnection implements RepositoryConnection {

	boolean isOpen = false;
	AGRepository repository = null;
	AllegroSail sail = null;
	AllegroSailConnection sailconn = null;
	int batchSize = 1000;
	
	AGRepositoryConnection(AGRepository repository) throws RepositoryException {
		// let's assume an initialized repository
		this.repository = repository;
		sail = repository.sail;
		try {
			sailconn = sail.getConnection();
			isOpen = true;
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	// RepositoryConnection API below
	
	public void add(Statement st, Resource... contexts)	throws RepositoryException {
		add(st.getSubject(),st.getPredicate(),st.getObject(), contexts);
	}

	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		// TODO: review for efficiency and intended use
		// TODO: can we stream this over instead? 
		try {
			Statement[] st = new Statement[batchSize];
			Iterator<? extends Statement> iter = statements.iterator();
			for (int i = 0; iter.hasNext();) {
				st[i] = iter.next();
				i++;
				if (batchSize == i || !iter.hasNext()) {
					// set up a batch of at most batchSize
					// i of the statements are set
					Object[] s = new Object[i];
					Object[] p = new Object[i];
					Object[] o = new Object[i];
					Object[] c = new Object[i];
					for (int j = 0; j < i; j++) {
						s[j] = repository.ags.coerceToAGPart(st[j].getSubject());
						p[j] = repository.ags.coerceToAGPart(st[j].getPredicate());
						o[j] = repository.ags.coerceToAGPart(st[j].getObject());
						c[j] = repository.ags.coerceToAGPart(st[j].getContext());
						if (contexts.length == 0) {
							// send 1 batch over using the context specified
							// in each statement
							repository.store.addStatements(s, p, o, c);
						} else {
							// send 1 batch over for each of the contexts
							// ignoring the context part of each statement
							for (Resource ctx : contexts) {
								repository.store.addStatements(s, p, o,
										repository.ags.coerceToAGPart(ctx));
							}
						}
						i = 0;
						sailconn.setUncommittedInsertions(true);
					}
				}
			}
		} catch (AllegroGraphException e) {
			throw new RepositoryException(e);
		}
	}

	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		// TODO: review for efficiency and intended use
		// TODO: can we stream this over instead? 
		// TODO: refactor this to share code with the above method
		try {
			Statement[] st = new Statement[batchSize];
			for (int i = 0; statementIter.hasNext();) {
				st[i] = statementIter.next();
				i++;
				if (batchSize == i || !statementIter.hasNext()) {
					// set up a batch of at most batchSize
					// i of the statements are set
					Object[] s = new Object[i];
					Object[] p = new Object[i];
					Object[] o = new Object[i];
					Object[] c = new Object[i];
					for (int j = 0; j < i; j++) {
						s[j] = repository.ags.coerceToAGPart(st[j].getSubject());
						p[j] = repository.ags.coerceToAGPart(st[j].getPredicate());
						o[j] = repository.ags.coerceToAGPart(st[j].getObject());
						c[j] = repository.ags.coerceToAGPart(st[j].getContext());
						if (contexts.length == 0) {
							// send 1 batch over using the context specified
							// in each statement
							repository.store.addStatements(s, p, o, c);
						} else {
							// send 1 batch over for each of the contexts
							// ignoring the context part of each statement
							for (Resource ctx : contexts) {
								repository.store.addStatements(s, p, o,
										repository.ags.coerceToAGPart(ctx));
							}
						}
						i = 0;
						sailconn.setUncommittedInsertions(true);
					}
				}
			}
		} catch (AllegroGraphException e) {
			throw new RepositoryException(e);
		}
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		// TODO: can we stream this directly instead? 
		// FIXME: assume UTF-8 encoding
		add(new InputStreamReader(in, "UTF-8"), baseURI, dataFormat, contexts);
	}

	public void add(Reader reader, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		if (dataFormat!=RDFFormat.NTRIPLES && dataFormat!=RDFFormat.RDFXML) {
			throw new UnsupportedRDFormatException("Only RDFFormat.NTRIPLES and RDFFormat.RDFXML are supported)");
		}
		BufferedReader br = new BufferedReader(reader);
		StringBuffer sb = new StringBuffer();
		String line;
		while ( (line = br.readLine()) != null) {
			sb.append(line);
		}
		try {
			if (dataFormat==RDFFormat.NTRIPLES) {
				if (0 == contexts.length) {
					// TODO: need a baseURI arg here?
					repository.store.parseNTriples(sb.toString());
				} else {
					for (Resource c : contexts) {
						// TODO: need a baseURI arg here?
						repository.store.parseNTriples(sb.toString(), repository.ags.coerceToAGPart(c));
					}
				}
			} else if (dataFormat == RDFFormat.RDFXML) {
				if (0 == contexts.length) {
					repository.store.parseRDFXML(sb.toString(), "", baseURI);
				} else {
					for (Resource c : contexts) {
						repository.store.parseRDFXML(sb.toString(), repository.ags.coerceToAGPart(c), baseURI);
					}
				}
			}
		//} catch (IOException e) {
			// FIXME: AllegroGraph should throw IOException
			// throw new IOException(e);
		} catch (IllegalArgumentException e) {
			// FIXME: AllegroGraph should throw a proper parse exception
			throw new RDFParseException(e);
		} catch (AllegroGraphException e) {
			throw new RepositoryException(e);
		}
		sailconn.setUncommittedInsertions(true);
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		if (dataFormat!=RDFFormat.NTRIPLES && dataFormat!=RDFFormat.RDFXML) {
			throw new UnsupportedRDFormatException("Only RDFFormat.NTRIPLES and RDFFormat.RDFXML are supported)");
		}
		String urlstring = url.toString();
		try {
			if (dataFormat == RDFFormat.NTRIPLES) {
				if (0 == contexts.length) {
					// TODO: need a baseURI arg here?
					repository.store.loadNTriples(urlstring);
				} else {
					for (Resource c : contexts) {
						// TODO: need a baseURI arg here?
						repository.store.loadNTriples(urlstring, repository.ags
								.coerceToAGPart(c));
					}
				}
			} else if (dataFormat == RDFFormat.RDFXML) {
				if (0 == contexts.length) {
					repository.store.loadRDFXML(urlstring, "", baseURI);
				} else {
					for (Resource c : contexts) {
						repository.store.loadRDFXML(urlstring, repository.ags
								.coerceToAGPart(c), baseURI);
					}
				}
			}
		//} catch (IOException e) {
			// FIXME: AllegroGraph should throw IOException
			// throw new IOException(e);
		} catch (IllegalArgumentException e) {
			// TODO: check that loadRDFXML throws IllegalArg for parse error
			// FIXME: AllegroGraph should throw a proper parse exception
			throw new RDFParseException(e);
		} catch (AllegroGraphException e) {
			throw new RepositoryException(e);
		}
		sailconn.setUncommittedInsertions(true);
	}

	public void add(File file, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		FileInputStream s = new FileInputStream(file);
		try {
			add(s, baseURI, dataFormat, contexts);
		} finally {
			s.close();
		}
	}

	public void add(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
		try {
			sailconn.addStatement(subject, predicate, object, contexts);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void clear(Resource... contexts) throws RepositoryException {
		try {
			sailconn.clear(contexts);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void clearNamespaces() throws RepositoryException {
		try {
			sailconn.clearNamespaces();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void close() throws RepositoryException {
		try {
			sailconn.close();
			isOpen = false;
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void commit() throws RepositoryException {
		try {
			sailconn.commit();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void export(RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		exportStatements(null,null,null,false,handler,contexts);
	}

	public void exportStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		// TODO: need to call handler.handleNamespace and handler.handleComment here?
		RepositoryResult<Statement> results = getStatements(subj,pred,obj,includeInferred,contexts);
		handler.startRDF();
		while (results.hasNext()) {
			handler.handleStatement(results.next());
		}
		handler.endRDF();
	}

	public RepositoryResult<Resource> getContextIDs()
			throws RepositoryException {
		RepositoryResult<Resource> result;
		try {
			result = new RepositoryResult<Resource>(new AGCloseableIteration<Resource>(sailconn.getContextIDs()));

		} catch (SailException e) {
			throw new RepositoryException(e);
		}
		return result;
	}

	public String getNamespace(String prefix) throws RepositoryException {
		String ns = null;
		try {
			ns = sailconn.getNamespace(prefix);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
		return ns;
	}

	public RepositoryResult<Namespace> getNamespaces()
			throws RepositoryException {
		RepositoryResult<Namespace> result;
		try {
			result = new RepositoryResult<Namespace>(new AGCloseableIteration<Namespace>(sailconn.getNamespaces()));

		} catch (SailException e) {
			throw new RepositoryException(e);
		}
		return result;
	}

	public Repository getRepository() {
		return repository;
	}

	public RepositoryResult<Statement> getStatements(Resource subj, URI pred,
			Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		RepositoryResult<Statement> result;
		try {
			result = new RepositoryResult<Statement>(new AGCloseableIteration<Statement>(sailconn.getStatements(subj, pred, obj, includeInferred, contexts)));
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
		return result;
	}

	public boolean hasStatement(Statement st, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		return hasStatement(st.getSubject(),st.getPredicate(),st.getObject(),includeInferred, contexts);
	}

	public boolean hasStatement(Resource subj, URI pred, Value obj,
			boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		boolean found = false;
		try {
			if (contexts.length == 0) {
				found = repository.store.hasStatement(includeInferred,
						repository.ags.coerceToAGPart(subj), repository.ags
								.coerceToAGPart(pred), repository.ags
								.coerceToAGPart(obj), null); // looks in all contexts in the store
			} else {
				for (int i = 0; i < contexts.length && !found; i++) {
					if (contexts[i] == null) {
						// looks only in the null context
						// coerceToAGPart(null) returns null, so this case needs
						// to be addressed separately.
						found = repository.store.hasStatement(includeInferred,
								repository.ags.coerceToAGPart(subj),
								repository.ags.coerceToAGPart(pred),
								repository.ags.coerceToAGPart(obj));
					} else {
						found = repository.store.hasStatement(includeInferred,
								repository.ags.coerceToAGPart(subj),
								repository.ags.coerceToAGPart(pred),
								repository.ags.coerceToAGPart(obj),
								repository.ags.coerceToAGPart(contexts[i]));
					}
				}
			}
		} catch (AllegroGraphException e) {
			throw new RepositoryException(e);
		}
		return found;
	}

	public boolean isAutoCommit() throws RepositoryException {
		return true;
	}

	public boolean isEmpty() throws RepositoryException {
		return (size()==0);
	}

	public boolean isOpen() throws RepositoryException {
		return isOpen;
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareBooleanQuery(ql, query, null);	
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
		if (ql != QueryLanguage.SPARQL) {
			throw new MalformedQueryException("Only SPARQL queries are supported.");
		}
		SPARQLQuery sq = new SPARQLQuery();
		sq.setTripleStore(repository.store);
		sq.setQuery(query);
		sq.setIncludeInferred(true);   // The default in Sesame is true.
		if (baseURI!=null) sq.setDefaultBase(baseURI);
		
		return new AGBooleanQuery(repository.ags, sq);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareGraphQuery(ql, query, null);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
		if (ql != QueryLanguage.SPARQL) {
			throw new MalformedQueryException("Only SPARQL queries are supported.");
		}
		SPARQLQuery sq = new SPARQLQuery();
		sq.setTripleStore(repository.store);
		sq.setQuery(query);
		sq.setIncludeInferred(true);   // The default in Sesame is true.
		if (baseURI!=null) sq.setDefaultBase(baseURI);
		
		return new AGGraphQuery(repository.ags,sq);
	}

	public Query prepareQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareQuery(ql, query, null);
	}

	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws RepositoryException, MalformedQueryException {
		// TODO review the need for this optional method
		return null;
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareTupleQuery(ql, query, null);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query,
			String baseURI) throws RepositoryException, MalformedQueryException {
		if (ql != QueryLanguage.SPARQL) {
			throw new MalformedQueryException("Only SPARQL queries are supported.");
		}
		SPARQLQuery sq = new SPARQLQuery();
		sq.setTripleStore(repository.store);
		sq.setQuery(query);
		sq.setIncludeInferred(true);   // The default in Sesame is true.
		if (baseURI!=null) sq.setDefaultBase(baseURI);
		
		return new AGTupleQuery(repository.ags,sq);
	}

	public void remove(Statement st, Resource... contexts)
			throws RepositoryException {
		remove(st.getSubject(),st.getPredicate(),st.getObject(),contexts);
	}

	public void remove(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		// TODO: review for efficiency and intended use
		Iterator<? extends Statement> iter = statements.iterator();
		while(iter.hasNext()) {
			remove(iter.next(),contexts);
		}
	}

	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		// TODO: review for efficiency and intended use
		while(statementIter.hasNext()) {
			remove(statementIter.next(),contexts);
		}
	}

	public void remove(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
		try {
			sailconn.removeStatements(subject, predicate, object, contexts);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void removeNamespace(String prefix) throws RepositoryException {
		try {
			sailconn.removeNamespace(prefix);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void rollback() throws RepositoryException {
		try {
			sailconn.rollback();
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		if (!autoCommit) {
			throw new RepositoryException(new UnsupportedOperationException());
		}
	}

	public void setNamespace(String prefix, String name) throws RepositoryException {
		try {
			sailconn.setNamespace(prefix, name);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}

	}

	public long size(Resource... contexts) throws RepositoryException {
		try {
			return sailconn.size(contexts);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	public ValueFactory getValueFactory() {
		return sail.getValueFactory();
	}

}
