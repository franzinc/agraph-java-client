/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.query.QuerySolution;


/**
 * A class for creating QueryExecution instances.
 */
public class AGQueryExecutionFactory {

    public static AGQueryExecution create(AGQuery query, AGModel model) {
        return new AGQueryExecution(query, model);
    }

    public static AGQueryExecution create(AGQuery query, AGModel model, QuerySolution initialBinding) {
        AGQueryExecution qexec = new AGQueryExecution(query, model);
        qexec.setInitialBinding(initialBinding);
        return qexec;
    }

}
