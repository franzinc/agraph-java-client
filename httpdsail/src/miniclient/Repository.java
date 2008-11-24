package miniclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import franz.exceptions.SoftException;

public class Repository {
	
	private String url;
	private Object environment = null;
	
	public Repository(String url) {
		this.url = url;
	}
	
	public String getName () {return this.url;}

	public Object jsonRequest(String method, String url) {
		return Request.jsonRequest( method, url, null, null, null);
	}

	public Object jsonRequest(String method, String url, JSONObject options, 
			String contentType, Object callback) {
		return Request.jsonRequest(method, url, options, contentType, callback);
	}
	
	public void nullRequest(String method, String url, JSONObject options, 
			String contentType) {
		Request.nullRequest(method, url, options, contentType);
	}


	/**
	 * Return the number of triples/quads in the repository.
	 */
	public long getSize(String context) {
		try {
			JSONObject options = new JSONObject().put("context", context);
			return (long)new Long((String)this.jsonRequest("GET", this.url + "/size", options, null, null));
		} catch (JSONException ex) {throw new SoftException(ex);} 
	}
	
	/**
	 * List the contexts (named graphs) that are present in this repository.
	 */
    public List<String> listContexts () {
        return (List)jsonRequest("GET", this.url + "/contexts");
    }

    public boolean isWriteable() {
        return (boolean)(Boolean)jsonRequest("GET", this.url + "/writeable");
    }
    
