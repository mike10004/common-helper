/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import org.apache.commons.io.FileUtils;
import java.io.File;
import com.google.common.io.Files;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mchaberski
 */
public class AntExecutorTest {
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    public static final String EXPECTED_UNIX_LS_PATHNAME = "/bin/ls";
    
    private File createTempDirWithFiles() throws IOException {
        File tmpDir;
        try {
            tmpDir = temporaryFolder.newFolder();
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
        String ls_pathname = EXPECTED_UNIX_LS_PATHNAME;
        assertNotNull(ls_pathname);
        String stdout = new AntExecutor().setExecutable(ls_pathname)
                .setArguments(tmpDir.getAbsolutePath()).execute().getStdout();
        System.out.println("---LS ACTUAL START---");
        System.out.println(stdout);
        System.out.println("---LS ACTUAL END---");
        assertEquals(String.format("a%nb%nc"), stdout);
    }

    static boolean isLinux() {
        return Platforms.getPlatform().isLinux();
    }
    
    static interface ExecutableTestCase {
        String getName();
        List<String> buildArguments() throws Exception;
        int getExpectedExitCode();
    }
    
    private static class NonwindowsNonzeroExitTestCase implements ExecutableTestCase {
        
        private final File emptyDir;

        public NonwindowsNonzeroExitTestCase(File emptyDir) {
            this.emptyDir = emptyDir;
        }

        @Override
        public String getName() {
            return "ls";
        }

        @Override
        public List<String> buildArguments() throws IOException {
            File file = new File(emptyDir, "fileThatDoesNotExist");
            return Arrays.asList(file.getAbsolutePath());
        }

        @Override
        public int getExpectedExitCode() {
            return 2;
        }
        
    }

    private static class NonwindowsZeroExitTestCase implements ExecutableTestCase {
        
        private final File emptyDir;

        public NonwindowsZeroExitTestCase(File emptyDir) {
            this.emptyDir = emptyDir;
        }

        @Override
        public String getName() {
            return "ls";
        }

        @Override
        public List<String> buildArguments() throws IOException {
            File file = new File(emptyDir, "newfile");
            Files.touch(file);
            return Arrays.asList(file.getAbsolutePath());
        }

        @Override
        public int getExpectedExitCode() {
            return 0;
        }
        
    }
    
    private static class WindowsZeroExitTestCase implements ExecutableTestCase {

        @Override
        public String getName() {
            return "cmd";
        }

        @Override
        public List<String> buildArguments() {
            return Arrays.asList("/C", "echo", "hello, world");
        }

        @Override
        public int getExpectedExitCode() {
            return 0;
        }
        
    }
    
    private static class WindowsNonzeroExitTestCase implements ExecutableTestCase {

        @Override
        public String getName() {
            return "cmd";
        }

        @Override
        public List<String> buildArguments() {
            return Arrays.asList("/c", "notacommand");
        }

        @Override
        public int getExpectedExitCode() {
            return 1;
        }
        
    }
    
    @Test(expected=BuildException.class)
    public void testNonzeroExitCode_defaultBehavior() throws Exception {
        System.out.println("testNonzeroExitCode_defaultBehavior");
        File emptyDir = temporaryFolder.newFolder();
        ExecutableTestCase testCase = Platforms.getPlatform().isWindows() 
                ? new WindowsNonzeroExitTestCase()
                : new NonwindowsNonzeroExitTestCase(emptyDir);
        String executableName = testCase.getName();
        new AntExecutor()
                .setExecutable(executableName)
                .setArguments(testCase.buildArguments())
                .execute();
    }
    
    @Test
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testNonzeroExitCode_NoFailOnError() throws Exception {
        System.out.println("testNonzeroExitCode_NoFailOnError");
        File emptyDir = temporaryFolder.newFolder();
        ExecutableTestCase testCase = Platforms.getPlatform().isWindows()
                ? new WindowsNonzeroExitTestCase()
                : new NonwindowsNonzeroExitTestCase(emptyDir);
        String executableName = testCase.getName();
        AntExecutor e = new AntExecutor();
        e.setExecutable(executableName);
        e.setArguments(testCase.buildArguments());
        e.getTask().setFailonerror(false);
        e.execute();
        Integer result = e.getResult();
        System.out.println("exit code " + result);
        assertNotNull("expected nonnull result", result);
        int expectedExitCode = testCase.getExpectedExitCode();
        assertEquals(expectedExitCode, result.intValue());
    }
    
    @Test
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testZeroExitCode() throws Exception {
        System.out.println("testZeroExitCode");
        File emptyDir = temporaryFolder.newFolder();
        ExecutableTestCase testCase = Platforms.getPlatform().isWindows() 
                ? new WindowsZeroExitTestCase()
                : new NonwindowsZeroExitTestCase(emptyDir);
        String executableName = testCase.getName();
        AntExecutor executor = new AntExecutor()
                .setExecutable(executableName)
                .setArguments(testCase.buildArguments())
                .execute();
        Integer result = executor.getResult();
        System.out.println("stdout:");
        System.out.println("==============================");
        System.out.println(executor.getStdout());
        System.out.println("==============================");
        System.out.println("exit code: " + result);
        assertNotNull("expect nonnull result", result);
        assertEquals(testCase.getExpectedExitCode(), result.intValue());
    }
    


}