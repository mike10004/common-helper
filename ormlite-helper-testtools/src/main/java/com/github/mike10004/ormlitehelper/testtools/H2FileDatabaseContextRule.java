/**
 * (c) 2016 Mike Chaberski. Distributed under terms of the MIT License.
 */
package com.github.mike10004.ormlitehelper.testtools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.github.mike10004.common.dbhelp.H2FileConnectionSource;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.github.mike10004.common.io.IOSupplier;
import org.h2.tools.RunScript;
import org.junit.rules.TemporaryFolder;

/**
 * Rule that produces an H2 file database context for each test.
 *
 * @author mchaberski
 */
public class H2FileDatabaseContextRule extends DatabaseContextRule {

    private File dbFile;
    private final PathnameProvider pathnameProvider;
    private final boolean autoMixedMode, onlyOpenIfExists;
    private final CharSource creationScriptSource;
    private final CharSource destructionScriptSource;
    private boolean verbose;

    public H2FileDatabaseContextRule(PathnameProvider pathnameProvider, boolean autoMixedMode, boolean onlyOpenIfExists, CharSource creationScriptSource, CharSource destructionScriptSource) {
        super();
        this.pathnameProvider = checkNotNull(pathnameProvider);
        this.autoMixedMode = autoMixedMode;
        this.onlyOpenIfExists = onlyOpenIfExists;
        this.creationScriptSource = checkNotNull(creationScriptSource);
        this.destructionScriptSource = checkNotNull(destructionScriptSource);
    }

    public static Builder forSource(ByteSource dbFileByteSource, final TemporaryFolder temporaryFolder) {
        return forSource(dbFileByteSource, new IOSupplier<File>() {

            @Override
            public File get() throws IOException {
                return temporaryFolder.getRoot();
            }
        });
    }

    public static Builder forSource(ByteSource dbFileByteSource, IOSupplier<File> parentDirSupplier) {
        return forPathnameProvider(new FreshlyCopiedFilePathnameProvider(dbFileByteSource, parentDirSupplier));
    }

    private static class FreshlyCopiedFilePathnameProvider implements PathnameProvider {

        private final ByteSource dbFileByteSource;
        private final IOSupplier<File> parentDirSupplier;

        public FreshlyCopiedFilePathnameProvider(ByteSource dbFileByteSource, IOSupplier<File> parentDirSupplier) {
            this.dbFileByteSource = dbFileByteSource;
            this.parentDirSupplier = parentDirSupplier;
        }

        @Override
        public File providePathname() throws IOException {
            File parentDir = parentDirSupplier.get();
            File destFile = File.createTempFile("H2FileDatabaseContextRule", ".h2.db", parentDir);
            dbFileByteSource.copyTo(Files.asByteSink(destFile));
            return destFile;
        }

    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public static Builder forPathnameProvider(PathnameProvider pathnameProvider) {
        return new Builder(pathnameProvider);
    }

    public static Builder forNewFile(TemporaryFolder temporaryFolder, String prefix) {
        checkArgument(prefix != null && prefix.length() > 3, "prefix must be non-null with length >= 4");
        PathnameProvider pp = new TemporaryFolderPathnameProvider(temporaryFolder, prefix);
        return forPathnameProvider(pp);
    }

    public static Builder forConstantFile(File pathname) {
        return forPathnameProvider(new ConstantPathnameProvider(pathname));
    }

    @NotThreadSafe
    public static class Builder {

        public static final CharSource DESTROY_ALL_CHAR_SOURCE = CharSource.wrap("\nDROP ALL OBJECTS;\n");

        private final PathnameProvider pathnameProvider;
        private boolean autoMixedMode = false, onlyOpenIfExists = false;
        private final List<CharSource> creationScriptSources = new ArrayList<>();
        private final List<CharSource> destructionScriptSources = Lists.newArrayList(DESTROY_ALL_CHAR_SOURCE);

        Builder(PathnameProvider pathnameProvider) {
            this.pathnameProvider = checkNotNull(pathnameProvider);
        }

        public Builder autoMixedMode() {
            autoMixedMode = true;
            return this;
        }

        public Builder onlyOpenIfExists() {
            onlyOpenIfExists = true;
            return this;
        }

