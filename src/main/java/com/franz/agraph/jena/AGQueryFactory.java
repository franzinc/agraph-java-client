/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGQueryLanguage;
import org.apache.jena.query.QueryParseException;
import org.eclipse.rdf4j.query.QueryLanguage;

/**
 * A class for creating AGQuery instances.
 */
public class AGQueryFactory {

    public static AGQuery create(String queryString) {
        return create(AGQueryLanguage.SPARQL, queryString);
    }

    public static AGQuery create(QueryLanguage language, String queryString) {

        if (queryString == null) {
            throw new QueryParseException("Query string is null", 0, 0);
        } else if (queryString.isEmpty()) {
            throw new QueryParseException("Query string is empty", 0, 0);
        }

        return new AGQuery(language, queryString);
    }

}
