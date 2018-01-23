package com.github.mike10004.nativehelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExposedExecTaskTest {
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    public ExposedExecTaskTest() {
        platform = Platforms.getPlatform();
    }
    
    private transient final Platform platform;
    
    private void configureTaskToExecuteProcessThatSleeps(ExposedExecTask task, int sleepDurationInSeconds) {
        Program<?> program;
        if (platform.isWindows()) {
            program = Program.running("cmd").arg("/C").arg(String.format("timeout %d >nul", sleepDurationInSeconds + 1)).ignoreOutput();
        } else {
            program = Program.running("sleep").arg(String.valueOf(sleepDurationInSeconds)).ignoreOutput();
        }
        task.configure(program);
    }

    private static final long TEST_ABORT_TIMEOUT = 5000L;

    /**
     * This tests the deprecated {@link ExposedExecTask#abort()} method manually. You probably
     * wouldn't use it this way, but instead launch an async task with {@link Program#executeAsync(ExecutorService)}
     * and call {@link ProgramFuture#cancelAndKill(long, TimeUnit)} on the future.
     */
    @Test(timeout = TEST_ABORT_TIMEOUT)
    public void testAbortProcess() throws Exception {
        System.out.println("\n\ntestAbortProcess");
        int sleepDurationSeconds = 5; // seconds
        final long killAfter = 50; // ms
        
        /*
         * we're gonna check later that the process didn't last longer than
         * half the specified duration, so here we make sure that our 
         * time-to-kill is low enough
         */
        checkState(killAfter < (sleepDurationSeconds * 1000 / 2));
        
        final ExposedExecTask task = new ExposedExecTask();
        System.out.format("configuring task to sleep for %d seconds%n", sleepDurationSeconds);
        configureTaskToExecuteProcessThatSleeps(task, sleepDurationSeconds);

        final AtomicBoolean taskEnded = new AtomicBoolean(false);
        final AtomicBoolean killTaskSucceeded = new AtomicBoolean(false);
        final AtomicLong taskDuration = new AtomicLong(-1L);
        final AtomicBoolean taskExecutionFailure= new AtomicBoolean(false);

        Object processAcquiredSignal = new Object();
        Thread runner = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                System.out.println("executor: executing task");
                task.executeProcess(p -> {
                    synchronized (processAcquiredSignal) {
                        processAcquiredSignal.notifyAll();
                    }
                });
                taskEnded.set(true);
                long endTime = System.currentTimeMillis();
                int exitCode = task.getProcessResultOrDie().getExitCode();
                System.out.println("executor: exit code " + exitCode);
                taskDuration.set(endTime - startTime);
                System.out.format("executor: task execution completed in %d milliseconds%n", taskDuration.longValue());
            } catch (Exception e) {
                System.out.println("executor: exception while executing task");
                taskExecutionFailure.set(true);
                e.printStackTrace(System.out);
            }
        });
        System.out.println("starting executor thread");
        runner.start();
        System.out.println("main: waiting for process to be acquired");
        synchronized (processAcquiredSignal) {
            processAcquiredSignal.wait();
        }
        System.out.println("main: process acquired");
        Thread killer = new Thread(() -> {
            try {
                System.out.println("killer: killing in " + killAfter + " milliseconds");
                Thread.sleep(killAfter);
                System.out.format("killer: slept for %d milliseconds; killing now%n", killAfter);
                boolean aborted = task.abort();
                System.out.println("killer: aborted: " + aborted);
                killTaskSucceeded.set(aborted);
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.out);
                throw new IllegalStateException(ex);
            }
        });
        System.out.println("main: starting killer thread");
        killer.start();
        System.out.format("main: waiting for execution thread to finish...%n");
        runner.join();
        System.out.format("main: waiting for killing thread to finish...%n");
        killer.join((sleepDurationSeconds + 1) * 1000);
        assertFalse("task execution exception", taskExecutionFailure.get());
        assertTrue("expected kill-task to succeed", killTaskSucceeded.get());
        String actualExitCode = String.valueOf(task.getProcessResultOrDie().getExitCode());
        assertFalse("expected nonzero exit code", "0".equals(actualExitCode));
        assertTrue("expected task execution to complete", taskEnded.get());
        long processDurationMs = sleepDurationSeconds * 1000;
        assertTrue(taskDuration.get() < (processDurationMs / 2));
    }
    
}
