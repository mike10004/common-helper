package com.github.mike10004.ormlitehelper.testtools;

import com.j256.ormlite.dao.Dao;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.*;

/**
 * Created by mchaberski on 2/1/16.
 */
public class DropAllObjectsOperationTest {

    @Test
    public void testPerform() throws Exception {
        System.out.println("testPerform");
        String schemaName  = "DropAllObjectsOperationTest";
        H2MemoryDatabaseContextRule rule = new H2MemoryDatabaseContextRule(true, schemaName, new CreateTablesOperation(Widget.class), new DropAllObjectsOperation());
        rule.before();

        Dao<Widget, ?> widgetDao = rule.getDatabaseContext().getDao(Widget.class);
        long widgetCount = widgetDao.countOf();
        System.out.println("dao returned count of widgets: " + widgetCount);
        checkState(0L == widgetCount);

        rule.after();

        String jdbcUrl = new H2.H2MemoryUrlBuilder().setSchema(schemaName).build();
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tableNames.add(rs.getString(1));
            }
        }
        System.out.println("tables after after() method: " + tableNames);
        assertEquals("expect 0 tables", 0, tableNames.size());
    }


}