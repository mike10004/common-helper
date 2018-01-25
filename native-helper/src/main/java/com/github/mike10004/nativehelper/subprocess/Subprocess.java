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

import com.github.mike10004.nativehelper.subprocess.StreamContext.UniformStreamContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Class that represents a subprocess to be executed. Instances of this class
 * are immutable and may be reused. This API adheres to an asynchronous model,
 * so after you launch a process, you receive a {@link ProcessMonitor monitor}
 * instance that allows you to wait for the result on the current thread or
 * attach a listener notified when the process terminates.
 *
 * <p>To launch a process and ignore the output:</p>
 * <pre>
 * {@code
 *     ProcessMonitor<?, ?> monitor = Subprocess.running("true").build()
 *             .launcher(ProcessTracker.create())
 *             .launch();
 *     ProcessResult<?, ?> result = monitor.await();
 *     System.out.println("exit with status " + result.exitCode()); // exit with status 0
 * }
 * </pre>
 *
 * <p>To launch a process and capture the output as strings:</p>
 * <pre>
 * {@code
 *     ProcessMonitor<String, String> monitor = Subprocess.running("echo")
 *             .arg("hello, world")
 *             .build()
 *             .launcher(ProcessTracker.create())
 *             .outputStrings(Charset.defaultCharset())
 *             .launch();
 *     ProcessResult<String, String> result = monitor.await();
 *     System.out.println(result.content().stdout()); // hello, world
 * }
 * </pre>
 *
 * <p>To launch a process and capture the output in files:</p>
 * <pre>
 * {@code
 *     ProcessMonitor<File, File> monitor = Subprocess.running("echo")
 *             .arg("this is in a file")
 *             .build()
 *             .launcher(ProcessTracker.create())
 *             .outputTempFiles(new File(System.getProperty("java.io.tmpdir")).toPath())
 *             .launch();
 *     ProcessResult<File, File> result = monitor.await();
 *     System.out.println("printed:");
 *     java.nio.file.Files.copy(result.content().stdout().toPath(), System.out); // this is in a file
 * }
 * </pre>
 *
 * <p>To launch a process and send it data on standard input:</p>
 * <pre>
 * {@code
 *     ByteSource input = Files.asByteSource(new File("/etc/passwd"));
 *     ProcessMonitor<String, String> monitor = Subprocess.running("grep")
 *             .arg("root")
 *             .build()
 *             .launcher(ProcessTracker.create())
 *             .outputStrings(Charset.defaultCharset(), input)
 *             .launch();
 *     ProcessResult<String, String> result = monitor.await();
 *     System.out.println("printed " + result.content().stdout()); // root:x:0:0:root:/root:/bin/bash
 * }
 * </pre>
 *
 * @since 7.1.0
 */
public class Subprocess {
    
    private final String executable;
    private final ImmutableList<String> arguments;
    @Nullable
    private final File workingDirectory;
    private final ImmutableMap<String, String> environment;
    private final Supplier<? extends ListeningExecutorService> launchExecutorServiceFactory;

    protected Subprocess(String executable, @Nullable File workingDirectory, Map<String, String> environment, Iterable<String> arguments) {
        this.executable = requireNonNull(executable, "executable");
        this.workingDirectory = workingDirectory;
        this.arguments = ImmutableList.copyOf(arguments);
        this.environment = ImmutableMap.copyOf(environment);
        launchExecutorServiceFactory = ExecutorServices.newSingleThreadExecutorServiceFactory("subprocess-launch");
    }

