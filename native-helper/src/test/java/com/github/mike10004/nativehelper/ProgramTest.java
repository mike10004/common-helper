/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mike10004.nativehelper;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.novetta.ibg.common.sys.ExposedExecTask;
import com.novetta.ibg.common.sys.OutputStreamEcho;
import com.novetta.ibg.common.sys.Platform;
import com.novetta.ibg.common.sys.Platforms;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class ProgramTest {

    private static final Random random = new Random(0x10004);
    private transient final Platform platform;
    
    public ProgramTest() {
        platform = Platforms.getPlatform();
    }

    @Test
    public void testExecute() {
        int trueExitCode = ProgramBuilders.shell().arg("exit 0").ignoreOutput().execute().getExitCode();
        System.out.println("true: " + trueExitCode);
        assertEquals("trueExitCode", 0, trueExitCode);
        int falseExitCode = ProgramBuilders.shell().arg("exit 1").ignoreOutput().execute().getExitCode();
        System.out.println("false: " + falseExitCode);
        assertEquals("falseExitCode", 1, falseExitCode);
    }
    
    @Test
    public void testExecute_executableNotFound() {
        System.out.println("testExecute_executableNotFound");
        String executableName = "u" + String.format("%10x", Math.abs(random.nextLong()));
        Program program = Program.running(executableName).ignoreOutput();
        try {
            program.execute().getExitCode();
            fail("should have failed");
        } catch (BuildException e) {
            System.out.println("for executable " + executableName + " exception thrown: " + e);
        }
        
    }
    
    private static final int testAbortProgram_commandedProcessDuration_seconds = 3;
    private static final int testAbortProgram_trials = 5;
    private static final long testAbortProgram_timeout = (testAbortProgram_commandedProcessDuration_seconds * testAbortProgram_trials * 2) * 1000;
    
    @Test(timeout = testAbortProgram_timeout)
    public void testAbortProgram() throws InterruptedException {
        System.out.println("\ntestAbortProgram");
        for (int trial = 0; trial < testAbortProgram_trials; trial++) {
            System.out.println("testAbortProgram trial " + trial);
            int commandedProcessDuration = testAbortProgram_commandedProcessDuration_seconds; // seconds
            final long killAfter = 50; // ms

            /*
             * we're gonna check later that the process didn't last longer than
             * half the specified duration, so here we make sure that our
             * time-to-kill is low enough
             */
            checkState(killAfter < (commandedProcessDuration * 1000 / 2));

            Program.Builder builder;
            if (platform.isWindows()) {
                String command = String.format("ping 127.0.0.1 -n %d > nul", commandedProcessDuration);
                System.out.println("windows command: " + command);
                builder = Program.running("cmd").arg("/C").arg(command);
            } else {
                builder = Program.running("sleep").arg(String.valueOf(commandedProcessDuration));
            }

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            Program program = builder.outputToStrings();
            assertNull(program.getStandardInput().getLeft());
            assertNull(program.getStandardInput().getRight());
            long executionStartTime = System.currentTimeMillis();
            ListenableFuture<ProgramResult> future = program.executeAsync(executorService);
            Thread.sleep(killAfter);
            boolean cancellationResult = future.cancel(true);
            System.out.println("cancellationResult: " + cancellationResult);
            try {
                System.out.println(future.get());
                fail("should have thrown cancellationexception");
            } catch (CancellationException expected) {
            } catch (ExecutionException unexpected) {
                fail("should have thrown cancellationexception, not " + unexpected);
            }
            assertTrue("expected future.isCancelled = true", future.isCancelled());
            long executionDuration = System.currentTimeMillis() - executionStartTime;
            long commandedProcessDurationMs = commandedProcessDuration * 1000;
            assertTrue("expect execution duration to be less than half commanded process duration", executionDuration < (commandedProcessDurationMs / 2));
        }
    }

    private static class OutputBucket implements OutputStreamEcho {

        private final ByteArrayOutputStream bucket;

        public OutputBucket() {
            bucket = new ByteArrayOutputStream(1024);
        }

        @Override
        public void writeEchoed(byte[] b, int off, int len) {
            bucket.write(b, off, len);
        }

        public byte[] toByteArray() {
            return bucket.toByteArray();
        }
    }

    @Test(timeout = 1000L)
    public void testProgramWithEchoableRedirector() {
        System.out.println("testProgramWithEchoableRedirector");

        EchoingProgramBuilder pb;
        if (Platforms.getPlatform().isWindows()) {
            pb = (EchoingProgramBuilder) new EchoingProgramBuilder("cmd").arg("/C");
        } else {
            pb = (EchoingProgramBuilder) new EchoingProgramBuilder("sh").arg("-c");
        }
        OutputBucket stderrBucket = new OutputBucket(), stdoutBucket = new OutputBucket();
        pb = pb.echoingTo(stdoutBucket, stderrBucket);
        Program program = pb.arg("echo 1 && echo 2 && echo 3").ignoreOutput();
        ProgramResult result = program.execute();
        assertEquals("exitCode", 0, result.getExitCode());
        Set<String> expected = ImmutableSet.of("1", "2", "3");
        Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();
        String stdout = new String(stdoutBucket.toByteArray());
        System.out.println("actual output: " + stdout);
        Set<String> actual = ImmutableSet.copyOf(splitter.split(stdout));
        System.out.println("actual output (split): " + actual);
        assertEquals("stdout (split)", expected, actual);
    }

    private static class MyTaskFactory extends Program.DefaultTaskFactory {

        private final OutputStreamEcho stdoutEcho;
        private final OutputStreamEcho stderrEcho;

        public MyTaskFactory(OutputStreamEcho stdoutEcho, OutputStreamEcho stderrEcho) {
            this.stdoutEcho = stdoutEcho;
            this.stderrEcho = stderrEcho;
        }

        @Override
        public ExposedExecTask get() {
            ExposedExecTask task = super.get();
            task.getRedirector().setStdoutEcho(stdoutEcho);
            task.getRedirector().setStderrEcho(stderrEcho);
            return task;
        }

    }

    private static class EchoingProgramBuilder extends Program.Builder {

        /**
         * Constructs a builder instance.
         *
         * @param executable the executable name or pathname of the executable file
         */
        public EchoingProgramBuilder(String executable) {
            super(executable);
        }

        public EchoingProgramBuilder echoingTo(OutputStreamEcho stdoutEcho, OutputStreamEcho stderrEcho) {
            return (EchoingProgramBuilder) super.usingTaskFactory(new MyTaskFactory(stdoutEcho, stderrEcho));
        }
    }
}
