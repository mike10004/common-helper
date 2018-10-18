package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.H2DatabaseType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class H2ConnectionSourceTest {
    
    @Test
    public void testForceGetDatabaseType() {
        
        System.out.println("testForceGetDatabaseType");
        H2ConnectionSource cs = new H2ConnectionSource() {

            @Override
            protected String getProtocol() {
                return "something";
            }

            @Override
            protected String getSchema() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        DatabaseType expected = new H2DatabaseType();
        System.out.println("expected: " + expected);
        DatabaseType actual = cs.forceGetDatabaseType();
        System.out.println("  actual: " + actual);
        assertEquals(expected.getClass().getName(), actual.getClass().getName());
    }
    
}
