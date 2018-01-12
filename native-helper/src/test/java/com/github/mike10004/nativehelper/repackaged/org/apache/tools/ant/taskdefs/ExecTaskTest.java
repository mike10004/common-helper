package com.github.mike10004.nativehelper.repackaged.org.apache.tools.ant.taskdefs;

import com.github.mike10004.nativehelper.Platforms;
import com.google.common.io.Files;
import org.apache.tools.ant.Project;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class ExecTaskTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void basic_ls() throws Exception {
        assumeNotWindows();
        ExecTask task = new ExecTask();
        assertNotNull("task.project", task.getProject());
        task.setExecutable("ls");
        File dir = temporaryFolder.getRoot();
        String filename = "testfile.empty";
        Files.touch(new File(dir, filename));
        task.setDir(dir);
        File outputFile = new File(dir, "stdout.txt");
        task.setOutput(outputFile);
        task.execute();
        String actual = Files.asCharSource(outputFile, Charset.defaultCharset()).read();
        assertEquals("output", filename, actual.trim());
    }

    protected static void assumeNotWindows() {
        Assume.assumeFalse("this test is ignored on Windows", Platforms.getPlatform().isWindows());
    }
}