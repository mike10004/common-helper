package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.io.CharSource;
import com.novetta.ibg.common.dbhelp.H2FileConnectionSource;
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

    private static class ExposedRule extends H2FileDatabaseContextRule {

        public ExposedRule(PathnameProvider pathnameProvider, boolean autoMixedMode, boolean onlyOpenIfExists) {
            super(pathnameProvider, autoMixedMode, onlyOpenIfExists, CharSource.empty(), CharSource.empty());
        }

        @Override
        protected void before() throws Throwable {
            super.before();
        }

        @Override
        protected void after() {
            super.after();
        }

        public H2FileConnectionSource connectionSource;

        @Override
        protected H2FileConnectionSource createConnectionSource() {
            connectionSource = super.createConnectionSource();
            return connectionSource;
        }

    }

    @Test
    public void testConsistencyOfJdbcUrl() throws Throwable {
        System.out.println("testConsistencyOfJdbcUrl");
        File dbFile = new File(temporaryFolder.getRoot(), "mydatabase.h2.db");
        System.out.println("pathname: " + dbFile);
        H2FileDatabaseContextRule.PathnameProvider pp = new H2FileDatabaseContextRule.ConstantPathnameProvider(dbFile);
        boolean[] autoMixedModes = { false, true };
        for (boolean autoMixedMode : autoMixedModes) {
            System.out.println("testing autoMixedMode=" + autoMixedMode);
            ExposedRule rule = new ExposedRule(pp, autoMixedMode, false);
            rule.before();
            try {
                rule.connectionSource.getReadOnlyConnection().close(); // so that prepare & initialize are called
                String jdbcUrlConfiguredByRule = rule.connectionSource.getUrl();
                String jdbcUrlFromUrlBuilder = rule.buildJdbcUrl();
                System.out.println("url from connectionsource configured by rule: " + jdbcUrlConfiguredByRule);
                System.out.println("url from url builder: " + jdbcUrlFromUrlBuilder);
                assertEquals(jdbcUrlConfiguredByRule, jdbcUrlFromUrlBuilder);
            } finally {
                rule.after();
            }
        }
    }
}
