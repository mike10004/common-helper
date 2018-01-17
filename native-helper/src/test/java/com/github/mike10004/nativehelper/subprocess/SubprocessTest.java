package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.io.ByteSource;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
        ProcessResult<ByteSource, ByteSource> processResult = Subprocess.running(pyEcho())
                .arg(arg).build()
                .launcher(CONTEXT)
                .outputInMemory()
                .launch().get();
        int exitCode = processResult.getExitCode();
        assertEquals("exit code", 0, exitCode);
        byte[] actualStdout = processResult.getOutput().getStdout().read();
        Charset charset = StandardCharsets.US_ASCII;
        byte[] actualStderr = processResult.getOutput().getStderr().read();
        assertArrayEquals("stdout bytes", (arg + System.lineSeparator()).getBytes(charset), actualStdout);
        String actualText = new String(actualStdout, charset).trim();
        System.out.format("output: %s%n", actualText);
        assertEquals("stdout", arg, actualText);
        assertArrayEquals("stderr", new byte[0], actualStderr);
    }

}