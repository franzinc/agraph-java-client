/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.ssl;

import org.junit.Test;

import test.AGAbstractTest;

import com.franz.agraph.repository.AGServer;

/**
 * Tests X.509 authentication
 * 
 * Set SSL directives in the server's agraph.cfg file, e.g:
 * 
 * SSLPort 10036
 * SSLClientAuthRequired true
 * SSLClientAuthUsernameField CN
 * SSLCertificate /path/agraph.cert
 * SSLCAFile /path/ca.cert
 * 
 */
public class SSLTests extends AGAbstractTest 
{
    @Test
    public void x509test() throws Exception {
    	String ks = System.getProperty("javax.net.ssl.keyStore", "src/test/ssl/test.p12");
    	System.setProperty("javax.net.ssl.keyStore", ks);
    	System.setProperty("javax.net.ssl.keyStorePassword", "foobar");
    	System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
    	String ts = System.getProperty("javax.net.ssl.trustStore", "src/test/ssl/ts");
    	System.setProperty("javax.net.ssl.trustStore", ts);

    	server = new AGServer(findSslServerUrl());
    	server.listCatalogs();
   }
}
