/*
 * (c) 2012 IBG LLC
 */
package com.novetta.ibg.common.sys;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.*;

/**
 * Task that runs a program in an external process and echos output. Provides 
 * hooks to read output as it arrives on the process's standard output and 
 * error streams. Uses a custom {@link EchoableRedirector Redirector} that
 * creates a {@link EchoingPumpStreamHandler stream handler} that provides a 
 * hook to hear output as it arrives. Allows a task to be aborted 
 * prematurely, if the {@code destructible} flag is true.
 * 
 * <p>The better way to do this would be to override the {@code Redirector}'s 
 * {@link Redirector#createStreams() createStreams()} method, but there are 
 * many private members used in that method, so it would take a lot of code
 * to override the class's setter methods to obtain visible references to 
 * them. 
 *   @author mchaberski
 */
public class ExposedExecTask extends ExecTask {

    private ExecuteWatchdog watchdog;
    private Execute execute;
    private Long timeout;
    private boolean destructible; 
    
    public ExposedExecTask() {
        redirector = new EchoableRedirector(this);
    }

    @Override
    public void setTimeout(Long value) {
        super.setTimeout(value);
        timeout = value;
    }
    
    /**
     * Create the Watchdog to kill a runaway process.
     *
     * @return instance of ExecuteWatchdog.
     *
     * @throws BuildException under unknown circumstances.
     */
    @Override
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout != null && destructible) {
            throw new IllegalStateException("setting timeout non-null and destructible=true is not yet supported");
        }
        if (timeout != null) {
            return watchdog = new ExecuteWatchdog(timeout.longValue());
        }
        if (destructible) {
            return watchdog = new LethalWatchdog(Long.MAX_VALUE);
        }
        return null;
    }

    /**
     * Checks whether the task is destructible.
     * @return 
     */
    public boolean isDestructible() {
        return destructible;
    }

    /**
     * Sets the task's destructibilty. True means that {@link #abort() }
     * can succeed.
     * @param destructible  true if task should be destructible
     */
    public void setDestructible(boolean destructible) {
        this.destructible = destructible;
    }
    
    /**
     * Returns the watchdog. This will be a {@link LethalWatchdog} instance if 
     * {@code destructible} was set to true and {@code createWatchdog()} has been
     * invoked. Invoking {@code execute()} invokes {@code createWatchdog()}.
     * @return  the watchdog
     */
    public ExecuteWatchdog getWatchdog() {
        return watchdog;
    }

    @Override
    protected Execute prepareExec() throws BuildException {
        execute = super.prepareExec();
        return execute;
    }

    public Execute getExecute() {
        return execute;
    }
    
    public EchoableRedirector getRedirector() {
        return (EchoableRedirector) redirector;
    }

    /**
     * Attempts to destroy the process. Calls {@link LethalWatchdog#destroy() }.
     * If this task's watchdog has not been set, then this method does nothing.
     * @return true if abort succeeded
     */
    public boolean abort() {
        ExecuteWatchdog dog = getWatchdog();
        if (dog != null) {
            if (dog instanceof LethalWatchdog) {
                ((LethalWatchdog)dog).destroy();
                return true;
            } else {
                throw new IllegalStateException("watchdog is not a LethalWatchdog; this task was incorrectly set up");
            }
        }
        return false;
    }
    
    
}
