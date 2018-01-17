package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.Executor.Execution;
import com.google.common.io.ByteSource;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class ExecutorTest {

    private static final ProcessContext CONTEXT = ProcessContext.create();

    @Test
    public void launch() throws ExecutionException, InterruptedException {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        Executor executor = new Executor(subprocess, CONTEXT);
        ByteBucket stdout = ByteBucket.create(), stderr = ByteBucket.create();
        ProcessStreamEndpoints endpoints = new ProcessStreamEndpoints(stdout, stderr, null);
        Execution execution = executor.launch(endpoints);
        Integer exitCode = execution.getFuture().get();
        assertEquals("exitcode", 0, exitCode.intValue());
        String actual = new String(stdout.dump(), StandardCharsets.US_ASCII);
        assertEquals("stdout", expected, actual.trim());
    }

    @Test
    public void launch_ProcessOutputControls_memory() throws Exception {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        Executor executor = new Executor(subprocess, CONTEXT);
        ProcessOutputControl<ByteSource, ByteSource> ctrl = ProcessOutputControls.memory();
        ProcessStreamEndpoints endpoints = ctrl.produceEndpoints();
        Execution execution = executor.launch(endpoints);
        Integer exitCode = execution.getFuture().get();
        assertEquals("exitcode", 0, exitCode.intValue());
        ByteSource stdout = ctrl.getTransform().apply(exitCode).get().getOutput().getStdout();
        String actual = new String(stdout.read(), StandardCharsets.US_ASCII);
        assertEquals("stdout", expected, actual.trim());
    }
}