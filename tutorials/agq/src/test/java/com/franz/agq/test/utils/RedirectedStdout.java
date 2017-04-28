package com.franz.agq.test.utils;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A closable that causes stdout to be redirected
 * to a given stream from the moment it is created
 * to the moment when it's closed.
 *
 * Intended for use with try-with-resources, like this:
 * <pre>{@code
 * final ByteArrayOutputStream bos = new ByteArrayOutputStream();
 * try (RedirectStdout unused = new RedirectStdout(bos) {
 *     // ... do something that prints to stdout ...
 * }
 * // Inspect 'bos'
 * }</pre>
 */
public class RedirectedStdout implements Closeable {
    private final PrintStream originalStdout;

    public RedirectedStdout(final OutputStream output) {
        originalStdout = System.out;
        System.setOut(new PrintStream(output));
    }

    @Override
    public void close() {
        System.setOut(originalStdout);
    }
}
