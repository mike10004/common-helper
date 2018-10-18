package com.github.mike10004.common.dbhelp;

import com.google.common.base.CharMatcher;
import java.util.function.Function;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.rules.ExternalResource;

/**
 * Connection source that creates a new unique schema on initialization and 
 * drops the schema on close.
 */
public class MysqlConnectionSourceRule extends ExternalResource {

    private final ConnectionParams persistentParams;
    private MysqlConnectionSource connectionSource;
    private final Function<ConnectionParams, MysqlConnectionSource> connectionSourceCreator;
    
    public MysqlConnectionSourceRule(ConnectionParams persistentParams, Function<ConnectionParams, MysqlConnectionSource> connectionSourceCreator) {
        super();
        this.connectionSourceCreator = checkNotNull(connectionSourceCreator, "connectionSourceCreator");
        this.persistentParams = checkNotNull(persistentParams, "persistentParams");
    }

    public MysqlConnectionSourceRule(int port, Function<ConnectionParams, MysqlConnectionSource> connectionSourceCreator) {
        this(newDefaultIntegrationTestConnectionParams(port), connectionSourceCreator);
    }
    
    public MysqlConnectionSourceRule(ConnectionParams persistentParams) {
        this(persistentParams, newDefaultConnectionSourceCreator());
    }
    
    public MysqlConnectionSourceRule(int port) {
        this(newDefaultIntegrationTestConnectionParams(port));
    }
    
    protected static ConnectionParams newDefaultIntegrationTestConnectionParams(int port) {
        checkArgument(port >= 1 && port <= 65535, "1 <= port <= 65535 required; not %d", port);
        return new ConnectionParams("localhost:" + port, "root", "root");
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
    
    public MysqlConnectionSource getConnectionSource() {
        checkState(connectionSource != null, "connectionSource not yet constructed");
        return connectionSource;
    }

    /**
     * Matcher of characters allowed in a new unique schema. Used by
     * {@link #newUniqueSchemaName()}.
     */
    @SuppressWarnings("deprecation")
    private static final CharMatcher allowedSchemaChars = CharMatcher.javaLetterOrDigit();

    protected static String newUniqueSchemaName() {
        return "u" + allowedSchemaChars.retainFrom(UUID.randomUUID().toString());
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

    private void dropSchemaIfExists() throws SQLException {
        MysqlConnectionSource currentConnectionSource = connectionSource;
        if (currentConnectionSource != null) {
            ConnectionParams connectionParams = currentConnectionSource.getConnectionParams();
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://" 
                    + connectionParams.host + "/", connectionParams.username, 
                    connectionParams.password);
                    Statement stmt = conn.createStatement()) {
                String sql = "DROP SCHEMA `" + connectionParams.schema + "`";
                System.out.println("executing: " + sql);
                stmt.execute(sql);
            }
        }
    }

    @Override
    protected void after() {
        try {
            dropSchemaIfExists();
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    protected void before() throws Throwable {
        ConnectionParams currentParams = newConnectionParams(newUniqueSchemaName());
        connectionSource = connectionSourceCreator.apply(currentParams);
        createSchema(currentParams);
    }
    
    protected ConnectionParams newConnectionParams(String schema) {
        ConnectionParams cp = persistentParams.copy();
        cp.schema = schema;
        return cp;
    }
    
}
