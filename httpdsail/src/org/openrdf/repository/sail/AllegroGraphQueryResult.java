package org.openrdf.repository.sail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.repository.RepositoryResult;

import franz.exceptions.SoftException;
import franz.exceptions.UnimplementedMethodException;

public class AllegroGraphQueryResult implements GraphQueryResult {
	
	private List<List<String>> stringTuples;
	private AllegroRepositoryResult repositoryResult;
	private AllegroRepositoryConnection connection = null;
	// THIS SKIP LOGIC NEEDS TO BE INCORPORATED INTO 'AllegroRepositoryResult'???:
	private boolean skipIllegalTuples = false;
	private List<List<String>> illegalTuples = new ArrayList<List<String>>();
	
	public AllegroGraphQueryResult(List<List<String>> stringTuples) {
		this.stringTuples = stringTuples;		
		this.repositoryResult = new AllegroRepositoryResult(stringTuples);
	}
	
	protected void setConnection(AllegroRepositoryConnection connection) {
		this.connection = connection;
	}
	
	public  Map<String, String> getNamespaces() {
		if (connection == null)
			throw new SoftException("Can't ask for namespaces prior to query executioin.");
		Map<String, String> map = new HashMap<String, String>();
		RepositoryResult rr = this.connection.getNamespaces();
		try {
			while (rr.hasNext()) {
				Namespace ns = (Namespace)rr.next();
				// I DON'T KNOW WHICH GOES FIRST HERE, THE PREFIX OR THE NAMESPACE: - RMM
				map.put(ns.getPrefix(), ns.getName());
			}
		} catch (Exception ex) {throw new SoftException(ex);}			
		return map;
	}
	
	/**
	 * Return 'true' if query tuples with illegal syntax should be collected
	 * silently.  Otherwise, an exception will be thrown if illegal syntax is detected. 
	 */
	public boolean skipIllegalTuples() {
		if (true) throw new UnimplementedMethodException("skipIllegalTuples");
		return this.skipIllegalTuples;
	}
	
	/**
	 * A setting of 'true' indicates that query tuples with illegal syntax should be collected
	 * silently.  Otherwise, an exception will be thrown if illegal syntax is detected. 
	 */
	public void setSkipIllegalTuples(boolean setting) {
		if (true) throw new UnimplementedMethodException("setSkipIllegalTuples");		
		this.skipIllegalTuples = setting;
	}

	/**
	 * Return a list of illegal tuples returned by the last query.
	 */
	public List<List<String>> getIllegalTuples () {
		if (true) throw new UnimplementedMethodException("setSkipIllegalTuples");	
		return this.illegalTuples;
	}
	
	public void close() {
	}
	
	/**
	 * Return a count of the number of tuples retrieved into the query result.
	 */
	public int getTupleCount() {
		return this.stringTuples.size();
	}

    /**
     * Return 'true' if the iterator has additional statement(s).
     */
    public boolean hasNext() {
    	return this.repositoryResult.hasNext();
    }

    /**
     * Return the next Statement in the answer, if there is one.
     */
    public Statement next() {
        return this.repositoryResult.next();
    }

	public void remove() {
		try {
			this.repositoryResult.remove();
		} catch (Exception ex) {throw new SoftException(ex);}
	}

}
