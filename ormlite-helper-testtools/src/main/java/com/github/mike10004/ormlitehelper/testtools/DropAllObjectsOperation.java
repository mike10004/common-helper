package com.github.mike10004.ormlitehelper.testtools;

import com.j256.ormlite.support.DatabaseConnection;
import com.github.mike10004.common.dbhelp.DatabaseContext;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Operation that drops all objects in an H2 database.
 */
public class DropAllObjectsOperation implements DatabaseContextRule.TeardownOperation {


    @Override
    public void perform(DatabaseContext db) throws SQLException, IOException {
        DatabaseConnection conn = db.getConnectionSource().getReadWriteConnection("");
        try {
            conn.executeStatement("DROP ALL OBJECTS", DatabaseConnection.DEFAULT_RESULT_FLAGS);
        } finally {
            conn.close();
        }
    }
}
