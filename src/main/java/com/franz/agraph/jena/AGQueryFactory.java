/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGQueryLanguage;
import org.eclipse.rdf4j.query.QueryLanguage;

/**
 * A class for creating AGQuery instances.
 */
public class AGQueryFactory {

    public static AGQuery create(String queryString) {
        return create(AGQueryLanguage.SPARQL, queryString);
    }

    public static AGQuery create(QueryLanguage language, String queryString) {
        return new AGQuery(language, queryString);
    }

}
