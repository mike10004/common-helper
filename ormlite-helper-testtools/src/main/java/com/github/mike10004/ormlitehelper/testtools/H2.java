package com.github.mike10004.ormlitehelper.testtools;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility methods relating to H2 databases.
 *
 * @author mchaberski
 */
public class H2 {

    private H2() {
    }

    /**
     * Class that facilitates dumping H2 databases.
     */
    @NotThreadSafe // it is somewhat thread-safe, but only for some multithreading circumstances
    public static class Dumper {

        public enum Compression {
            GZIP, DEFLATE, LZF, ZIP, NONE;
        }

        public enum DumpOption {
            SIMPLE,
            NODATA,
            NOPASSWORDS,
            NOSETTINGS,
            DROP;

            public String toClause() {
                return name();
            }

            private static final Function<DumpOption, String> toClauseFunction = DumpOption::toClause;

            public static Function<DumpOption, String> toClauseFunction() {
                return toClauseFunction;
            }
        }

        public static final Compression DEFAULT_COMPRESSION = Compression.NONE;

        private final Compression compression;
        private final Charset charset;
        private boolean verbose;
        private EnumSet<DumpOption> dumpOptions;
        private ImmutableSet<String> tables = ImmutableSet.of();

        /**
         * Constructs an instance with the given charset and default compression mode.
         * @param charset the charset to write the sql file in
         * @see #DEFAULT_COMPRESSION
         */
        public Dumper(Charset charset) {
            this(charset, DEFAULT_COMPRESSION);
        }

        /**
         * Constructs an instance with the given charset and compression mode.
         * @param charset the charset to write the sql file in
         * @param compression the compression to use
         */
        public Dumper(Charset charset, Compression compression) {
            this.charset = Preconditions.checkNotNull(charset);
            this.compression = Preconditions.checkNotNull(compression);
            this.dumpOptions = EnumSet.noneOf(DumpOption.class);
        }

        public synchronized Dumper setOption(DumpOption option) {
            dumpOptions.add(checkNotNull(option));
            return this;
        }

        public synchronized Dumper unsetOption(DumpOption option) {
            dumpOptions.remove(checkNotNull(option));
            return this;
        }

        public synchronized boolean isVerbose() {
            return verbose;
        }

        public synchronized Dumper setVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public synchronized Dumper setTables(String firstTable, String...otherTables) {
            tables = ImmutableSet.copyOf(Lists.asList(checkNotNull(firstTable), otherTables));
            return this;
        }

        public ImmutableSet<String> getTables() {
            return tables;
        }

        public synchronized Dumper allTables() {
            tables = ImmutableSet.of();
            return this;
        }

