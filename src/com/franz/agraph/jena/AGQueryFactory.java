package com.franz.agraph.jena;

public class AGQueryFactory {

	public static AGQuery create(String queryString) {
    	AGQuery query = new AGQuery(queryString);
        return query ;
    }

}