    /**
     * Execute a SPARQL query. Context can be None or a list of
     * contexts -- strings in "http://foo.com" form or "null" for the
     * default context. Return type depends on the query type. ASK
     * gives a boolean, SELECT a {names, values} map with 'values' containing
     * lists of lists of terms (strings). CONSTRUCT and DESCRIBE return a list
     * of lists representing statements. Callback WILL NOT work on
     * ASK queries."""
     */
    public Object evalSparqlQuery(String query, boolean infer, Object contexts, Object namedContexts, Object callback) {
    	try {
    		JSONObject options = new JSONObject().put("query", query).put("infer", infer).put("context", contexts).
    			put("environment", this.environment);
    		// TODO: FOLD THIS BACK IN ABOVE AFTER WE DEBUG IT:
    		if (true && namedContexts != null && !"null".equals(namedContexts))
    			options.put("namedContext", namedContexts);
    		if (true && contexts != null && !"null".equals(contexts))
    			options.put("context", contexts);
   		return jsonRequest("GET", this.url, options, null, callback);
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }
    
    /**
     * Execute a Prolog query. Returns a {names, values} map with 'values' containing
     * a list of lists of terms (strings).
     */
    public Object evalPrologQuery(String query, boolean infer, Object callback) {
    	try {
    		JSONObject options = new JSONObject().put("query", query). put("infer", infer).put("queryLn", "prolog").
    			put("environment", this.environment);
    		return jsonRequest("POST", this.url, options, null, callback);
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }

    public void definePrologFunctor(String definition) {
    	try {
    		JSONObject options = new JSONObject().put("definition", definition).put("environment", this.environment);
    		nullRequest("POST", this.url + "/functor", options, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }

	/**
	 * Retrieve all statements matching the given constraints.
     * Context can be None or a list of contexts, as in
     * evalSparqlQuery.
     */
	public List<List<String>> getStatements(String subj, String pred, String obj, Object context, boolean infer, Object callback) {
		try {
			JSONObject options = new JSONObject().put("subj", subj).put("pred", pred).put("obj", obj).
				put("context", context).put("infer", infer);
			return (List)jsonRequest("GET", this.url + "/statements", options, null, callback);
		} catch (JSONException ex) { throw new SoftException(ex); }
	}
    
	/**
	 * Add a single statement to the repository.
	 * 'context' is a string or a list of strings.
	 */
    public void addStatement(String subj, String pred, String obj, Object context) {
    	//System.out.println("ADD STATEMENT " + subj + " " + pred + " " + obj + " " + context);
    	try {
    		JSONArray row = new JSONArray().put(subj).put(pred).put(obj).put(context);
    		JSONArray rows = new JSONArray().put(row);
    		JSONObject options = new JSONObject().put("body", rows);
    		nullRequest("POST", this.url + "/statements", options, "application/json");
        } catch (JSONException ex) { throw new SoftException(ex); }	
    }

    /**
     * Delete all statements matching the constraints from the
     * repository. Context can be None or a single graph name.
     * TODO: IF httpd CAN'T HANDLE A LIST OF CONTEXTS HERE, ADD LOGIC HERE
     * TO HANDLE IT.
     */
    public void deleteMatchingStatements(String subj, String pred, String obj, Object context) {
    	try {
            JSONObject options = new JSONObject().put("subj", subj).put("pred", pred).put("obj",obj).
        		put("context", context);
    		nullRequest("DELETE", this.url + "/statements", options, null);
        } catch (JSONException ex) { throw new SoftException(ex); }
    }
    
    /**
     * Convert a map or list into a JSONObject or JSONArray.
     */
    private Object jsonize(Object json) {
    	if (json instanceof String) { 
    		return (String)json;
    	} else if (json instanceof List) {
    		JSONArray array = new JSONArray();
    		for (Object v : (List)json) {
    			array.put(jsonize(v));
    		}
    		return array;
    	} else if (json instanceof Map) {
    		JSONObject dict = new JSONObject();
    		for (Map.Entry entry : (Collection<Map.Entry>)((Map)json).entrySet()) {
    			try {
    				dict.put((String)entry.getKey(), jsonize(entry.getValue()));
    			} catch (JSONException ex) { throw new SoftException(ex); }
    		}
    		return dict;
    	} else if (json == null) {
    		return null; // this seems to work
    	} else {
    		return json.toString();
    	}
    }
    
    /**
     * Convert 'json' into a JSONObject or JSONArray, and return a JSONObject
     * having the converted value as the value for a 'content' entry.
     */
    // NOT SURE IF THIS IS WHAT THE SERVER WANTS TO SEE:
    private JSONObject contentize(Object json) {
    	JSONObject dict = new JSONObject();
    	try {
    		dict.put("body", jsonize(json));
    	} catch (JSONException ex) { throw new SoftException(ex); }
    	return dict;
    }

    /**
     * Add a collection of statements to the repository. Quads
     * should be an list of four-element arrays, where the fourth
     * element, the context name, may be null.
     */
    public void addStatements(List<List> quads) {
		nullRequest("POST", this.url + "/statements/json", contentize(quads), "application/json");
    }
    	
    public static class UnsupportedFormatError extends RuntimeException {
    	private String format;
    	
    	public UnsupportedFormatError(String format) {
            this.format = format;
    	}
        
    	public String getMessage() {
            return "'" + this.format + "' file format not supported (try 'ntriples' or 'rdf/xml').";
    	}
    }
    
    private String[] formatToURLFormatAndContentType (String format) {
        String urlformat = null;
        String mime = null;
         if ("ntriples".equalsIgnoreCase(format)) {
            urlformat = "ntriples";
            mime = "text/plain";
        } else if ("rdf/xml".equalsIgnoreCase(format)) {
            urlformat = "rdfxml";
            mime = "application/rdf+xml";
        } else {
            throw new Repository.UnsupportedFormatError(format);
        }
        return new String[]{urlformat, mime};
    }
    
    public void loadData (String data, String format, String baseURI, String context) {
    	try {
	    	String[] pair = formatToURLFormatAndContentType(format);
	    	JSONObject options = new JSONObject().put("context", context).put("baseURI", baseURI).
	    		put("format", pair[0]).put("body", data);
	    	nullRequest("POST", this.url + "/statements", options, pair[1]);
    	} //catch (UnsupportedEncodingException ex) {throw new SoftException(ex);}
		  catch (JSONException ex) {throw new SoftException(ex);}    	
    }
    	
    public void loadFile(String fileURL, String format, String baseURI, String context, boolean serverSide) {
    	try {
	        if (!serverSide) {
	        	File file = new File(fileURL);
	        	int size = (int) file.length();
	        	InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
	            char[] buffer = new char[size];
	            reader.read(buffer);
	            String s = new String(buffer);
	            reader.close();
	            loadData(s, format, baseURI, context);
	        } else {
	    		String[] pair = formatToURLFormatAndContentType(format);
		        JSONObject options = new JSONObject().put("file", fileURL).put("context", context).put("baseURI", baseURI).
		            put("format", pair[0]);	                        
		    	nullRequest("POST", this.url + "/statements", options, pair[1]);
	        }
	} catch (UnsupportedEncodingException ex) {throw new SoftException(ex);}
	  catch (JSONException ex) { throw new SoftException(ex); }	
	  catch (IOException ex) { throw new SoftException(ex); }		  
    }
    
    	
    /**
     * Delete a collection of statements from the repository.
     */
    public void deleteStatements(List<List> quads) {
		nullRequest("POST", this.url + "/statements/json/delete", contentize(quads), "application/json");
    }
    	
    /**
     * List the SPOGI-indices that are active in the repository.
     */
    public List listIndices() {
		return (List)jsonRequest("GET", this.url + "/indices", null, null, null);
    }
    	
    /**
     * Register a SPOGI index.
     */
    public void addIndex(String type) {
    	try {
    		JSONObject options = new JSONObject().put("type", type);
    		nullRequest("POST", this.url + "/indices", options, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }
    }
    	
    /**
     * Drop a SPOGI index.
     */
    public void deleteIndex(String type) {
    	try {
    		JSONObject options = new JSONObject().put("type", type);
    		nullRequest("DELETE", this.url + "/indices", options, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }
    }
    	
    /**
     * Returns the proportion (0-1) of the repository that is indexed.
     */
    public String getIndexCoverage() {
		return (String)jsonRequest("GET", this.url + "/indexing", null, null, null);
    }
    	   
    /**
     * Index any unindexed statements in the repository. If all is
     * true, the whole repository is re-indexed.
     */
    public void indexStatements(boolean all) {
    	try {
    		JSONObject options = new JSONObject().put("all", all);
    		nullRequest("POST", this.url + "/indexing", options, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }
    }
    	
    /**
     * Use free-text indices to search for the given pattern.
     * Returns a list of statements.
     */
    public List evalFreeTextSearch(String pattern, boolean infer, Object callback) {
    	try {
    		JSONObject options = new JSONObject().put("pattern", pattern).put("infer", infer);
    		return (List)jsonRequest("GET", this.url + "/freetext", options, null, callback);
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }
    	
    /**
     * List the predicates that are used for free-text indexing.
     */
    public List listFreeTextPredicates() {
		return (List)jsonRequest("GET", this.url + "/freetextPredicates");
    }
    	
    /**
     * Add a predicate for free-text indexing.
     */
    public void registerFreeTextPredicate(String predicate) { 		
    	try {
        	JSONObject options = new JSONObject().put("predicate", predicate);
    		nullRequest("POST", this.url + "/freetextPredicates", options, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }
    }
    	
    /**
     * Repositories use a current environment, which are
     * containers for namespaces and Prolog predicates. Every
     * server-side repository has a default environment that is used
     * when no environment is specified.
     */
    public void setEnvironment(String name) { 	
		this.environment = name;
    }
    
    /**
     * 
     */
    public List listEnvironments() { 		
		return (List)jsonRequest("GET", this.url + "/environments", null, null, null);
    }
    	
    /** NOT SURE WHAT THIS RETURNS  - RMM */
    public Object createEnvironment(String name) { 		
    	try {
    		JSONObject options = new JSONObject().put("name", name);
    		return jsonRequest("POST", this.url + "/environments", options, null, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }
    	
    /**
     * 
     */
    public void deleteEnvironment(String name) {
    	try {
    		JSONObject options = new JSONObject().put("name", name);
    		nullRequest("DELETE", this.url + "/environments", options, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }   
    
    /**
     * 
     */
    public List<String> listNamespaces() { 		
    	try {
    		JSONObject options = new JSONObject().put("environment", this.environment);
    		return (List<String>)jsonRequest("GET", this.url + "/namespaces", options, null, null);
    	} catch (JSONException ex) { throw new SoftException(ex); }
    }
    
    /**
     * 
     */
    public void addNamespace(String prefix, String namespace) {
    	try {
    		JSONObject options = new JSONObject().put("prefix", prefix).put("uri", namespace).
    			put("environment", this.environment);
    		nullRequest("POST", this.url + "/namespaces", options, "text/plain");                    
    	} catch (JSONException ex) { throw new SoftException(ex); }
    }
    
    /**
     * Remove the namespace with prefix 'prefix'.
     */
    public void deleteNamespace(String prefix) {
    	try {
    		JSONObject options = new JSONObject().put("prefix", prefix).put("environment", this.environment);
    		nullRequest("DELETE", this.url + "/namespaces", options, null);                    
    	} catch (JSONException ex) { throw new SoftException(ex); }	
    }


}