        public Builder createdWith(CharSource creationScriptSource) {
            creationScriptSources.add(checkNotNull(creationScriptSource));
            return this;
        }

        public Builder clearCreationScripts() {
            creationScriptSources.clear();
            return this;
        }

        public Builder clearDestructionScripts() {
            destructionScriptSources.clear();
            return this;
        }

        public Builder destroyedWith(CharSource destructionScriptSource) {
            destructionScriptSources.add(checkNotNull(destructionScriptSource));
            return this;
        }

        public H2FileDatabaseContextRule build() {
            return new H2FileDatabaseContextRule(pathnameProvider, autoMixedMode, onlyOpenIfExists,
                    CharSource.concat(creationScriptSources),
                    CharSource.concat(destructionScriptSources));
        }

    }

    @Override
    protected H2FileConnectionSource createConnectionSource() {
        checkState(dbFile == null, "dbFile already created");
        try {
            dbFile = pathnameProvider.providePathname();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create database file", ex);
        }
        try {
            executeCreationScript();
        } catch (IOException | SQLException ex) {
            throw new IllegalStateException("database creation script execution failed", ex);
        }
        H2FileConnectionSource connectionSource = new H2FileConnectionSource(dbFile);
        configureNewConnectionSource(connectionSource);
        return connectionSource;
    }

    protected String buildJdbcUrl() {
        H2.H2FileUrlBuilder builder = new H2.H2FileUrlBuilder(dbFile);
        if (autoMixedMode) {
            builder.setAutoMixedMode(true);
        }
        if (onlyOpenIfExists) {
            builder.setOnlyOpenIfExists(true);
        }
        String jdbcUrl = builder.build();
        return jdbcUrl;
    }

    protected void executeCreationScript() throws IOException, SQLException {
        executeScript(creationScriptSource);
    }

    protected void executeDestructionScript() throws IOException, SQLException {
        executeScript(destructionScriptSource);
    }

    protected void executeScript(CharSource scriptSource) throws IOException, SQLException {
        String jdbcUrl = buildJdbcUrl();
        runScriptOnJdbcUrl(jdbcUrl, scriptSource);
    }

    protected void runScriptOnJdbcUrl(String jdbcUrl, CharSource scriptSource) throws IOException, SQLException {
        if (isVerbose()) {
            System.out.println("=================================================================");
            System.out.println("=================================================================");
            System.out.print("-- ");
            System.out.print(H2FileDatabaseContextRule.class.getSimpleName());
            System.out.format(" executing script on %s from %n", jdbcUrl, scriptSource);
            scriptSource.copyTo(System.out);
            System.out.println("=================================================================");
            System.out.println("=================================================================");
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Reader reader = scriptSource.openStream()) {
            RunScript.execute(conn, reader);
        }
    }

    protected void configureNewConnectionSource(H2FileConnectionSource connectionSource) {
        connectionSource.setAutoMixedMode(autoMixedMode);
    }

    @Override
    protected void after() {
        try {
            executeDestructionScript();
        } catch (IOException | SQLException ex) {
            Logger.getLogger(H2FileDatabaseContextRule.class.getName()).log(Level.WARNING, "failed to execute rule post-script", ex);
        }
        super.after();
    }

    public static interface PathnameProvider {
        File providePathname() throws IOException;
    }

    public static class ConstantPathnameProvider implements PathnameProvider {

        private final File pathname;

        public ConstantPathnameProvider(File pathname) {
            this.pathname = checkNotNull(pathname);
        }

        @Override
        public File providePathname() throws IOException {
            return pathname;
        }
    }

    public static class TemporaryFolderPathnameProvider implements PathnameProvider {

        public final TemporaryFolder temporaryFolder;
        public final String prefix;

        public TemporaryFolderPathnameProvider(TemporaryFolder temporaryFolder, @Nullable String prefix) {
            this.temporaryFolder = temporaryFolder;
            this.prefix = Strings.isNullOrEmpty(prefix) ? "TemporaryDatabase" : prefix;
        }

        @Override
        public File providePathname() throws IOException {
            File subdir = temporaryFolder.newFolder();
            File dbFile = new File(subdir, prefix + ".h2.db");
            return dbFile;
        }
    }

    public File getDatabaseFile() {
        return dbFile;
    }
}
