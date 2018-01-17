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

import com.google.common.base.Suppliers;
import org.apache.tools.ant.taskdefs.ExecTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class representing a program whose output is written to file.
 * @author mchaberski
 */
public class ProgramWithOutputFiles extends ProgramWithOutput<ProgramWithOutputFilesResult> {

    private static final String KEY_STDERR = ProgramWithOutputFiles.class.getName() + ".stderrFile";
    private static final String KEY_STDOUT = ProgramWithOutputFiles.class.getName() + ".stdoutFile";

    private final Supplier<File> stdoutFileSupplier, stderrFileSupplier;
    
    protected ProgramWithOutputFiles(String executable, StandardInputSource stdinSource, File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory, Supplier<File> stdoutFileSupplier, Supplier<File> stderrFileSupplier) {
        super(executable, stdinSource, workingDirectory, environment, arguments, taskFactory);
        this.stdoutFileSupplier = Suppliers.memoize(stdoutFileSupplier::get);
        this.stderrFileSupplier = Suppliers.memoize(stderrFileSupplier::get);
    }

    @Override
    protected void configureTask(ExposedExecTask task, Map<String, Object> executionContext) {
        super.configureTask(task, executionContext);
        File stdoutFile = stdoutFileSupplier.get();
        File stderrFile = stderrFileSupplier.get();
        executionContext.put(KEY_STDOUT, stdoutFile);
        executionContext.put(KEY_STDERR, stderrFile);
        task.setOutput(stdoutFile);
        task.setError(stderrFile);
    }
    
    @Override
    protected ProgramWithOutputFilesResult produceResultFromExecutedTask(ExecTask task, Map<String, Object> executionContext) {
        File stdoutFile = (File) executionContext.get(KEY_STDOUT);
        File stderrFile = (File) executionContext.get(KEY_STDERR);
        int exitCode = getExitCode(task, executionContext);
        ProgramWithOutputFilesResult result = new ProgramWithOutputFilesResult(exitCode, stdoutFile, stderrFile);
        return result;
    }

    public static class TempDirCreationException extends RuntimeException {

        public TempDirCreationException() {
        }

        public TempDirCreationException(String message) {
            super(message);
        }

        public TempDirCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public TempDirCreationException(Throwable cause) {
            super(cause);
        }
        
    }
    
    public static class TempFileCreationException extends RuntimeException {
        public TempFileCreationException(Path parentDirectory, Throwable cause) {
            this("failed to create file in parent directory " + parentDirectory, cause);
        }

        public TempFileCreationException() {
        }

        public TempFileCreationException(String message) {
            super(message);
        }

        public TempFileCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public TempFileCreationException(Throwable cause) {
            super(cause);
        }
        
    }
    
    public static class TempDirSupplier implements Supplier<Path> {

        private final Path parent;

        public TempDirSupplier(Path parent) {
            this.parent = checkNotNull(parent);
        }
        
        @Override
        public Path get() {
            try {
                return Files.createTempDirectory(parent, "TempDirSupplier");
            } catch (IOException ex) {
                throw new TempDirCreationException(ex);
            }
        }
        
    }
    
    public static class TempFileSupplier implements Supplier<File> {

        private final String prefix, suffix;
        private final Supplier<Path> temporaryDirectorySupplier;

        public TempFileSupplier(String prefix, String suffix, Supplier<Path> temporaryDirectorySupplier) {
            this.prefix = checkNotNull(prefix);
            this.suffix = checkNotNull(suffix);
            this.temporaryDirectorySupplier = checkNotNull(temporaryDirectorySupplier);
        }
        
        public TempFileSupplier(String prefix, String suffix, File temporaryDirectory) {
            this(prefix, suffix, Suppliers.ofInstance(temporaryDirectory.toPath()));
        }
        
        @Override
        public File get() {
            Path tempDir = temporaryDirectorySupplier.get();
            File tempFile;
            try {
                tempFile = File.createTempFile(prefix, suffix, tempDir.toFile());
            } catch (IOException ex) {
                throw new TempFileCreationException(tempDir, ex);
            }
            return tempFile;
        }
        
    }
}
