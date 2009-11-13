
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.agjena.test;

import java.io.IOException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraph;
import com.franz.agjena.AllegroGraphGraphMaker;
import com.franz.agjena.AllegroGraphModel;
import com.franz.agjena.StartUp;
import com.franz.agjena.query.AllegroGraphQueryExecutionFactory;
import com.franz.agjena.query.AllegroGraphQueryFactory;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable;
import com.hp.hpl.jena.sparql.vocabulary.ResultSetGraphVocab;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;



public class TestARQueries extends TestCase
{
    public static TestSuite suite()
    {
        TestSuite ts = new TestSuite(TestARQueries.class) ;
        ts.setName("ARQuery-Scripts") ;
        return ts ;
    }
    
    static final String root = "doc-src/jena/ARQueries/" ;
//    static final String datafile = "testing/LARQ/data-1.ttl" ;
//    static final String results1 = "testing/LARQ/results-1.srj" ;
//    static final String results2 = "testing/LARQ/results-2.srj" ;
//    static final String results3 = "testing/LARQ/results-3.srj" ;
    
    public TestARQueries(String name)
    { 
        super(name) ;
    }
    
    // See TestLARQ.
        
    public static void runTestScript(String queryFile, String dataFile, String resultsFile)
    {
        Query query = AllegroGraphQueryFactory.read(root+queryFile) ;
        Model model = new AllegroGraphModel(AllegroGraphGraphMaker.getInstance().createGraph());
        //Model model = AllegroGraphModelFactory.createDefaultModel() ; 
        FileManager.get().readModel(model, root+dataFile) ;        
        QueryExecution qe = AllegroGraphQueryExecutionFactory.create(query, model) ;
        
//        Query query = QueryFactory.read(root+queryFile) ;
//        Model model = ModelFactory.createDefaultModel() ; 
//        FileManager.get().readModel(model, root+dataFile) ;        
//        QueryExecution qe = QueryExecutionFactory.create(query, model) ;

//        ResultSetRewindable rsExpected = 
//            ResultSetFactory.makeRewindable(ResultSetFactory.load(root+resultsFile)) ;
        
        ResultSetRewindable rsActual = ResultSetFactory.makeRewindable(qe.execSelect()) ;
        System.out.println("Testing query " + query.toString());
        System.out.println("== Result") ;
        ResultSetFormatter.out(rsActual) ;
        qe.close() ; 
    }
    
    public void test_arq_1()
    { runTestScript("arq-q-1.rq", "data.ttl", "results-1.srj"); }

    public void test_arq_2()
    { runTestScript("arq-q-2.rq", "data.ttl", "results-2.srj") ; }

    public void test_arq_3()
    { runTestScript("arq-q-3.rq", "data-1.ttl", "results-3.srj") ; }
    
    public void test_arq_4()
    { runTestScript("arq-q-4.rq", "data-1.ttl", "results-4.srj") ; }
    
    public void test_arq_5()
    { runTestScript("arq-q-5.rq", "data-1.ttl", "results-5.srj") ; }

    public void test_arq_6()
    { runTestScript("arq-q-6.rq", "data-1.ttl", "results-6.srj") ; }

    public void test_arq_7()
    { runTestScript("arq-q-7.rq", "data-1.ttl", "results-7.srj") ; }
    
    public void test_larq_1()
    { runTestScript("larq-q-1.rq", "data.ttl", "results-1.srj"); }

    
    private static Model resultSetToModel(ResultSet rs)
    {
    	// 
    	if (true)
    		throw new RuntimeException("Need to call 'GraphUtils.makeDefaultModel()', which is not in the jar files.");
    	// THIS CODE IS IN THE JENA SOURCES, BUT NOT IN THE JAR FILES.  IF WE
    	// WANT TO RUN IT, WE NEED TO FIX THIS SOMEHOW
        //Model m = GraphUtils.makeDefaultModel() ;
    	Model m = null;
        ResultSetFormatter.asRDF(m, rs) ;
        if ( m.getNsPrefixURI("rs") == null )
            m.setNsPrefix("rs", ResultSetGraphVocab.getURI() ) ;
        if ( m.getNsPrefixURI("rdf") == null )
            m.setNsPrefix("rdf", RDF.getURI() ) ;
        if ( m.getNsPrefixURI("xsd") == null )
            m.setNsPrefix("xsd", XSDDatatype.XSD+"#") ;
        return m ;
        
    }
    
    /** Are two result sets the same (isomorphic)?
    *
    * @param rs1
    * @param rs2
    * @return boolean
    */

   static public boolean resultSetEquivalent(Query query,
       ResultSet rs1, ResultSet rs2)
   {
       Model model2 = resultSetToModel(rs2) ;
       return resultSetEquivalent(query, rs1, model2) ;
   }

   static public boolean resultSetEquivalent(Query query,
                                             ResultSet rs1,
                                             Model model2)
   {
       Model model1 = resultSetToModel(rs1) ;
       return model1.isIsomorphicWith(model2) ;
   }
   
   
   
   public static void main(String[] args) throws AllegroGraphException, IOException {
	   AllegroGraph agStore = StartUp.startUpTripleStore(
   			StartUp.AccessTripleStore.RENEW,
   			"localhost", "test",
   			//"/Users/bmacgregor/Desktop/AGFolder"
   			"/tmp/agtest", 
   			args);
	   AllegroGraphGraphMaker.setDefaultMaker(agStore);
	   if (agStore != null) {
		   	try {
		   			System.out.println("About to try the ARQuery tests.");		   			
		   			int choice = 2;
		   			switch (choice) {
		   			case 1: new TestARQueries("ARQuery-Scripts").test_arq_1();
		   					break;
		   			case 2: new TestARQueries("ARQuery-Scripts").test_arq_2();
		   					break;
		   			case 8: new TestARQueries("ARQuery-Scripts").test_larq_1();
						break;
		  			}
		   			System.out.println("Finished ARQuery test " + choice);
		   	} catch (Exception ex) {
		   		System.out.println("Failure in ARQuery test\n" + ex.toString() + ex.getMessage());
		   		ex.printStackTrace();
		   	} finally {
		   		StartUp.shutDownTripleStore(agStore);
		   	}
	   	}
   }


}

/*
 * (c) Copyright 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */