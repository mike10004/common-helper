/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.repackaged.org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.Uninterruptibles;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Class that represents a watchdog providing a method to kill the process
 * it is watching. That method performs the same actions as the superclass's
 * {@link ExecuteWatchdog#timeoutOccured(org.apache.tools.ant.util.Watchdog) }
 * method.
 * @author mchaberski
 */
public class LethalWatchdog extends ExecuteWatchdog {

    private static final Logger log = Logger.getLogger(LethalWatchdog.class.getName());

    protected Process process;
    private final List<ProcessStartListener> processStartListeners;
    
    public LethalWatchdog() {
        super(Long.MAX_VALUE);
        processStartListeners = Collections.synchronizedList(new ArrayList<>());
    }

    public interface ProcessStartListener {
        void started(Process process);
    }

    public void removeProcessStartListener(ProcessStartListener processStartListener) {
        processStartListeners.remove(processStartListener);
    }

    public void addProcessStartListener(ProcessStartListener processStartListener) {
        processStartListeners.add(processStartListener);
    }

    @Override
    public synchronized final void start(Process process) {
        checkState(this.process == null, "process already set");
        this.process = checkNotNull(process);
        this.processStartListeners.forEach(listener -> listener.started(process));
    }

    @Override
    protected synchronized void cleanUp() {
        process = null;
    }

    public enum DestroyStatus {

        /**
         * Term signal sent and the process stopped.
         * The process was sent SIGTERM and then stopped executing. This sends SIGTERM to
         * the process, but there's no way for us to know why the process terminated.
         * If it did exit due to the signal, it would probably exit with code 143, because
         * by convention, the exit code for termination due to a signal is 128 + the signal
         * number, and SIGTERM = 15. However, a program can decide to flout that convention
         * by handling the signal itself, or it can decide to exit with code 143 for any
         * reason whatsoever.
         *
         */
        TERMINATED,

        /**
         * Kill signal was sent and the process stopped.
         * The process was sent SIGKILL and then stopped executing. As with {@link #TERMINATED},
         * we can only speculate as to whether our SIGKILL actually killed it.
         */
        TERMINATED_FORCIBLY,

        /**
         * Already exited. The process had already exited on its own, so no signal was sent.
         */
        ALREADY_EXITED,

        /**
         * Timeout while waiting for the process to stop executing.
         * We waited for the process to die after sending SIGTERM and then SIGKILL, but
         * it still wouldn't die, even after the specified timeout. We don't know what the
         * process's status is.
         */
        TIMED_OUT,

        /**
         * Thread interrupt while waiting for process to stop executing.
         * While waiting for the process to stop executing, the thread that was waiting
         * was interrupted. We don't know what the process's status is.
         */
        INTERRUPTED;

        public boolean isDefinitelyDead() {
            switch (this) {
                case TERMINATED:
                case TERMINATED_FORCIBLY:
                case ALREADY_EXITED:
                    return true;
                case TIMED_OUT:
                case INTERRUPTED:
                    return false;
            }
            throw new IllegalStateException("unhandled: " + this);
        }

    }

    private static final int EXPECTED_SIGTERM_EXIT_CODE = 128 + 15;
    private static final int EXPECTED_SIGKILL_EXIT_CODE = 128 + 9;

    public static DestroyStatus destroy(Process process, long halfTimeout, TimeUnit unit) {
        @Nullable Integer exitValue = maybeGetExitValue(process);
        if (exitValue != null) {
            return DestroyStatus.ALREADY_EXITED;
        }
        boolean forcibly = false;
        process.destroy();
        boolean cleanExit = false;
        try {
            cleanExit = process.waitFor(halfTimeout, unit);
        } catch (InterruptedException e) {
            log.info("interrupted while waiting for process to terminate after sending SIGTERM");
            forcibly = true;
        }
        if (!cleanExit) {
            forcibly = true;
            try {
                cleanExit = process.destroyForcibly().waitFor(halfTimeout, unit);
            } catch (InterruptedException e) {
                log.info("interrupted while waiting for process to terminate after sending SIGKILL");
                return DestroyStatus.INTERRUPTED;
            }
        }
        if (cleanExit) {
            exitValue = process.exitValue();
            if (!windowsCheck.get() && exitValue != (forcibly ? EXPECTED_SIGKILL_EXIT_CODE : EXPECTED_SIGTERM_EXIT_CODE)) {
                log.warning(String.format("process exited with unexpected code %s after %s", exitValue, forcibly ? "sigkill" : "sigterm"));
            }
            return forcibly ? DestroyStatus.TERMINATED_FORCIBLY : DestroyStatus.TERMINATED;
        }
        if (process.isAlive()) {
            return DestroyStatus.TIMED_OUT;
        }
        //noinspection ConstantConditions
        return forcibly ? DestroyStatus.TERMINATED_FORCIBLY : DestroyStatus.TERMINATED;
    }

    private static final Supplier<Boolean> windowsCheck = Suppliers.memoize(() -> Platforms.getPlatform().isWindows());

    @Nullable
    private static Integer maybeGetExitValue(Process process) {
        try {
            return process.exitValue();
        } catch (IllegalThreadStateException ignore) {
            return null;
        }
    }
}
