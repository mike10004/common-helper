/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.novetta.ibg.common.dbhelp;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;

/**
 * Interface for a database context. A database context is a configuration
 * that allows client code to interact with a database through an ORM layer.
 *
 * <p>To establish a database connection, you must configure the type of 
 * connection source to be created by providing an appropriate 
 * {@link ConnectionSource}.
 * @author mchaberski
 */
public interface DatabaseContext {

    /**
     * Gets the data access object for an entity class that has an integer
     * primary key data type. Dao caching is performed by 
     * the ORM library, so you don't need to take pains to hold on to this
     * reference.
     * @param <T> the entity class type
     * @param clz the entity class
     * @return the dao
     * @throws java.sql.SQLException on error getting the dao
     */
    <T> Dao<T, ?> getDao(Class<T> clz) throws SQLException;
    
    /**
     * Gets the data access object for an entity class with a given key type. 
     * Dao caching is performed by the ORM library, so you don't need to take 
     * pains to hold on to this reference.
     * @param <T> the entity class type
     * @param <K> the key type
     * @param clazz the entity class 
     * @param keyType the primary key type
     * @return the dao
     * @throws SQLException on error getting the dao
     */
    <T, K> Dao<T, K> getDao(Class<T> clazz, Class<K> keyType) throws SQLException;
    
    /**
     * Closes all open connections in this context. You can specify whether 
     * an exception thrown from the {@link ConnectionSource#close() } method
     * should bubble up or be swallowed. The appropriate place to call this
     * method is probably in a {@code finally} block, and 
     * {@code swallowClosingException} should be false if previous code
     * exectued cleanly and false if the block is being
     * executed after an earlier exception. That is, use a pattern like this:
     * <pre>
     *     boolean clean = false;
     *     try {
     *         // do stuff that might throw an exception...
     *         clean = true;
     *     } finally {
     *         databaseContext.closeConnections(!clean);
     *     }
     * </pre>
     * @param swallowClosingException true if an exception thrown from 
     * invoking the {@code close} method on an open connection should be
     * suppressed, false if it should be re-thrown
     * @throws SQLException 
     */
    void closeConnections(boolean swallowClosingException) throws SQLException;
    
    /**
     * Gets the connection source associated with this context instance. 
     * This would probably only be used in situations where you have to call
     * uncommon methods from the ORMLite API directly. You probably want to use 
     * a {@link #getDao(java.lang.Class) dao}.
     * 
     * @return the connection source
     * @throws SQLException if getting the ocnnection source fails
     */
    ConnectionSource getConnectionSource() throws SQLException;
    
    /**
     * Gets the transaction manager for this instance.
     * @return the transaction manager
     * @see ContextTransactionManager
     * @see com.j256.ormlite.misc.TransactionManager
     */
    ContextTransactionManager getTransactionManager();

    /**
     * Gets the table utils instance for this context.
     * @return the table utils instance 
     * @see ContextTableUtils
     * @see com.j256.ormlite.table.TableUtils
     */
    ContextTableUtils getTableUtils();
    
}
