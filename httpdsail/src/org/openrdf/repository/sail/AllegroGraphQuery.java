package org.openrdf.repository.sail;

import java.util.List;
import java.util.Map;

import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.rio.RDFHandler;

import franz.exceptions.UnimplementedMethodException;

public class AllegroGraphQuery extends AllegroQuery implements GraphQuery {
 
	public AllegroGraphQuery(QueryLanguage queryLanguage, String queryString, String baseURI) {
		super(queryLanguage, queryString, baseURI);
	}

    /**
     * Execute the embedded SELECT query against the RDF store.  Return
     * an iterator that produces for each step a tuple of values
     * (resources and literals) corresponding to the variables
     * or expressions in a 'select' clause (or its equivalent).         
     */  
    public AllegroGraphQueryResult evaluate() {
        List<List<String>> response = (List<List<String>>)this.evaluateGenericQuery();
        AllegroGraphQueryResult result = new AllegroGraphQueryResult(response);
        result.setConnection(this.getConnection());
        return result;
    }
    
    public JDBCResultSet jdbcEvaluate() {
    	List<List<String>> response = (List<List<String>>)this.evaluateGenericQuery();
        return new JDBCResultSet(JDBCResultSet.STATEMENT_COLUMN_NAMES , response, true);
    }

	public void evaluate(RDFHandler arg0) {
		throw new UnimplementedMethodException("evaluate<RDFHandler>");
	}

}
