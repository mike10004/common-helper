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

import com.github.mike10004.nativehelper.Program.SimpleProgramResult;
import com.google.common.io.ByteSource;
import java.nio.charset.Charset;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author mchaberski
 */
public class ProgramWithOutputStringsResult extends SimpleProgramResult implements ProgramWithOutputResult {
    
    private final String stdoutString, stderrString;
    private final Charset charset;
    private ByteSource stdout, stderr;
    private transient final Object bytesLock;
    
    public ProgramWithOutputStringsResult(int exitCode, String stdoutString, String stderrString, Charset charset) {
        super(exitCode);
        this.stdoutString = checkNotNull(stdoutString);
        this.stderrString = checkNotNull(stderrString);
        this.charset = checkNotNull(charset);
        bytesLock = new Object();
    }
    
    public String getStdoutString() {
        return stdoutString;
    }
    
    public String getStderrString() {
        return stderrString;
    }

    @Override
    public ByteSource getStdout() {
        synchronized (bytesLock) {
            if (stdout == null) {
                stdout = ByteSource.wrap(stdoutString.getBytes(charset));
            }
            return stdout;
        }
    }

    @Override
    public ByteSource getStderr() {
        synchronized (bytesLock) {
            if (stderr == null) {
                stderr = ByteSource.wrap(stderrString.getBytes(charset));
            }
            return stderr;
        }
    }
    
}
