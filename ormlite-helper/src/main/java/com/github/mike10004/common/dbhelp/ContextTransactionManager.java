/*
 * (c) 2015 Mike Chaberski 
 */

package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.misc.TransactionManager;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Interface for transaction managers that are associated with a specific
 * database context. The {@link TransactionManager transaction manager}'s 
 * connection source is taken from the database context.
 * @see TransactionManager
 */
public interface ContextTransactionManager {

	/**
	 * Execute the {@link Callable} class inside of a transaction. If the callable returns then the transaction is
	 * committed. If the callable throws an exception then the transaction is rolled back and a {@link SQLException} is
	 * thrown by this method.
	 * 
	 * <p>
	 * <b> NOTE: </b> If your callable block really doesn't have a return object then use the Void class and return null
	 * from the call method.
	 * </p>
	 * 
	 * @param callable
	 *            Callable to execute inside of the transaction.
	 * @param <T> callable return type
	 * @return The object returned by the callable.
	 * @throws SQLException
	 *             If the callable threw an exception then the transaction is rolled back and a SQLException wraps the
	 *             callable exception and is thrown by this method.
	 */
    <T> T callInTransaction(Callable<T> callable) throws SQLException;
    
}
