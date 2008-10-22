package org.openrdf.repository.sail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.AbstractQuery;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import franz.exceptions.SoftException;
import franz.exceptions.UnimplementedMethodException;

public class AllegroQuery extends AbstractQuery {

	private QueryLanguage queryLanguage;
	private String queryString;
	private String baseURI;
	private AllegroRepositoryConnection connection = null;
	private Dataset dataset = null;
	
    public AllegroQuery(QueryLanguage queryLanguage, String queryString, String baseURI) {
        //Query.checkLanguage(queryLanguage);
        this.queryString = queryString;
        this.queryLanguage = queryLanguage;
        this.baseURI = baseURI;
    }
    
    protected void setConnection (AllegroRepositoryConnection connection) {
    	this.connection = connection;
    }
    
//    protected static void checkLanguage(String queryLanguage) {
//        if (!(queryLanguage  == QueryLanguage.SPARQL) || (queryLanguage == QueryLanguage.PROLOG))
//            throw new SoftException("Can't evaluate the query language '" + queryLanguage + "'.  Options are: SPARQL and PROLOG.");
//    }


    /**
     * Add build-it and registered prefixes to 'query' when needed.
     */
    protected static String splicePrefixesIntoQuery(String query, AllegroRepositoryConnection connection)  {
    	try {
        String lcQuery = query.toLowerCase();
        HashMap referenced = new HashMap<String, String>();
        RepositoryResult<Namespace> namespaces = connection.getNamespaces();
        while (namespaces.hasNext()) {
        	Namespace ns = (Namespace)namespaces.next();
        	String prefix = ns.getPrefix();
            if ((lcQuery.indexOf(prefix) >= 0) && (lcQuery.indexOf("prefix " + prefix) < 0)) {
                referenced.put(prefix, ns.getName());
            }
        }
        for (Map.Entry entry : (Set<Map.Entry>)referenced.entrySet()) {
        	String prefix = (String)entry.getKey();
        	String namespace = (String)entry.getValue();
            query = "PREFIX " + prefix + ": <" + namespace + "> " + query;
        }
        return query;
    	} catch (RepositoryException ex) {throw new SoftException(ex);}
    }
        
    /**
     * Evaluate a SPARQL or PROLOG query, which may be a 'select', 'construct', 'describe'
     * or 'ask' query (in the SPARQL case).  Return an appropriate response.
     */
    protected Object evaluateGenericQuery() {
//        if ((this.dataset != null) && (this.dataset.getDefaultGraphs() != null)
//    		// TODO: ADD THIS IN:
//        	//&& (this.dataset.getDefaultGraphs() != AllegroRepositoryConnection.ALL_CONTEXTS)
//        	) {
//            throw new UnimplementedMethodException("Query datasets not yet implemented for default graphs.");
//        }
        Set<URI> regularGraphs = (this.dataset != null) ? this.dataset.getDefaultGraphs() : AllegroRepositoryConnection.ALL_CONTEXTS;
        if (regularGraphs.isEmpty()) regularGraphs = AllegroRepositoryConnection.ALL_CONTEXTS;
        List<String> regularContexts = this.connection.contextsToNtripleContexts(regularGraphs, false);        
        Set<URI> namedGraphs = (this.dataset != null) ? this.dataset.getNamedGraphs() : null;
        List<String> namedContexts = this.connection.contextsToNtripleContexts(namedGraphs, false);
        miniclient.Repository mini = this.connection.getMiniRepository();
        Object response;
        if (this.queryLanguage == QueryLanguage.SPARQL) {            
            String query = splicePrefixesIntoQuery(this.queryString, this.connection);
            response = mini.evalSparqlQuery(query, this.includeInferred, regularContexts, namedContexts, null);
        } else { // NEED TO REDEFINE 'QueryLanguage' STUPID!! if (this.queryLanguage == QueryLanguage.PROLOG) {
            response = mini.evalPrologQuery(this.queryString, this.includeInferred, namedContexts);
        }
        return response;
    }

	public int getMaxQueryTime() {
		throw new UnimplementedMethodException("getMaxQueryTime");
	}

	public void setMaxQueryTime(int time) {
		throw new UnimplementedMethodException("setMaxQueryTime");
	}



}
