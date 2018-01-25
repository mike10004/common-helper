package com.github.mike10004.common.io;

import com.google.common.base.Supplier;

/**
 * Interface of a supplier whose getter may throw an exception.
 * @param <T> type of the supplied instance
 * @param <X> type of the exception that may be thrown
 * @see java.util.function.Supplier
 */
public interface CheckedSupplier<T, X extends Throwable>  {

    /**
     * Gets a value.
     * @return the value
     * @throws X if an exception is to be thrown
     */
    T get() throws X;

    /**
     * Memoizes a checked supplier.
     * @param delegate the delegate
     * @param <T> value type
     * @param <X> exception type
     * @return a memoizing supplier
     * @see com.google.common.base.Suppliers#memoize(Supplier)
     */
    static <T, X extends Throwable> CheckedSupplier<T, X> memoize(CheckedSupplier<T, X> delegate) {
        return new MemoizingSupplier<>(delegate);
    }
}
