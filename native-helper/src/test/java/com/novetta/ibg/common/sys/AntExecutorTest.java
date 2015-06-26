/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import org.apache.commons.io.FileUtils;
import java.io.File;
import com.google.common.io.Files;
import java.io.IOException;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class AntExecutorTest {
    
    public AntExecutorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
//    public static String ls_pathname;
    
    public static final String EXPECTED_WIN_LS_PATHNAME = new File("c:/Program Files (x86)/GnuWin32/bin/ls.exe").getAbsolutePath();
    public static final String EXPECTED_UNIX_LS_PATHNAME = "/bin/ls";
    
    private static File createTempDirWithFiles() throws IOException {
        File tmpDir;
        try {
            tmpDir = Files.createTempDir();
            System.out.println("created: " + tmpDir);
        } catch (IllegalStateException e) {
            throw new IOException("temp dir not created: " + e);
        }
        for (String fn : new String[]{"a", "b", "c"}) {
            Files.touch(new File(tmpDir, fn));
        }
        return tmpDir;
    }
    
    @Test
    public void testExecuteTree() throws Exception {
        if (!Platforms.getPlatform().isWindows()) {
            return;
        }
        File tmpDir = createTempDirWithFiles();
        String tree_pathname = "c:/windows/system32/tree.com";
        
        try {
            String stdout = new AntExecutor().setExecutable(tree_pathname)
                    .setArguments(tmpDir.getAbsolutePath(), "/F").execute().getStdout();
            /**
             * Expected output is like
             * ============
             * Folder PATH listing
             * Volume serial number is BLAH-BLAH
             * C:\SOMETHING\SOMETHING\YADA\YADA
             *     a
             *     b
             *     c
             * 
             * No subfolders exist
             * =============
             */
            System.out.println("---TREE ACTUAL START---");
            System.out.println(stdout);
            System.out.println("---TREE ACTUAL END---");
            assertNotNull(stdout);
            assertFalse(stdout.isEmpty());
            Function<String, String> trim = new Function<String, String>() {

                @Override
                public String apply(String input) {
                    return input.trim();
                }
                
            };
            List<String> filenameLines = ImmutableList.copyOf(Iterables.transform(CharSource.wrap(stdout).readLines().subList(3, 6), trim));
            assertEquals(ImmutableList.of("a", "b", "c"), filenameLines);
        } finally {
            try {
                if (tmpDir.exists()) {
                    FileUtils.deleteDirectory(tmpDir);
                    System.out.println("deleted: " + tmpDir);
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }
    
    @Test
    public void testExecuteLS() throws Exception {
        if (!isLinux()) return;
        File tmpDir = createTempDirWithFiles();
        String ls_pathname = "/bin/ls";
        assertNotNull(ls_pathname);
        try {
            String stdout = new AntExecutor().setExecutable(ls_pathname)
                    .setArguments(tmpDir.getAbsolutePath()).execute().getStdout();
            System.out.println("---LS ACTUAL START---");
            System.out.println(stdout);
            System.out.println("---LS ACTUAL END---");
            assertEquals(String.format("a%nb%nc"), stdout);
        } finally {
            try {
                if (tmpDir.exists()) {
                    FileUtils.deleteDirectory(tmpDir);
                    System.out.println("deleted: " + tmpDir);
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    static boolean isLinux() {
        return Platforms.getPlatform().isLinux();
    }
    
    @Test(expected=BuildException.class)
    public void testWhichUnsuccessful() throws BuildException {
        new AntExecutor().setExecutable("which").setArguments("fakefile").execute();
    }
    
    @Test
    public void testWhichUnsuccessfulNoFailOnError() throws BuildException {
        AntExecutor e = new AntExecutor();
        e.setExecutable("which");
        e.setArguments("fakefile");
        e.getTask().setFailonerror(false);
        e.execute();
        Integer result = e.getResult();
        System.out.println("which fakefile returned " + result);
        assertEquals(result, Integer.valueOf(1));
        String stdout = e.getStdout();
        assertEquals("", stdout);
//        String stderr = e.getStderr();
//        assertEquals("", stderr);
    }
}