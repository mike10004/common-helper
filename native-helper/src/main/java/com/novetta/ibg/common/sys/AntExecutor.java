/*
 * (c) 2014 Novetta Solutions
 */
package com.novetta.ibg.common.sys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline;

/**
 * Class that provides a fluent interface to execute external processes with
 * Apache Ant. By default, the process exit code, stdout, and stderr are 
 * saved as project properties, and {@code searchPath} is set to true.
 * The exit code and output are available from the {@link #getStderr() },
 * {@link #getStdout() }, and {@link #getResult() } methods.
 * @author mchaberski
 * @see ExecTask
 * @deprecated use {@link com.github.mike10004.nativehelper.Program Program} API instead
 */
@Deprecated
public class AntExecutor {
    
    public static final String TASK_NAME = "antexecutor";
    public static final String TASK_TYPE = TASK_NAME;
    public static final String PROP_STDOUT = "antexecutor.stdout";
    public static final String PROP_STDERR = "antexecutor.stderr";
    public static final String PROP_RESULT = "antexecutor.result";
    private final Project project;
    private final Target target;
    private final ExecTask task;
    
    /**
     * Constructs an instance of the class using a newly-constructed task.
     */
    public AntExecutor() {
        this(new ExecTask());
    }
    
    /**
     * Constructs an instance of the class.
     * @param task the task to be executed
     */
    public AntExecutor(ExecTask task) {
        super();
        project = new Project();
        project.init();
        target = new Target();
        this.task = task;
        task.setProject(project);
        task.setOwningTarget(target);
        task.setTaskName(TASK_NAME);
        task.setTaskType(TASK_TYPE);
        task.setFailIfExecutionFails(true);
        task.setFailonerror(true);
        task.setSearchPath(true);
        task.setErrorProperty(PROP_STDERR);
        task.setOutputproperty(PROP_STDOUT);
        task.setResultProperty(PROP_RESULT);
    }
    
    /**
     * Gets the exit code from the process. If the task failed to execute,
     * this may be null even after the process finisheds.
     * @return the exit code, or null if the process never started
     */
    public Integer getResult() {
        String rpStr = task.getProject().getProperty(PROP_RESULT);
        if (rpStr == null) {
            return null;
        }
        return Integer.parseInt(rpStr);
    }
    
    /**
     * Gets the task.
     * @return the task
     */
    public ExecTask getTask() {
        return task;
    }
    
    /**
     * Gets the project.
     * @return the project
     */
    public Project getProject() {
        return project;
    }
    
    /**
     * Gets the stdout from the process.
     * @return the stdout
     */
    public String getStdout() {
        return project.getProperty(PROP_STDOUT);
    }
    
    /**
     * Gets the stderr from the process.
     * @return the stderr
     */
    public String getStderr() {
        return project.getProperty(PROP_STDERR);
    }
    
    /**
     * Sets the arguments to be added to the command line. Warning: don't mix
     * with {@code addArgument} or {@code addArguments}.
     * @param first first argument
     * @param others additional arguments
     * @return this executor instance
     */
    public AntExecutor setArguments(String first, String...others) {
        setArguments(Lists.asList(first, others));
        return this;
    }
    
    /**
     * Sets the arguments to be added to the command line. Warning: don't
     * mix this with {@code addArgument} or {@code addArguments} methods.
     * @param args the arguments
     * @return this instance
     */
    public AntExecutor setArguments(Iterable<String> args) {
        List<String> list;
        if (args instanceof ImmutableList) {
            list = (ImmutableList<String>) args;
        } else {
            list = ImmutableList.copyOf(args);
        }
        Commandline cmdl = new Commandline();
        cmdl.addArguments(list.toArray(new String[list.size()]));
        task.setCommand(cmdl);
        return this;
    }
    
    /**
     * Set the executable. This can be the filename or the full path.
     * @param executable the executable filename or pathname
     * @return this instance
     */
    public AntExecutor setExecutable(String executable) {
        task.setExecutable(executable);
        return this;
    }
    
    /**
     * Execute the external process.
     * @return this instance
     * @throws BuildException if {@link org.apache.tools.ant.Task#execute() }
     * throws one
     */
    public AntExecutor execute() throws BuildException {
        task.execute();
        return this;
    }

    /**
     * Add an argument to the command line.
     * @param value the argument 
     * @return this instance
     */
    public AntExecutor addArgument(String value) {
        task.createArg().setValue(value);
        return this;
    }

    /**
     * Add an argument to the command line.
     * @param value the argument 
     * @return this instance
     * @see Commandline.Argument#setFile(java.io.File) 
     */
    public AntExecutor addArgument(File value) {
        task.createArg().setFile(value);
        return this;
    }

    /**
     * Add arguments to the command line.
     * @param values the arguments 
     * @return this instance
     * @see Commandline.Argument#setValue(java.lang.String) 
     */
    public AntExecutor addArguments(Iterable<String> values) {
        for (String value : values) {
            task.createArg().setValue(value);
        }
        return this;
    }

    /**
     * Add an argument to the command line.
     * @param values the arguments
     * @return this instance
     * @see Commandline.Argument#setFile(java.io.File) 
     */
    public AntExecutor addFileArguments(Iterable<File> values) {
        for (File value : values) {
            task.createArg().setFile(value);
        }
        return this;
    }

}