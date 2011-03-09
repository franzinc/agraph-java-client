package test.stress;

import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closeable;

public class AGConnPool <ObjectPoolType extends ObjectPool>
implements ObjectPool, Closeable {
	
	public final ObjectPoolType delegate;

	public AGConnPool(ObjectPoolType delegate) {
		this.delegate = delegate;
	}

	@Override
	public void addObject() throws Exception, IllegalStateException, UnsupportedOperationException {
		delegate.addObject();
	}

	@Override
	public Object borrowObject() throws Exception, NoSuchElementException, IllegalStateException {
		return delegate.borrowObject();
	}

	public AGRepositoryConnection borrowConnection() throws RepositoryException {
		try {
			return (AGRepositoryConnection) delegate.borrowObject();
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void clear() throws Exception, UnsupportedOperationException {
		delegate.clear();
	}

	@Override
	public void close() throws Exception {
		delegate.close();
	}

	@Override
	public int getNumActive() throws UnsupportedOperationException {
		return delegate.getNumActive();
	}

	@Override
	public int getNumIdle() throws UnsupportedOperationException {
		return delegate.getNumIdle();
	}

	@Override
	public void invalidateObject(Object obj) throws Exception {
		delegate.invalidateObject(obj);
	}

	@Override
	public void returnObject(Object obj) throws Exception {
		delegate.returnObject(obj);
	}

	@Override
	@Deprecated
	public void setFactory(PoolableObjectFactory obj) throws IllegalStateException, UnsupportedOperationException {
		delegate.setFactory(obj);
	}

	public void addObjects(int n) throws Exception {
		for (int i = 0; i < n; i++) {
			addObject();
		}
	}
	
}
