/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.parser.sparql;

import junit.framework.Test;

import org.openrdf.query.Dataset;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;

import test.AGAbstractTest;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGServer;

public class HTTPSPARQL11QueryTest extends SPARQLQueryTest {

	public static Test suite()
		throws Exception
	{
		return SPARQL11ManifestTest.suite(new Factory() {

			public HTTPSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality)
			{
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
			}
			
			public HTTPSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder)
			{
				return new HTTPSPARQL11QueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, checkOrder);
			}
		});
	}

	protected HTTPSPARQL11QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality)
	{
		this(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
	}

	protected HTTPSPARQL11QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, boolean checkOrder)
	{
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder);
	}
	
	protected Repository newRepository() {
		String repoName = this.getClass().getSimpleName();
		try {
			new AGServer(AGAbstractTest.findServerUrl(),AGAbstractTest.username(), AGAbstractTest.password()).getCatalog(AGAbstractTest.CATALOG_ID).createRepository(repoName);
		} catch (AGHttpException e) {
			throw new RuntimeException(e);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
    	HTTPRepository httprepo = 
    		new HTTPRepository(AGAbstractTest.findServerUrl()+"/catalogs/"+AGAbstractTest.CATALOG_ID+"/repositories/"+repoName);
    	httprepo.setUsernameAndPassword(AGAbstractTest.username(), AGAbstractTest.password());
    	return httprepo;
	}
}
