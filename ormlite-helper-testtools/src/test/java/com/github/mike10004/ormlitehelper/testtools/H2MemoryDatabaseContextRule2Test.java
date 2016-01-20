package com.github.mike10004.ormlitehelper.testtools;

import com.novetta.ibg.common.dbhelp.DatabaseContext;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by mchaberski on 1/19/16.
 */
public class H2MemoryDatabaseContextRule2Test {

    @Rule
    public H2MemoryDatabaseContextRule databaseContextRule = new H2MemoryDatabaseContextRule(new CreateTablesOperation(Widget.class));

    @Test
    public void testTablesCreated() throws Exception {
        System.out.println("testTablesCreated");

        DatabaseContext db = databaseContextRule.getDatabaseContext();
        try {
            assertEquals("widget count", 0L, db.getDao(Widget.class).countOf());
        } finally {
            db.closeConnections(false);
        }
    }

}