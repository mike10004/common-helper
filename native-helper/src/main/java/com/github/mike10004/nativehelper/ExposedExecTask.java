/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import org.apache.tools.ant.BuildException;
import com.github.mike10004.nativehelper.repackaged.org.apache.tools.ant.taskdefs.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

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

    private final LethalWatchdog watchdog;
    private Execute execute;

    public ExposedExecTask() {
        redirector = new EchoableRedirector(this);
        watchdog = new LethalWatchdog();
    }

    @Override
    public void setTimeout(Long value) {
        throw new UnsupportedOperationException("no longer supported: use Program.executeAsync() and java.util.concurrent API");
    }
    
    /**
     * Returns the watchdog created in the constructor.
     */
    @Override
    protected LethalWatchdog createWatchdog() throws BuildException {
        return watchdog;
    }

    /**
     * @return true, always
     */
    @Deprecated
    public boolean isDestructible() {
        return true;
    }

    /**
     * Sets the task's destructibilty. True means that {@link #abort() }
     * can succeed.
     * @param destructible  true if task should be destructible
     */
    @Deprecated
    public void setDestructible(boolean destructible) {
        checkArgument(destructible, "all ExposedExecTask instances are destructible");
    }
    
    /**
     * Returns the watchdog created in the constructor.
     * @return  the watchdog
     */
    public LethalWatchdog getWatchdog() {
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
     * @return true always; the value at one time meant something, but now it does not
     */
    public boolean abort() {
        watchdog.destroy();
        return true;
    }

}
