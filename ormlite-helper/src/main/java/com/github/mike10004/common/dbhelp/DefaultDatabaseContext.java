/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.github.mike10004.common.dbhelp;

import java.util.function.Function;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import java.io.IOException;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that is the default implementation of a database context.
 * @author mchaberski
 */
public class DefaultDatabaseContext implements DatabaseContext {

    private final ConnectionSource connectionSource;
    private final Function<ConnectionSource, ContextTableUtils> tableUtilsFactory;
    private final Function<ConnectionSource, ContextTransactionManager> transactionManagerFactory;
    private ContextTableUtils tableUtils;
    private ContextTransactionManager transactionManager;
    private transient final Object lock = new Object();
    
    /**
     * Constructs an instance of the class with the given connection source and default
     * context utility factories.
     * @param connectionSource the connection source
     * @see DefaultContextTableUtils
     * @see DefaultContextTransactionManager
     */
    public DefaultDatabaseContext(ConnectionSource connectionSource) {
        this(connectionSource, new DefaultTableUtilsFactory(), new DefaultTransactionManagerFactory());
    }
    
    /**
     * Constructs an instance of the class with the given connection source and 
     * context utility factories.
     * @param connectionSource  the connection source
     * @param tableUtilsFactory the table utils factory
     * @param transactionManagerFactory the transaction manager factory
     */
    public DefaultDatabaseContext(ConnectionSource connectionSource, 
            Function<ConnectionSource, ContextTableUtils> tableUtilsFactory, 
            Function<ConnectionSource, ContextTransactionManager> transactionManagerFactory) {
        this.connectionSource = checkNotNull(connectionSource, "connectionSource");
        this.tableUtilsFactory = checkNotNull(tableUtilsFactory, "tableUtilsFactory");
        this.transactionManagerFactory = checkNotNull(transactionManagerFactory, "transactionManagerFactory");
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
                transactionManager = transactionManagerFactory.apply(connectionSource);
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
        } catch (IOException ex) {
            if (!swallowClosingException) {
                throw new SQLException(ex);
            }
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
    public <T> Dao<T, ?> getDao(Class<T> clz) throws SQLException {
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
    
    private static class DefaultTableUtilsFactory implements Function<ConnectionSource, ContextTableUtils> {

        @Override
        public ContextTableUtils apply(ConnectionSource connectionSource) {
            return new DefaultContextTableUtils(connectionSource);
        }
    
    }
    
    private static class DefaultTransactionManagerFactory implements Function<ConnectionSource, ContextTransactionManager> {

        @Override
        public ContextTransactionManager apply(ConnectionSource connectionSource) {
            return new DefaultContextTransactionManager(connectionSource);
        }
        
    }

}
