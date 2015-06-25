/*
 * (c) 2015 Mike Chaberski 
 */

package com.github.mike10004.ormlitehelper;

import static com.google.common.base.Preconditions.checkNotNull;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.github.mike10004.ormlitehelper.ConnectionSources.UnrecloseableConnectionSource;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class that is the default implementation of a database context.
 * @author mchaberski
 */
public class DefaultDatabaseContext implements DatabaseContext {

    private final UnrecloseableConnectionSource connectionSource;
    private ContextTransactionManager transactionManager;
    private ContextTableUtils tableUtils;
    private transient final Object lock = new Object();
    
    /**
     * Constructs an instance of the class with the given connection source.
     * @param connectionSource  the connection source
     */
    public DefaultDatabaseContext(ConnectionSource connectionSource) {
        checkNotNull(connectionSource, "connectionSource");
        this.connectionSource = ConnectionSources.unrecloseable(connectionSource);
    }

    @Override
    public ConnectionSource getConnectionSource() throws SQLException {
        return connectionSource;
    }

    @Override
    public ContextTableUtils getTableUtils() {
        synchronized(lock) {
            if (tableUtils == null) {
                tableUtils = new TableUtilsImpl();
            }
            return tableUtils;
        }
    }
    
    @Override
    public ContextTransactionManager getTransactionManager() {
        synchronized(lock) {
            if (transactionManager == null) {
                transactionManager = new DefaultTransactionManager();
            }
            return transactionManager;
        }
    }
    
    private void resetCaches() {
        transactionManager = null;
        tableUtils = null;
    }

    @Override
    public void closeConnections(boolean swallowClosingException) throws SQLException {
        try {
            synchronized (lock) {
                resetCaches();
                getConnectionSource().close();
            }
        } catch (SQLException ex) {
            if (!swallowClosingException) {
                throw ex;
            }
        }
    }    
    
    private class DefaultTransactionManager implements ContextTransactionManager {
        
        @Override
            public <T> T callInTransaction(Callable<T> callable) throws SQLException {
                return com.j256.ormlite.misc.TransactionManager.callInTransaction(getConnectionSource(), callable);
            }
    }

    /**
     * Gets the data access object for an entity class that has an integer
     * primary key data type. Dao caching is performed by 
     * the ORM library, so you don't need to take pains to hold on to this
     * reference.
     * @param <T> the entity class type
     * @param clz the entity class
     * @return the dao
     * @throws java.sql.SQLException if 
     * {@link #getDao(java.lang.Class, java.lang.Class) } throws one
     */
    @Override
    public <T> Dao<T, Integer> getDao(Class<T> clz) throws SQLException {
        return DaoManager.createDao(getConnectionSource(), clz);
    }

    /**
     * Gets the data access object for an entity class with a given key type. 
     * Dao caching is performed by the ORM library, so you don't need to take 
     * pains to hold on to this reference.
     * @param <T> the entity class type
     * @param <K> the key type
     * @param clazz the entity class 
     * @param keyType the primary key type
     * @return the dao
     * @throws SQLException if 
     * {@link DaoManager#createDao(com.j256.ormlite.support.ConnectionSource, java.lang.Class) } 
     * throws one
     */
     @Override
    public <T, K> Dao<T, K> getDao(Class<T> clazz, Class<K> keyType) throws SQLException {
        return DaoManager.createDao(getConnectionSource(), clazz);
    }
    
    private class TableUtilsImpl implements ContextTableUtils {

        private TableUtilsImpl() {}
        
        @Override
        public <T> int createTable(Class<T> dataClass) throws SQLException {
            boolean ifNotExists = false;
            ConnectionSource connectionSource_ = getConnectionSource();
            if (connectionSource_.getDatabaseType() instanceof com.j256.ormlite.db.H2DatabaseType) {
                H2TableCreator tableCreator = new H2TableCreator();
                return tableCreator.createTable(connectionSource_, dataClass, ifNotExists);
            } else {
                return com.j256.ormlite.table.TableUtils.createTable(getConnectionSource(), dataClass);
            }
        }

        @Override
        public <T> int createTableIfNotExists(Class<T> dataClass) throws SQLException {
            boolean ifNotExists = true;
            ConnectionSource connectionSource = getConnectionSource();
            if (connectionSource.getDatabaseType() instanceof com.j256.ormlite.db.H2DatabaseType) {
                H2TableCreator tableCreator = new H2TableCreator();
                return tableCreator.createTable(connectionSource, dataClass, ifNotExists);
            } else {
                return com.j256.ormlite.table.TableUtils.createTableIfNotExists(getConnectionSource(), dataClass);
            }
        }

        @Override
        public <T> int createTable(DatabaseTableConfig<T> tableConfig) throws SQLException {
            return com.j256.ormlite.table.TableUtils.createTable(getConnectionSource(), tableConfig);
        }

        @Override
        public <T> int createTableIfNotExists(DatabaseTableConfig<T> tableConfig) throws SQLException {
            return com.j256.ormlite.table.TableUtils.createTableIfNotExists(getConnectionSource(), tableConfig);
        }

        @Override
        public int createAllTablesIfNotExists(Iterable<Class<?>> dataClasses) throws SQLException {
            int totalNumStatementsExecuted = 0;
            for (Class<?> clz : dataClasses) {
                totalNumStatementsExecuted += createTableIfNotExists(clz);
            }
            return totalNumStatementsExecuted;
        }

        @Override
        public int createAllTables(Iterable<Class<?>> dataClasses) throws SQLException {
            int totalNumStatementsExecuted = 0;
            for (Class<?> clz : dataClasses) {
                totalNumStatementsExecuted += createTable(clz);
            }
            return totalNumStatementsExecuted;
        }

        @Override
        public <T, ID> List<String> getCreateTableStatements(Class<T> dataClass) throws SQLException {
            return com.j256.ormlite.table.TableUtils.getCreateTableStatements(getConnectionSource(), dataClass);
        }

        @Override
        public <T, ID> List<String> getCreateTableStatements(DatabaseTableConfig<T> tableConfig) throws SQLException {
            return com.j256.ormlite.table.TableUtils.getCreateTableStatements(getConnectionSource(), tableConfig);
        }

        @Override
        public <T, ID> int dropTable(Class<T> dataClass, boolean ignoreErrors) throws SQLException {
            return com.j256.ormlite.table.TableUtils.dropTable(getConnectionSource(), dataClass, ignoreErrors);
        }

        @Override
        public <T, ID> int dropTable(DatabaseTableConfig<T> tableConfig, boolean ignoreErrors) throws SQLException {
            return com.j256.ormlite.table.TableUtils.dropTable(getConnectionSource(), tableConfig, ignoreErrors);
        }

        @Override
        public <T> int clearTable(Class<T> dataClass) throws SQLException {
            return com.j256.ormlite.table.TableUtils.clearTable(getConnectionSource(), dataClass);
        }

        @Override
        public <T> int clearTable(DatabaseTableConfig<T> tableConfig) throws SQLException {
            return com.j256.ormlite.table.TableUtils.clearTable(getConnectionSource(), tableConfig);
        }
    }
}
