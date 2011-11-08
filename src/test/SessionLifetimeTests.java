/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGRepositoryConnection;

public class SessionLifetimeTests extends AGAbstractTest {

	public static int connLife = 10; 
	public static int conn2Life = 5; 
	public static int conn3Life = 30; 
    @Test
    @Category(TestSuites.Prepush.class)
    public void sessionLifetime_rfe9634() throws Exception {
    	Assert.assertEquals("expected default session lifetime ", AGHttpRepoClient.getDefaultSessionLifetime(),conn.getSessionLifetime());
		conn.setSessionLifetime(connLife);
		Assert.assertEquals("expected lifetime "+connLife, connLife, conn.getSessionLifetime());
		AGAbstractRepository repo = conn.getRepository();
		AGRepositoryConnection conn2 = repo.getConnection();
		Assert.assertEquals("expected default session lifetime ", AGHttpRepoClient.getDefaultSessionLifetime(),conn2.getSessionLifetime());
		conn2.setSessionLifetime(conn2Life);
		Assert.assertEquals("expected lifetime "+conn2Life, conn2Life, conn2.getSessionLifetime());
		Assert.assertEquals("expected lifetime "+connLife, connLife, conn.getSessionLifetime());
		AGHttpRepoClient.setDefaultSessionLifetime(conn3Life);
		Assert.assertEquals("expected default session lifetime "+conn3Life, conn3Life, AGHttpRepoClient.getDefaultSessionLifetime());
		AGRepositoryConnection conn3 = repo.getConnection();
		Assert.assertEquals("expected session lifetime "+conn3Life, conn3Life, conn3.getSessionLifetime());
		Assert.assertEquals("expected lifetime "+conn2Life, conn2Life, conn2.getSessionLifetime());
		Assert.assertEquals("expected lifetime "+connLife, connLife, conn.getSessionLifetime());
		
		conn.setAutoCommit(false);
		conn2.setAutoCommit(false);
		Thread.sleep((conn2Life+2)*1000);
		try {
			conn2.size();
			Assert.fail("expected conn2 session to expire");
		} catch (RepositoryException e) {
		}
		conn.ping(); // extends the life of conn's session
		Thread.sleep((connLife-conn2Life)*1000);
		conn.size(); // fails if ping doesn't work
		try {
			conn2.close();
			Assert.fail("closing expired conn2 session should throw an exception");
		} catch (RepositoryException e) {
			// TODO: want a more specific exception here?
		}
		conn3.close();
		// conn is closed in test tearDown
    }

}
