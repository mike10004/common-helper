/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.github.mike10004.common.dbhelp;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Supplier that declares an SQL exception on the supply method.
 * @author mchaberski
 * @param <T> the type of object to be supplied
 */
public interface SqlSupplier<T> {

    /**
     * Retrieves an instance of the appropriate type. The returned object may or
     * may not be a new instance, depending on the implementation.
     *
     * @return an instance of the appropriate type
     * @throws java.sql.SQLException if getting the instance fails
     */
    T get() throws SQLException;

    /**
     * Supplier that memoizes its delegate. 
     * This is copied from Guava: see Suppliers.memoize method.
     * @param <T>  the type of object to be memo-supplied
     * @author the Guava team
     */
    public static class MemoizingSqlSupplier<T> implements SqlSupplier<T> {

        final SqlSupplier<T> delegate;
        transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        transient T value;

        public MemoizingSqlSupplier(SqlSupplier<T> delegate) {
            this.delegate = checkNotNull(delegate, "delegate");
        }

        @SuppressWarnings("Duplicates")
        @Override
        public T get() throws SQLException {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "MemoizedSqlSupplier{" + delegate + "}";
        }

        private static final long serialVersionUID = 0;
    }
}
