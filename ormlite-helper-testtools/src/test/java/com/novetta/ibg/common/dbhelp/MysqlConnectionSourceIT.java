/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.dbhelp;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.openide.util.RequestProcessor;

/**
 * Test for MysqlConnectionSource class in core project. This test has 
 * to be in this package because it utilizes the MysqlConnectionSourceRule
 * class in the testtools project.
 * @author mchaberski
 */
public class MysqlConnectionSourceIT {
    
    @Rule
    public MysqlConnectionSourceRule connectionSourceRule = new MysqlConnectionSourceRule(IntegrationTests.getMysqlPort(), 
            new Function<ConnectionParams, MysqlConnectionSource>() {
                @Override
                public MysqlConnectionSource apply(ConnectionParams t) {
                    return new CountingConnectionSource(t);
                }
    });
    
    public static class CountingConnectionSource extends MysqlConnectionSource {

        private transient final AtomicInteger numPrepareCalls = new AtomicInteger(0);
        
        public CountingConnectionSource(ConnectionParams connectionParams) {
            super(connectionParams);
        }

        @Override
        protected void prepare() throws SQLException {
            super.prepare();
            numPrepareCalls.incrementAndGet();
        }
        
        public int getNumPrepareCalls() {
            return numPrepareCalls.get();
        }
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void testReadingAndWritingConcurrently() 
            throws SQLException, InterruptedException, ExecutionException {
        System.out.println("testReadingAndWritingConcurrently");
        int numThreads = 20;
        
        int numReaders = 500, numWriters = 50;
        AtomicInteger numInsertedCount = new AtomicInteger(), taskCount = new AtomicInteger();    
        
        boolean clean = false;
        List<DbTask> tasks = new ArrayList<>();
        int count;
        CountingConnectionSource cs = (CountingConnectionSource) connectionSourceRule.getConnectionSource();
        DatabaseContext db = new DefaultDatabaseContext(cs);
        List<Customer> customers = Collections.synchronizedList(new ArrayList<Customer>());
        try {
            db.getTableUtils().createTable(Customer.class);
            for (int i = 0; i < numWriters; i++) {
                int numToInsert = (i + 1) * 10;
                DbWriter writer = new DbWriter(db, numToInsert, customers, taskCount, numInsertedCount);
                tasks.add(writer);
            }
            for (int i = 0; i < numReaders; i++) {
                tasks.add(new DbReader(db, taskCount));
            }
            Collections.shuffle(tasks);
            
            RequestProcessor rp = new RequestProcessor(MysqlConnectionSourceIT.class.getName(), numThreads);
            
            System.out.println("invoking all tasks using " + numThreads + " threads");
            List<Future<Void>> futures = rp.invokeAll(tasks);
            System.out.println("awaiting termination...");
            for (Future future : futures) {
                future.get();
            }
            
            count = (int) db.getDao(Customer.class).countOf();
            clean = true;
        } finally {
            db.closeConnections(!clean);
        }

        System.out.println("all tasks terminated");
        System.out.println("max connections ever used: " + cs.getMaxConnectionsEverUsed());
        System.out.println(cs.getOpenCount() + " opened");
        System.out.println(cs.getCloseCount() + " closed");
        
        assertTrue("expect max connections ever used to be no more than num threads", 
                cs.getMaxConnectionsEverUsed() <= numThreads);
        assertEquals("expected num opened == num closed", cs.getOpenCount(), cs.getCloseCount());
        
        System.out.println(numInsertedCount.get() + " inserted; collection.size = " + customers.size());
        assertEquals(numInsertedCount.get(), customers.size());
        assertEquals(numInsertedCount.get(), count);
        System.out.println(cs.getNumPrepareCalls() + " prepare() calls");
        assertEquals(1, cs.getNumPrepareCalls());
    }
    
    public static abstract class DbTask implements Callable<Void> {
        
        private transient final DatabaseContext db;

        public DbTask(DatabaseContext db) {
            this.db = db;
        }
        
        protected abstract void doWork(DatabaseContext db) throws SQLException;

        @Override
        public Void call() throws Exception {
            doWork(db);
            return (Void)null;
        }
        
    }
    
    public static class DbReader extends DbTask {

        private final AtomicInteger taskCount;
        
        public DbReader(DatabaseContext db, AtomicInteger taskCount) {
            super(db);
            this.taskCount = checkNotNull(taskCount);
        }

        @Override
        protected void doWork(DatabaseContext db) throws SQLException {
            for (Class<?> entity : ImmutableList.<Class<?>>of(Customer.class)) {
                List<?> list = db.getDao(entity).queryForAll();
                System.out.format("%5d %08x: read %d%n", taskCount.incrementAndGet(), System.identityHashCode(this), list.size());
            }
        }
        
    }
    
    public static class DbWriter extends DbTask {

        protected Random random = new Random(DbWriter.class.hashCode());
        private final int numToInsert;
        private final Collection<Customer> customers;
        private final AtomicInteger numInsertedCount, taskCount;
        
        public DbWriter(DatabaseContext db, int numToInsert, Collection<Customer> customers, AtomicInteger taskCount, AtomicInteger numInsertedCount) {
            super(db);
            this.numToInsert = numToInsert;
            this.customers = checkNotNull(customers);
            this.taskCount = checkNotNull(taskCount);
            this.numInsertedCount = checkNotNull(numInsertedCount);
        }

        @Override
        protected void doWork(final DatabaseContext db) throws SQLException {
            db.getTransactionManager().callInTransaction(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    List<Customer> subset = new ArrayList<>();
                    for (int i = 0; i < numToInsert; i++) {
                        Customer c = construct(Customer.class);
                        db.getDao(Customer.class).create(c);
                        subset.add(c);
                    }         
                    customers.addAll(subset);
                    return (Void) null;
                }
            });
            numInsertedCount.addAndGet(numToInsert);
            System.out.format("%5d %08x: inserted %d%n", taskCount.incrementAndGet(), System.identityHashCode(this), numToInsert);
        }
        
        protected <T> T construct(Class<T> entity) {
            if (Customer.class.equals(entity)) {
                Customer c = new Customer();
                c.address = newRandomString();
                c.name = newRandomString();
                return (T) c;
            } else {
                throw new IllegalArgumentException("not supported yet: " + entity);
            }
        }
        
        protected String newRandomString() {
            int numLongs = 10;
            StringBuilder sb = new StringBuilder(8 * numLongs); 
            for (int i = 0; i < numLongs; i++) {
                long value = Math.abs(random.nextLong());
                sb.append(String.format("%08x", value));
            }
            String s = sb.toString();
            return s;
        }
    }
}
