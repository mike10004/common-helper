package com.github.mike10004.common.dbhelp;

import com.google.common.base.Preconditions;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Class that implements a default context transaction manager.
 */
public class DefaultContextTransactionManager implements ContextTransactionManager {
    
    private final ConnectionSource connectionSource;

    /**
     * Constructs an instance for the given connection source.
     * @param connectionSource the connection source
     */
    public DefaultContextTransactionManager(ConnectionSource connectionSource) {
        this.connectionSource = Preconditions.checkNotNull(connectionSource, "connectionSource");
    }

    @Override
    public <T> T callInTransaction(Callable<T> callable) throws SQLException {
        return com.j256.ormlite.misc.TransactionManager.callInTransaction(connectionSource, callable);
    }
    
}
