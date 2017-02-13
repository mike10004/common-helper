/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import java.io.IOException;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility methods relating to connection sources.
 * @see ConnectionSource
 *
 * @author mchaberski
 * @since 3.1.0
 */
public class ConnectionSources {
    
    public static class NullConnectionSourceException extends IllegalStateException {
        
    }
    
    public abstract static class ConnectionSourceDelegator implements ConnectionSource {
        
        protected abstract ConnectionSource getDelegate();
        
        protected ConnectionSource getCheckedDelegate() throws NullConnectionSourceException {
            ConnectionSource delegate = getDelegate();
            if (delegate == null) {
                throw new NullConnectionSourceException();
            }
            return delegate;
        }
        
        @Override
        public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.getReadOnlyConnection(tableName);
        }

        @Override
        public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.getReadWriteConnection(tableName);
        }

        @Override
        public void releaseConnection(DatabaseConnection connection) throws SQLException {
            ConnectionSource delegate = getCheckedDelegate();
            delegate.releaseConnection(connection);
        }

        @Override
        public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.saveSpecialConnection(connection);
        }

        @Override
        public void clearSpecialConnection(DatabaseConnection connection) {
            ConnectionSource delegate = getCheckedDelegate();
            delegate.clearSpecialConnection(connection);
        }

        @Override
        public DatabaseConnection getSpecialConnection(String tableName) {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.getSpecialConnection(tableName);
        }

        @Override
        public void close() throws IOException {
            ConnectionSource delegate = getCheckedDelegate();
            delegate.close();
        }

        @Override
        public void closeQuietly() {
            ConnectionSource delegate = getCheckedDelegate();
            delegate.closeQuietly();
        }

        @Override
        public DatabaseType getDatabaseType() {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.getDatabaseType();
        }

        @Override
        public boolean isOpen(String tableName) {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.isOpen(tableName);
        }

        @Override
        public boolean isSingleConnection(String tableName) {
            ConnectionSource delegate = getCheckedDelegate();
            return delegate.isSingleConnection(tableName);
        }
    }
    
    public static class ConnectionSourceRuntimeException extends RuntimeException {

        public ConnectionSourceRuntimeException() {
        }

        public ConnectionSourceRuntimeException(String message) {
            super(message);
        }

        public ConnectionSourceRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }

        public ConnectionSourceRuntimeException(Throwable cause) {
            super(cause);
        }

        protected ConnectionSourceRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
        
    }
    
    public static class ConnectionSourceCreationException extends ConnectionSourceRuntimeException {

        public ConnectionSourceCreationException(String message) {
            super(message);
        }

        public ConnectionSourceCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public ConnectionSourceCreationException(Throwable cause) {
            super(cause);
        }
        
    }
    
    public static class IntentionallyBrokenConnectionSourceException extends ConnectionSourceRuntimeException {

        public IntentionallyBrokenConnectionSourceException() {
            super("connection source is intentionally broken");
        }
        
    }
    
    static class IntentionallyBrokenConnectionSource implements ConnectionSource {

        @Override
        public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
            throw new SQLException("connection source is intentionally broken");
        }

        @Override
        public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
            throw new SQLException("connection source is intentionally broken");
        }

        @Override
        public void releaseConnection(DatabaseConnection connection) throws SQLException {
            throw new SQLException("connection source is intentionally broken");
        }

        @Override
        public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
            throw new SQLException("connection source is intentionally broken");
        }

        @Override
        public void clearSpecialConnection(DatabaseConnection connection) {
            throw new IntentionallyBrokenConnectionSourceException();
        }

        @Override
        public DatabaseConnection getSpecialConnection(String tableName) {
            throw new IntentionallyBrokenConnectionSourceException();
        }

        @Override
        public void close() throws IOException {
            throw new IntentionallyBrokenConnectionSourceException();
        }

        @Override
        public void closeQuietly() {
            throw new IntentionallyBrokenConnectionSourceException();
        }

        @Override
        public DatabaseType getDatabaseType() {
            throw new IntentionallyBrokenConnectionSourceException();
        }

        @Override
        public boolean isOpen(String s) {
            throw new IntentionallyBrokenConnectionSourceException();
        }

        @Override
        public boolean isSingleConnection(String s) {
            throw new IntentionallyBrokenConnectionSourceException();
        }

    }
    
    public static ConnectionSource broken() {
        return new IntentionallyBrokenConnectionSource();
    }
    
    public static class SimpleConnectionSourceDelegator extends ConnectionSourceDelegator {

        private final ConnectionSource delegate;

        public SimpleConnectionSourceDelegator(ConnectionSource delegate) {
            this.delegate = checkNotNull(delegate);
        }
        
        @Override
        public ConnectionSource getDelegate() {
            return delegate;
        }
        
    }
    
    public static class CreationMonitoringConnectionSource extends SimpleConnectionSourceDelegator {

        private boolean connectionCreated;

        public CreationMonitoringConnectionSource(ConnectionSource delegate) {
            super(delegate);
        }

        public boolean isConnectionCreated() {
            return connectionCreated;
        }
        
        @Override
        public DatabaseConnection getSpecialConnection(String tableName) {
            DatabaseConnection connection = super.getSpecialConnection(tableName);
            connectionCreated = true;
            return connection;
        }

        @Override
        public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
            DatabaseConnection connection = super.getSpecialConnection(tableName);
            connectionCreated = true;
            return connection;
        }

        @Override
        public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
            DatabaseConnection connection = super.getSpecialConnection(tableName);
            connectionCreated = true;
            return connection;
        }
        
    }
    
}
