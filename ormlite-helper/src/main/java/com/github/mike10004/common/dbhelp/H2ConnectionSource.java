/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.github.mike10004.common.dbhelp;

import static com.google.common.base.Preconditions.checkState;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.DatabaseTypeUtils;
import java.sql.SQLException;

/**
 * Abstract superclass for H2 connection source implementations.
 * @author mchaberski
 */
public abstract class H2ConnectionSource extends LazyJdbcConnectionSource {
    
    /**
     * Gets the protocol. Return value should be something like "jdbc:h2:blah"
     * <i>without</i> the terminal colon.
     * @return the protocol, without terminal colon
     */
    protected abstract String getProtocol();

    protected abstract String getSchema();
    
    protected String constructJdbcUrl() {
        String protocol = getProtocol();
        checkState(protocol != null, "protocol must be non-null");
        String schema = getSchema();
        checkState(schema != null, "schema must be non-null");
        String jdbcUrl = protocol + ':' + schema;
        String urlSuffix = getJdbcUrlSuffix();
        jdbcUrl += urlSuffix;
        return jdbcUrl;
    }
    
    protected String getJdbcUrlSuffix() {
        return "";
    }
    
    @Override
    protected DatabaseType forceGetDatabaseType() {
        return DatabaseTypeUtils.createDatabaseType("jdbc:h2:" + getProtocol() + ":anyschema");
    }

    @Override
    protected void prepare() throws SQLException {
        String url = constructJdbcUrl();
        setUrl(url);
    }
    
    
}
