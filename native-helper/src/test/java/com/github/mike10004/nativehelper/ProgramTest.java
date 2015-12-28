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

import com.novetta.ibg.common.sys.Platform;
import com.novetta.ibg.common.sys.Platforms;
import java.util.Map;
import java.util.Random;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;

/**
 *
 * @author mchaberski
 */
public class ProgramTest {

    private transient final Platform platform;
    
    public ProgramTest() {
        platform = Platforms.getPlatform();
    }

    @Test
    public void testExecute_nonWindows() {
        Assume.assumeTrue(!platform.isWindows());
        System.out.println("testExecute_nonWindows");
        
        int trueExitCode = Program.builder("true").build().execute().getExitCode();
        System.out.println("true: " + trueExitCode);
        assertEquals("trueExitCode", 0, trueExitCode);
        int falseExitCode = Program.builder("false").build().execute().getExitCode();
        System.out.println("false: " + falseExitCode);
        assertEquals("falseExitCode", 1, falseExitCode);
    }

    @Test
    public void testExecute_windows() {
        Assume.assumeTrue(platform.isWindows());
        int trueExitCode = Program.builder("cmd").args("/C", "exit 0").build().execute().getExitCode();
        System.out.println("true: " + trueExitCode);
        assertEquals("trueExitCode", 0, trueExitCode);
        int falseExitCode = Program.builder("cmd").args("/C", "exit 1").build().execute().getExitCode();
        System.out.println("false: " + falseExitCode);
        assertEquals("falseExitCode", 1, falseExitCode);
    }
    
    @Test
    public void testExecute_executableNotFound() {
        System.out.println("testExecute_executableNotFound");
        String executableName = "u" + String.format("%10x", Math.abs(random.nextLong()));
        Program program = Program.builder(executableName).build();
        try {
            program.execute().getExitCode();
            fail("should have failed");
        } catch (BuildException e) {
            System.out.println("for executable " + executableName + " exception thrown: " + e);
        }
        
    }
    
    private static final Random random = new Random(0x10004);
}
