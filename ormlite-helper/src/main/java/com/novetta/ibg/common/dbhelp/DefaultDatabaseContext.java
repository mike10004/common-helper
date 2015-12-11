/*
 * (c) 2015 Mike Chaberski 
 */

package com.novetta.ibg.common.dbhelp;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.novetta.ibg.common.dbhelp.ConnectionSources.UnrecloseableConnectionSource;
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
    private Function<ConnectionSource, ContextTableUtils> tableUtilsFactory;
    private ContextTableUtils tableUtils;
    private transient final Object lock = new Object();
    
    public DefaultDatabaseContext(ConnectionSource connectionSource) {
        this(connectionSource, new DefaultTableUtilsFactory());
    }
    
    /**
     * Constructs an instance of the class with the given connection source.
     * @param connectionSource  the connection source
     * @param tableUtils the table utils
     */
    public DefaultDatabaseContext(ConnectionSource connectionSource, Function<ConnectionSource, ContextTableUtils> tableUtilsFactory) {
        checkNotNull(connectionSource, "connectionSource");
        this.connectionSource = ConnectionSources.unrecloseable(connectionSource);
        this.tableUtilsFactory = checkNotNull(tableUtilsFactory, "tableUtilsFactory");
    }

    @Override
    public ConnectionSource getConnectionSource() throws SQLException {
        return connectionSource;
    }

    @Override
    public ContextTableUtils getTableUtils() {
        synchronized (lock) {
            if (tableUtils == null) {
                tableUtils = tableUtilsFactory.apply(connectionSource);
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
    
    public static class DefaultTableUtilsFactory implements Function<ConnectionSource, ContextTableUtils> {

        @Override
        public ContextTableUtils apply(ConnectionSource connectionSource) {
            if (connectionSource.getDatabaseType() instanceof com.j256.ormlite.db.H2DatabaseType) {
                return new H2ContextTableUtils(connectionSource);
            } else {
                return new DefaultContextTableUtils(connectionSource);
                
            }
        }
    
    }
    
    private static class DefaultContextTableUtils implements ContextTableUtils {

        private final ConnectionSource connectionSource;
        
        public DefaultContextTableUtils(ConnectionSource connectionSource) {
            this.connectionSource = checkNotNull(connectionSource);
        }
        
        protected ConnectionSource getConnectionSource() {
            return connectionSource;
        }
        
        @Override
        public <T> int createTable(Class<T> dataClass) throws SQLException {
            return com.j256.ormlite.table.TableUtils.createTable(getConnectionSource(), dataClass);
        }

        @Override
        public <T> int createTableIfNotExists(Class<T> dataClass) throws SQLException {
            return com.j256.ormlite.table.TableUtils.createTableIfNotExists(getConnectionSource(), dataClass);
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

    private static class H2ContextTableUtils extends DefaultContextTableUtils {

        private final H2TableCreator tableCreator = new H2TableCreator();

        public H2ContextTableUtils(ConnectionSource connectionSource) {
            super(connectionSource);
        }
        
        @Override
        public <T> int createTable(Class<T> dataClass) throws SQLException {
            boolean ifNotExists = false;
            return tableCreator.createTable(getConnectionSource(), dataClass, ifNotExists);
        }

        @Override
        public <T> int createTableIfNotExists(Class<T> dataClass) throws SQLException {
            boolean ifNotExists = true;
            return tableCreator.createTable(getConnectionSource(), dataClass, ifNotExists);
        }
    }
}
