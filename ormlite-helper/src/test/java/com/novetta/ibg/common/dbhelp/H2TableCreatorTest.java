/*
 * (c) 2015 Mike Chaberski 
 */

package com.novetta.ibg.common.dbhelp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author mchaberski
 */
public class H2TableCreatorTest {
    
    public H2TableCreatorTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @DatabaseTable
    public static class EntityWithCustomColumnDefinition {
        @DatabaseField(generatedId=true)
        public Integer id;

        @DatabaseField(uniqueIndex = true, canBeNull = false, 
                columnDefinition = "VARCHAR(255) CHARACTER SET 'utf8' NOT NULL")
        public String data;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("data", data)
                    .toString();
        }

        
    }
    
    @Test(expected=SQLException.class)
    public void testInvalidColumnDefinitionCausesFailureWithDefaultTableUtils() throws SQLException {
        System.out.println("testInvalidColumnDefinitionCausesFailureWithDefaultTableUtils");
        
        ConnectionSource connectionSource = new H2MemoryConnectionSource();
        try {
            System.out.println("expecting failure on table creation...");
            for (String stmt : TableUtils.getCreateTableStatements(connectionSource, EntityWithCustomColumnDefinition.class)) {
                System.out.println(" > " + stmt);
            }
            System.out.println();
            TableUtils.createTable(connectionSource, EntityWithCustomColumnDefinition.class);
            fail("should have thrown exception already");
        } finally {
            connectionSource.close();
        }
    }
    
    @Test
    public void testContextTableUtilsStripsInvalidColumnDefinitionArgs() throws SQLException {
        System.out.println("testContextTableUtilsStripsInvalidColumnDefinitionArgs");
        DefaultDatabaseContext dbContext = new DefaultDatabaseContext(new H2MemoryConnectionSource());
        try {
            ContextTableUtils tableUtils = dbContext.getTableUtils();
            tableUtils.createTable(EntityWithCustomColumnDefinition.class);
            System.out.println("table created successfully");
        } finally {
            dbContext.closeConnections(false);
        }
    }
    
    @Test
    public void testRegexMatchesIllegalClause() {
        System.out.println("testRegexMatchesIllegalClause");
        
        String columnDefinition = "VARCHAR(255) CHARACTER SET 'utf8' NOT NULL";
        System.out.println("original: " + columnDefinition);
        String scrubbed = new H2TableCreator().stripIllegalColumnDefinitionClauses(columnDefinition);
        System.out.println("scrubbed: " + scrubbed);
        assertNotEquals(columnDefinition, scrubbed);
        assertEquals("VARCHAR(255)  NOT NULL", scrubbed);
    }
    
    @Test
    public void testRegexIgnoresValidStatement() {
        System.out.println("testRegexIgnoresValidStatement");
        String columnDefinition = "VARCHAR(255) NOT NULL";
        System.out.println("original: " + columnDefinition);
        String scrubbed = new H2TableCreator().stripIllegalColumnDefinitionClauses(columnDefinition);
        System.out.println("scrubbed: " + scrubbed);
        assertEquals(columnDefinition, scrubbed);
    }
            
}
