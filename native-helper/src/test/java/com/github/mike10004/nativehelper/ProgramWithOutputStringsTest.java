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
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mchaberski
 */
public class ProgramWithOutputStringsTest {
    
    private transient final Platform platform;
    
    public ProgramWithOutputStringsTest() {
        platform = Platforms.getPlatform();
    }

    @Test
    public void testExecute_stdoutToString() throws IOException {
        System.out.println("testExecute_filesSpecified");
        
        Program.Builder builder;
        if (platform.isWindows()) {
            builder = Program.running("cmd").arg("/C");
        } else {
            builder = Program.running("sh").arg("-c");
        }
        ProgramWithOutputStrings program = builder.arg("echo hello").outputToStrings();
        ProgramWithOutputStringsResult result = program.execute();
        System.out.println("exit code " + result.getExitCode());
        assertEquals("exitCode", 0, result.getExitCode());
        String actualStdout = result.getStdoutString();
        System.out.println(actualStdout);
        assertEquals("hello", actualStdout);
        String actualStderr = result.getStderrString();
        assertEquals("stderr.length " + actualStderr.length(), 0, actualStderr.length());
    }

}
