/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.query.QueryLanguage;

import com.franz.agraph.repository.AGQueryLanguage;

/**
 * 
 * A class for creating AGQuery instances.
 *
 */
public class AGQueryFactory {

	public static AGQuery create(String queryString) {
		return create(AGQueryLanguage.SPARQL, queryString);
    }

	public static AGQuery create(QueryLanguage language, String queryString) {
		return new AGQuery(language, queryString);
	}

}
