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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import com.github.mike10004.nativehelper.repackaged.org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Environment;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

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
public abstract class Program<R extends ProgramResult> {
    
    public static final String RESULT_PROPERTY_NAME = Program.class.getName() + ".exitCode";
    
    private final String executable;
    private final StandardInputSource standardInputSource;
    private final ImmutableList<String> arguments;
    @Nullable
    private final File workingDirectory;
    private final Supplier<? extends ExposedExecTask> taskFactory;
    private final ImmutableMap<String, String> environment;

    @Deprecated
    protected Program(String executable, @Nullable String standardInput, @Nullable File standardInputFile, @Nullable File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory) {
        this(executable, new StandardInputSource(standardInput, standardInputFile), workingDirectory, environment, arguments, taskFactory);
    }

    protected Program(String executable, StandardInputSource standardInputSource, @Nullable File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory) {
        this.executable = requireNonNull(executable, "executable");
        this.standardInputSource = requireNonNull(standardInputSource, "standardInputSource");
        this.workingDirectory = workingDirectory;
        this.arguments = ImmutableList.copyOf(arguments);
        this.taskFactory = checkNotNull(taskFactory);
        this.environment = ImmutableMap.copyOf(environment);
    }

    protected static final class StandardInputSource {

        @Nullable
        private final String memory;
        @Nullable
        private final File disk;

        private StandardInputSource(@Nullable String memory, @Nullable File disk) {
            this.memory = memory;
            this.disk = disk;
            checkArgument(memory == null || disk == null, "must specify memory or file source, but not both; both can be null");
        }

        public void apply(ExecTask task) {
            if (memory != null) {
                task.setInputString(memory);
            }
            if (disk != null) {
                task.setInput(disk);
            }
        }

        public String toString() {
            if (disk != null) {
                return disk.getAbsolutePath();
            }
            if (memory != null) {
                return String.format("\"%s\"[%d]", StringEscapeUtils.escapeJava(StringUtils.abbreviate(memory, 128)), memory.length());
            }
            return "<EOF>";
        }

        public boolean isEmpty() {
            return disk == null && memory == null;
        }
    }

    /**
     * Launches the external process, blocking on the same thread until complete.
     * @return the execution result
     * @throws BuildException if the program fails to execute, for example because 
     * the executable is not found 
     */
    public R execute() throws BuildException {
        ExposedExecTask task = taskFactory.get();
        return execute(task);
    }
    
    protected R execute(ExposedExecTask task) throws BuildException {
        Map<String, Object> localContext = new HashMap<>();
        configureTask(task, localContext);
        task.execute();
        R result = produceResultFromExecutedTask(task, localContext);
        return result;
    }

    protected class TaskCallable implements Callable<R> {

        private final ExposedExecTask task;
        private final TaskListener taskListener;

        public TaskCallable(ExposedExecTask task, TaskListener taskListener) {
            this.task = requireNonNull(task);
            this.taskListener = requireNonNull(taskListener);
        }

        @Override
        public R call() {
            taskListener.reached(TaskStage.CALLED);
            return execute(task);
        }
    }

    protected Callable<R> newExecutingCallable(final ExposedExecTask task, TaskListener taskListener) {
        return new TaskCallable(task, taskListener);
    }
    
    private static class TaskAbortingFuture<R> extends SimpleForwardingListenableFuture<R> {
        
        private final ExposedExecTask task;

        public TaskAbortingFuture(ExposedExecTask task, ListenableFuture<R> wrapped) {
            super(wrapped);
            this.task = task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            task.abort();
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
        return executeAsync(executorService, INACTIVE_TASK_LISTENER);
    }

    public enum TaskStage {
        /**
         * Invoked when the task is submitted to an executor service. Depending on the
         * executor service's execution constraints, it may not be called immediately.
         * For example, an executor with a limited thread pool whose threads are all
         * currently occupied will wait before invoking {@link Callable#call()}.
         * @see ExecutorService#submit(Callable)
         */
        SUBMITTED,

        /**
         * Invoked after the callable wrapping the task has been called. This means
         * that {@link Callable#call()} has been invoked and a {@link Future}
         * returned.
         */
        CALLED,

        /**
         * Invoked after the task process has been created. This is invoked after
         * the equivalent of {@link Runtime#exec(String)} is invoked and a {@link Process}
         * object has been created. If this method has been invoked, the process has for
         * sure been visible on the system, but it may have already exited by the time
         * this is invoked.
         */
        EXECUTED
    }

    /**
     * Listener whose methods are invoked at various stages of asynchronous execution.
     * The order in which a task is processed is (1) submitted, (2) called, (3) executed.
     */
    public interface TaskListener {
        void reached(TaskStage stage);
    }

    private static final TaskListener INACTIVE_TASK_LISTENER = stage -> {};

    public ListenableFuture<R> executeAsync(ExecutorService executorService, TaskListener taskListener) {
        ListeningExecutorService listeningService = MoreExecutors.listeningDecorator(executorService);
        ExposedExecTask task = taskFactory.get();
        task.getWatchdog().addProcessStartListener(process -> {
            taskListener.reached(TaskStage.EXECUTED);
        });
        ListenableFuture<R> innerFuture = listeningService.submit(newExecutingCallable(task, taskListener));
        taskListener.reached(TaskStage.SUBMITTED);
        TaskAbortingFuture<R> future = new TaskAbortingFuture<>(task, innerFuture);
        return future;
    }

