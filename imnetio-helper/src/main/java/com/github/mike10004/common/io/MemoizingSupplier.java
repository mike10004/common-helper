package com.github.mike10004.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Supplier that retains a reference to the object it first supplies and then returns that
 * object every subsequent time.
 * <pre>
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * </pre>
 * @param <T>
 *
 */
final class MemoizingSupplier<T, X extends Throwable> implements CheckedSupplier<T, X> {

    private final CheckedSupplier<T, ? extends X> delegate;
    private transient volatile boolean initialized;
    // "value" does not need to be volatile; visibility piggy-backs
    // on volatile read of "initialized".
    private transient T value;

    public MemoizingSupplier(CheckedSupplier<T, ? extends X> delegate) {
        this.delegate = checkNotNull(delegate, "delegate");
    }

    @SuppressWarnings("Duplicates")
    @Override
    public T get() throws X {
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
        return toString(CheckedSupplier.class, delegate);
    }

    static String toString(Class<?> implementation, CheckedSupplier<?, ?> delegate) {
        return "Memoizing" + implementation.getSimpleName() + "{" + delegate + "}";
    }

}
