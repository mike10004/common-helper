/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.LethalWatchdog.ProcessStartListener;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Execute;
import com.github.mike10004.subprocess.Processes;
import com.github.mike10004.subprocess.Processes.DestroyStatus;
import org.apache.tools.ant.BuildException;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

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
 * {@code Redirector#createStreams()} method, but there are
 * many private members used in that method, so it would take a lot of code
 * to override the class's setter methods to obtain visible references to 
 * them. 
 *   @author mchaberski
 */
public class ExposedExecTask extends ExecTask {

    private static final Logger log = Logger.getLogger(ExposedExecTask.class.getName());

    private final LethalWatchdog watchdog;
    private Execute execute;
    private final transient Object executeLock = new Object();
    private transient volatile boolean killed;
    private transient volatile Process process;

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
     * Do not call this; it will throw an exception if the argument is false, and otherwise
     * it will do nothing. All instances of this class are destructible.
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
     * Attempts to destroy the process.
     * If this task's process has not been set, then this method does nothing. The process is set
     * some time after {@link #execute()} is invoked. Therefore, this method's result is not very
     * predictable, so you should prefer to capture the process yourself using {@link #executeProcess(Consumer)}
     * and destroy it directly. At some point in the future, this method will be removed.
     * <p>This calls {@link Processes#destroy(Process, long, TimeUnit)} with a timeout of one second.
     * @return true if this task was never executed or it was successfully killed
     * @deprecated prefer using {@link #executeProcess(Consumer)} and doing what you want with that
     * process object
     */
    @Deprecated
    public boolean abort() {
        killed = true;
        DestroyStatus status = null;
        if (process != null) {
            status = Processes.destroy(process, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        return status != null && status.isDefinitelyDead();
    }

    private final static int DEFAULT_TIMEOUT_MILLIS = 1000;

    @Override
    public void execute() {
        synchronized (executeLock) {
            if (killed) {
                throw new IllegalStateException("kill() already called on this instance");
            }
            executeProcess(process -> {});
        }
    }

    /**
     * Executes the process, providing the process object in a callback that executes on a
     * different thread.
     */
    public void executeProcess(Consumer<? super Process> processCallback) {
        ProcessStartListener listener = process -> {
            processCallback.accept(process);
            this.process = process;
        };
        watchdog.addProcessStartListener(listener);
        try {
            super.execute();
            checkState(this.process != null, "process was never set");
        } finally {
            watchdog.removeProcessStartListener(listener);
        }
    }

    @Nullable
    Process getProcess() {
        return process;
    }
}
