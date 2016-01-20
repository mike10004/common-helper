package com.github.mike10004.ormlitehelper.testtools;

import com.novetta.ibg.common.dbhelp.DatabaseContext;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by mchaberski on 1/19/16.
 */
public class H2MemoryDatabaseContextRule1Test {

    @Rule
    public H2MemoryDatabaseContextRule databaseContextRule = new H2MemoryDatabaseContextRule();

    @Test
    public void testDatabaseContextIsUsable() throws Exception {
        System.out.println("testDatabaseContextIsUsable");

        DatabaseContext db = databaseContextRule.getDatabaseContext();
        try {
            db.getTableUtils().createTable(Widget.class);
            assertEquals("widget count", 0L, db.getDao(Widget.class).countOf());
            db.getDao(Widget.class).create(new Widget("red"));
            assertEquals("widget count", 1L, db.getDao(Widget.class).countOf());
            Widget widget = db.getDao(Widget.class).queryForAll().get(0);
            assertEquals("widget color", "red", widget.color);
        } finally {
            db.closeConnections(false);
        }
    }

}