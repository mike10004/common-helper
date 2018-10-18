package com.github.mike10004.common.io;

import java.io.IOException;

/**
 * Supplier that declares an I/O exception on the supply method.
 *
 * @param <T> the type of object to be supplied
 */
public interface IOSupplier<T> extends CheckedSupplier<T, IOException> {
    
    static <T> IOSupplier<T> memoize(IOSupplier<T> delegate) {
        CheckedSupplier<T, IOException> memoized = CheckedSupplier.memoize(delegate);
        return new IOSupplier<T>() {
            @Override
            public T get() throws IOException {
                return memoized.get();
            }
            @Override
            public String toString() {
                return MemoizingSupplier.toString(IOSupplier.class, delegate);
            }
        };
    }
}
