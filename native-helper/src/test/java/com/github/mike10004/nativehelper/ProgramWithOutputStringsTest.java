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

import com.google.common.util.concurrent.ListenableFuture;
import com.novetta.ibg.common.sys.Platforms;
import java.io.IOException;
import java.util.concurrent.Executors;
import org.apache.tools.ant.BuildException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class ProgramWithOutputStringsTest {
    
    @Test
    public void testExecute_stdoutToString() {
        System.out.println("testExecute_stdoutToString");
        testExecute("echo hello", 0, "hello", "");
    }
    
    @Test
    public void testExecute_stderrToString() {
        System.out.println("testExecute_stderrToString");
        testExecute("echo goodbye >&2", 0, "", "goodbye");
    }
    
    private void testExecute(String shellCommand, int expectedExitCode, String expectedStdout, String expectedStderr) throws BuildException {
        Program.Builder builder = ProgramBuilders.shell();
        System.out.println("executing command: " + shellCommand);
        ProgramWithOutputStrings program = builder.arg(shellCommand).outputToStrings();
        ProgramWithOutputStringsResult result = program.execute();
        System.out.println(result);
        assertEquals("exitCode", expectedExitCode, result.getExitCode());
        assertEquals("stdout", expectedStdout, result.getStdoutString());
        assertEquals("stderr", expectedStderr, result.getStderrString());
    }

    @Test
    public void testExecuteAsync() throws Exception {
        System.out.println("testExecuteAsync");
        ProgramWithOutputStrings program = ProgramBuilders.shell().arg("echo hello").outputToStrings();
        ListenableFuture<ProgramWithOutputStringsResult> future = program.executeAsync(Executors.newSingleThreadExecutor());
        ProgramWithOutputStringsResult result = future.get();
        System.out.println("program result: " + result);
        assertEquals("hello", result.getStdoutString());
    }
    
    @Test
    public void testExecute_inputString() {
        System.out.println("testExecute_inputString");
        String inputString = "hello";
        Program.Builder builder = ProgramBuilders.shell().reading(inputString);
        if (Platforms.getPlatform().isWindows()) {
            builder = builder.arg("type CON");
        } else {
            builder = builder.arg("cat");
        }
        ProgramWithOutputStringsResult result = builder.outputToStrings().execute();
        System.out.println("result: " + result);
        assertEquals(inputString, result.getStdoutString());
    }
}
