package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ScopedProcessTrackerTest {

    private static final int NUM_TRIALS = 50;

    @Parameters
    public static List<Object[]> trialPlaceholders() {
        return Arrays.asList(new Object[NUM_TRIALS][0]);
    }

    @Test
    public void close() throws Exception {
        ProcessMonitor<?, ?> monitor;
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()) {
            Subprocess subprocess = createSubprocessThatWaitsUntilDestroyed();
            monitor = subprocess.launcher(tracker).launch();
        }
        assertFalse("isAlive", monitor.process().isAlive());
        ProcessResult<?, ?> result = monitor.await(0, TimeUnit.MILLISECONDS);
        System.out.format("exit code %d%n", result.exitCode());
        assertNotEquals("exit code", 0, result.exitCode());
    }

    private static Subprocess createSubprocessThatWaitsUntilDestroyed() {
        return Tests.runningPythonFile(Tests.pySignalListener()).build();
    }
}