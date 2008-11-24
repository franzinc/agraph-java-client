/*

 */
package org.openrdf.repository.sail;

import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryConnectionBase;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import franz.exceptions.SoftException;
import franz.exceptions.UnimplementedMethodException;

/**
 * An implementation of the {@link RepositoryConnection} interface that wraps a
 * {@link SailConnection}.
 * 
 * @author jeen
 * @author Arjohn Kampman
 */
public class AllegroRepositoryConnection extends RepositoryConnectionBase {

	protected static Set<URI> ALL_CONTEXTS = new HashSet<URI>(); 
	protected static String MINI_NULL_CONTEXT = "null";
	
	/**
	 * Constructor.
	 * Creates a new repository connection that will wrap the supplied
	 * SailConnection. SailRepositoryConnection objects are created by
	 * {@link SailRepository#getConnection}.
	 */
	protected AllegroRepositoryConnection(AllegroRepository repository) {
		super(repository);
	}	

	
	//------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------

	protected miniclient.Repository getMiniRepository() {
		return ((AllegroRepository)this.getRepository()).getMiniRepository();
	}
	
	/**
	 * If 'term' is an OpenRDF term, convert it to a string.  If its already
     * a string; assume its in ntriples format, and just pass it through.
	 */
	private String toNtriples(Object term) {
		if (term == null) return (String)term;
		else if (term instanceof String) return (String)term;
		else if (term instanceof URI) return "<" + ((URI)term).toString() + ">";
		else if (term instanceof Literal) {
			String badTerm = term.toString();
			int pos = badTerm.indexOf("^^");
			String goodTerm;
			if (pos > 0) {
				String value = badTerm.substring(0, pos);
				String datatype = badTerm.substring(pos + 2);
				goodTerm = value + "^^<" + datatype + ">";
			} else {
				goodTerm = badTerm;
			}
			return goodTerm;
		}
		// THIS IS BOGUS: SHOULD NOT NEED STRING QUOTES AROUND BNODE EXPRESSION:
		//else if (term instanceof BNode) return "\"" + term.toString() + "\"";
		else if (term instanceof BNode) return term.toString();		
		else throw new SoftException("Unexpected datatype passed to 'toNTriples' " + term);
	}
	
	private String contextToNtriples(Resource context, boolean noneIsMiniNull) {
        //if context == MINI_NULL_CONTEXT: return MINI_NULL_CONTEXT
        if (context != null) return this.toNtriples(context);
        else if (noneIsMiniNull) return MINI_NULL_CONTEXT;
        else return null;     
	}
       
    /**
     * Do three transformations here.  Convert from context object(s) to
     * context strings (angle brackets).
     * Also, convert singleton context to list of contexts, and convert
     * ALL_CONTEXTS to None.
     * And, convert None context to 'null'.
     * On assertions, an empty list of contexts means "use the null context".
     * On retrieval, an empty list of contexts means "use all contexts".
     * The "emptyMeansNullContext" tells you which interpretation to use here.
     */
    protected List<String> contextsToNtripleContexts(Object contexts, boolean emptyMeansNullContext) {
//    	System.out.println("CONTEXTS TO NTRIPLE CONTEXTS " + contexts + " " + emptyMeansNullContext);
//    	if (contexts == null) System.out.println("   NULLLLLL");
//    	else System.out.println("    " + contexts.getClass());
    	List<String> cxts = new ArrayList<String>();
        if (contexts == ALL_CONTEXTS) { // PROBABLY THIS CASE NEVER OCCURS
            // consistency would dictate that  null => [null], but this would
            // likely surprise users, so we don't do that:
            cxts = null;
        	// The Java semantics for ... treats the 'null' input as a corner case.
        	// Instead of passing in a singleton array containing one null item,
        	// it passes in a null.  That's kind of stupid.  We handle that here:
        } else if (contexts == null) {
        	cxts.add(MINI_NULL_CONTEXT);
        } else if (((Object[])contexts).length == 0) {
            if (emptyMeansNullContext) cxts.add(MINI_NULL_CONTEXT);
            else cxts = null;  // interpreted as wildcard
        } else if (contexts instanceof Resource[]) {
        	if ((((Resource[])contexts).length == 0) && emptyMeansNullContext) {
        		cxts.add(MINI_NULL_CONTEXT);
        	} else {
        		for (Resource c : (Resource[])contexts) {
        			cxts.add(this.contextToNtriples(c, true));
        		}
        	}
        } else if (contexts instanceof Object[]) {
        	for (Object c : (Object[])contexts) {
        		cxts.add(this.contextToNtriples((Resource)c, true));   
        	}
        } else if (contexts instanceof List) {
        	for (URI c : (List<URI>)contexts) {
        		cxts.add(this.contextToNtriples(c, true));   
        	}
    	} else {
    		cxts.add(this.contextToNtriples((Resource)contexts, true));
    	}
        return cxts;
	}

