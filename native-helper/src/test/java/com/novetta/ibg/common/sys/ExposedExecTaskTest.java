/*
 * (c) 2014 IBG, A Novetta Solutions Company.
 */
package com.novetta.ibg.common.sys;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.novetta.ibg.common.sys.OutputStreamEcho.BucketEcho;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mchaberski
 */
public class ExposedExecTaskTest {
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    public ExposedExecTaskTest() {
        platform = Platforms.getPlatform();
    }
    
    private transient final Platform platform;
    
    private static void printAbbreviated(PrintStream out, String tag, String content) {
        printAbbreviated(out, tag, content, 64);
    }
    
    private static void printAbbreviated(PrintStream out, String tag, String content, int maxLength) {
        out.println(tag + ": " + StringUtils.abbreviateMiddle(content, "...", maxLength));
    }
    
    private void configureTaskToExecuteProcessThatSleeps(ExecTask task, int sleepDurationInSeconds) {
        if (platform.isWindows()) {
            task.setExecutable("cmd");
            task.createArg().setValue("/C");
            task.createArg().setValue(String.format("timeout %d >nul", sleepDurationInSeconds + 1));
        } else {
            task.setExecutable("sleep");
            task.createArg().setValue(String.valueOf(sleepDurationInSeconds));
        }
    }
    
