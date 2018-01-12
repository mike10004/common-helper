package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.OutputStreamEcho.BucketEcho;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
    
    @SuppressWarnings("SameParameterValue")
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
        checkState(killAfter < (processDuration * 1000 / 2));
        
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
        
        Thread runner = new Thread(() -> {
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
        });
        runner.start();
        
        Thread killer = new Thread(() -> {
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
    
    @Test(expected = UnsupportedOperationException.class)
    public void testOriginalAntTimeoutFails() throws Exception {
        System.out.println("\n\ntestOriginalAntTimeoutFails");
        new ExposedExecTask().setTimeout(123);
    }

    @Test
    public void testOutputEcho_fromFile() throws Exception {
        System.out.println("\n\ntestOutputEcho_fromFile");
        
        byte[] randomBytes = new byte[numBytesForOutputEchoTest];
        Random random = new Random();
        random.nextBytes(randomBytes);
        String inputString = new Base64().encodeAsString(randomBytes);
        final File inputFile = File.createTempFile("random", ".txt", temporaryFolder.newFolder());
        Files.asCharSink(inputFile, Charsets.US_ASCII).write(inputString);
        printAbbreviated(System.out, "input", inputString);
        ExposedExecTask task = new OutputEchoTester() {

            @Override
            protected void provideInputSource(ExposedExecTask task) {
                task.createArg().setFile(inputFile);
            }
            
        }.testOutputEcho();
        
        String processStdout = task.getProject().getProperty("stdout");
        printAbbreviated(System.out, "expected process stdout", inputString);
        assertEquals(inputString, processStdout);
    }

    int numBytesForOutputEchoTest = 16385;
    
    private String generateRandomString() {
        byte[] randomBytes = new byte[numBytesForOutputEchoTest];
        Random random = new Random();
        random.nextBytes(randomBytes);
        Base64 encoder = new Base64(80);
        String inputString = encoder.encodeAsString(randomBytes);
        return inputString.trim();
    }
    
    @Test
    public void testOutputEcho_fromStdin() throws Exception {
        System.out.println("\n\ntestOutputEcho_fromStdin");
        
        final File inputFile = File.createTempFile("random", ".txt", temporaryFolder.newFolder());
        String inputString = generateRandomString();
        Files.asCharSink(inputFile, Charsets.US_ASCII).write(inputString);
        printAbbreviated(System.out, "input", inputString);
        ExposedExecTask task = new OutputEchoTester() {

            @Override
            protected void provideInputSource(ExposedExecTask task) {
                task.setInput(inputFile);
            }
            
        }.testOutputEcho();
        
        String processStdout = task.getProject().getProperty("stdout");
        printAbbreviated(System.out, "expected process stdout", inputString);
        assertThat("process stdout does not equal input string", inputString, lenientStringMatcher(processStdout, true));
    }

    @Test
    public void testOutputEcho_stderr() throws Exception {
        System.out.println("\n\ntestOutputEcho_stderr");
        final File file = new File(temporaryFolder.newFolder(), "ThisFileDoesNotExist");
        checkState(!file.exists());
        OutputEchoTester tester = new OutputEchoTester("1") {

            @Override
            protected void provideInputSource(ExposedExecTask task) {
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
         */
        public ExposedExecTask testOutputEcho() throws IOException, BuildException {
            
            final ExposedExecTask task = new ExposedExecTask();
            final Project project = new Project();
            project.init();
            task.setProject(project);
            task.setResultProperty("exitCode");
            configureTaskToCatStdin(task);
            task.setOutputproperty("stdout");
            task.setErrorProperty("stderr");
            task.getRedirector().setStdoutEcho(stdoutEcho);
            task.getRedirector().setStderrEcho(stderrEcho);
            provideInputSource(task);
            task.execute();
            assertEquals("expect exit code " + expectedExitCode, expectedExitCode, project.getProperty("exitCode"));
            String processStdout = project.getProperty("stdout");
            String processStderr = project.getProperty("stderr");
            String echoedStdout = new String(stdoutEcho.toByteArray(), Charset.defaultCharset()); // executable would have used default platform charset
            String echoedStderr = new String(stderrEcho.toByteArray(), Charset.defaultCharset());
            processStdout = normalizeLineEndings(processStdout);
            processStderr = normalizeLineEndings(processStderr);
            echoedStdout = normalizeLineEndings(echoedStdout);
            echoedStderr = normalizeLineEndings(echoedStderr);
            printAbbreviated(System.out, "process stdout", processStdout);
            printAbbreviated(System.out, "process stderr", processStderr);
            printAbbreviated(System.out, "echoed stdout", echoedStdout);
            printAbbreviated(System.out, "echoed stderr", echoedStderr);
            assertThat("process stdout != echoed stdout", echoedStdout, lenientStringMatcher(processStdout, true));
            assertThat("process stderr != echoed stderr", echoedStderr, lenientStringMatcher(processStderr, true));
            return task;
        }
        
        protected abstract void provideInputSource(ExposedExecTask task);

        private void configureTaskToCatStdin(ExecTask task) {
            if (platform.isWindows()) {
                task.setExecutable("findstr.exe");
                task.createArg().setValue("^");
            } else {
                task.setExecutable("cat");
            }
        }

    }

    private static String normalizeLineEndings(String input) {
        input = input.replaceAll("\\r\\n", "\n");
        input = input.replaceAll("\\r", "\n");
        return input;
    }
        
    private static Matcher<String> lenientStringMatcher(final String unnormalizedExpected, final boolean normalizeLineEndings) {
        final String expected = (normalizeLineEndings ? normalizeLineEndings(unnormalizedExpected) : unnormalizedExpected).trim();
        return new BaseMatcher<String>() {

            private Object item;
            private int indexOfFirstMismatch = -1;

            @Override
            public boolean matches(Object item) {
                this.item = item;
                if (item == null && expected == null) {
                    return true;
                }
                if (item == null) {
                    return false;
                }
                String actual = (normalizeLineEndings ? normalizeLineEndings((String) item) : (String) item).trim();
                final int expectedLen = expected.length(), actualLen = actual.length();
                for (int i = 0; i < Math.max(expectedLen, actualLen); i++) {
                    if (i < expectedLen && i < actualLen) {
                        char ech = expected.charAt(i);
                        char ach = actual.charAt(i);
                        if (ech != ach) {
                            indexOfFirstMismatch = i;
                            return false;
                        }
                    } else {
                        indexOfFirstMismatch = i;
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description d) {
                if (item == null || expected == null) {
                    d.appendText("actual and expected null-ness mismatch");
                } else {
                    int i = indexOfFirstMismatch;
                    checkState(i >= 0);
                    String actual = (String) item;
                    d.appendText("first mismatch at index ").appendValue(i);
                    d.appendText("; expected = ").appendValue(i < expected.length() ? expected.charAt(i) : "<len>");
                    d.appendText(", actual = ").appendValue(i < actual.length() ? actual.charAt(i) : "<len>");
                }
            }
        };
    }

}
