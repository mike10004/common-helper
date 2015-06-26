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
package com.novetta.ibg.common.dbhelp;

import com.novetta.ibg.common.dbhelp.SqlSupplier.MemoizingSqlSupplier;
import java.sql.SQLException;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlSupplierTest {
    
    public SqlSupplierTest() {
    }

    @Test
    public void testMemoizingSqlSupplier() throws Exception {
        System.out.println("testMemoizingSqlSupplier");
        SqlSupplier<Object> supplier = new SqlSupplier<Object>() {
            @Override
            public Object get() throws SQLException {
                return new Object();
            }
        };

        Object first = supplier.get();
        Object second = supplier.get();
        System.out.format("first = %s, second = %s%n", first, second);
        assertNotSame("expect supplied object to be unique", first, second);
        
        SqlSupplier<Object> memoizedSupplier = new MemoizingSqlSupplier<>(supplier);
        Object firstMemoized = memoizedSupplier.get();
        Object secondMemoized = memoizedSupplier.get();
        System.out.format("memoized first = %s, second = %s%n", firstMemoized, secondMemoized);
        assertSame("expect memoized supplier to supply same object", firstMemoized, secondMemoized);
    }
    
}
