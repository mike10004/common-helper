package com.github.mike10004.common.dbhelp;

import java.sql.SQLException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;

/**
 * Test that confirms that we are using unique schemas on each @Test method.
 */
public class MysqlConnectionSourceRuleIT {
    
    @Rule
    public MysqlConnectionSourceRule connectionSourceRule = new MysqlConnectionSourceRule(IntegrationTests.getMysqlPort());

    private DatabaseContext context;
    
    @Before
    public void setUp() throws SQLException {
        context = new DefaultDatabaseContext(connectionSourceRule.getConnectionSource());
        context.getTableUtils().createAllTables(Arrays.asList(Customer.class, Order.class));
    }
    
    @After
    public void tearDown() throws SQLException {
        context.closeConnections(false);
    }
    
    @Test
    public void testFirst() throws Exception {
        System.out.println("testFirst");
        Customer customer = new Customer();
        customer.name = "Alice";
        customer.address = "123 Anystreet Lane, Someplace, ZX 01929";
        context.getDao(Customer.class).create(customer);
        
        long count = context.getDao(Customer.class).countOf();
        assertEquals(1L, count);
        
    }
    
    @Test
    public void testSecond() throws Exception {
        System.out.println("testSecond");

        Customer customer = new Customer();
        customer.name = "Bob";
        customer.address = "456 Anystreet Lane, Somewhere, XZ 97338";
        context.getDao(Customer.class).create(customer);
    
        
        long count = context.getDao(Customer.class).countOf();
        assertEquals(1L, count);
    }
    
}
