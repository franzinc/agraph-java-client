/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.web;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closer;

public class AGTestServlet extends HttpServlet {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final long serialVersionUID = 770497520167657818L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Closer c = new Closer();
        try {
            Context initCtx = c.closeLater(new InitialContext());
            Context envCtx = (Context) c.closeLater(initCtx.lookup("java:comp/env"));
            AGConnPool pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
            AGRepositoryConnection conn = c.closeLater(pool.borrowConnection());
            
            resp.getWriter().println("size=" + conn.size());
            resp.getWriter().flush();
        } catch (Exception e) {
            throw new ServletException(e);
        } finally{
            c.close();
        }
    }
    
    @Override
    public void destroy() {
        Closer c = new Closer();
        AGConnPool pool = null;
        try {
        	Context initCtx = c.closeLater(new InitialContext());
        	Context envCtx = (Context) c.closeLater(initCtx.lookup("java:comp/env"));
        	pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
        	pool.close();
        } catch (Exception e) {
        	RuntimeException re = new RuntimeException("Error closing the AGConnPool: " + pool, e);
        	log.error(re.getMessage(), re);
            throw re;
        } finally{
            c.close();
        }
    }
    
}
