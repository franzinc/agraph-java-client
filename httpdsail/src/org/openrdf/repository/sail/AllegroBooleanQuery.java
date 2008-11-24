package org.openrdf.repository.sail;

import java.util.Map;

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;

public class AllegroBooleanQuery extends AllegroQuery implements BooleanQuery {
 
	public AllegroBooleanQuery(QueryLanguage queryLanguage, String queryString, String baseURI) {
		super(queryLanguage, queryString, baseURI);
	}

    /**
     * Execute the embedded SELECT query against the RDF store.  Return
     * an iterator that produces for each step a tuple of values
     * (resources and literals) corresponding to the variables
     * or expressions in a 'select' clause (or its equivalent).         
     */  
    public boolean evaluate() {
        String response = (String)this.evaluateGenericQuery();
        //String truth = (String)response.get("result");
        return response.equals("true") ? true : false;
    }


}