	//------------------------------------------------------------------------------------
	// API Methods
	//------------------------------------------------------------------------------------

	public ValueFactory getValueFactory() {
		return this.getRepository().getValueFactory();
	}

	/**
	 * Returns the underlying SailConnection.
	 */
	public SailConnection getSailConnection() {
		throw new UnimplementedMethodException("getSailConnection");
	}

	public void commit() {
	}

	public void rollback() {
		System.out.println("WHY IS IT CALLING 'rollback' ???");
		//throw new UnimplementedMethodException("rollback");
	}


	// currently, there is nothing to close in AllegroGraph	
//	public void close(){
//	}

	public TupleQuery prepareQuery(QueryLanguage ql, String queryString, String baseURI) {
		throw new UnimplementedMethodException("prepareQuery");
	}

	/**
	 * Embed 'queryString' into a query object which can be
     * executed against the RDF storage.  'queryString' must be a SELECT
     * query.  The result of query
     * execution is an iterator of tuples.
	 */
	public AllegroTupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI) {
		AllegroTupleQuery query = new AllegroTupleQuery(ql, queryString, baseURI);
		query.setConnection(this);
        return query;
	}

	public AllegroGraphQuery prepareGraphQuery(QueryLanguage ql, String queryString, String baseURI){
		AllegroGraphQuery query = new AllegroGraphQuery(ql, queryString, baseURI);
		query.setConnection(this);
        return query;
	}

	/**
	 * Embed 'queryString' into a query object which can be
     * executed against the RDF storage.  'queryString' must be a Boolean
     * query.  The result of query execution is a boolean true/false.
	 */
	public AllegroBooleanQuery prepareBooleanQuery(QueryLanguage ql, String queryString, String baseURI){
		AllegroBooleanQuery query = new AllegroBooleanQuery(ql, queryString, baseURI);
		query.setConnection(this);
        return query;
	}

	public RepositoryResult<Resource> getContextIDs() {
		throw new UnimplementedMethodException("getContextIDs");
	}

	public RepositoryResult<Statement> getStatements(Resource subject, URI predicate, Value object,
			boolean includeInferred, Resource... contexts) {
		Object callback = null;
		AllegroRepository rep = (AllegroRepository)this.getRepository();
		AllegroValueFactory factory = (AllegroValueFactory)rep.getValueFactory(); 
        object = factory.objectPositionTermToOpenRDFTerm(object, predicate);
        List<List<String>>stringTuples = this.getMiniRepository().getStatements(this.toNtriples(subject), this.toNtriples(predicate),
        		this.toNtriples(object), this.contextsToNtripleContexts(contexts, false), false, callback);
        return new AllegroRepositoryResult(stringTuples);
	}
	
	public JDBCResultSet getJDBCStatements(Resource subject, URI predicate, Value object,
			boolean includeInferred, Resource... contexts) {
		Object callback = null;
		AllegroRepository rep = (AllegroRepository)this.getRepository();
		AllegroValueFactory factory = (AllegroValueFactory)rep.getValueFactory(); 
        object = factory.objectPositionTermToOpenRDFTerm(object, predicate);
        List<List<String>>stringTuples = this.getMiniRepository().getStatements(this.toNtriples(subject), this.toNtriples(predicate),
        		this.toNtriples(object), this.contextsToNtripleContexts(contexts, false), false, callback);
        return new JDBCResultSet(JDBCResultSet.STATEMENT_COLUMN_NAMES, stringTuples, true);
	}

	public void exportStatements(Resource subj, URI pred, Value obj, boolean includeInferred,
			RDFHandler handler, Resource... contexts)
		throws RepositoryException, RDFHandlerException
	{
		handler.startRDF();

		// Export namespace information
		CloseableIteration<? extends Namespace, RepositoryException> nsIter = getNamespaces();
		try {
			while (nsIter.hasNext()) {
				Namespace ns = nsIter.next();
				handler.handleNamespace(ns.getPrefix(), ns.getName());
			}
		}
		finally {
			nsIter.close();
		}

		// Export statements
		CloseableIteration<? extends Statement, RepositoryException> stIter = getStatements(subj, pred, obj,
				includeInferred, contexts);

		try {
			while (stIter.hasNext()) {
				handler.handleStatement(stIter.next());
			}
		}
		finally {
			stIter.close();
		}

		handler.endRDF();
	}

	public long size(Resource... contexts) 	{
		List<String> ntripleContexts = this.contextsToNtripleContexts(contexts, true);
		if (ntripleContexts.isEmpty())
			return this.getMiniRepository().getSize(null);
		else {
			long total = 0;
			for (String cxt : ntripleContexts) {
				total += this.getMiniRepository().getSize(cxt);
			}
			return total;
		}
	}
	
	private static String translateRDFFormat (RDFFormat rdfFormat) {
		if (rdfFormat == RDFFormat.NTRIPLES) return "NTRIPLES";
		else if (rdfFormat == RDFFormat.RDFXML) return "RDF/XML";
		else throw new SoftException("Unsupported data format " + rdfFormat);
	}
	
	public void add(String file, String baseURI, RDFFormat dataFormat, Resource... contexts) {
		add(new File(file), baseURI, dataFormat, false, contexts);
	}

	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts) {
		add(file, baseURI, dataFormat, false, contexts);
	}

	public void add(String file, String baseURI, RDFFormat dataFormat, boolean serverSide, Resource... contexts) {
		add(new File(file), baseURI, dataFormat, serverSide, contexts);
	}
	
	public void add(File file, String baseURI, RDFFormat dataFormat, boolean serverSide, Resource... contexts) {
		if (baseURI == null) {
			// default baseURI to file
			baseURI = file.toURI().toString();
		}
		String rdfFormat = translateRDFFormat(dataFormat);
		List<String> cxts = this.contextsToNtripleContexts(contexts, false);
		if (cxts == null)
			this.getMiniRepository().loadFile(file.getAbsolutePath(), rdfFormat, baseURI, null, serverSide);
		else
			for (String cxt : cxts) {
				this.getMiniRepository().loadFile(file.getAbsolutePath(), rdfFormat, baseURI, cxt, serverSide);
			}
	}

	/**
	 * Add the supplied triple of values to this repository, optionally to
     * one or more named contexts.        
	 */
	protected void addWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts) {
		
		//System.out.println("ADD WITHOUT COMMIT " + subject + " " + predicate + " " + object + " " + contexts);
		List<String> cxts = this.contextsToNtripleContexts(contexts, true);
		for (String cxt : cxts) {
			this.getMiniRepository().addStatement(this.toNtriples(subject), this.toNtriples(predicate),
					this.toNtriples(object), cxt);
		}
	}
	
	private void deleteMatchingStatements(Resource subject, URI predicate, Value object, Resource... contexts) {
		String subj = this.toNtriples(subject);
		String pred = this.toNtriples(predicate);
		String obj = this.toNtriples(object);
		List<String> ntripleContexts = this.contextsToNtripleContexts(contexts, true);
		if (ntripleContexts.isEmpty())
			this.getMiniRepository().deleteMatchingStatements(subj, pred, obj, null);
		else
			for (String cxt : ntripleContexts) {
				this.getMiniRepository().deleteMatchingStatements(subj, pred, obj, cxt);
			}
	}
	
	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts) {
		deleteMatchingStatements(subject, predicate, object, contexts);
	}

	@Override
	public void clear(Resource... contexts) {
		deleteMatchingStatements(null, null, null, contexts);
	}
	
	//----------------------------------------------------------------------------------------------
	//  Namespaces
	//----------------------------------------------------------------------------------------------
	
	private Map<String, Namespace> namespaceMap = new HashMap<String, Namespace>();

	public void setNamespace(String prefix, String name) {
		this.namespaceMap.put(prefix, new NamespaceImpl(prefix, name));
	}

	public void removeNamespace(String prefix) {
		this.namespaceMap.remove(prefix);
	}

	public void clearNamespaces() {
		this.namespaceMap.clear();
	}

	public RepositoryResult<Namespace> getNamespaces() {		
		return createRepositoryResult(new ListIterator(this.namespaceMap.values()));
	}

	public String getNamespace(String prefix) {
		Namespace ns = this.namespaceMap.get(prefix);
		return (ns != null) ? ns.getName() : null;
	}

	//----------------------------------------------------------------------------------------------
	//  Namespaces
	//----------------------------------------------------------------------------------------------
	
	public static class  ListIterator<X> implements CloseableIteration<X, Exception> {
		private int cursor = 0;
		private List items = new ArrayList();
		
		public ListIterator (Collection items) {
			this.items.addAll(items);
		}
		
		public void close () {}
		
		public boolean hasNext() { return this.cursor < this.items.size(); }
		
		public X next() {
			if (this.hasNext()) {
				X value = (X)this.items.get(this.cursor);
				this.cursor++;
				return value;
			} else {
				return null;
			}
		}
		
		public void remove () {throw new UnimplementedMethodException("remove");}
	}
	

	/**
	 * Wraps a CloseableIteration coming from a Sail in a RepositoryResult
	 * object, applying the required conversions
	 */
	protected <E> RepositoryResult<E> createRepositoryResult(
			CloseableIteration<? extends E, SailException> sailIter)
	{
		return new RepositoryResult<E>(new SailCloseableIteration<E>(sailIter));
	}
}
