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

import com.github.mike10004.nativehelper.Program.ExitCodeProgramResult;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import java.io.File;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author mchaberski
 */
public class ProgramWithOutputFilesResult extends ExitCodeProgramResult implements ProgramWithOutputResult {
    
    private final File stdoutFile;
    private final File stderrFile;
    private final ByteSource stdout, stderr;
    
    public ProgramWithOutputFilesResult(int exitCode, File stdoutFile, File stderrFile) {
        super(exitCode);
        this.stdoutFile = checkNotNull(stdoutFile);
        this.stderrFile = checkNotNull(stderrFile);
        stdout = Files.asByteSource(stdoutFile);
        stderr = Files.asByteSource(stderrFile);
    }
    
    @Override
    public ByteSource getStdout() {
        return stdout;
    }

    @Override
    public ByteSource getStderr() {
        return stderr;
    }

    public File getStdoutFile() {
        return stdoutFile;
    }

    public File getStderrFile() {
        return stderrFile;
    }
    
}
