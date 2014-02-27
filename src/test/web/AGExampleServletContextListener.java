/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.web;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.franz.agraph.pool.AGConnPool;
import com.franz.util.Closer;

public class AGExampleServletContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent event) {
        Closer c = new Closer();
        try {
            Context initCtx = c.closeLater(new InitialContext());
            Context envCtx = (Context) c.closeLater(initCtx.lookup("java:comp/env"));
            AGConnPool pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
            pool.close();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally{
            c.close();
        }
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
	}
	
}
