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
 * Class that represents the result of a program that finished executing
 * and whose output was written to file.
 * @see Program.Builder#outputToFiles(java.io.File, java.io.File) 
 * @see Program.Builder#outputToTempFiles(java.nio.file.Path) 
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

    /**
     * Gets the pathname of the file containing the process standard output stream contents.
     * @return the pathname
     */
    public File getStdoutFile() {
        return stdoutFile;
    }

    /**
     * Gets the pathname of the file containing the process standard error stream contents.
     * @return the pathname
     */
    public File getStderrFile() {
        return stderrFile;
    }

    @Override
    public String toString() {
        return "ProgramWithOutputFilesResult{" + "exitCode=" + exitCode + ", stdoutFile=" + stdoutFile + ", stderrFile=" + stderrFile + '}';
    }
    
}
