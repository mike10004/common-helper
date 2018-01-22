package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.ProcessMissionControl.Execution;
import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class ProcessMissionControlTest {

    private static final ProcessContext CONTEXT = ProcessContext.create();

    @Test
    public void launch() throws ExecutionException, InterruptedException {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, CONTEXT, createExecutorService());
        ByteBucket stdout = ByteBucket.create(), stderr = ByteBucket.create();
        ProcessStreamEndpoints endpoints = new ProcessStreamEndpoints(stdout, stderr, null);
        Execution execution = executor.launch(endpoints);
        Integer exitCode = execution.getFuture().get();
        assertEquals("exitcode", 0, exitCode.intValue());
        String actual = new String(stdout.dump(), US_ASCII);
        assertEquals("stdout", expected, actual.trim());
    }

    @Test
    public void launch_ProcessOutputControls_memory() throws Exception {
        String expected = "hello";
        Subprocess subprocess = Subprocess.running("echo")
                .args(expected)
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, CONTEXT, createExecutorService());
        ProcessOutputControl<ByteSource, ByteSource> ctrl = ProcessOutputControls.memoryByteSources();
        ProcessStreamEndpoints endpoints = ctrl.produceEndpoints();
        Execution execution = executor.launch(endpoints);
        Integer exitCode = execution.getFuture().get();
        assertEquals("exitcode", 0, exitCode.intValue());
        ByteSource stdout = ctrl.getTransform().apply(exitCode).getOutput().getStdout();
        String actual = new String(stdout.read(), US_ASCII);
        assertEquals("stdout", expected, actual.trim());
    }

    @Test
    public void readStdin() throws Exception {
        byte[] input = { 1, 2, 3, 4 };
        Subprocess subprocess = Subprocess.running(Tests.getPythonFile("nht_length.py"))
                .build();
        ProcessMissionControl executor = new ProcessMissionControl(subprocess, CONTEXT, createExecutorService());
        ByteBucket stdoutBucket = ByteBucket.create();
        ProcessStreamEndpoints endpoints = ProcessStreamEndpoints.builder()
                .stdin(ByteSource.wrap(input))
                .stdout(stdoutBucket)
                .build();
        Execution execution = executor.launch(endpoints);
        int exitCode = execution.getFuture().get(5, TimeUnit.SECONDS);
        System.out.println(stdoutBucket);
        assertEquals("exitcode", 0, exitCode);
        String length = new String(stdoutBucket.dump(), US_ASCII);
        System.out.format("output: %s%n", length);
        assertEquals("length", String.valueOf(input.length), length);
    }

    private ListeningExecutorService createExecutorService() {
        return ExecutorServices.newSingleThreadExecutorServiceFactory().get();
    }
}