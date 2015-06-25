/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.ormlitehelper;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author mchaberski
 */
public class DatabaseTests {

    private DatabaseTests() {
        
    }
    
    public static void testBasicInsertionAndRetrieval(DatabaseContext db) throws SQLException {
            
            Customer c = new Customer();
            c.name = "Jason";
            c.address = "123 Anywhere La";
            db.getDao(Customer.class).create(c);
            
            Order order = new Order();
            order.customer = c;
            order.productName = "Widgets";
            order.quantity = 4;
            db.getDao(Order.class).create(order);
            System.out.println("created:\n" + c + "\n" + order);
            Customer retrievedC = db.getDao(Customer.class).queryForAll().get(0);
            Order retrievedOrder = db.getDao(Order.class).queryForAll().get(0);
            System.out.println("retrieved: \n" + retrievedC + "\n" + retrievedOrder);
            assertEquals(c, retrievedC);
            
            assertEquals(order, retrievedOrder);
        
    }
}
