/******************************************************************************
** Copyright (c) 2008-2012 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.openrdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.io.FileUtil;
import info.aduna.io.ZipUtil;

import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.parser.sparql.SPARQLQueryTest;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.turtle.TurtleParser;
import org.openrdf.sail.memory.MemoryStore;

/**
 * This class is a variant of 
 * 
 * org.openrdf.query.parser.sparql.SPARQL11ManifestTest
 * 
 * that allows users of the class to set parameters:
 * 
 * REMOTE, LOCAL_DAWG_TESTS, USE_SUBSET and subDirs.
 * 
 */
public class SPARQL11ManifestTest {

	static final Logger logger = LoggerFactory.getLogger(SPARQL11ManifestTest.class);

	/** use DAWG SPARQL 1.1 tests located on www.3.org instead of local resources */
	public static boolean REMOTE = false;

	/** use local copy of DAWG SPARQL 1.1 tests instead of own test suite */
	public static boolean LOCAL_DAWG_TESTS = true;

	/**
	 * use only a subset of all available tests, where the subset is defined by
	 * an array of subdirectory names.
	 */
	public static boolean USE_SUBSET = false;

	public static String[] subDirs = { "negation" };

	public static TestSuite suite(SPARQLQueryTest.Factory factory)
		throws Exception
	{

		final String manifestFile;
		final File tmpDir;
		if (REMOTE) {
			manifestFile = "http://www.w3.org/2009/sparql/docs/tests/data-sparql11/manifest-all.ttl";
			tmpDir = null;
		}
		else {
			URL url = null;
			if (LOCAL_DAWG_TESTS) {
				url = SPARQL11ManifestTest.class.getResource("/testcases-dawg-sparql-1.1/manifest-all.ttl");
			}
			else {
				url = SPARQL11ManifestTest.class.getResource("/testcases-sparql-1.1/manifest-evaluation.ttl");
			}

			if ("jar".equals(url.getProtocol())) {
				// Extract manifest files to a temporary directory
				try {
					tmpDir = FileUtil.createTempDir("sparql-1.1-evaluation");

					JarURLConnection con = (JarURLConnection)url.openConnection();
					JarFile jar = con.getJarFile();

					ZipUtil.extract(jar, tmpDir);

					File localFile = new File(tmpDir, con.getEntryName());
					manifestFile = localFile.toURI().toURL().toString();
				}
				catch (IOException e) {
					throw new AssertionError(e);
				}
			}
			else {
				manifestFile = url.toString();
				tmpDir = null;
			}
		}

		TestSuite suite = new TestSuite(factory.getClass().getName()) {

			@Override
			public void run(TestResult result) {
				try {
					super.run(result);
				}
				finally {
					if (tmpDir != null) {
						try {
							FileUtil.deleteDir(tmpDir);
						}
						catch (IOException e) {
							System.err.println("Unable to clean up temporary directory '" + tmpDir + "': "
									+ e.getMessage());
						}
					}
				}
			}
		};

		Repository manifestRep = new SailRepository(new MemoryStore());
		manifestRep.initialize();
		RepositoryConnection con = manifestRep.getConnection();

		addTurtle(con, new URL(manifestFile), manifestFile);

		String query = "SELECT DISTINCT manifestFile FROM {x} rdf:first {manifestFile} "
				+ "USING NAMESPACE mf = <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>, "
				+ "  qt = <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>";

		TupleQueryResult manifestResults = con.prepareTupleQuery(QueryLanguage.SERQL, query, manifestFile).evaluate();

		while (manifestResults.hasNext()) {
			BindingSet bindingSet = manifestResults.next();
			String subManifestFile = bindingSet.getValue("manifestFile").toString();

			if (includeSubManifest(subManifestFile)) {
				suite.addTest(SPARQLQueryTest.suite(subManifestFile, factory, false));
			}
		}

		manifestResults.close();
		con.close();
		manifestRep.shutDown();

		logger.info("Created aggregated test suite with " + suite.countTestCases() + " test cases.");
		return suite;
	}

	private static boolean includeSubManifest(String subManifestFile) {
		boolean result = true;

		if (USE_SUBSET && subDirs != null && subDirs.length > 0) {
			result = false;
			for (String subdir : subDirs) {
				int index = subManifestFile.lastIndexOf("/");
				String path = subManifestFile.substring(0, index);
				String sd = path.substring(path.lastIndexOf("/") + 1);
				if (sd.equals(subdir)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	static void addTurtle(RepositoryConnection con, URL url, String baseURI, Resource... contexts)
		throws IOException, RepositoryException, RDFParseException
	{
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		InputStream in = url.openStream();

		try {
			OpenRDFUtil.verifyContextNotNull(contexts);
			final ValueFactory vf = con.getRepository().getValueFactory();
			RDFParser rdfParser = new TurtleParser();
			rdfParser.setValueFactory(vf);

			rdfParser.setVerifyData(false);
			rdfParser.setStopAtFirstError(true);
			rdfParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

			RDFInserter rdfInserter = new RDFInserter(con);
			rdfInserter.enforceContext(contexts);
			rdfParser.setRDFHandler(rdfInserter);

			boolean autoCommit = con.isAutoCommit();
			con.setAutoCommit(false);

			try {
				rdfParser.parse(in, baseURI);
			}
			catch (RDFHandlerException e) {
				if (autoCommit) {
					con.rollback();
				}
				// RDFInserter only throws wrapped RepositoryExceptions
				throw (RepositoryException)e.getCause();
			}
			catch (RuntimeException e) {
				if (autoCommit) {
					con.rollback();
				}
				throw e;
			}
			finally {
				con.setAutoCommit(autoCommit);
			}
		}
		finally {
			in.close();
		}
	}
}
