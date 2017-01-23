package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.io.CharSource;
import com.j256.ormlite.jdbc.JdbcConnectionSource;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mchaberski
 */
public class H2FileDatabaseContextRuleTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testConsistencyOfJdbcUrl() throws Throwable {
        System.out.println("testConsistencyOfJdbcUrl");
        File dbFile = new File(temporaryFolder.getRoot(), "mydatabase.h2.db");
        System.out.println("pathname: " + dbFile);
        H2FileDatabaseContextRule.PathnameProvider pp = new H2FileDatabaseContextRule.ConstantPathnameProvider(dbFile);
        boolean[] autoMixedModes = { false, true };
        for (boolean autoMixedMode : autoMixedModes) {
            System.out.println("testing autoMixedMode=" + autoMixedMode);
            DatabaseContextRule rule = new H2FileDatabaseContextRule(pp, autoMixedMode, false, CharSource.empty(), CharSource.empty());
            rule.before();
            try {
                rule.getConnectionSource().getReadOnlyConnection("blah").close(); // so that prepare & initialize are called
                String jdbcUrlConfiguredByRule = ((JdbcConnectionSource)rule.getConnectionSource()).getUrl();
                String jdbcUrlFromUrlBuilder = ((H2FileDatabaseContextRule)rule).buildJdbcUrl();
                System.out.println("url from connectionsource configured by rule: " + jdbcUrlConfiguredByRule);
                System.out.println("url from url builder: " + jdbcUrlFromUrlBuilder);
                assertEquals(jdbcUrlConfiguredByRule, jdbcUrlFromUrlBuilder);
            } finally {
                rule.after();
            }
        }
    }
}
