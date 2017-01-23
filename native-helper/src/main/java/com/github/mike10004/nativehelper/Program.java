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
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a program that is to be executed in an external process.
 * Instances of this class are immutable, so it is safe to execute the same 
 * program in a multithreaded context. All configuration is performed with a 
 * {@link Builder} instance. Use {@link #running(java.lang.String) } to create
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
    private final ImmutablePair<String, File> standardInput;
    private final ImmutableList<String> arguments;
    private final @Nullable File workingDirectory;
    private final Supplier<? extends ExposedExecTask> taskFactory;
    private final ImmutableMap<String, String> environment;

    protected Program(String executable, @Nullable String standardInput, @Nullable File standardInputFile, @Nullable File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory) {
        this.executable = checkNotNull(executable);
        this.standardInput = ImmutablePair.of(standardInput, standardInputFile);
        checkArgument(standardInput == null || standardInputFile == null, "can't set both standard input string and file");
        this.workingDirectory = workingDirectory;
        this.arguments = ImmutableList.copyOf(arguments);
        this.taskFactory = checkNotNull(taskFactory);
        this.environment = ImmutableMap.copyOf(environment);
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
    
    protected Callable<R> newExecutingCallable(final ExposedExecTask task) {
        return new Callable<R>(){
            @Override
            public R call() throws BuildException {
                return execute(task);
            }
        };
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
     * com.google.common.util.concurrent.FutureCallback, java.util.concurrent.Executor) }
     * to add a callback to the future. (Using the overloaded version of that method that 
     * provides a {@code directExecutor} as the executor argument is probably okay; see the
     * discussion in {@link ListenableFuture#addListener(java.lang.Runnable, java.util.concurrent.Executor) }.
     * Calling {@link java.util.concurrent.Future#cancel(boolean) } will invoke 
     * {@link Process#destroy() } on the process instance.
     * @param executorService the executor service
     * @return a future representing the computation
     */
    public ListenableFuture<R> executeAsync(ExecutorService executorService) {
        ListeningExecutorService listeningService = MoreExecutors.listeningDecorator(executorService);
        ExposedExecTask task = taskFactory.get();
        ListenableFuture<R> innerFuture = listeningService.submit(newExecutingCallable(task));
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
        if (standardInput.getLeft() != null) {
            task.setInputString(standardInput.getLeft());
        } else if (standardInput.getRight() != null) {
            task.setInput(standardInput.getRight());
        } else if (standardInput.getLeft() != null && standardInput.getRight() != null) {
            throw new IllegalStateException("stdin misconfiguration");
        }
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
     * instance with {@link Program#running(java.lang.String) }.
     * @see Program 
     */
    @NotThreadSafe
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
         * @see #args(java.lang.Iterable) 
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
            checkArgument(Iterables.all(arguments, Predicates.notNull()), "all arguments must be non-null");
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

    protected String getExecutable() {
        return executable;
    }

    protected ImmutablePair<String, File> getStandardInput() {
        return standardInput;
    }

    protected ImmutableList<String> getArguments() {
        return arguments;
    }

    protected @Nullable File getWorkingDirectory() {
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
