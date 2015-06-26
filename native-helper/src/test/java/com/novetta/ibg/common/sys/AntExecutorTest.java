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
    
    static interface FileLister {
        String getExecutableName();
        int getFileNotFoundExitCode();
    }
    
    private FileLister getPlatformFileLister() {
        Platform platform = Platforms.getPlatform();
        if (platform.isWindows()) {
            return new FileLister(){

                @Override
                public String getExecutableName() {
                    return "DIR";
                }

                @Override
                public int getFileNotFoundExitCode() {
                    return 1;
                }
            };
        } else {
            return new FileLister(){

                @Override
                public String getExecutableName() {
                    return "ls";
                }

                @Override
                public int getFileNotFoundExitCode() {
                    return 2;
                }
            };
        }
    }
    
    @Test(expected=BuildException.class)
    public void testNonzeroExitCode_defaultBehavior() throws BuildException, IOException {
        System.out.println("testNonzeroExitCode_defaultBehavior");
        FileLister fileLister = getPlatformFileLister();
        String executableName = fileLister.getExecutableName();
        File targetFile = new File(temporaryFolder.newFolder(), "fakefile");
        checkState(!targetFile.exists());
        new AntExecutor()
                .setExecutable(executableName)
                .setArguments(targetFile.getAbsolutePath())
                .execute();
    }
    
    @Test
    public void testNonzeroExitCode_NoFailOnError() throws BuildException, IOException {
        System.out.println("testNonzeroExitCode_NoFailOnError");
        FileLister fileLister = getPlatformFileLister();
        String executableName = fileLister.getExecutableName();
        File targetFile = new File(temporaryFolder.newFolder(), "fakefile");
        checkState(!targetFile.exists());
        AntExecutor e = new AntExecutor();
        e.setExecutable(executableName);
        e.setArguments(targetFile.getAbsolutePath());
        e.getTask().setFailonerror(false);
        e.execute();
        Integer result = e.getResult();
        System.out.println("list fake target file returned " + result);
        assertNotNull("expected nonnull result", result);
        int expectedExitCode = fileLister.getFileNotFoundExitCode();
        assertEquals(expectedExitCode, result.intValue());
    }
}