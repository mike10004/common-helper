package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.KillAttempt;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.TermAttempt;
import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.sun.javafx.scene.control.skin.IntegerFieldSkin;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SubprocessKillTest extends SubprocessTestBase {

    private static File pySignalFile() {
        return Tests.getPythonFile("signal_listener.py");
    }

    private static Subprocess pySignal(boolean swallowSigterm) {
        Subprocess.Builder builder = Subprocess.running(pySignalFile());
        if (swallowSigterm) {
            builder.arg("--swallow-sigterm");
        }
        return builder.build();
    }

    @Test(timeout = 10000L)
    public void killTerm() throws Exception {
        ProcessMonitor<?, ?> monitor = pySignal(true).launcher(CONTEXT)
                .launch();
        ExitCheck<ProcessResult<?, ?>> exitCheck = new ExitCheck<>(result -> result.getExitCode() != 0);
        Futures.addCallback(monitor.future(), exitCheck, MoreExecutors.directExecutor());
        TermAttempt termAttempt = monitor.destructor().sendTermSignal();
        assertEquals("term attempt result", DestroyResult.STILL_ALIVE, termAttempt.result());
        KillAttempt attempt = termAttempt.kill();
        attempt.awaitKill();
        int exitCode = monitor.await().getExitCode();
        if (Platforms.getPlatform().isLinux()) {
            assertEquals("exit code", EXPECTED_SIGKILL_EXIT_CODE, exitCode);
        }
        exitCheck.doAssert();
    }

    private static final int EXPECTED_SIGTERM_EXIT_CODE = 128 + 15;
    private static final int EXPECTED_SIGKILL_EXIT_CODE = 128 + 9;

    private static class ExitCheck<T> extends AlwaysCallback<T> implements FutureCallback<T> {
        private final Predicate<? super T> exitCodePredicate;
        private volatile boolean check;
        private T result;

        private ExitCheck(Predicate<? super T> exitCodePredicate) {
            this.exitCodePredicate = exitCodePredicate;
        }

        @Override
        protected void always(@Nullable T result, @Nullable Throwable t) {
            this.check = exitCodePredicate.test(result);
            this.result = result;
        }

        @SuppressWarnings("UnusedReturnValue")
        public T doAssert() {
            assertNotNull("check not performed", result);
            assertTrue("failed exit check with " + result, check);
            return result;
        }

    }
}
