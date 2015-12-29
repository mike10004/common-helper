/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mike10004.nativehelper;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.novetta.ibg.common.sys.Platform;
import com.novetta.ibg.common.sys.Platforms;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mchaberski
 */
public class ProgramWithOutputFilesTest {
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testExecute_outputToFiles_stdout() throws IOException {
        System.out.println("testExecute_outputToFiles_stdout");
        byte[] expectedStdout = ("hello" + System.getProperty("line.separator")).getBytes(Charset.defaultCharset());
        testExecute_outputToFiles("echo hello", 0, expectedStdout, new byte[0]);
    }
    
    @Test
    public void testExecute_outputToFiles_stderr() throws IOException {
        System.out.println("testExecute_outputToFiles_stderr");
        byte[] expectedStderr = ("hello" + System.getProperty("line.separator")).getBytes(Charset.defaultCharset());
        testExecute_outputToFiles("echo hello >&2", 0, new byte[0], expectedStderr);
    }
    
    private void testExecute_outputToFiles(String command, int expectedExitCode, byte[] expectedStdout, byte[] expectedStderr) throws IOException {
        final File stdoutFile = temporaryFolder.newFile();
        final File stderrFile = temporaryFolder.newFile();
        System.out.format("output files: %s, %s%n", stdoutFile, stderrFile);
        ProgramWithOutputFilesResult result = testExecute(command, expectedExitCode, expectedStdout, expectedStderr, new Function<Program.Builder, ProgramWithOutputFiles>() {
            @Override
            public ProgramWithOutputFiles apply(Program.Builder builder) {
                return builder.outputToFiles(stdoutFile, stderrFile);
            }
        });
        assertEquals(stdoutFile, result.getStdoutFile());
        assertEquals(stderrFile, result.getStderrFile());
    }
    
    private ProgramWithOutputFilesResult testExecute(String command, int expectedExitCode, byte[] expectedStdout, byte[] expectedStderr, Function<Program.Builder, ProgramWithOutputFiles> programMaker) throws IOException {
        Program.Builder builder = ProgramBuilders.shell();
        ProgramWithOutputFiles program = programMaker.apply(builder.arg(command));
        @SuppressWarnings("null")
        ProgramWithOutputFilesResult result = program.execute();
        System.out.println("exit code " + result.getExitCode());
        assertEquals("exitCode", expectedExitCode, result.getExitCode());
        byte[] actualStdout= Files.toByteArray(result.getStdoutFile());
        byte[] actualStderr = Files.toByteArray(result.getStderrFile());
        System.out.println("stdout length: " + actualStdout.length);
        System.out.println("stderr length: " + actualStderr.length);
        assertArrayEquals("stderr", expectedStderr, actualStderr);
        assertArrayEquals("stdout", expectedStdout, actualStdout);
        return result;
    }

    @Test
    public void testExecute_tempDirSpecified_stdout() throws IOException {
        System.out.println("testExecute_tempDirSpecified_stdout");
        File tempDir = temporaryFolder.newFolder();
        byte[] expectedStdout = ("hello" + System.getProperty("line.separator")).getBytes(Charset.defaultCharset());
        testExecute_tempDirSpecified(tempDir, "echo hello", 0, expectedStdout, new byte[0]);
    }
    
    @Test
    public void testExecute_tempDirSpecified_stderr() throws IOException {
        System.out.println("testExecute_tempDirSpecified_stderr");
        File tempDir = temporaryFolder.newFolder();
        byte[] expectedStderr = ("hello" + System.getProperty("line.separator")).getBytes(Charset.defaultCharset());
        testExecute_tempDirSpecified(tempDir, "echo hello >&2", 0, new byte[0], expectedStderr);
    }
    
    private void testExecute_tempDirSpecified(final File tempDir, String command, int expectedExitCode, byte[] expectedStdout, byte[] expectedStderr) throws IOException {
        
        ProgramWithOutputFilesResult result = testExecute(command, expectedExitCode, expectedStdout, expectedStderr, new Function<Program.Builder, ProgramWithOutputFiles>() {
            @Override
            public ProgramWithOutputFiles apply(Program.Builder input) {
                return input.outputToTempFiles(tempDir.toPath());
            }
        });
        
        // check that they're the only two files in the temp dir
        Set<File> filesInTempDir = ImmutableSet.copyOf(FileUtils.listFiles(tempDir, null, true));
        assertEquals("expect 2 files in temp dir", 2, filesInTempDir.size());
        assertEquals(filesInTempDir, ImmutableSet.of(result.getStdoutFile(), result.getStderrFile()));
    }
    
}
