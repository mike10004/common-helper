package com.github.mike10004.common.dbhelp;

import com.github.mike10004.common.dbhelp.SqlSupplier.MemoizingSqlSupplier;
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
            @SuppressWarnings("RedundantThrows")
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
