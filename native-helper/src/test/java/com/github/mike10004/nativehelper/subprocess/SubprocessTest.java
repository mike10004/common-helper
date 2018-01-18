package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class SubprocessTest {

    private static final ProcessContext CONTEXT = ProcessContext.create();

    @Test
    public void launch_true() throws Exception {
        int exitCode = Subprocess.running(pyTrue()).build()
                .launcher(CONTEXT)
                .launch().get().getExitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void execute_true() {
        int exitCode = Subprocess.running(pyTrue()).build()
                .launcher(CONTEXT)
                .execute().getExitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void launch_exit_3() throws Exception {
        int expected = 3;
        int exitCode = Subprocess.running(pyExit())
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
        ProcessResult<String, String> processResult = Subprocess.running(pyEcho())
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
                Subprocess.running(Tests.getPythonFile("stereo.py"))
                .args(args)
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().get();
        String actualStdout = result.getOutput().getStdout();
        String actualStderr = result.getOutput().getStderr();
        assertEquals("stdout", joinPlus(linesep, stdout), actualStdout);
        assertEquals("stderr", joinPlus(linesep, stderr), actualStderr);
    }

    private static final String linesep = System.lineSeparator();

    private static String joinPlus(String delimiter, Iterable<String> items) {
        return String.join(delimiter, items) + delimiter;
    }

    @Test
    public void launch_cat_stdin() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 2 * 1024 * 1024; // overwhelm StreamPumper's buffer
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        ProcessResult<byte[], byte[]> result =
                Subprocess.running(Tests.getPythonFile("bin_cat.py"))
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().get();
        System.out.println(result);
        assertEquals("exit code", 0, result.getExitCode());
        assertArrayEquals("stdout", bytes, result.getOutput().getStdout());
    }

    @Test
    public void launch_readInput_predefined() throws Exception {
        String expected = String.format("foo%nbar%n");
        ProcessResult<String, String> result = Subprocess.running(Tests.getPythonFile("read_input.py"))
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII, CharSource.wrap(expected + System.lineSeparator()).asByteSource(US_ASCII))
                .launch().get();
        System.out.println(result);
        assertEquals("output", expected, result.getOutput().getStdout());
        assertEquals("exit code", 0, result.getExitCode());
    }

    @Test
    public void launch_readInput_piped() throws Exception {
        PipedInputStream pipeIn = new PipedInputStream();
        PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);
        Charset charset = UTF_8;
        ByteSource stdin = new ByteSource() {
            @Override
            public InputStream openStream() {
                return pipeIn;
            }
        };
        ListenableFuture<ProcessResult<String, String>> resultFuture = Subprocess.running(Tests.getPythonFile("read_input.py"))
                .build()
                .launcher(CONTEXT)
                .outputStrings(charset, stdin)
                .launch();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Futures.addCallback(resultFuture, new AlwaysCallback<Object>() {
            @Override
            protected void always(@Nullable Object result, @Nullable Throwable t) {
                System.out.println("program ended: " + result);
            }
        }, executor);
        List<String> lines = Arrays.asList("foo", "bar", "baz", "");
        PrintWriter printer = new PrintWriter(new OutputStreamWriter(pipeOut, charset));
        for (String line : lines) {
            printer.println(line);
            printer.flush();
        }
        ProcessResult<String, String> result = resultFuture.get();
        assertEquals("exit code", 0, result.getExitCode());
        String expected = joinPlus(System.lineSeparator(), lines.subList(0, 3));
        assertEquals("output", expected, result.getOutput().getStdout());
        executor.awaitTermination(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    

}