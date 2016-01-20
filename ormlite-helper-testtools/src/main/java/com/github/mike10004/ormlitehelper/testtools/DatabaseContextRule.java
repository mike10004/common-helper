/**
 * (c) 2016 Mike Chaberski. Distributed under terms of the MIT License.
 */
package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.collect.ImmutableList;
import com.j256.ormlite.support.ConnectionSource;
import com.novetta.ibg.common.dbhelp.DatabaseContext;
import com.novetta.ibg.common.dbhelp.DefaultDatabaseContext;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.rules.ExternalResource;

/**
 *
 * @author mchaberski
 */
public abstract class DatabaseContextRule extends ExternalResource {

    private ConnectionSource connectionSource;
    private DatabaseContext databaseContext;
    private final Iterable<SetupOperation> setupOperations;

    /**
     * Flag that specifies whether an exception thrown when closing
     * the database connection source should be suppressed or thrown.
     * Not sure whether this should be true or false or exposed/mutable
     * to the client.
     *
     * @see DatabaseContext#closeConnections(boolean)
     * @see ConnectionSource#close()
     * @see ConnectionSource#closeQuietly()
     */
    private final boolean doNotSwallowClosingException;

    /**
     * Constructs an instance of the class and executes actions
     * requiring a database context.
     * @param setupOperations zero or more actions to take after creating
     * the database context instance
     */
    public DatabaseContextRule(SetupOperation...setupOperations) {
        this.setupOperations = ImmutableList.copyOf(setupOperations);
        doNotSwallowClosingException = false;
    }

    public DatabaseContext getDatabaseContext() {
        return databaseContext;
    }

    @Override
    protected void after() {
        DatabaseContext db = databaseContext;
        if (db != null) {
            try {
                db.closeConnections(!doNotSwallowClosingException);
            } catch (SQLException ex) {
                Logger.getLogger(DatabaseContextRule.class.getName())
                        .log(Level.WARNING, "error closing database connection", ex);
            }
        }
    }

    protected abstract ConnectionSource createConnectionSource();

    protected DatabaseContext createDatabaseContext(ConnectionSource connectionSource) {
        return new DefaultDatabaseContext(connectionSource);
    }

    @Override
    protected void before() throws Throwable {
        connectionSource = createConnectionSource();
        databaseContext = createDatabaseContext(connectionSource);
        for (SetupOperation action : setupOperations) {
            if (action != null) {
                action.perform(databaseContext);
            }
        }
    }

    /**
     * Interface representing an operation to be performed after
     * creating the database context instance. These operations
     * are performed in the {@link ExternalResource#before() }
     * method.
     */
    public static interface SetupOperation {
        void perform(DatabaseContext db) throws Exception;
    }

}
