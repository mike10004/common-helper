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
package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.ProcessOutputControl.UniformOutputControl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Class that represents a subprocess to be executed.
 */
public class Subprocess {
    
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
    public <SO, SE> ProcessMonitor<SO, SE> launch(ProcessOutputControl<SO, SE> outputControl, ProcessContext processContext) throws ProcessException {
        ListeningExecutorService waitingExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        ProcessLauncher.Execution execution;
        try {
            execution = new ProcessLauncher(this, processContext).launch(outputControl.produceEndpoints());
        } catch (IOException e) {
            throw new ProcessLaunchException("failed to produce stream endpoints", e);
        }
        Function<Integer, ProcessResult<SO, SE>> transform = outputControl.getTransform();
        ListenableFuture<ProcessResult<SO, SE>> fullResultFuture = execution.getFuture().transform(transform::apply, waitingExecutor);
        ProcessMonitor<SO, SE> monitor = new ProcessMonitor<>(execution.getProcess(), fullResultFuture);
        return monitor;
    }

    @SuppressWarnings("unused")
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
         * Each entry in the argument map is put into this builder's environment map.
         * Existing entries are not cleared; use {@link #clearEnv()} for that.
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
         * Clears this builder's environment map.
         * @return this instance
         */
        @SuppressWarnings("unused")
        public Builder clearEnv() {
            this.environment.clear();
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
            //noinspection ConstantConditions // inspector thinks Objects::nonNull will always return true
            checkArgument(Iterables.all(arguments, Objects::nonNull), "all arguments must be non-null");
            Iterables.addAll(this.arguments, arguments);
            return this;
        }

        public Subprocess build() {
            return new Subprocess(executable, workingDirectory, environment, arguments);
        }

    }

    public Launcher<Void, Void> launcher(ProcessContext context) {
        return toSinkhole(context);
    }
    private Launcher<Void, Void> toSinkhole(ProcessContext processContext) {
        return new Launcher<Void, Void>(processContext, ProcessOutputControls.sinkhole()){};
    }

    /**
     * Helper class that retains references to some dependencies so that you can
     * use a builder-style pattern to launch a process. Instances of this class are immutable
     * and methods that have return type {@code Launcher} return new instances.
     * @param <SO> standard output capture type
     * @param <SE> standard error capture type
     */
    public abstract class Launcher<SO, SE> {

        private final ProcessContext processContext;
        protected final ProcessOutputControl<SO, SE> outputControl;

        private Launcher(ProcessContext processContext, ProcessOutputControl<SO, SE> outputControl) {
            this.processContext = requireNonNull(processContext);
            this.outputControl = requireNonNull(outputControl);
        }

        public <S> UniformLauncher<S> output(UniformOutputControl<S> outputControl) {
            return uniformOutput(outputControl);
        }

        public <S> UniformLauncher<S> uniformOutput(ProcessOutputControl<S, S> outputControl) {
            return new UniformLauncher<S>(processContext, outputControl) {};
        }

        public <SO2, SE2> Launcher<SO2, SE2> output(ProcessOutputControl<SO2, SE2> outputControl) {
            return new Launcher<SO2, SE2>(processContext, outputControl) {};
        }

        public ProcessMonitor<SO, SE> launch() throws ProcessException {
            return Subprocess.this.launch(outputControl, processContext);
        }

        public <SO2, SE2> Launcher<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
            return output(outputControl.map(stdoutMap, stderrMap));
        }

        public UniformLauncher<String> outputStrings(Charset charset) {
            requireNonNull(charset, "charset");
            return outputStrings(charset, null);
        }

        public UniformLauncher<String> outputStrings(Charset charset, @Nullable ByteSource stdin) {
            requireNonNull(charset, "charset");
            return output(ProcessOutputControls.strings(charset, stdin));
        }

        public UniformLauncher<byte[]> outputInMemory() {
            return outputInMemory(null);
        }

        public UniformLauncher<byte[]> outputInMemory(@Nullable ByteSource stdin) {
            UniformOutputControl<byte[]> m = ProcessOutputControls.byteArrays(stdin);
            return output(m);
        }

    }

    public abstract class UniformLauncher<S> extends Launcher<S, S> {

        private UniformLauncher(ProcessContext processContext, ProcessOutputControl<S, S> outputControl) {
            super(processContext, outputControl);
        }

        public <T> UniformLauncher<T> map(Function<? super S, T> mapper) {
            UniformOutputControl<S> u = UniformOutputControl.wrap(this.outputControl);
            UniformOutputControl<T> t = u.map(mapper);
            return uniformOutput(t);
        }
    }

    /**
     * Constructs a builder instance that will produce a program that 
     * launches the given executable. Checks that a file exists at the given pathname
     * and that it is executable by the operating system.
     * @param executable the executable file
     * @return a builder instance
     * @throws IllegalArgumentException if {@link File#canExecute() } is false
     */
    public static Builder running(File executable) {
        checkArgument(executable.isFile(), "file not found: %s", executable);
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
        return "Subprocess{" +
                "executable='" + executable + '\'' +
                ", arguments=" + StringUtils.abbreviateMiddle(String.valueOf(arguments), "...", 64) +
                ", workingDirectory=" + workingDirectory +
                ", environment=" + StringUtils.abbreviateMiddle(String.valueOf(environment), "...", 64) +
                '}';
    }

}