    private static Environment.Variable newVariable(String name, String value) {
        Environment.Variable variable = new Environment.Variable();
        variable.setKey(name);
        variable.setValue(value);
        return variable;
    }

    protected void configureTask(ExposedExecTask task, Map<String, Object> executionContext) {
        task.setDestructible(true);
        task.setFailonerror(false);
        task.setResultProperty(RESULT_PROPERTY_NAME);
        task.setExecutable(executable);
        standardInputSource.apply(task);
        task.setDir(workingDirectory);
        for (String variableName : environment.keySet()) {
            String variableValue = environment.get(variableName);
            task.addEnv(newVariable(variableName, variableValue));
        }
        for (String argument : arguments) {
            task.createArg().setValue(argument);
        }
    }
    
    protected abstract R produceResultFromExecutedTask(ExecTask task, Map<String, Object> executionContext);
    
    protected static class SimpleProgram extends Program<ProgramResult> {
        
        public SimpleProgram(String executable, String standardInput, File standardInputFile, File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory) {
            super(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, taskFactory);
        }

        @Override
        protected ProgramResult produceResultFromExecutedTask(ExecTask task, Map<String, Object> executionContext) {
            int exitCode = getExitCode(task, executionContext);
            return new ExitCodeProgramResult(exitCode);
        }
        
    }
    
    protected static class DefaultTaskFactory implements Supplier<ExposedExecTask> {
        @Override
        public ExposedExecTask get() {
            ExposedExecTask task = new ExposedExecTask();
            task.setProject(new Project()); // do not call Project.init()
            return task;
        }
    }
    
    /**
     * Class that represents a builder of program instances. Create a builder
     * instance with {@link Program#running(String) }.
     * @see Program 
     */
    @NotThreadSafe
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        
        public static final Charset DEFAULT_STRING_OUTPUT_CHARSET = Charsets.UTF_8;
        
        protected final String executable;
        protected String standardInput;
        protected File standardInputFile;
        protected File workingDirectory;
        protected Supplier<? extends ExposedExecTask> taskFactory;
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
            taskFactory = new DefaultTaskFactory();
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

        protected Builder usingTaskFactory(Supplier<? extends ExposedExecTask> taskFactory) {
            this.taskFactory = checkNotNull(taskFactory, "taskFactory");
            return this;
        }

        /**
         * Constructs a program whose output is ignored. Use this if you only care
         * about the process exit code.
         * @return the program
         */
        public Program<ProgramResult> ignoreOutput() {
            return new SimpleProgram(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, taskFactory);
        }
        
        /**
         * Constructs a program that writes output to the given files.
         * @param stdoutFile the file to which standard output contents are to be written
         * @param stderrFile the file to which standard error contents are to be written
         * @return the program
         */
        public ProgramWithOutputFiles outputToFiles(File stdoutFile, File stderrFile) {
            return new ProgramWithOutputFiles(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, taskFactory, Suppliers.ofInstance(checkNotNull(stdoutFile)), Suppliers.ofInstance(checkNotNull(stderrFile)));
        }
        
        /**
         * Constructs a program that writes output to uniquely-named files 
         * in a given directory.
         * @param directory the directory
         * @return the program
         */
        public ProgramWithOutputFiles outputToTempFiles(Path directory) {
            return new ProgramWithOutputFiles(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, taskFactory, new TempFileSupplier("ProgramWithOutputFiles_stdout", ".tmp", directory.toFile()), new TempFileSupplier("ProgramWithOutputFiles_stderr", ".tmp", directory.toFile()));
        }
        
        /**
         * Constructs a program that saves output in memory as strings.
         * @return the program
         */
        public ProgramWithOutputStrings outputToStrings() {
            return new ProgramWithOutputStrings(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, taskFactory, DEFAULT_STRING_OUTPUT_CHARSET);
        }

        /**
         * Constructs a program that saves output in memory as strings. The 
         * charset argument only matters if {@link ProgramWithOutputResult#getStdout() }
         * or {@code getStderr()} is called, in which case the byte source will
         * represent the byte encoding of the output strings in the given charset.
         * @param charset the charset
         * @return the program
         */
        public ProgramWithOutputStrings outputToStrings(Charset charset) {
            return new ProgramWithOutputStrings(executable, standardInput, standardInputFile, workingDirectory, environment, arguments, taskFactory, charset);
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
    
    protected int getExitCode(ExecTask task, Map<String, Object> executionContext) {
        String resultPropertyStr = task.getProject().getProperty(RESULT_PROPERTY_NAME);
        if (resultPropertyStr == null) {
            throw new IllegalStateException("result property not set (maybe failonerror=false?) or task not yet executed");
        }
        int exitCode = Integer.parseInt(resultPropertyStr);
        return exitCode;
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
    public String getExecutable() {
        return executable;
    }

    protected StandardInputSource getStandardInput() {
        return standardInputSource;
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
                ", standardInput=" + standardInputSource +
                ", arguments=" + StringUtils.abbreviate(String.valueOf(arguments), 128) +
                ", workingDirectory=" + workingDirectory +
                ", environment=" + StringUtils.abbreviate(String.valueOf(environment), 128) +
                '}';
    }
}
