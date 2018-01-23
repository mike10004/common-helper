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

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ProcessTracker;
import com.github.mike10004.nativehelper.subprocess.StreamContent;
import com.github.mike10004.nativehelper.subprocess.StreamContext;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.github.mike10004.nativehelper.subprocess.Subprocess.Launcher;
import com.google.common.base.Suppliers;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    
    protected ProgramWithOutputFiles(String executable, String standardInput, File standardInputFile, File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<File> stdoutFileSupplier, Supplier<File> stderrFileSupplier) {
        super(executable, standardInput, standardInputFile, workingDirectory, environment, arguments);
        this.stdoutFileSupplier = Suppliers.memoize(stdoutFileSupplier::get);
        this.stderrFileSupplier = Suppliers.memoize(stderrFileSupplier::get);
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

    private static class FilesStreamControl extends ExecTaskStreamControl {

        private final Supplier<File> stdoutFileSupplier, stderrFileSupplier;

        public FilesStreamControl(ImmutablePair<String, File> standardInputSource, Supplier<File> stdoutFileSupplier, Supplier<File> stderrFileSupplier) {
            super(standardInputSource);
            this.stdoutFileSupplier = stdoutFileSupplier;
            this.stderrFileSupplier = stderrFileSupplier;
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return new FileOutputStream(stdoutFileSupplier.get());
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return new FileOutputStream(stderrFileSupplier.get());
        }
    }

    private class FileOutputStreamContext implements StreamContext<FilesStreamControl, File, File> {

        private FileOutputStreamContext() {
        }

        @Override
        public FilesStreamControl produceControl() throws IOException {
            return new FilesStreamControl(getStandardInput(), stdoutFileSupplier, stderrFileSupplier);
        }

        @Override
        public StreamContent<File, File> transform(int exitCode, FilesStreamControl context) {
            return StreamContent.direct(context.stdoutFileSupplier.get(), context.stderrFileSupplier.get());
        }
    }

    @Override
    protected SubprocessBridge<?> buildSubprocessBridge() {
        return new SubprocessBridge<File>() {
            @Override
            public Launcher<File, File> buildLauncher(Subprocess subprocess, ProcessTracker tracker) {
                return subprocess.launcher(tracker)
                        .output(new FileOutputStreamContext());
            }

            @Override
            public ProgramWithOutputFilesResult buildResult(ProcessResult<File, File> subprocessResult) {
                return new ProgramWithOutputFilesResult(subprocessResult.exitCode(), subprocessResult.content().stdout(), subprocessResult.content().stderr());
            }
        };
    }
}
