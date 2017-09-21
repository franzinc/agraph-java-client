package com.franz.util;

import java.io.Closeable;

/**
 * A closeable that doesn't throw any checked exceptions on close.
 */
public interface Ctx extends Closeable {
    @Override
    void close();
}
