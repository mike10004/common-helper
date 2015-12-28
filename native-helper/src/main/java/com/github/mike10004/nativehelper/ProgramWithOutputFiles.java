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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.novetta.ibg.common.sys.ExposedExecTask;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.tools.ant.taskdefs.ExecTask;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author mchaberski
 */
public class ProgramWithOutputFiles extends ProgramWithOutput {

    private static final String KEY_STDERR = ProgramWithOutputFiles.class.getName() + ".stderrFile";
    private static final String KEY_STDOUT = ProgramWithOutputFiles.class.getName() + ".stdoutFile";

    private final Supplier<File> stdoutFileSupplier, stderrFileSupplier;
    
    protected ProgramWithOutputFiles(String executable, String standardInput, File standardInputFile, File workingDirectory, Iterable<String> arguments, Supplier<ExposedExecTask> taskFactory, Supplier<File> stdoutFileSupplier, Supplier<File> stderrFileSupplier) {
        super(executable, standardInput, standardInputFile, workingDirectory, arguments, taskFactory);
        this.stdoutFileSupplier = Suppliers.memoize(stdoutFileSupplier);
        this.stderrFileSupplier = Suppliers.memoize(stderrFileSupplier);
    }

    @Override
    public ProgramWithOutputFilesResult execute() {
        return (ProgramWithOutputFilesResult) super.execute();
    }

    @Override
    protected void configureTask(ExposedExecTask task, Map<String, Object> executionContext) {
        super.configureTask(task, executionContext);
        File stdoutFile = stdoutFileSupplier.get();
        File stderrFile = stderrFileSupplier.get();
        executionContext.put(KEY_STDOUT, stdoutFile);
        executionContext.put(KEY_STDERR, stderrFile);
    }
    
    @Override
    protected ProgramWithOutputFilesResult produceResultFromExecutedTask(ExecTask task, Map<String, Object> executionContext) {
        File stdoutFile = (File) executionContext.get(KEY_STDOUT);
        File stderrFile = (File) executionContext.get(KEY_STDERR);
        int exitCode = getExitCode(task, executionContext);
        ProgramWithOutputFilesResult result = new ProgramWithOutputFilesResult(exitCode, stdoutFile, stderrFile);
        return result;
    }

    public static class Builder extends Program.Builder {
        
        protected Supplier<File> stdoutFileSupplier, stderrFileSupplier;
        
        protected Builder(Program.Builder superclassBuilder, Supplier<File> stdoutFileSupplier, Supplier<File> stderrFileSupplier) {
            super(superclassBuilder.executable);
            initFrom(superclassBuilder);
            this.stdoutFileSupplier = checkNotNull(stdoutFileSupplier);
            this.stderrFileSupplier = checkNotNull(stderrFileSupplier);
        }
        
        protected Builder(Program.Builder superclassBuilder, File stdoutFile, File stderrFile) {
            this(superclassBuilder, Suppliers.ofInstance(checkNotNull(stdoutFile)), Suppliers.ofInstance(checkNotNull(stderrFile)));
        }

        protected Builder(Program.Builder superclassBuilder, File directory) {
            this(superclassBuilder, new TempFileSupplier("ProgramWithOutputFiles_stdout", ".tmp", directory), new TempFileSupplier("ProgramWithOutputFiles_stderr", ".tmp", directory));
        }
        
        @Override
        public ProgramWithOutputFiles build() {
            return new ProgramWithOutputFiles(executable, standardInput, standardInputFile, workingDirectory, arguments, taskFactory, stdoutFileSupplier, stderrFileSupplier);
        }
        
        private void initFrom(Program.Builder superclassBuilder) {
            copyFields(superclassBuilder, this);
        }
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
        public TempFileCreationException(File parentDirectory, Throwable cause) {
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
    
    public static class TempDirSupplier implements Supplier<File> {

        private final File parent;

        public TempDirSupplier(@Nullable File parent) {
            this.parent = parent == null ? new File(System.getProperty("java.io.tmpdir")) : parent;
        }
        
        @Override
        public File get() {
            try {
                return java.nio.file.Files.createTempDirectory(parent.toPath(), "TempDirSupplier").toFile();
            } catch (IOException ex) {
                throw new TempDirCreationException(ex);
            }
        }
        
    }
    
    public static class TempFileSupplier implements Supplier<File> {

        private final String prefix, suffix;
        private final Supplier<File> temporaryDirectorySupplier;

        public TempFileSupplier(String prefix, String suffix, Supplier<File> temporaryDirectorySupplier) {
            this.prefix = checkNotNull(prefix);
            this.suffix = checkNotNull(suffix);
            this.temporaryDirectorySupplier = checkNotNull(temporaryDirectorySupplier);
        }
        
        public TempFileSupplier(String prefix, String suffix, File temporaryDirectory) {
            this(prefix, suffix, Suppliers.ofInstance(temporaryDirectory));
        }
        
        @Override
        public File get() {
            File tempDir = temporaryDirectorySupplier.get();
            File tempFile;
            try {
                tempFile = File.createTempFile(prefix, suffix, tempDir);
            } catch (IOException ex) {
                throw new TempFileCreationException(tempDir, ex);
            }
            return tempFile;
        }
        
    }
}
