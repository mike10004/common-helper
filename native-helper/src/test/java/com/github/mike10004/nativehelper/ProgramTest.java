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
import com.google.common.util.concurrent.ListenableFuture;
import com.novetta.ibg.common.sys.Platform;
import com.novetta.ibg.common.sys.Platforms;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.tools.ant.BuildException;
import org.junit.Test;
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
    private static final long testAbortProgram_timeout = (testAbortProgram_commandedProcessDuration_seconds * 2) * 1000;
    
    @Test(timeout = testAbortProgram_timeout)
    public void testAbortProgram() throws InterruptedException {
        System.out.println("\ntestAbortProgram");
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
            builder = Program.running("cmd").arg("/C").arg(String.format("timeout %d >nul", commandedProcessDuration + 1));
        } else {
            builder = Program.running("sleep").arg(String.valueOf(commandedProcessDuration));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Program program = builder.ignoreOutput();
        long executionStartTime = System.currentTimeMillis();
        ListenableFuture<ProgramResult> future = program.executeAsync(executorService);
        Thread.sleep(killAfter);
        boolean cancellationResult = future.cancel(true);
        System.out.println("cancellationResult: " + cancellationResult);
        assertTrue("expected future.isCancelled = true", future.isCancelled());
        try {
            future.get();
            fail("should have thrown cancellationexception");
        } catch (CancellationException expected) {
        } catch (ExecutionException unexpected) {
            fail("should have thrown cancellationexception, not " + unexpected);
        }
        long executionDuration = System.currentTimeMillis() - executionStartTime;
        long commandedProcessDurationMs = commandedProcessDuration * 1000;
        assertTrue("expect execution duration to be less than half commanded process duration", executionDuration < (commandedProcessDurationMs / 2));
    }
    

}
