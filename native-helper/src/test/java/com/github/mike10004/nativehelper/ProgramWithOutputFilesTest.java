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

//import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ProgramWithOutputFilesTest {

    private static final int NUM_TRIALS = 10;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

    @Parameterized.Parameters
    public static List<Object[]> trials() {
        return Arrays.asList(new Object[NUM_TRIALS][0]);
    }

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
        testExecute_outputToFiles("echo hello>&2", 0, new byte[0], expectedStderr);
    }
    
    private void testExecute_outputToFiles(String command, int expectedExitCode, byte[] expectedStdout, byte[] expectedStderr) throws IOException {
        final File stdoutFile = temporaryFolder.newFile();
        final File stderrFile = temporaryFolder.newFile();
        System.out.format("output files: %s, %s%n", stdoutFile, stderrFile);
        ProgramWithOutputFilesResult result = testExecute(command, expectedExitCode, expectedStdout, expectedStderr, builder -> builder.outputToFiles(stdoutFile, stderrFile));
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
        Charset chs = Charset.defaultCharset();
        if (!Arrays.equals(expectedStdout, actualStdout)) {
            System.out.format("expecting %s, actual is %s%n", describeByteArrayAsString(expectedStdout, chs), describeByteArrayAsString(actualStdout, chs));
        }
        if (!Arrays.equals(expectedStderr, actualStderr)) {
            System.out.format("expecting %s, actual is %s%n", describeByteArrayAsString(expectedStderr, chs), describeByteArrayAsString(actualStderr, chs));
        }
        assertArrayEquals("stdout", expectedStdout, actualStdout);
        assertArrayEquals("stderr", expectedStderr, actualStderr);
        return result;
    }

    private String describeByteArrayAsString(byte[] bytes, Charset charset) {
        String s = new String(bytes, charset);
        s = StringEscapeUtils.escapeJava(s);
        return String.format("%s -> \"%s\"%n", Arrays.toString(bytes), s);
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
        testExecute_tempDirSpecified(tempDir, "echo hello>&2", 0, new byte[0], expectedStderr);
    }
    
    private void testExecute_tempDirSpecified(final File tempDir, String command, int expectedExitCode, byte[] expectedStdout, byte[] expectedStderr) throws IOException {
        
        ProgramWithOutputFilesResult result = testExecute(command, expectedExitCode, expectedStdout, expectedStderr, input -> input.outputToTempFiles(tempDir.toPath()));
        
        // check that they're the only two files in the temp dir
        Set<File> filesInTempDir = ImmutableSet.copyOf(FileUtils.listFiles(tempDir, null, true));
        assertEquals("expect 2 files in temp dir", 2, filesInTempDir.size());
        assertEquals(filesInTempDir, ImmutableSet.of(result.getStdoutFile(), result.getStderrFile()));
    }
    
}
