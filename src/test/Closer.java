package test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.franz.util.Closeable;

/**
 * Extend this class to add easy ability to safely close various resources.
 * 
 * TODO: move class to com.franz.util.
 */
public abstract class Closer implements Closeable {

	private final List toClose = new LinkedList();
	
	/**
	 * Add a resource to be closed with {@link #close()}.
	 */
    public <Obj extends Object>
    Obj closeLater(Obj o) {
		toClose.add(o);
		return o;
    }

	/**
	 * Must be called in a finally block, to close all resources
	 * added with closeLater().
	 */
    @Override
	public void close() {
		while (toClose.isEmpty() == false) {
			close( toClose.get(0) );
		}
	}
    
	/**
	 * Close all objects immediately, will not be closed "later".
	 */
    public Collection closeAll(Collection objects) {
    	for (Object object : objects) {
			close(object);
		}
    	return null;
    }

	/**
	 * Close an object immediately, will not be closed "later".
	 */
    public <Obj extends Object>
    Obj close(Obj o) {
    	test.Util.close(o);
		while (toClose.remove(o)) {
		}
        return null;
    }
    
}
