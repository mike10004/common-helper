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

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.novetta.ibg.common.sys.ExposedExecTask;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;

/**
 *
 * @author mchaberski
 */
public abstract class Program {
    
    private static final CharMatcher alphanumeric = CharMatcher.JAVA_LETTER_OR_DIGIT;
    public static final String KEY_RESULT_PROPERTY_NAME = Program.class.getName() + ".resultPropertyName";
    
    private final String executable;
    private final ImmutablePair<String, File> standardInput;
    private final ImmutableList<String> arguments;
    private final Optional<File> workingDirectory;
    private final Supplier<? extends ExposedExecTask> taskFactory;
    
    protected Program(String executable, @Nullable String standardInput, @Nullable File standardInputFile, @Nullable File workingDirectory, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory) {
        this.executable = checkNotNull(executable);
        this.standardInput = ImmutablePair.<String, File>of(standardInput, standardInputFile);
        checkArgument(standardInput == null || standardInputFile == null, "can't set both standard input string and file");
        this.workingDirectory = Optional.fromNullable(workingDirectory);
        this.arguments = ImmutableList.copyOf(arguments);
        this.taskFactory = checkNotNull(taskFactory);
    }

    public ProgramResult execute() {
        Map<String, Object> localContext = new HashMap<>();
        ExposedExecTask task = taskFactory.get();
        task.setProject(new Project());
        configureTask(task, localContext);
        executeInContext(task, localContext);
        ProgramResult result = produceResultFromExecutedTask(task, localContext);
        return result;
    }
    
    protected void executeInContext(ExecTask task, Map<String, Object> executionContext) {
        task.execute();
    }
    
    protected void configureTask(ExposedExecTask task, Map<String, Object> executionContext) {
        task.setFailonerror(false);
        String resultPropertyName = Program.class.getName() + "." + alphanumeric.retainFrom(UUID.randomUUID().toString());
        executionContext.put(KEY_RESULT_PROPERTY_NAME, resultPropertyName);
        task.setResultProperty(resultPropertyName);
        task.setExecutable(executable);
        if (standardInput.getLeft() != null) {
            task.setInputString(standardInput.getLeft());
        } else if (standardInput.getRight() != null) {
            task.setInput(standardInput.getRight());
        } else if (standardInput.getLeft() != null && standardInput.getRight() != null) {
            throw new IllegalStateException("stdin misconfiguration");
        }
        task.setDir(workingDirectory.orNull());
        for (String argument : arguments) {
            task.createArg().setValue(argument);
        }
    }
    
    protected abstract ProgramResult produceResultFromExecutedTask(ExecTask task, Map<String, Object> executionContext);
    
    public static class Builder {
        
        protected final String executable;
        protected String standardInput;
        protected File standardInputFile;
        protected File workingDirectory;
        protected Supplier<ExposedExecTask> taskFactory;
        protected final List<String> arguments;
        
        protected Builder(String executable) {
            this.executable = checkNotNull(executable);
            checkArgument(!executable.isEmpty(), "executable must be non-empty string");
            arguments = new ArrayList<>();
            taskFactory = new Supplier<ExposedExecTask>(){
                @Override
                public ExposedExecTask get() {
                    return new ExposedExecTask();
                }
            };
        }
        
        public Program build() {
            return new SimpleProgram(executable, standardInput, standardInputFile, workingDirectory, arguments, taskFactory);
        }
        
        protected static void copyFields(Builder src, Builder dst) {
            checkArgument(!dst.getClass().isAssignableFrom(src.getClass()), "expected destination instance to be a proper subclass of source class");
            dst.arguments.clear();
            dst.arguments.addAll(src.arguments);
            dst.standardInput = src.standardInput;
            dst.standardInputFile = src.standardInputFile;
            dst.workingDirectory = src.workingDirectory;
            dst.taskFactory = src.taskFactory;
        }
        
        public Builder read(String standardInputString) {
            this.standardInput = checkNotNull(standardInputString);
            return this;
        }
        
        public Builder read(File standardInputFile) {
            this.standardInputFile = checkNotNull(standardInputFile);
            return this;
        }
        
        public Builder in(File workingDirectory) {
            this.workingDirectory = checkNotNull(workingDirectory);
            return this;
        }
        
        public Builder arg(String argument) {
            this.arguments.add(checkNotNull(argument));
            return this;
        }
        
        public Builder args(String firstArgument, String...otherArguments) {
            return args(Lists.asList(checkNotNull(firstArgument), otherArguments));
        }
        
        public Builder args(Iterable<String> arguments) {
            checkArgument(Iterables.all(arguments, Predicates.notNull()), "all arguments must be non-null");
            Iterables.addAll(this.arguments, arguments);
            return this;
        }
        
        public ProgramWithOutputFiles.Builder writeOutputToFiles(File stdoutFile, File stderrFile) {
            return new ProgramWithOutputFiles.Builder(this, stdoutFile, stderrFile);
        }
    }
    
    public static Builder builder(File executable) {
        checkArgument(executable.canExecute(), "executable.canExecute");
        return builder(executable.getPath());
    }
    
    public static Builder builder(String executable) {
        return new Builder(executable);
    }
    
    protected static class SimpleProgram extends Program {

        public SimpleProgram(String executable, String standardInput, File standardInputFile, File workingDirectory, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory) {
            super(executable, standardInput, standardInputFile, workingDirectory, arguments, taskFactory);
        }

        @Override
        protected ProgramResult produceResultFromExecutedTask(ExecTask task, Map<String, Object> executionContext) {
            int exitCode = getExitCode(task, executionContext);
            return new SimpleProgramResult(exitCode);
        }
        
        protected int getExitCode(ExecTask task, Map<String, Object> executionContext) {
            String resultPropertyName = (String) executionContext.get(KEY_RESULT_PROPERTY_NAME);
            String resultPropertyStr = task.getProject().getProperty(resultPropertyName);
            if (resultPropertyStr == null) {
                throw new IllegalStateException("result property not set (maybe failonerror=false?) or task not yet executed");
            }
            int exitCode = Integer.parseInt(resultPropertyStr);
            return exitCode;
        }
    }
    
    protected static class SimpleProgramResult implements ProgramResult {

        private final int exitCode;

        public SimpleProgramResult(int exitCode) {
            this.exitCode = exitCode;
        }
        
        @Override
        public int getExitCode() {
            return exitCode;
        }
        
    }
}
