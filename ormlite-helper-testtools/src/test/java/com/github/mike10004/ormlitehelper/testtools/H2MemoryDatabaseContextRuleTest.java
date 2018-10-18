package com.github.mike10004.ormlitehelper.testtools;

import com.github.mike10004.common.dbhelp.DatabaseContext;
import org.junit.Test;

import static org.junit.Assert.*;

public class H2MemoryDatabaseContextRuleTest {

    @Test
    public void testTablesCreated() throws Exception {
        System.out.println("testTablesCreated");
        H2MemoryDatabaseContextRule databaseContextRule = new H2MemoryDatabaseContextRule(new CreateTablesOperation(Widget.class));
        databaseContextRule.before();
        DatabaseContext db = databaseContextRule.getDatabaseContext();
        assertEquals("widget count", 0L, db.getDao(Widget.class).countOf());
        databaseContextRule.after();
    }

    @Test
    public void testDatabaseContextIsUsable() throws Exception {
        System.out.println("testDatabaseContextIsUsable");
        H2MemoryDatabaseContextRule databaseContextRule = new H2MemoryDatabaseContextRule();
        databaseContextRule.before();
        DatabaseContext db = databaseContextRule.getDatabaseContext();
        db.getTableUtils().createTable(Widget.class);
        assertEquals("widget count", 0L, db.getDao(Widget.class).countOf());
        db.getDao(Widget.class).create(new Widget("red"));
        assertEquals("widget count", 1L, db.getDao(Widget.class).countOf());
        Widget widget = db.getDao(Widget.class).queryForAll().get(0);
        assertEquals("widget color", "red", widget.color);
        databaseContextRule.after();
    }
}