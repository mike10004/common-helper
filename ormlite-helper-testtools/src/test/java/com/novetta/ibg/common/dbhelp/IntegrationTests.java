/*
 * (c) 2015 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.dbhelp;

/**
 *
 * @author mchaberski
 */
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
