/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import com.google.common.math.LongMath;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.Redirector;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    protected NotifyingWatchdog createWatchdog() throws BuildException {
        if (timeout != null && destructible) {
            throw new IllegalStateException("setting timeout non-null and destructible=true is not yet supported");
        }
        final NotifyingWatchdog notifyingWatchdog;
        if (destructible) {
            notifyingWatchdog = new LethalWatchdog(Long.MAX_VALUE);
        } else {
            Long timeout_ = timeout;
            notifyingWatchdog = new NotifyingWatchdog(timeout_ == null ? Long.MAX_VALUE : timeout_.longValue());
        }
        notifyingWatchdog.setProcessListener(processWatcher);
        watchdog = notifyingWatchdog;
        return notifyingWatchdog;
    }

    private transient final Object startedLock = new Object();
    private transient final ProcessWatcher processWatcher = new ProcessWatcher();

    private class ProcessWatcher implements NotifyingWatchdog.ProcessListener {

        private transient final AtomicBoolean started = new AtomicBoolean();

        @Override
        public void started(Process process) {
            synchronized (startedLock) {
                started.set(true);
                startedLock.notifyAll();
            }
        }

        public boolean isAlreadyStarted() {
            return started.get();
        }
    }

    /**
     * Waits until the process instance has been started. This utililzes a listener that is notified
     * after the process has been constructed (via a call, say, to {@link java.lang.ProcessBuilder#start}
     * or {@link Runtime#exec(String)}, but before {@link Process#waitFor()} has been invoked.
     * @param timeout the timeout amount
     * @param timeUnit the unit of time for the timeout
     * @throws InterruptedException if interrupted while waiting
     * @return true if process has started after waiting has concluded (roughly, this means that the process
     * started before the timeout period elapsed)
     */
    public boolean waitUntilProcessStarts(int timeout, TimeUnit timeUnit) throws InterruptedException {
        synchronized (startedLock) {
            long timeWaited = 0L;
            long timeToWait = timeUnit.toMillis(timeout);
            while (!processWatcher.isAlreadyStarted() && timeWaited < timeToWait) {
                long waitStart = System.currentTimeMillis();
                startedLock.wait(timeToWait - timeWaited);
                long waitFinish = System.currentTimeMillis();
                timeWaited = LongMath.checkedAdd(timeWaited,  waitFinish - waitStart);
            }
        }
        return processWatcher.isAlreadyStarted();
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
