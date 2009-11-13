
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

package com.franz.agjena.query;

import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.util.FileManager;


public class AllegroGraphQueryFactory {

    // ---- static methods for making a query
    
    /** Create a SPARQL query from the given string by calling the parser.
     *
     * @param queryString      The query string
     * @throws QueryException  Thrown when a parse error occurs
     */
    
    static public AllegroGraphQuery create(String queryString)
    {
    	AllegroGraphQuery query = new AllegroGraphQuery() ;
        query.setQueryString(queryString);
        return query ;
    }

//    /** Create a query from the given string by calling the parser.
//     *
//     * @param queryString      The query string
//     * @param langURI          URI for the syntax
//     * @throws QueryException  Thrown when a parse error occurs
//     */
//    
//    static public Query create(String queryString, Syntax langURI)
//    {
//        return create(queryString, null, langURI) ;
//    }
//
//    /** Create a query from the given string by calling the parser.
//     *
//     * @param queryString      The query string
//     * @param baseURI          Base URI
//     * @throws QueryException  Thrown when a parse error occurs
//     */
//    
//    static public Query create(String queryString, String baseURI)
//    {
//        Query query = new Query() ;
//        query.setQueryString(queryString);
//        return query ;
//
//    }
//    
//    /** Create a query from the given string by calling the parser.
//    *
//    * @param queryString      The query string
//    * @param baseURI          Base URI
//    * @param querySyntax      URI for the syntax
//    * @throws QueryException  Thrown when a parse error occurs
//    */
//   
//   static public Query create(String queryString, String baseURI, Syntax querySyntax)
//   {
//       Query query = new Query() ;
//       query.setQueryString(queryString);
//       return query ;
//       
//   }
   
    /**
     * Make a query - nothing inside
     */
    static public AllegroGraphQuery make() { return new AllegroGraphQuery() ; }

    /**
     * Make a query from another one by deep copy (a clone).
     * The returned query will be .equals to the original.
     * The returned query can be mutated without changing the
     * original (at which point it will stop being .equals)
     * 
     * @param originalQuery  The query to clone.
     *   
     */

    static public AllegroGraphQuery create(AllegroGraphQuery originalQuery)
    {
        return create(originalQuery.getQueryString());
    }

    /**
     * Read a SPARQL query from a file.
     * 
     * @param url
     *            URL (file: or http: or anything a FileManager can handle)
     * @return A new query object
     */
    static public AllegroGraphQuery read(String url)
    {
        return read(url, null, null, null) ;
    }

    /** Read a SPARQL query from a file.
     * 
     * @param url            URL (file: or http: or anything a FileManager can handle)
     * @param baseURI        BaseURI for the query
     * @return               A new query object 
     */
    static public AllegroGraphQuery read(String url, String baseURI)
    {
        return read(url, null, baseURI, null) ;
    }

    /** Read a query from a file.
     * 
     * @param url            URL (file: or http: or anything a FileManager can handle)
     * @param baseURI        BaseURI for the query
     * @param langURI        Query syntax
     * @return               A new query object 
     */
    static public AllegroGraphQuery read(String url, String baseURI, Syntax langURI)
    {
        return read(url, null, baseURI, langURI) ;
    }

    /** Read a query from a file.
     * 
     * @param url            URL (file: or http: or anything a FileManager can handle)
     * @param filemanager    Optional filemanager
     * @param baseURI        BaseURI for the query
     * @param langURI        Query syntax
     * @return               A new query object 
     */
    static public AllegroGraphQuery read(String url, FileManager filemanager, String baseURI, Syntax langURI)
    {
        if ( filemanager == null )
            filemanager = FileManager.get() ;
        String qStr = filemanager.readWholeFileAsUTF8(url) ;
        if ( baseURI == null )
            baseURI = url ;
        if ( langURI == null )
            langURI = Syntax.guessQueryFileSyntax(url) ;
        // for now, we drop baseURI and langURI on the floor; AG parser doesn't
        // recognise them
        return create(qStr) ;
    }
    
 
}


/*
 *  (c) Copyright 2004, 2005, 2006, 2007, 2008 Hewlett-Packard Development Company, LP
 *  All rights reserved.
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

