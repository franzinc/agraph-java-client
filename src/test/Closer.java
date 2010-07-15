package test;

import info.aduna.iteration.CloseableIteration;

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
	public void close() {
		while (toClose.isEmpty() == false) {
			Object o = toClose.get(0);
			close(o);
			while (toClose.remove(o)) {
			}
		}
	}

	/**
	 * TODO: move to com.franz.util.Util
	 */
	public static <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> close(CloseableIteration<Elem, Exc> o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
                System.err.println("ignoring error with close:" + e);
                e.printStackTrace();
            }
        }
        return null;
    }

	/**
	 * TODO: move to com.franz.util.Util
	 */
    public static <Obj extends Object>
    Obj close(Obj o) {
        if (o instanceof Closeable) {
            com.franz.util.Util.close((Closeable)o);
        } else if (o instanceof java.io.Closeable) {
            com.franz.util.Util.close((java.io.Closeable)o);
        } else if (o instanceof CloseableIteration) {
        	close((CloseableIteration)o);
        } else if (o != null) {
            try {
                o.getClass().getMethod("close").invoke(o);
            } catch (Exception e) {
                System.err.println("ignoring error with close:" + e);
                e.printStackTrace();
            }
        }
        return null;
    }
    
}