    @Test
    public void testAbortProcess() throws Exception {
        System.out.println("\n\ntestAbortProcess");
        int processDuration = 5; // seconds
        final long killAfter = 50; // ms
        
        /*
         * we're gonna check later that the process didn't last longer than
         * half the specified duration, so here we make sure that our 
         * time-to-kill is low enough
         */
        assertTrue(killAfter < (processDuration * 1000 / 2));
        
        final ExposedExecTask task = new ExposedExecTask();
        final Project project = new Project();
        project.init();
        task.setProject(project);
        task.setResultProperty("exitCode");
        configureTaskToExecuteProcessThatSleeps(task, processDuration);
        task.setDestructible(true);
        
        final AtomicBoolean taskEnded = new AtomicBoolean(false);
        final AtomicBoolean killTaskSucceeded = new AtomicBoolean(false);
        final AtomicLong taskDuration = new AtomicLong(-1L);
        final AtomicBoolean taskExecutionFailure= new AtomicBoolean(false);
        
        Thread runner = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    long startTime = System.currentTimeMillis();
                    System.out.println("executing task");
                    task.execute();
                    taskEnded.set(true);
                    long endTime = System.currentTimeMillis();
                    String exitCode = project.getProperty("exitCode");
                    System.out.println("exit code " + exitCode);
                    taskDuration.set(endTime - startTime);
                    System.out.format("task execution completed in %d milliseconds%n", taskDuration.longValue());
                } catch (Exception e) {
                    System.out.println("exception while executing task");
                    taskExecutionFailure.set(true);
                    e.printStackTrace(System.out);
                }
            }
        });
        runner.start();
        
        Thread killer = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    System.out.println("killing in " + killAfter + " milliseconds");
                    Thread.sleep(killAfter);
                    System.out.format("slept for %d milliseconds; killing now%n", killAfter);
                    boolean aborted = task.abort();
                    System.out.println("aborted: " + aborted);
                    killTaskSucceeded.set(aborted);
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.out);
                    throw new IllegalStateException(ex);
                }
            }
            
        });
        killer.start();
        runner.join();
        killer.join();
        assertFalse("task execution exception", taskExecutionFailure.get());
        String actualExitCode = project.getProperty("exitCode");
        assertFalse("expected nonzero exit code", "0".equals(actualExitCode));
        assertTrue("expected task execution to complete", taskEnded.get());
        assertTrue("expected kill-task to succeed", killTaskSucceeded.get());
        long processDurationMs = processDuration * 1000;
        assertTrue(taskDuration.get() < (processDurationMs / 2));
    }
    
    @Test
    public void testOutputEcho_fromFile() throws Exception {
        System.out.println("\n\ntestOutputEcho_fromFile");
        
        byte[] randomBytes = new byte[numBytesForOutputEchoTest];
        Random random = new Random();
        random.nextBytes(randomBytes);
        String inputString = new Base64().encodeAsString(randomBytes);
        final File inputFile = File.createTempFile("random", ".txt", temporaryFolder.newFolder());
        
        Files.write(inputString, inputFile, Charsets.US_ASCII);
        printAbbreviated(System.out, "input", inputString);
        ExposedExecTask task = new OutputEchoTester() {

            @Override
            protected void configureTask(ExposedExecTask task) {
                task.createArg().setFile(inputFile);
            }
            
        }.testOutputEcho();
        
        String processStdout = task.getProject().getProperty("stdout");
        printAbbreviated(System.out, "expected process stdout", inputString);
        assertEquals(inputString, processStdout);
    }

    int numBytesForOutputEchoTest = 16385;
    
    @Test
    public void testOutputEcho_fromStdin() throws Exception {
        System.out.println("\n\ntestOutputEcho_fromStdin");
        
        byte[] randomBytes = new byte[numBytesForOutputEchoTest];
        Random random = new Random();
        random.nextBytes(randomBytes);
        String inputString = new Base64().encodeAsString(randomBytes);
        final File inputFile = File.createTempFile("random", ".txt", temporaryFolder.newFolder());
        
        Files.write(inputString, inputFile, Charsets.US_ASCII);
        printAbbreviated(System.out, "input", inputString);
        ExposedExecTask task = new OutputEchoTester() {

            @Override
            protected void configureTask(ExposedExecTask task) {
                task.setInput(inputFile);
            }
            
        }.testOutputEcho();
        
        String processStdout = task.getProject().getProperty("stdout");
        printAbbreviated(System.out, "expected process stdout", inputString);
        assertEquals(inputString, processStdout);
    }

    @Test
    public void testOutputEcho_stderr() throws Exception {
        System.out.println("\n\ntestOutputEcho_stderr");
        OutputEchoTester tester = new OutputEchoTester("1") {

            @Override
            protected void configureTask(ExposedExecTask task) throws IOException {
                File file = new File(temporaryFolder.newFolder(), "ThisFileDoesNotExist");
                task.createArg().setFile(file);
                task.setFailonerror(false);
            }
            
        };
        ExposedExecTask task = tester.testOutputEcho();
        String processStdout = task.getProject().getProperty("stdout");
        System.out.println("stdout: " + processStdout);
        String processStderr = task.getProject().getProperty("stderr");
        System.out.println("stderr: " + processStderr);
        assertTrue("expected empty stdout", processStdout.isEmpty());
        assertFalse("expected nonempty stderr", processStderr.isEmpty());
        byte[] stdoutBytes = tester.stdoutEcho.toByteArray();
        byte[] stderrBytes = tester.stderrEcho.toByteArray();
        System.out.println(stdoutBytes.length + " stdout bytes");
        assertEquals("expected 0 stdout bytes", 0, stdoutBytes.length);
        System.out.println(stderrBytes.length + " stderr bytes");
        assertTrue("expected nonzero stderr bytes", stderrBytes.length > 0);
    }
    
    public abstract class OutputEchoTester {
    
        transient final BucketEcho stdoutEcho = new BucketEcho();
        transient final BucketEcho stderrEcho = new BucketEcho();
        
        private @Nullable String expectedExitCode;

        public OutputEchoTester(@Nullable String expectedExitCode) {
            this.expectedExitCode = expectedExitCode;
        }

        public OutputEchoTester() {
            this("0");
        }
        
        
        
        /**
         * 
         * @return the task, after execution, with exitCode, stdout, and stderr
         * properties set
         * @throws FileNotFoundException
         * @throws BuildException 
         */
        public ExposedExecTask testOutputEcho() throws IOException, BuildException {
            
            final ExposedExecTask task = new ExposedExecTask();
            final Project project = new Project();
            project.init();
            task.setProject(project);
            task.setTimeout(3000);
            task.setResultProperty("exitCode");
            configureTaskToCatStdin(task);
            task.setOutputproperty("stdout");
            task.setErrorProperty("stderr");
            task.getRedirector().setStdoutEcho(stdoutEcho);
            task.getRedirector().setStderrEcho(stderrEcho);
            configureTask(task);
            task.execute();
            assertEquals("expect exit code " + expectedExitCode, expectedExitCode, project.getProperty("exitCode"));
            String processStdout = project.getProperty("stdout");
            String processStderr = project.getProperty("stderr");
            String echoedStdout = new String(stdoutEcho.toByteArray(), Charset.defaultCharset()); // executable would have used default platform charset
            String echoedStderr = new String(stderrEcho.toByteArray(), Charset.defaultCharset());
            printAbbreviated(System.out, "process stdout", processStdout);
            printAbbreviated(System.out, "process stderr", processStderr);
            printAbbreviated(System.out, "echoed stdout", echoedStdout);
            printAbbreviated(System.out, "echoed stderr", echoedStderr);
            assertEquals(processStdout.trim(), echoedStdout.trim());
            assertEquals(processStderr.trim(), echoedStderr.trim());
            return task;
        }
        
        protected abstract void configureTask(ExposedExecTask task) throws IOException;

        private void configureTaskToCatStdin(ExecTask task) {
            if (platform.isWindows()) {
                task.setExecutable("cmd");
                task.createArg().setValue("/C");
                task.createArg().setValue("type CON");
            } else {
                task.setExecutable("cat");
            }
        }

    }
}
