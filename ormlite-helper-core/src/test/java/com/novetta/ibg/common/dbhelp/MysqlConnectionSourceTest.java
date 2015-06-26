/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.dbhelp;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.MysqlDatabaseType;
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
public class MysqlConnectionSourceTest {
    
    public MysqlConnectionSourceTest() {
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
        MysqlConnectionSource cs = new MysqlConnectionSource();
        DatabaseType expected = new MysqlDatabaseType();
        DatabaseType actual = cs.forceGetDatabaseType();
        
        System.out.format("expect %s actual %s%n", expected, actual);
        assertEquals(expected.getClass(), actual.getClass());
    }
    
}