        /**
         * Dumps the database at a given JDBC URL to a SQL script in a file at the given pathname.
         * @param url the JDBC URL
         * @param username the username to use, or null for none
         * @param password the password to use, or null for none
         * @param outputH2SqlFile pathname of the file to write
         * @throws IOException on I/O error
         * @throws SQLException on database error
         */
        public synchronized void dump(String url, @Nullable String username, @Nullable String password, File outputH2SqlFile) throws IOException, SQLException {
            Preconditions.checkNotNull(outputH2SqlFile);
            Preconditions.checkNotNull(url);
            org.h2.Driver.load();
            String outputPathname = outputH2SqlFile.getAbsolutePath();
            outputPathname = FilenameUtils.normalize(outputPathname, true); // use unix separators
            Preconditions.checkArgument(CharMatcher.anyOf("'\"`").matchesNoneOf(outputPathname),
                    "output path must not contain quotation characters");
            StringBuilder statementBuilder = new StringBuilder("SCRIPT ");
            Iterable<DumpOption> sortedDumpOptions = Ordering.<DumpOption>natural().immutableSortedCopy(dumpOptions);
            Joiner.on(' ').appendTo(statementBuilder, Iterables.transform(sortedDumpOptions, DumpOption.toClauseFunction()::apply));
            statementBuilder.append(" TO '").append(outputPathname).append("' ");
            if (compression != Compression.NONE) {
                statementBuilder.append(" COMPRESSION ")
                        .append(compression.name()).append(' ');
            }
            statementBuilder.append(" CHARSET '").append(charset.name()).append("' ");
            if (!tables.isEmpty()) {
                statementBuilder.append(" TABLE ");
                Joiner.on(", ").appendTo(statementBuilder, tables);
            }
            String statementStr = statementBuilder.toString();
            if (isVerbose()) {
                System.out.println(statementStr);
            }
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(statementStr);
                rs.close();
            }
        }
    }

    /**
     * Exports the contents of a database at a given JDBC URL to another JDBC URL.
     * Uses a temp directory to write an intermediate
     * @param fromUrl the source JDBC URL
     * @param toJdbcUrl the destination JDBC URL
     * @param tempDir directory to use for temp files
     * @throws IOException on I/O error
     * @throws SQLException on database error
     */
    public static void transfer(String fromUrl, String toJdbcUrl, File tempDir) throws IOException, SQLException {
        Charset charset = StandardCharsets.UTF_8;
        Dumper.Compression compression = Dumper.Compression.NONE;
        Dumper dumper = new Dumper(charset, compression);
        File scriptFile = File.createTempFile("databasedump", ".h2.sql", tempDir);
        try {
            dumper.dump(fromUrl, null, null, scriptFile);
            CharSource scriptSource = Files.asCharSource(scriptFile, charset);
            try (Reader reader = scriptSource.openStream();
                 Connection conn = DriverManager.getConnection(toJdbcUrl)) {
                ResultSet rs = org.h2.tools.RunScript.execute(conn, reader);
                if (rs != null) {
                    rs.close();
                }
            }
        } finally {
            if (!scriptFile.delete()) {
                Logger.getLogger(H2.class.getName()).log(Level.WARNING, "failed to delete temporary script file at {0}", scriptFile);
            }
        }
    }

    /**
     * Abstract superclass for classes that build H2 JDBC URLs.
     */
    public abstract static class H2UrlBuilder {

        public String build() {
            String jdbcUrl = "jdbc:h2:" + constructDatabaseSpec();
            return jdbcUrl;
        }

        protected abstract String constructDatabaseSpec();
    }

    /**
     * Builder for H2 memory database JDBC URLs.
     */
    @NotThreadSafe
    public static class H2MemoryUrlBuilder extends H2UrlBuilder {

        private String schema;
        private boolean keepContentForLifeOfVM;

        public H2MemoryUrlBuilder() {
            schema = 'u' + UUID.randomUUID().toString().replace('-', '_');
        }

        public boolean isKeepContentForLifeOfVM() {
            return keepContentForLifeOfVM;
        }

        public H2MemoryUrlBuilder setKeepContentForLifeOfVM(boolean keepContentForLifeOfVM) {
            this.keepContentForLifeOfVM = keepContentForLifeOfVM;
            return this;
        }

        public String getSchema() {
            return schema;
        }

        public H2MemoryUrlBuilder setSchema(String schema) {
            this.schema = Preconditions.checkNotNull(schema);
            return this;
        }

        @Override
        protected String constructDatabaseSpec() {
            String spec = "mem:" + schema;
            if (isKeepContentForLifeOfVM()) {
                spec += ";DB_CLOSE_DELAY=-1";
            }
            return spec;
        }

    }

    /**
     * Builder for H2 file database JDBC URLs.
     */
    @NotThreadSafe
    public static class H2FileUrlBuilder extends H2UrlBuilder {

        private final File h2DbFile;
        private final Set<String> clauses;

        public H2FileUrlBuilder(File h2DbFile) {
            this.h2DbFile = Preconditions.checkNotNull(h2DbFile);
            clauses = new LinkedHashSet<>();
        }

        public H2FileUrlBuilder setOnlyOpenIfExists(boolean onlyOpenIfExists) {
            return addClause("IFEXISTS=" + onlyOpenIfExists);
        }

        public H2FileUrlBuilder setAutoMixedMode(boolean autoMixedMode) {
            return addClause("AUTO_SERVER=" + autoMixedMode);
        }

        public H2FileUrlBuilder addClause(String clause) {
            clauses.add(clause);
            return this;
        }

        @Override
        protected String constructDatabaseSpec() {
            File databaseDir = h2DbFile.getParentFile();
            checkDatabaseFilename(h2DbFile.getName());
            String nameWithoutRequiredSuffix = stripRequiredSuffix(h2DbFile.getName());
            String databaseSpecStart = new File(databaseDir, nameWithoutRequiredSuffix).getAbsolutePath();
            databaseSpecStart = FilenameUtils.normalize(databaseSpecStart, true);
            StringBuilder databaseSpecBuilder = new StringBuilder(databaseSpecStart);
            for (String clause : ImmutableSet.copyOf(clauses)) {
                databaseSpecBuilder.append(';').append(clause);
            }
            String databaseSpec = databaseSpecBuilder.toString();
            Logger.getLogger(H2.class.getName())
                    .log(Level.FINER, "using databaseSpec {0} corresponding to file {1}",
                            new Object[]{databaseSpec, h2DbFile.getAbsolutePath()});
            return "file:" + databaseSpec;
        }

        private static void checkDatabaseFilename(String filename) {
            if (!filename.endsWith(REQUIRED_SUFFIX)) {
                throw new IllegalArgumentException("invalid database filename " + filename + "; must end with " + REQUIRED_SUFFIX);
            }
        }

        private static final String REQUIRED_SUFFIX = ".h2.db";

        private static String stripRequiredSuffix(String filename) {
            return filename.substring(0, filename.length() - REQUIRED_SUFFIX.length());
        }
    }

}
