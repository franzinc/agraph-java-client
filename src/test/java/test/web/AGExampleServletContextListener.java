/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.web;

import com.franz.agraph.pool.AGConnPool;
import com.franz.util.Closer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AGExampleServletContextListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try (final Closer c = new Closer()) {
            Context initCtx = new InitialContext();
            c.closeLater(initCtx::close);
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            c.closeLater(envCtx::close);
            AGConnPool pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
            pool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
    }

}