    /**
     * Launches a subprocess. It's probably easier to use {@link #launcher(ProcessTracker)}
     * to build a launcher and then invoke {@link Launcher#launch()}.
     * @return a future representing the computation
     */
    public <C extends StreamControl, SO, SE> ProcessMonitor<SO, SE> launch(ProcessTracker processTracker, StreamContext<C, SO, SE> streamContext) throws ProcessException {
        C streamControl;
        try {
            streamControl = streamContext.produceControl();
        } catch (IOException e) {
            throw new ProcessLaunchException("failed to produce output context", e);
        }
        // a one-time use executor service; it is shutdown immediately after exactly one task is submitted
        ListeningExecutorService launchExecutorService = launchExecutorServiceFactory.get();
        ProcessMissionControl.Execution<SO, SE> execution = new ProcessMissionControl(this, processTracker, launchExecutorService)
                .launch(streamControl, exitCode -> {
                    StreamContent<SO, SE> content = streamContext.transform(exitCode, streamControl);
                    return ProcessResult.direct(exitCode, content);
                });
        ListenableFuture<ProcessResult<SO, SE>> fullResultFuture = execution.getFuture();
        launchExecutorService.shutdown(); // previously submitted tasks are executed
        ProcessMonitor<SO, SE> monitor = new BasicProcessMonitor<>(execution.getProcess(), fullResultFuture, processTracker);
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

    /**
     * Creates a new launcher in the given process context. The launcher
     * created does not specify that output is to be captured. Use the
     * {@code Launcher} methods to specify how input is to be sent to the process and how
     * output is to be captured.
     * @param processTracker the process context
     * @return the launcher
     */
    public Launcher<Void, Void> launcher(ProcessTracker processTracker) {
        return toSinkhole(processTracker);
    }

    private Launcher<Void, Void> toSinkhole(ProcessTracker processTracker) {
        return new Launcher<Void, Void>(processTracker, StreamContexts.sinkhole()){};
    }

    /**
     * Helper class that retains references to some dependencies so that you can
     * use a builder-style pattern to launch a process. Instances of this class are immutable
     * and methods that have return type {@code Launcher} return new instances.
     * @param <SO> standard output capture type
     * @param <SE> standard error capture type
     */
    public abstract class Launcher<SO, SE> {

        private final ProcessTracker processTracker;
        protected final StreamContext<?, SO, SE> streamContext;

        private Launcher(ProcessTracker processTracker, StreamContext<?, SO, SE> streamContext) {
            this.processTracker = requireNonNull(processTracker);
            this.streamContext = requireNonNull(streamContext);
        }

        public <S> UniformLauncher<S> output(UniformStreamContext<?, S> streamContext) {
            return uniformOutput(streamContext);
        }

        public <S> UniformLauncher<S> uniformOutput(StreamContext<?, S, S> streamContext) {
            return new UniformLauncher<S>(processTracker, streamContext) {};
        }

        public <SO2, SE2> Launcher<SO2, SE2> output(StreamContext<?, SO2, SE2> streamContext) {
            return new Launcher<SO2, SE2>(processTracker, streamContext) {};
        }

        public ProcessMonitor<SO, SE> launch() throws ProcessException {
            return Subprocess.this.launch(processTracker, streamContext);
        }

        public <SO2, SE2> Launcher<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
            return output(streamContext.map(stdoutMap, stderrMap));
        }

        public UniformLauncher<String> outputStrings(Charset charset) {
            requireNonNull(charset, "charset");
            return outputStrings(charset, null);
        }

        public UniformLauncher<String> outputStrings(Charset charset, @Nullable ByteSource stdin) {
            requireNonNull(charset, "charset");
            return output(StreamContexts.strings(charset, stdin));
        }

        @SuppressWarnings("unused")
        public UniformLauncher<byte[]> outputInMemory() {
            return outputInMemory(null);
        }

        public UniformLauncher<byte[]> outputInMemory(@Nullable ByteSource stdin) {
            UniformStreamContext<?, byte[]> m = StreamContexts.byteArrays(stdin);
            return output(m);
        }

        @SuppressWarnings("unused")
        public Launcher<Void, Void> inheritAllStreams() {
            return output(StreamContexts.inheritAll());
        }

        public Launcher<Void, Void> inheritOutputStreams() {
            return output(StreamContexts.inheritOutputs());
        }

        public UniformLauncher<File> outputFiles(File stdoutFile, File stderrFile, @Nullable ByteSource stdin) {
            return output(StreamContexts.outputFiles(stdoutFile, stderrFile, stdin));
        }

        public UniformLauncher<File> outputFiles(File stdoutFile, File stderrFile) {
            return outputFiles(stdoutFile, stderrFile, null);
        }

        public UniformLauncher<File> outputTempFiles(Path directory) {
            return outputTempFiles(directory, null);
        }

        public UniformLauncher<File> outputTempFiles(Path directory, @Nullable ByteSource stdin) {
            return output(StreamContexts.outputTempFiles(directory, stdin));
        }
    }

    /**
     * Class that represents a launcher using a uniform output control.
     * @param <S>
     */
    public abstract class UniformLauncher<S> extends Launcher<S, S> {

        private UniformLauncher(ProcessTracker processTracker, StreamContext<?, S, S> streamContext) {
            super(processTracker, streamContext);
        }

        public <T> UniformLauncher<T> map(Function<? super S, T> mapper) {
            UniformStreamContext<?, S> u = UniformStreamContext.wrap(this.streamContext);
            UniformStreamContext<?, T> t = u.map(mapper);
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

    /**
     * Gets the executable to be executed.
     * @return the executable
     */
    @SuppressWarnings("unused")
    public String executable() {
        return executable;
    }

    /**
     * Gets the arguments to be provided to the process.
     * @return the arguments
     */
    @SuppressWarnings("unused")
    public ImmutableList<String> arguments() {
        return arguments;
    }

    /**
     * Gets the working directory of the process. Null means the working
     * directory of the JVM.
     * @return the working directory pathname
     */
    @Nullable
    protected File workingDirectory() {
        return workingDirectory;
    }

    /**
     * Gets the map of environment variables to override or supplement
     * the process environment.
     * @return the environment
     */
    @SuppressWarnings("unused")
    public ImmutableMap<String, String> environment() {
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
