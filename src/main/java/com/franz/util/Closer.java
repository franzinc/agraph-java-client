/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Use this class to collect objects that should be closed at a later time.
 * <p>
 * Subclasses can override {@code handleCloseException()} to decide what should
 * happen with exceptions thrown during close. The default behavior is to
 * log a warning and ignore the exception.
 *
 * @since AG v4.3.3
 */
public class Closer implements AutoCloseable {
    private final static Logger log = LoggerFactory.getLogger(Closer.class);

    private final List<AutoCloseable> toClose = Collections.synchronizedList(new ArrayList<>());

    /**
     * Add a resource to be closed with {@link #close()}.
     * <p>
     * Resources will be closed in reverse registration order.
     *
     * @param <Obj> The type of objects to be closed by this Closer
     * @param o     Object to note for later closing
     * @return Obj  always returns o
     */
    public <Obj extends AutoCloseable>
    Obj closeLater(Obj o) {
        toClose.add(o);
        return o;
    }

    /**
     * Remove object from collection so close will not be called later.
     *
     * @param o Object to be removed from this Closer
     * @return boolean  Returns true if o was removed, else false
     * @see #closeLater(AutoCloseable)
     */
    public boolean remove(AutoCloseable o) {
        boolean removed = false;
        if (o != null) {
            while (toClose.remove(o)) {
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Closes all resources registered with {@link #closeLater(AutoCloseable)}.
     */
    @Override
    public void close() {
        Collections.reverse(toClose);
        // Can't just call close(toClose) - that would cause
        // a ConcurrentModificationException.
        toClose.forEach(this::closeQuietly);
        toClose.clear();
    }

    /**
     * Close given objects immediately, will not be closed "later".
     *
     * @param objects The collection of objects to close
     */
    public void close(Collection<? extends AutoCloseable> objects) {
        objects.forEach(this::close);
    }

    @Override
    public String toString() {
        return "{" + super.toString()
                + " toClose=" + toClose.size()
                + "}";
    }

    /**
     * Deal with exceptions thrown when closing an object.
     * Subclass may override, default behavior is to log.warn and return the object.
     *
     * @param o The object that caused e to be thrown
     * @param e The exception thrown when o was closed
     */
    protected void handleCloseException(Object o, Throwable e) {
        log.warn("ignoring error with close: " + o, e);
    }

    /**
     * Closes the argument, passing all exceptions to handleCloseException().
     *
     * @param o Object to be closed. Might be null.
     */
    private void closeQuietly(final AutoCloseable o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
                handleCloseException(o, e);
            }
        }
    }

    /**
     * Close an object immediately, will not be closed "later".
     * <p>
     * If the object is registered on the list of resources to be closed
     * later by this Closer, it will be removed from that list.
     *
     * @param <CloseableType> The type of objects that can be closed by this Closer
     * @param o               The object being closed
     * @return Obj  o is always returned.
     */
    public <CloseableType extends AutoCloseable>
    CloseableType close(CloseableType o) {
        closeQuietly(o);
        remove(o);
        return o;
    }
}
