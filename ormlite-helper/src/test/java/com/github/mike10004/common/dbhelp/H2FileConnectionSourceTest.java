package com.github.mike10004.common.dbhelp;

import com.github.mike10004.common.dbhelp.AbstractH2FileConnectionSource.DefaultSchemaTransform;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class H2FileConnectionSourceTest {
    
    @BeforeClass
    public static void setUpClass() {
        System.out.println("ensure this is on classpath: " 
                + AbstractH2FileConnectionSource.DefaultSchemaTransform.class.getName());
        System.setOut(new PrintStream(new CloseShieldOutputStream(System.out)));
        System.setErr(new PrintStream(new CloseShieldOutputStream(System.err)));
    }
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected boolean isLinux() {
        return isOsNameStartsWith("linux");
    }
    
    protected boolean isWindows() {
        return isOsNameStartsWith("windows");
    }
    
    protected boolean isOsNameStartsWith(String prefix) {
        checkNotNull(prefix);
        return System.getProperty("os.name").toLowerCase().startsWith(prefix);
    }
    
    @Test
    public void testConstructSchema_nonWindows() {
        System.out.println("testConstructSchema_linux");
        if (isWindows()) {
            System.out.format("os.name = %s; skipping test%n", System.getProperty("os.name"));
            return;
        }
        DefaultSchemaTransform xform = new DefaultSchemaTransform();
        String expected, actual;
        File file;
        
        expected = "/path/to/some/database/file";
        file = new File(expected + ".h2.db");
        actual = xform.apply(file);
        System.out.format("%s -> %s%n", file, actual);
        assertEquals(expected, actual);
        
        expected = "/path/with spaces/some/database/filename with spaces";
        file = new File(expected + ".h2.db");
        actual = xform.apply(file);
        System.out.format("%s -> %s%n", file, actual);
        assertEquals(expected, actual);
        
        expected = System.getProperty("user.dir") + "/relative-path-file";
        file = new File("relative-path-file.h2.db");
        actual = xform.apply(file);
        System.out.format("%s -> %s%n", file, actual);
        assertEquals(expected, actual);
        
        try {
            xform.apply(new File("/path/to/file/with-bad-extension.db"));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals(DefaultSchemaTransform.ILLEGAL_DB_FILENAME_MESSAGE, e.getMessage());
        }

        try {
            xform.apply(new File("/path/to/file/with-bad-extension"));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals(DefaultSchemaTransform.ILLEGAL_DB_FILENAME_MESSAGE, e.getMessage());
        }
    
    }
    
    static void assertTransformCorrect(String expected, Function<File, String> xform, File input) {
        String actual = xform.apply(input);
        System.out.format("%s -> '%s' (expected '%s')%n", input, actual, expected);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testConstructSchema_windows() {
        if (!isWindows()) {
            return;
        }
        System.out.println("testConstructSchema_windows");
        DefaultSchemaTransform xform = new DefaultSchemaTransform();
        String expected;
        File file;
        
        expected = "C:/path/to/some/database/file";
        file = new File(expected + ".h2.db");
        assertTransformCorrect(expected, xform, file);
        
        expected = "C:/path/with spaces/some/database/filename with spaces";
        file = new File(expected + ".h2.db");
        assertTransformCorrect(expected, xform, file);
        
        expected = FilenameUtils.normalizeNoEndSeparator(System.getProperty("user.dir"), true) 
                + "/relative-path-file";
        file = new File("relative-path-file.h2.db");
        assertTransformCorrect(expected, xform, file);
        
        try {
            xform.apply(new File("C:/path/to/file/with-bad-extension.db"));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals(DefaultSchemaTransform.ILLEGAL_DB_FILENAME_MESSAGE, e.getMessage());
        }

        try {
            xform.apply(new File("C:/path/to/file/with-bad-extension"));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals(DefaultSchemaTransform.ILLEGAL_DB_FILENAME_MESSAGE, e.getMessage());
        }
    
    }
    
    @Test
    public void testInsertionAndRetrieval() throws Exception {
        System.out.println("testInsertionAndRetrieval");
        File tmpDir = temporaryFolder.newFolder();
        File dbFile = new File(tmpDir, "commonfacility.h2.db");
        System.out.println("h2 db file: " + dbFile);
        Files.touch(dbFile);
        H2FileConnectionSource cs = new H2FileConnectionSource(dbFile);
        DatabaseContext db = new DefaultDatabaseContext(cs);
        try {
            db.getTableUtils().createAllTables(ImmutableList.of(Customer.class, Order.class));
            DatabaseTests.insertCustomersAndOrders(db);
        } finally {
            db.closeConnections(true);
        }

        System.out.println("dbFile.length = " + dbFile.length());
        assertTrue("expect positive database file size, but is " + dbFile.length(), dbFile.length() > 0);
    }
    
    @Test
    public void testInsertionAndRetrieval_pathWithSpaces() throws Exception {
        System.out.println("testInsertionAndRetrieval_pathWithSpaces");
        File tmpDir = temporaryFolder.newFolder();
        File dbFile = new File(tmpDir, "common facility test database.h2.db");
        Files.touch(dbFile);
        System.out.println("h2 db file: " + dbFile);
        H2FileConnectionSource cs = new H2FileConnectionSource(dbFile);
        String jdbcUrl = cs.constructJdbcUrl();
        System.out.println("jdbc url = '" + jdbcUrl + "'");
        DatabaseContext db = new DefaultDatabaseContext(cs);
        try {
            db.getTableUtils().createAllTables(ImmutableList.of(Customer.class, Order.class));
            DatabaseTests.insertCustomersAndOrders(db);
        } finally {
            db.closeConnections(true);
        }

        System.out.println("dbFile.length = " + dbFile.length());
        assertTrue("expect positive database file size, but is " + dbFile.length(), dbFile.length() > 0);
    }

    //@org.junit.Ignore
    @Test(timeout = 30000L)
    public void testMixedMode()  throws Exception {
        if (!isLinux()) {
            return; // hard to detect Maven home on other platforms
        }
        System.out.println("testMixedMode");
        File dbTmpDir = temporaryFolder.newFolder();
        File tmpDir = temporaryFolder.newFolder();
        File dbFile = new File(dbTmpDir, "commonfacility.h2.db");
        System.out.println("h2 db file: " + dbFile);
        
        H2FileConnectionSource cs = new H2FileConnectionSource(dbFile);
        cs.setAutoMixedMode(true);
        System.out.println("jdbc url = " + cs.constructJdbcUrl());
        System.out.println("M2_HOME=" + System.getenv("M2_HOME"));
        String mavenHomeDirPathname = System.getenv("M2_HOME");
        List<File> parents = new ArrayList<>();
        if (mavenHomeDirPathname != null) {
            parents.add(new File(mavenHomeDirPathname, "bin"));
        } else {
            String[] possibleHomes = {
                    "/usr/local/share/maven3",
                    "/usr/local/share/maven2",
                    "/usr/local/share/maven",
                    "/usr/share/maven3",
                    "/usr/share/maven2",
                    "/usr/share/maven",
            };
            for (String possibleHome : possibleHomes) {
                File binDir = new File(possibleHome, "bin");
                parents.add(binDir);
            }
        }
        List<File> systemPathDirs = Splitter.on(File.pathSeparatorChar).splitToList(System.getenv("PATH"))
                .stream().map(File::new).collect(Collectors.toList());
        Iterables.addAll(parents, systemPathDirs);
        File mavenExecutable = null;
        for (File parent : parents) {
            File possibleMaven = new File(parent, "mvn");
            if (possibleMaven.isFile() && possibleMaven.canExecute()) {
                mavenExecutable = possibleMaven;
                break;
            }
        }
        assertNotNull("test requires detectable maven executable file that exists and is executable", 
                mavenExecutable);
        File mavenBinDir = mavenExecutable.getParentFile();
        assertNotNull("mvn executable " + mavenExecutable + " in unexpected directory", mavenBinDir);
        File mavenHomeDir = mavenBinDir.getParentFile();
        assertNotNull("mvn home, parent of " + mavenBinDir + ", is null", mavenHomeDir);
        DatabaseContext db = new DefaultDatabaseContext(cs);
        try {
            db.getTableUtils().createAllTables(ImmutableList.of(Customer.class, Order.class));
            DatabaseTests.insertCustomersAndOrders(db); // create some records
            
            System.out.println("created tmp directory for maven pom.xml");
            File pomFile = new File(tmpDir, "pom.xml");
            URL projectPomResource = getClass().getResource("h2-execute-pom.xml");
            checkNotNull(projectPomResource, "failed to find h2-execute-pom.xml in package " + getClass().getPackage());
            Resources.asByteSource(projectPomResource).copyTo(com.google.common.io.Files.asByteSink(pomFile));
            String jdbcUrl = cs.constructJdbcUrl();
            String sqlCommand = "'SELECT id, name, address FROM Customer WHERE 1'";
            ProcessResult<String, String> ae = execute(mavenExecutable, mavenHomeDir, jdbcUrl, sqlCommand, tmpDir);
            // printing this stuff confuses the JetBrains IDEA Maven output parser
//            System.out.println("=======================================");
//            System.out.println("======================================= STDOUT START");
//            System.out.println("=======================================");
//            System.out.println(ae.content().stdout());
//            System.out.println("=======================================");
//            System.out.println("======================================= STDOUT END");
//            System.out.println("=======================================");
//            System.out.println("=======================================");
//            System.out.println("======================================= STDERR START");
//            System.out.println("=======================================");
//            System.out.println(ae.content().stderr());
//            System.out.println("=======================================");
//            System.out.println("======================================= STDERR END");
//            System.out.println("=======================================");
            int exitCode = ae.exitCode();
            System.out.println("exit code " + exitCode);
            assertEquals("exit code", 0, exitCode);
            String actualStdout = ae.content().stdout();
            String expectedStdout = "1  | Jason | 123 Anywhere La";
            assertTrue("actual stdout must contain" + expectedStdout, 
                    actualStdout.contains(expectedStdout));
        } finally {
            db.closeConnections(true);
        }
    }
    
    private ProcessResult<String, String> execute(File mavenExecutable, File mavenHomeDir, String jdbcUrl, String sqlCommand, File tmpDir) {
        String[] args = {
                "exec:java",
                "--batch-mode",
                "-Dexec.killAfter=-1",
                "-Dexec.mainClass=" + org.h2.tools.Shell.class.getName(),
                "-Dexec.args=-url " + jdbcUrl + " -sql " + sqlCommand + ""
        };
        String javaHomePath = System.getProperty("java.home");
        System.out.println("in Maven process environment, setting JAVA_HOME=" + javaHomePath);
        Subprocess subprocess = Subprocess.running(mavenExecutable)
                .from(tmpDir)
                .env("JAVA_HOME", javaHomePath)
                .env("M2_HOME", mavenHomeDir.getAbsolutePath())
                .args(Arrays.asList(args))
                .build();
        ProcessResult<String, String> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            result = subprocess.launcher(processTracker)
                    .outputStrings(Charset.defaultCharset()) // whatever Maven uses on the platform
                    .launch()
                    .await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
