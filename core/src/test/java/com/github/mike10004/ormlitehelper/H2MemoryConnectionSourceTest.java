/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.ormlitehelper;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
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
public class H2MemoryConnectionSourceTest {
    
    public H2MemoryConnectionSourceTest() {
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
    public void testAnonymousSchema() throws SQLException {
        System.out.println("testAnonymousSchema");
        
        H2MemoryConnectionSource cs = new H2MemoryConnectionSource();
        System.out.println("h2 mem connection source schema: " + cs.getSchema());
        DatabaseContext db = new DefaultDatabaseContext(cs);
        try {
            db.getTableUtils().createAllTables(ImmutableList.of(Customer.class, Order.class));
            DatabaseTests.testBasicInsertionAndRetrieval(db);
        } finally {
            db.closeConnections(true);
        }
    }
}
