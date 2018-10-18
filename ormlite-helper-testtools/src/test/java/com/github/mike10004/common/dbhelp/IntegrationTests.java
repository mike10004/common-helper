package com.github.mike10004.common.dbhelp;

public class IntegrationTests {
    
    /**
     * Port on which mysql is listening. This is set by 
     * org.codehaus.mojo:build-helper-maven-plugin, goal reserve-network-port.
     */
    private static final int MYSQL_PORT = Integer.parseInt(System.getProperty("mysql.port"));

    private IntegrationTests() {}
    
    public static int getMysqlPort() {
        return MYSQL_PORT;
    }
}
