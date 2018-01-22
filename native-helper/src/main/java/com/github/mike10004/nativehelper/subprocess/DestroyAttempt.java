package com.github.mike10004.nativehelper.subprocess;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public interface DestroyAttempt {

    DestroyResult result();

    interface TermAttempt extends DestroyAttempt {
        TermAttempt await() throws InterruptedException;
        TermAttempt timeout(long duration, TimeUnit unit);
        KillAttempt kill();
    }

    interface KillAttempt extends DestroyAttempt {
        void awaitKill() throws InterruptedException;
        boolean timeoutKill(long duration, TimeUnit timeUnit);
        void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException;
    }

    class ProcessStillAliveException extends ProcessException {

        public ProcessStillAliveException() {
            super("kill failed");
        }
    }

}
