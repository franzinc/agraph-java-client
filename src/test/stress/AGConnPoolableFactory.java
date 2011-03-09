package test.stress;

import org.apache.commons.pool.PoolableObjectFactory;
import org.openrdf.repository.RepositoryException;

import test.Closer;

import com.franz.agraph.repository.AGRepositoryConnection;

/**
 * 
 *
 */
public abstract class AGConnPoolableFactory
extends Closer
implements PoolableObjectFactory {

	@Override
	public Object makeObject() throws Exception {
		return makeConnection();
	}

	protected abstract AGRepositoryConnection makeConnection() throws RepositoryException;

	@Override
	public void activateObject(Object obj) throws Exception {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		conn.rollback();
	}

	@Override
	public void destroyObject(Object obj) throws Exception {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		conn.close();
	}

	@Override
	public void passivateObject(Object obj) throws Exception {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		if (conn.isAutoCommit()) {
			conn.rollback();
		}
	}

	@Override
	public boolean validateObject(Object obj) {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		try {
			conn.ping();
			return true;
		} catch (RepositoryException e) {
			return false;
		}
	}
	
}
