/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.H2DatabaseType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class H2ConnectionSourceTest {
    
    public H2ConnectionSourceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

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
