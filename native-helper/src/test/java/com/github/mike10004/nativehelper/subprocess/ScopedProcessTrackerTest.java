package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ScopedProcessTrackerTest {

    @Test
    public void close() throws Exception {
        ProcessMonitor<?, ?> monitor;
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()) {
            Subprocess subprocess = Subprocess.running(Tests.pySignalListener()).build();
            monitor = subprocess.launcher(tracker).launch();
        }
        assertFalse("isAlive", monitor.process().isAlive());
        ProcessResult<?, ?> result = monitor.await(0, TimeUnit.MILLISECONDS);
        assertNotEquals("exit code", 0, result.exitCode());
    }
}