/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.dbhelp;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Objects;

/**
 *
 * @author mchaberski
 */
public class ConnectionParams {
    
    public static final String FIELD_NAME_HOST = "host";
    public static final String FIELD_NAME_USERNAME = "username";
    public static final String FIELD_NAME_PASSWORD = "password";
    public static final String FIELD_NAME_SCHEMA = "schema";
    
    public static final ImmutableList<String> FIELD_NAMES = 
            ImmutableList.of(FIELD_NAME_HOST, FIELD_NAME_USERNAME, 
                    FIELD_NAME_PASSWORD, FIELD_NAME_SCHEMA);
    
    public String host;
    public String username;
    public String password;
    public String schema;

    public ConnectionParams() {
    }

    public ConnectionParams(String host, String username, String password, String schema) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.schema = schema;
    }

    public ConnectionParams(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.host);
        hash = 37 * hash + Objects.hashCode(this.username);
        hash = 37 * hash + Objects.hashCode(this.password);
        hash = 37 * hash + Objects.hashCode(this.schema);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConnectionParams other = (ConnectionParams) obj;
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        if (!Objects.equals(this.schema, other.schema)) {
            return false;
        }
        return true;
    }

    public ConnectionParams copy() {
        ConnectionParams copy = new ConnectionParams(host, username, password, schema);
        return copy;
    }
}
