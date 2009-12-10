package com.franz.agraph.jena;


public class AGQueryExecutionFactory {

	public static AGQueryExecution create(AGQuery query, AGModel model) {
		return new AGQueryExecution(query,model);
	}

}
