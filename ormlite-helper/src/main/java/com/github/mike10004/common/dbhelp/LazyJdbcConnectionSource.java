package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.DatabaseTypeUtils;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Abstract connection source implementation that lazily prepares and initializes
 * a connection source the first time it is needed. Subclasses must set JDBC
 * connection parameters, such as the JDBC URL, in the implementation of the 
 * {@link #prepare() prepare} method.
 */
public abstract class LazyJdbcConnectionSource extends JdbcConnectionSource {

    private transient final Object preparationLock = new Object();
    
    protected void maybePrepareAndInitialize() throws SQLException {
        synchronized (preparationLock) {
            if (!initialized) {
                prepare();
                initialize();
            }
        }
    }
    
    /**
     * Prepares for initialization. Must set the {@code url} field at minimum.
     * @throws SQLException if preparation fails
     */
    protected abstract void prepare() throws SQLException;
    
    protected abstract DatabaseType forceGetDatabaseType();

    /**
     * Gets the database type. We override the superclass to allow this 
     * method to succeed even if this instance has not been initialized. 
     * To do so, we require implementation of {@link #forceGetDatabaseType() }
     * to return a non-null value <i>always</i>. This means that subclasses
     * must already know their database type.
     * 
     * <p>This method will not set the {@code databaseType} field, though.</p>
     * @return  the database type, possible forced
     */
    @Override
    public DatabaseType getDatabaseType() {
        DatabaseType current = databaseType;
        if (current == null) {
            current = forceGetDatabaseType();
            if (current == null) {
                throw new IllegalStateException("faulty implementation of "  
                        + LazyJdbcConnectionSource.class.getSimpleName() 
                        + "; forceGetDatabaseType must return a non-null value");
            }
        }
        return current;
    }
    
    @Override
	public void initialize() throws SQLException {
		if (initialized) {
			return;
        }
        String url = getUrl();
		if (url == null) {
			throw new SQLException("url was never set on " + getClass().getSimpleName());
		}
        DatabaseType databaseType_ = getDatabaseType();
		if (databaseType_ == null) {
			databaseType_ = DatabaseTypeUtils.createDatabaseType(url);
		}
		databaseType_.loadDriver();
		databaseType_.setDriver(DriverManager.getDriver(url));
		initialized = true;
	}

    @Override
    public void close() throws IOException {
        try {
            maybePrepareAndInitialize();
        } catch (SQLException e) {
            throw new IOException(e);
        }
        super.close();
    }

    @Override
    public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
        maybePrepareAndInitialize();
        return super.getReadOnlyConnection(tableName);
    }

    @Override
    public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
        maybePrepareAndInitialize();
        return super.getReadWriteConnection(tableName);
    }
    
    @Override
    public void releaseConnection(DatabaseConnection connection) throws SQLException {
        maybePrepareAndInitialize();
        super.releaseConnection(connection);
    }
    
    /**
     * Invokes prepare and initialize methods.
     * @throws SQLException if there is a database error
     * @see #prepare() 
     * @see #initialize() 
     */
    public void forcePrepareAndInitialize() throws SQLException {
        maybePrepareAndInitialize();
    }
}
