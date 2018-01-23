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

import com.github.mike10004.nativehelper.ProgramWithOutputFiles.TempFileSupplier;
import com.github.mike10004.nativehelper.subprocess.ProcessMonitor;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ProcessTracker;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.github.mike10004.nativehelper.subprocess.Subprocess.Launcher;
import com.google.common.base.Charsets;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a program that is to be executed in an external process.
 * Instances of this class are immutable, so it is safe to execute the same 
 * program in a multithreaded context. All configuration is performed with a 
 * {@link Builder} instance. Use {@link #running(String) } to create
 * a builder instance.
 * 
 * <p>The interface is designed to be somewhat fluent, so you can write
 * <pre>    Program program = Program.running("echo").arg("hello").outputToStrings();
 *    String output = program.execute().getStdoutString();
 *    System.out.println(output); // "hello"
 * </pre>
 * @author mchaberski
 */
@Deprecated
public abstract class Program<R extends ProgramResult> {

    @Deprecated
    public static final String RESULT_PROPERTY_NAME = Program.class.getName() + ".exitCode";
    
    private final String executable;
    private final ImmutablePair<String, File> standardInput;
    private final ImmutableList<String> arguments;
    private final @Nullable File workingDirectory;
    private final ImmutableMap<String, String> environment;

    protected Program(String executable, @Nullable String standardInput, @Nullable File standardInputFile, @Nullable File workingDirectory, Map<String, String> environment, Iterable<String> arguments) {
        this.executable = checkNotNull(executable);
        this.standardInput = ImmutablePair.of(standardInput, standardInputFile);
        checkArgument(standardInput == null || standardInputFile == null, "can't set both standard input string and file");
        this.workingDirectory = workingDirectory;
        this.arguments = ImmutableList.copyOf(arguments);
        this.environment = ImmutableMap.copyOf(environment);
    }

    /**
     * Launches the external process, blocking on the same thread until complete.
     * @return the execution result
     */
    public R execute() {
        return buildSubprocessBridge().execute();
    }

    protected static final ProcessTracker GLOBAL_CONTEXT = ProcessTracker.create();

    protected abstract class SubprocessBridge<T> {

        public abstract Subprocess.Launcher<T, T> buildLauncher(Subprocess subprocess, ProcessTracker tracker);
        public abstract R buildResult(ProcessResult<T, T> subprocessResult);

        public R execute() {
            ProcessResult<T, T> result;
            try {
                result = launch().await();
            } catch (InterruptedException e) {
                throw new ProcessWaitInterruptedException(e);
            }
            return buildResult(result);
        }

        protected ProcessMonitor<T, T> launch() {
            Subprocess.Launcher<T, T> launcher = buildLauncher(buildSubprocess(), GLOBAL_CONTEXT);
            return launcher.launch();
        }
    }

    public static class ProcessWaitInterruptedException extends RuntimeException {
        public ProcessWaitInterruptedException(Throwable cause) {
            super(cause);
        }
    }

    protected abstract SubprocessBridge<?> buildSubprocessBridge();
    
    private static class TaskAbortingFuture<R> extends SimpleForwardingListenableFuture<R> {
        
        public TaskAbortingFuture(ListenableFuture<R> wrapped) {
            super(wrapped);
            throw new UnsupportedOperationException("not yet implemented");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(mayInterruptIfRunning);
        }

    }
    
    /**
     * Executes the program asynchronously with a given executor service. Use
     * {@link Futures#addCallback(ListenableFuture, 
     * FutureCallback, Executor) }
     * to add a callback to the future. (Using the overloaded version of that method that 
     * provides a {@code directExecutor} as the executor argument is probably okay; see the
     * discussion in {@link ListenableFuture#addListener(Runnable, Executor) }.
     * Calling {@link Future#cancel(boolean) } will invoke
     * {@link Process#destroy() } on the process instance.
     * @param executorService the executor service
     * @return a future representing the computation
     */
    public ListenableFuture<R> executeAsync(ExecutorService executorService) {
        ListeningExecutorService listeningService = MoreExecutors.listeningDecorator(executorService);
        Callable<R> callable = () -> {
            return execute();
        };
        ListenableFuture<R> innerFuture = listeningService.submit(callable);
        return new TaskAbortingFuture<>(innerFuture);
    }

    protected Subprocess buildSubprocess() {
        Subprocess.Builder b = Subprocess.running(executable)
                .args(arguments)
                .env(environment);
        if (workingDirectory != null) {
            b.from(workingDirectory);
        }
        return b.build();
    }

    protected static class SimpleProgram extends Program<ProgramResult> {
        
        public SimpleProgram(String executable, String standardInput, File standardInputFile, File workingDirectory, Map<String, String> environment, Iterable<String> arguments) {
            super(executable, standardInput, standardInputFile, workingDirectory, environment, arguments);
        }

        @Override
        protected SubprocessBridge<?> buildSubprocessBridge() {
            return new SubprocessBridge<Void>() {
                @Override
                public Launcher<Void, Void> buildLauncher(Subprocess subprocess, ProcessTracker tracker) {
                    return subprocess.launcher(tracker);
                }

                @Override
                public ProgramResult buildResult(ProcessResult<Void, Void> subprocessResult) {
                    return new ExitCodeProgramResult(subprocessResult.exitCode());
                }
            };
        }
    }
    
    /**
     * Class that represents a builder of program instances. Create a builder
     * instance with {@link Program#running(String) }.
     * @see Program 
     */
    @NotThreadSafe
    public static class Builder {
        
        public static final Charset DEFAULT_STRING_OUTPUT_CHARSET = Charsets.UTF_8;
        
        protected final String executable;
        protected String standardInput;
        protected File standardInputFile;
        protected File workingDirectory;
        protected final List<String> arguments;
        protected final Map<String, String> environment;

        /**
         * Constructs a builder instance.
         * @param executable the executable name or pathname of the executable file
         */
        protected Builder(String executable) {
            this.executable = checkNotNull(executable);
            checkArgument(!executable.isEmpty(), "executable must be non-empty string");
            arguments = new ArrayList<>();
            environment = new LinkedHashMap<>();
        }
        
        /**
         * Feeds the given string to the process's standard input stream.
         * @param standardInputString the input string
         * @return this builder instance
         */
        public Builder reading(String standardInputString) {
            this.standardInput = checkNotNull(standardInputString);
            return this;
        }
        
        /**
         * Feeds the contents of the gtiven file to the process standard input stream.
         * @param standardInputFile the input file
         * @return this builder instance
         */
        public Builder reading(File standardInputFile) {
            this.standardInputFile = checkNotNull(standardInputFile);
            return this;
        }
        
        /**
         * Launch the process from this working directory.
         * @param workingDirectory the directory
         * @return this builder instance
         */
        public Builder from(File workingDirectory) {
            this.workingDirectory = checkNotNull(workingDirectory);
            return this;
        }

        /**
         * Set variables in the environment of the process to be executed.
         * @param environment map of variables to set
         * @return this builder instance
         */
        @SuppressWarnings("unused")
        public Builder env(Map<String, String> environment) {
            this.environment.putAll(environment);
            return this;
        }

        /**
         * Set variable in the environment of the process to be executed.
         * @param name variable name
         * @param value variable value
         * @return this builder instance
         */
        public Builder env(String name, String value) {
            environment.put(name, checkNotNull(value, "value must be non-null"));
            return this;
        }

        /**
         * Clears the argument list of this builder.
         * @return this builder instance
         */
        @SuppressWarnings("unused")
        public Builder clearArgs() {
            this.arguments.clear();
            return this;
        }
        
        /**
         * Appends an argument to the argument list of this builder.
         * @param argument the argument
         * @return this builder instance
         */
        public Builder arg(String argument) {
            this.arguments.add(checkNotNull(argument));
            return this;
        }

        /**
         * Appends arguments to the argument list of this builder.
         * @param firstArgument the first argument to append
         * @param otherArguments the other arguments to append
         * @return this builder instance
         * @see #args(Iterable)
         */        
        public Builder args(String firstArgument, String...otherArguments) {
            return args(Lists.asList(checkNotNull(firstArgument), otherArguments));
        }

        /**
         * Appends arguments to the argument list of this builder.
         * @param arguments command line arguments
         * @return this builder instance
         */
        public Builder args(Iterable<String> arguments) {
            checkArgument(Iterables.all(arguments, Objects::nonNull), "all arguments must be non-null");
            Iterables.addAll(this.arguments, arguments);
            return this;
        }

        /**
         * Constructs a program whose output is ignored. Use this if you only care
         * about the process exit code.
         * @return the program
         */
        public Program<ProgramResult> ignoreOutput() {
            return new SimpleProgram(executable, standardInput, standardInputFile, workingDirectory, environment, arguments);
        }
        
        /**
         * Constructs a program that writes output to the given files.
         * @param stdoutFile the file to which standard output contents are to be written
         * @param stderrFile the file to which standard error contents are to be written
         * @return the program
         */
        public ProgramWithOutputFiles outputToFiles(File stdoutFile, File stderrFile) {
            return new ProgramWithOutputFiles(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, Suppliers.ofInstance(checkNotNull(stdoutFile)), Suppliers.ofInstance(checkNotNull(stderrFile)));
        }
        
        /**
         * Constructs a program that writes output to uniquely-named files 
         * in a given directory.
         * @param directory the directory
         * @return the program
         */
        public ProgramWithOutputFiles outputToTempFiles(Path directory) {
            return new ProgramWithOutputFiles(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, new TempFileSupplier("ProgramWithOutputFiles_stdout", ".tmp", directory.toFile()), new TempFileSupplier("ProgramWithOutputFiles_stderr", ".tmp", directory.toFile()));
        }
        
        /**
         * Constructs a program that saves output in memory as strings.
         * @return the program
         */
        public ProgramWithOutputStrings outputToStrings() {
            return new ProgramWithOutputStrings(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, DEFAULT_STRING_OUTPUT_CHARSET);
        }

        /**
         * Constructs a program that saves output in memory as strings. The 
         * charset argument only matters if {@link ProgramWithOutputResult#getStdout() }
         * or {@code getStderr()} is called, in which case the byte source will
         * represent the byte encoding of the output strings in the given charset.
         * @param charset the charset
         * @return the program
         */
        @SuppressWarnings("unused")
        public ProgramWithOutputStrings outputToStrings(Charset charset) {
            return new ProgramWithOutputStrings(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, charset);
        }
    }
    
    /**
     * Constructs a builder instance that will produce a program that 
     * launches the given executable.
     * @param executable the executable file
     * @return a builder instance
     * @throws IllegalArgumentException if {@link File#canExecute() } is false
     */
    @SuppressWarnings("unused")
    public static Builder running(File executable) {
        checkArgument(executable.canExecute(), "executable.canExecute");
        return running(executable.getPath());
    }
    
    /**
     * Constructs a builder instance that will produce a program that 
     * launches an executable corresponding to the argument string.
     * @param executable the name of an executable or a path to a file
     * @return a builder instance
     */
    public static Builder running(String executable) {
        return new Builder(executable);
    }
    
    protected static class ExitCodeProgramResult implements ProgramResult {

        protected final int exitCode;

        public ExitCodeProgramResult(int exitCode) {
            this.exitCode = exitCode;
        }
        
        @Override
        public int getExitCode() {
            return exitCode;
        }

        @Override
        public String toString() {
            return "ProgramResult{" + "exitCode=" + exitCode + '}';
        }
        
    }

    @SuppressWarnings("unused")
    public final String getExecutable() {
        return executable;
    }

    public final ImmutablePair<String, File> getStandardInput() {
        return standardInput;
    }

    @SuppressWarnings("unused")
    public final ImmutableList<String> getArguments() {
        return arguments;
    }

    @SuppressWarnings("unused")
    @Nullable
    public final File getWorkingDirectory() {
        return workingDirectory;
    }

    private static String describeStandardInput(Pair<String, File> stdin) {
        File stdinFile = stdin.getRight();
        if (stdinFile != null) {
            return stdinFile.getAbsolutePath();
        }
        String stdinStr = stdin.getLeft();
        if (stdinStr != null) {
            return "\"" + StringEscapeUtils.escapeJava(StringUtils.abbreviate(stdinStr, 128)) + "\"";
        }
        return "<EOF>";
    }

    @Override
    public String toString() {
        return "Program{" +
                "executable='" + executable + '\'' +
                ", standardInput=" + describeStandardInput(standardInput) +
                ", arguments=" + StringUtils.abbreviate(String.valueOf(arguments), 128) +
                ", workingDirectory=" + workingDirectory +
                ", environment=" + StringUtils.abbreviate(String.valueOf(environment), 128) +
                '}';
    }
}
