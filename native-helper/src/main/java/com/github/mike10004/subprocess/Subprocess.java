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
package com.github.mike10004.subprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

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
public abstract class Subprocess {
    
    private final String executable;
    private final ImmutableList<String> arguments;
    @Nullable
    private final File workingDirectory;
    private final ImmutableMap<String, String> environment;

    protected Subprocess(String executable, @Nullable File workingDirectory, Map<String, String> environment, Iterable<String> arguments) {
        this.executable = requireNonNull(executable, "executable");
        this.workingDirectory = workingDirectory;
        this.arguments = ImmutableList.copyOf(arguments);
        this.environment = ImmutableMap.copyOf(environment);
    }

    /**
     * @return a future representing the computation
     */
    public <T extends ProcessOutput> ProcessFuture<T> launch(ProcessOutputControl<T> outputControl, ProcessContext processContext) throws ProcessException {
        ListeningExecutorService waitingExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        StandardLauncher.Execution execution;
        try {
            execution = new StandardLauncher(this, processContext).launch(outputControl.produceEndpoints());
        } catch (IOException e) {
            throw new ProcessLaunchException("failed to produce stream endpoints", e);
        }
        AsyncFunction<Integer, ProcessResult<T>> transform = outputControl.getTransform();
        ListenableFuture<ProcessResult<T>> fullResultFuture = execution.getFuture().transformAsync(transform, waitingExecutor);
        ProcessFuture<T> processFuture = new ProcessFuture<>(execution.getProcess(), fullResultFuture);
        return processFuture;
    }

    public static class ProcessExecutionException extends ProcessException {
        public ProcessExecutionException(String message) {
            super(message);
        }

        public ProcessExecutionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProcessExecutionException(Throwable cause) {
            super(cause);
        }
    }

    public static class WaitingThreadInterruptedException extends ProcessExecutionException {
        public WaitingThreadInterruptedException(InterruptedException cause) {
            super(cause);
        }
    }

    public static class ProcessExecutionInnerException extends ProcessExecutionException {
        public ProcessExecutionInnerException(Throwable cause) {
            super(cause);
        }
    }

    public <T extends ProcessOutput> ProcessResult<T> execute(ProcessOutputControl<T> outputControl, ProcessContext processContext) throws ProcessException {
        try {
            return launch(outputControl, processContext).get();
        } catch (InterruptedException e) {
            throw new WaitingThreadInterruptedException(e);
        } catch (ExecutionException e) {
            throw new ProcessExecutionInnerException(e.getCause());
        }
    }

    /**
     * Class that represents a builder of program instances. Create a builder
     * instance with {@link Subprocess#running(String) }.
     * @see Subprocess
     */
    @NotThreadSafe
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        
        protected final String executable;
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
            //noinspection ConstantConditions // inspector thinks Objects::nonNull will always return true
            checkArgument(Iterables.all(arguments, Objects::nonNull), "all arguments must be non-null");
            Iterables.addAll(this.arguments, arguments);
            return this;
        }

    }
    
    /**
     * Constructs a builder instance that will produce a program that 
     * launches the given executable.
     * @param executable the executable file
     * @return a builder instance
     * @throws IllegalArgumentException if {@link File#canExecute() } is false
     */
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
    
    @SuppressWarnings("unused")
    public String getExecutable() {
        return executable;
    }

    @SuppressWarnings("unused")
    public ImmutableList<String> getArguments() {
        return arguments;
    }

    @Nullable
    protected File getWorkingDirectory() {
        return workingDirectory;
    }

    @SuppressWarnings("unused")
    public ImmutableMap<String, String> getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return "Program{" +
                "executable='" + executable + '\'' +
                ", arguments=" + StringUtils.abbreviateMiddle(String.valueOf(arguments), "...", 64) +
                ", workingDirectory=" + workingDirectory +
                ", environment=" + StringUtils.abbreviateMiddle(String.valueOf(environment), "...", 64) +
                '}';
    }
}
