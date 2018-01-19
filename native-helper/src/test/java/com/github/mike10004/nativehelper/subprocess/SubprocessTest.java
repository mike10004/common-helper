package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.github.mike10004.nativehelper.subprocess.Subprocess.running;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SubprocessTest extends SubprocessTestBase {

    @Test
    public void launch_true() throws Exception {
        int exitCode = running(pyTrue()).build()
                .launcher(CONTEXT)
                .launch().get().getExitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void execute_true() {
        int exitCode = running(pyTrue()).build()
                .launcher(CONTEXT)
                .execute().getExitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void launch_exit_3() throws Exception {
        int expected = 3;
        int exitCode = running(pyExit())
                .arg(String.valueOf(expected))
                .build()
                .launcher(CONTEXT)
                .launch().get().getExitCode();
        assertEquals("exit code", expected, exitCode);
    }

    private static File pyEcho() {
        return Tests.getPythonFile("bin_echo.py");
    }

    private static File pyTrue() {
        return Tests.getPythonFile("bin_true.py");
    }

    private static File pyExit() {
        return Tests.getPythonFile("bin_exit.py");
    }

    @Test
    public void launch_echo() throws Exception {
        String arg = "hello";
        ProcessResult<String, String> processResult = running(pyEcho())
                .arg(arg).build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().get();
        int exitCode = processResult.getExitCode();
        assertEquals("exit code", 0, exitCode);
        String actualStdout = processResult.getOutput().getStdout();
        String actualStderr = processResult.getOutput().getStderr();
        System.out.format("output: \"%s\"%n", StringEscapeUtils.escapeJava(actualStdout));
        assertEquals("stdout", arg, actualStdout);
        assertEquals("stderr", "", actualStderr);
    }

    @Test
    public void launch_stereo() throws Exception {
        List<String> stdout = Arrays.asList("foo", "bar", "baz"), stderr = Arrays.asList("gaw", "gee");
        List<String> args = new ArrayList<>();
        for (int i = 0; i < Math.max(stdout.size(), stderr.size()); i++) {
            if (i < stdout.size()) {
                args.add(stdout.get(i));
            }
            if (i < stderr.size()) {
                args.add(stderr.get(i));
            }
        }
        ProcessResult<String, String> result =
                running(Tests.getPythonFile("stereo.py"))
                .args(args)
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().get();
        String actualStdout = result.getOutput().getStdout();
        String actualStderr = result.getOutput().getStderr();
        assertEquals("stdout", Tests.joinPlus(linesep, stdout), actualStdout);
        assertEquals("stderr", Tests.joinPlus(linesep, stderr), actualStderr);
    }

    private static final String linesep = System.lineSeparator();

    @Test
    public void launch_cat_stdin() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 2 * 1024 * 1024; // overwhelm StreamPumper's buffer
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        ProcessResult<byte[], byte[]> result =
                running(Tests.pyCat())
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().get();
        System.out.println(result);
        assertEquals("exit code", 0, result.getExitCode());
        assertArrayEquals("stdout", bytes, result.getOutput().getStdout());
        assertEquals("stderr length", 0, result.getOutput().getStderr().length);
    }

    @Test
    public void launch_cat_file() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 256 * 1024;
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        File dataFile = File.createTempFile("SubprocessTest", ".dat");
        Files.asByteSink(dataFile).write(bytes);
        ProcessResult<byte[], byte[]> result =
                running(Tests.pyCat())
                        .arg(dataFile.getAbsolutePath())
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().get();
        System.out.println(result);
        assertEquals("exit code", 0, result.getExitCode());
        checkState(Arrays.equals(bytes, Files.asByteSource(dataFile).read()));
        assertArrayEquals("stdout", bytes, result.getOutput().getStdout());
        assertEquals("stderr length", 0, result.getOutput().getStderr().length);
        //noinspection ResultOfMethodCallIgnored
        dataFile.delete();
    }

    @Test
    public void launch_readInput_predefined() throws Exception {
        String expected = String.format("foo%nbar%n");
        ProcessResult<String, String> result = running(Tests.pyReadInput())
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII, CharSource.wrap(expected + System.lineSeparator()).asByteSource(US_ASCII))
                .launch().get();
        System.out.println(result);
        assertEquals("output", expected, result.getOutput().getStdout());
        assertEquals("exit code", 0, result.getExitCode());
    }

}