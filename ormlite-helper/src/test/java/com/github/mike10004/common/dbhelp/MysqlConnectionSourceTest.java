package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.MysqlDatabaseType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MysqlConnectionSourceTest {
    
    @Test
    public void testForceGetDatabaseType() {
        System.out.println("testForceGetDatabaseType");
        MysqlConnectionSource cs = new MysqlConnectionSource();
        DatabaseType expected = new MysqlDatabaseType();
        DatabaseType actual = cs.forceGetDatabaseType();
        System.out.format("expect %s actual %s%n", expected, actual);
        assertEquals(expected.getClass(), actual.getClass());
    }
    
}
