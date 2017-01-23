/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.github.mike10004.common.dbhelp;

import com.google.common.base.MoreObjects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.DatabaseTypeUtils;
import java.sql.SQLException;

/**
 *
 * @author mchaberski
 */
public class MysqlConnectionSource extends LazyJdbcPooledConnectionSource {

    private ConnectionParams connectionParams;

    public MysqlConnectionSource(ConnectionParams connectionParams) {
        this.connectionParams = checkNotNull(connectionParams);
    }

    public MysqlConnectionSource() {
        this(new ConnectionParams());
    }
    
    @Override
    protected DatabaseType forceGetDatabaseType() {
        return DatabaseTypeUtils.createDatabaseType("jdbc:mysql://localhost:3306/");
    }

    @Override
    protected void prepare() throws SQLException {
        setPassword(connectionParams.password);
        setUsername(connectionParams.username);
        setUrl(constructJdbcUrl());
    }

    public ConnectionParams getConnectionParams() {
        return connectionParams;
    }

    public void setConnectionParams(ConnectionParams connectionParams) {
        this.connectionParams = checkNotNull(connectionParams);
    }

    protected String constructJdbcUrl() {
        String url = constructJdbcUrl(connectionParams);
        return url;
    }
    
    public String constructJdbcUrl(ConnectionParams connectionParams_) {
        String url = "jdbc:mysql://" 
                + MoreObjects.firstNonNull(connectionParams_.host, "localhost") 
                + "/" 
                + MoreObjects.firstNonNull(connectionParams_.schema, "");
        return url;
    }
}
