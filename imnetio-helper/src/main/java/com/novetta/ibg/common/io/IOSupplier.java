/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.novetta.ibg.common.io;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * @author mchaberski
 */
public interface IOSupplier<T> {
    
    T get() throws IOException;

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
    public static final class MemoizingIOSupplier<T> implements IOSupplier<T> {

        private final IOSupplier<T> delegate;
        private transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        private transient T value;

        MemoizingIOSupplier(IOSupplier<T> delegate) {
            this.delegate = checkNotNull(delegate, "delegate");
        }

        @Override
        public T get() throws IOException {
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
            return "MemoizedIOSupplier{" + delegate + "}";
        }

    }

}
