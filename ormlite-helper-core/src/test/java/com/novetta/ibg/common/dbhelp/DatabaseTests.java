/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.dbhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.sql.SQLException;
import java.util.Collection;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author mchaberski
 */
public class DatabaseTests {

    private DatabaseTests() {
    }

    public static void testBasicInsertionAndRetrieval(DatabaseContext db) throws SQLException {
        Collection<Object> objects = insertCustomersAndOrders(db);
        Customer customer = Iterables.filter(objects, Customer.class).iterator().next();
        Order order = Iterables.filter(objects, Order.class).iterator().next();
        Customer retrievedCustomer = db.getDao(Customer.class).queryForAll().get(0);
        Order retrievedOrder = db.getDao(Order.class).queryForAll().get(0);
        System.out.println("retrieved: \n" + retrievedCustomer + "\n" + retrievedOrder);
        assertEquals(customer, retrievedCustomer);
        assertEquals(order, retrievedOrder);
    }
    
    public static Collection<Object> insertCustomersAndOrders(DatabaseContext db) throws SQLException {
        Customer customer = new Customer();
        customer.name = "Jason";
        customer.address = "123 Anywhere La";
        db.getDao(Customer.class).create(customer);

        Order order = new Order();
        order.customer = customer;
        order.productName = "Widgets";
        order.quantity = 4;
        db.getDao(Order.class).create(order);
        System.out.println("created:\n" + customer + "\n" + order);
        return ImmutableList.of(customer, order);
    }
}
