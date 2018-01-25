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
package com.github.mike10004.common.io;

import java.io.IOException;

/**
 * Supplier that declares an I/O exception on the supply method.
 * @param <T> the type of object to be supplied
 * @author mchaberski
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
