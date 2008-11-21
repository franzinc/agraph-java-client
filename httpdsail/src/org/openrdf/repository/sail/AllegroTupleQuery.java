package org.openrdf.repository.sail;

import java.util.List;
import java.util.Map;

import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;

import franz.exceptions.UnimplementedMethodException;

public class AllegroTupleQuery extends AllegroQuery implements TupleQuery {
 
	public AllegroTupleQuery(QueryLanguage queryLanguage, String queryString, String baseURI) {
		super(queryLanguage, queryString, baseURI);
	}

    /**
     * Execute the embedded SELECT query against the RDF store.  Return
     * an iterator that produces for each step a tuple of values
     * (resources and literals) corresponding to the variables
     * or expressions in a 'select' clause (or its equivalent).         
     */  
    public TupleQueryResult evaluate() {
        Map response = (Map)this.evaluateGenericQuery();
        return new AllegroTupleQueryResult((List<String>)response.get("names"),(List<List<String>>) response.get("values"));
    }
    
    public JDBCResultSet jdbcEvaluate() {
    	Map response = (Map)this.evaluateGenericQuery();
        return new JDBCResultSet((List<String>)response.get("names"),(List<List<String>>) response.get("values"), false);
    }

	public void evaluate(TupleQueryResultHandler arg0) {
		throw new UnimplementedMethodException("evaluate<TupleQueryResultHandler>");
	}


}
