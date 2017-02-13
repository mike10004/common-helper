/**
 * (c) 2016 Mike Chaberski. Distributed under terms of the MIT License.
 */
package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.j256.ormlite.support.ConnectionSource;
import com.github.mike10004.common.dbhelp.DatabaseContext;
import com.github.mike10004.common.dbhelp.DefaultDatabaseContext;
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
    private final Iterable<? extends BookendOperation> bookendOperations;

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
     * @param bookendOperations zero or more actions to take after creating
     * the database context instance
     */
    public DatabaseContextRule(BookendOperation...bookendOperations) {
        this.bookendOperations = ImmutableList.copyOf(bookendOperations);
        doNotSwallowClosingException = false;
    }

    /**
     * Gets the connection source created
     * @return the connection source
     */
    public ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    /**
     * Gets the database context instance.
     * @return the database context instance
     */
    public DatabaseContext getDatabaseContext() {
        return databaseContext;
    }

    @Override
    protected void after() {
        DatabaseContext db = databaseContext;
        if (db != null) {
            for (TeardownOperation action : Iterables.filter(bookendOperations, TeardownOperation.class)) {
                try {
                    action.perform(databaseContext);
                } catch (Exception e) {
                    Logger.getLogger(DatabaseContextRule.class.getName())
                            .log(Level.WARNING, "teardown operation threw exception; skipping remaining teardown operations", e);
                    break;
                }
            }
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
    protected void before() throws Exception {
        connectionSource = createConnectionSource();
        databaseContext = createDatabaseContext(connectionSource);
        for (SetupOperation action : Iterables.filter(bookendOperations, SetupOperation.class)) {
            action.perform(databaseContext);
        }

    }

    /**
     * Interface representing an operation to be performed
     * either before or after the test. These operations are
     * performed in the {@link ExternalResource#before()} and
     * {@link ExternalResource#after()} methods.
     */
    public interface BookendOperation {
        void perform(DatabaseContext db) throws Exception;
    }

    /**
     * Interface representing an operation to be performed before the test,
     * after creating the database context instance. These operations
     * are performed in the {@link ExternalResource#before() }
     * method.
     */
    public interface SetupOperation extends BookendOperation {
    }

    /**
     * Interface representing an operation to be performed after the test.
     * These operations are performed in the {@link ExternalResource#after() }
     * method.
     */
    public interface TeardownOperation extends BookendOperation {
    }

}
