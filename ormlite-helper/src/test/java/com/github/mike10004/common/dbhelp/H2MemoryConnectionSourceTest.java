/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class H2MemoryConnectionSourceTest {
    
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

    @Test
    public void testKeepContent() throws Exception {
        System.out.println("testKeepContent");

        H2MemoryConnectionSource csTrash = new H2MemoryConnectionSource(false);
        H2MemoryConnectionSource csKeep = new H2MemoryConnectionSource(true);
        TableUtils.createTable(csTrash, Customer.class);
        TableUtils.createTable(csKeep, Customer.class);
        DaoManager.createDao(csTrash, Customer.class).create(new Customer("Eric", "Anywhere"));
        DaoManager.createDao(csKeep, Customer.class).create(new Customer("Eric", "Anywhere"));

        csTrash.close();
        csKeep.close();

        List<Customer> keepCustomers = DaoManager.createDao(csKeep, Customer.class).queryForAll();
        System.out.println("keep customers: " + keepCustomers);
        assertEquals("1 customer in keep connection", 1L, keepCustomers.size());

        try {
            Dao<Customer, ?> dao = DaoManager.createDao(csTrash, Customer.class);
            assertEquals("0 customers in trash connection source", 0L, dao.countOf());
            fail("table should not exist in trash connection");
        } catch (org.h2.jdbc.JdbcSQLException expected) {
            System.out.println("as expected, table not found in trash connection: " + expected);
        }

    }

}
