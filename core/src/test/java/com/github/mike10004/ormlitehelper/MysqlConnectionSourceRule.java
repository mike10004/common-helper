/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.github.mike10004.ormlitehelper;

import com.google.common.base.CharMatcher;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.function.Function;
import org.junit.rules.ExternalResource;

/**
 * Connection source that creates a new unique schema on initialization and 
 * drops the schema on close.
 * @author mchaberski
 */
public class MysqlConnectionSourceRule extends ExternalResource {

    /**
     * Port on which mysql is listening. This is set by 
     * org.codehaus.mojo:build-helper-maven-plugin, goal reserve-network-port.
     */
    private static final int MYSQL_PORT = Integer.parseInt(System.getProperty("mysql.port"));
    
    static ConnectionParams newConnectionParams(String schema) {
        ConnectionParams cp = new ConnectionParams("localhost:" + MYSQL_PORT, "root", "root");
        cp.schema = schema;
        return cp;
    }
    
    private ConnectionParams currentParams;
    private final Function<ConnectionParams, MysqlConnectionSource> connectionSourceCreator;
    
    public MysqlConnectionSourceRule(Function<ConnectionParams, MysqlConnectionSource> connectionSourceCreator) {
        super();
        this.connectionSourceCreator = checkNotNull(connectionSourceCreator);
    }

    public MysqlConnectionSourceRule() {
        this(newDefaultConnectionSourceCreator());
    }
    
    public static Function<ConnectionParams, MysqlConnectionSource> newDefaultConnectionSourceCreator() {
        return new Function<ConnectionParams, MysqlConnectionSource>() {

            @Override
            public MysqlConnectionSource apply(ConnectionParams connectionParams) {
                checkArgument(connectionParams != null, "connectionParams is null; before() method has not been called");
                return new MysqlConnectionSource(connectionParams);
            }
        };
    }
    
    public MysqlConnectionSource createConnectionSource() {
        return connectionSourceCreator.apply(currentParams.copy());
    }
    
    protected static String newUniqueSchemaName() {
        return "u" + CharMatcher.JAVA_LETTER_OR_DIGIT.retainFrom(UUID.randomUUID().toString());
    }

    protected void createSchema(ConnectionParams cp) throws SQLException {
        checkNotNull(cp);
        checkNotNull(cp.host);
        checkNotNull(cp.schema);
        String url = "jdbc:mysql://" + cp.host + "/";
        try (Connection conn = DriverManager.getConnection(url, cp.username, cp.password);
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA `" + cp.schema + "`");
        } 
        
    }

    private void dropSchemaIfExists(ConnectionParams connectionParams) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://" 
                + connectionParams.host + "/", connectionParams.username, 
                connectionParams.password);
                Statement stmt = conn.createStatement()) {
            String sql = "DROP SCHEMA `" + connectionParams.schema + "`";
            System.out.println("executing: " + sql);
            stmt.execute(sql);
        }
    }

    @Override
    protected void after() {
        try {
            dropSchemaIfExists(currentParams);
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    protected void before() throws Throwable {
        currentParams = newConnectionParams(newUniqueSchemaName());
        createSchema(currentParams);
    }
    

}
