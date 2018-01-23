/*
 * The MIT License
 *
 * Copyright 2016 Mike Chaberski.
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

import com.github.mike10004.nativehelper.subprocess.ProcessLaunchException;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProgramTest {

    private static final Random random = new Random(ProgramTest.class.hashCode());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
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
        } catch (ProcessLaunchException e) {
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
            Program<ProgramWithOutputStringsResult> program = builder.outputToStrings();
            assertNull(program.getStandardInput().getLeft());
            assertNull(program.getStandardInput().getRight());
            long executionStartTime = System.currentTimeMillis();
            ListenableFuture<ProgramWithOutputStringsResult> future = program.executeAsync(executorService);
            Thread.sleep(killAfter);
            boolean cancellationResult = future.cancel(true);
            System.out.println("cancellationResult: " + cancellationResult);
            try {
                System.out.println(future.get());
                fail("should have thrown cancellationexception");
            } catch (CancellationException ignored) {
            } catch (ExecutionException unexpected) {
                fail("should have thrown cancellationexception, not " + unexpected);
            }
            assertTrue("expected future.isCancelled = true", future.isCancelled());
            long executionDuration = System.currentTimeMillis() - executionStartTime;
            long commandedProcessDurationMs = commandedProcessDuration * 1000;
            assertTrue("expect execution duration to be less than half commanded process duration", executionDuration < (commandedProcessDurationMs / 2));
        }
    }

    @Test
    public void programWithEnvironmentVariableSet() throws Exception {
        String variableName = String.format("X%012x", Math.abs(random.nextLong())).toUpperCase();
        String variableValue = String.format("%012x", Math.abs(random.nextLong()));
        System.out.format("%s=%s%n", variableName, variableValue);
        Program.Builder builder;
        if (platform.isWindows()) {
            Path batFile = tmp.newFile("echofoo.bat").toPath();
            java.nio.file.Files.write(batFile, Arrays.asList(
                    "if \"%" + variableName + "%\"==\"\" (",
                    "  echo.",
                    ") else (",
                    "  echo %" + variableName + "%",
                    ")"), Charset.defaultCharset(), StandardOpenOption.TRUNCATE_EXISTING);
            builder = Program.running("cmd")
                    .args("/Q", "/C", batFile.toAbsolutePath().toString());
        } else {
            builder = Program.running("sh")
                    .args("-c", "echo $" + variableName);
        }
        builder.env(variableName, variableValue);
        ProgramWithOutputStrings program = builder.outputToStrings();
        ProgramWithOutputStringsResult result = program.execute();
        System.out.format("result: %s%n", result);
        System.out.format("stdout: %s%n", StringEscapeUtils.escapeJava(result.getStdoutString()));
        System.out.format("stderr: %s%n", StringEscapeUtils.escapeJava(result.getStderrString()));
        assertEquals("exit code", 0, result.getExitCode());
        assertEquals("stdout", variableValue, result.getStdoutString().trim());
    }

    @Test
    public void testToString() {
        String inputFilename = "input.txt";
        Program program = Program.running("hello")
                .args("world")
                .reading(new File(System.getProperty("java.io.tmpdir"), inputFilename))
                .ignoreOutput();
        String programStr = program.toString();
        System.out.format("testToString: %s%n", programStr);
        assertNotNull("program.toString()", programStr);
        assertTrue("contains filename", programStr.contains(inputFilename));
    }

}

