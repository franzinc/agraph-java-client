/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.web;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AGTestServlet extends HttpServlet {

    private static final long serialVersionUID = 770497520167657818L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Closer c = new Closer()) {
            Context initCtx = new InitialContext();
            c.closeLater(initCtx::close);
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            c.closeLater(envCtx::close);
            AGConnPool pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
            AGRepositoryConnection conn = c.closeLater(pool.borrowConnection());

            resp.getWriter().println("size=" + conn.size());
            resp.getWriter().flush();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
        AGConnPool pool = null;
        try (Closer c = new Closer()) {
            Context initCtx = new InitialContext();
            c.closeLater(initCtx::close);
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            c.closeLater(envCtx::close);
            pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
            pool.close();
        } catch (Exception e) {
            RuntimeException re = new RuntimeException("Error closing the AGConnPool: " + pool, e);
            log.error(re.getMessage(), re);
            throw re;
        }
    }
}
