package com.github.mike10004.nativehelper.subprocess;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class AbstractDestroyAttempt implements DestroyAttempt {

    protected final DestroyResult result;
    protected final ProcessWaiter waiter;

    public AbstractDestroyAttempt(DestroyResult result, ProcessWaiter waiter) {
        this.result = requireNonNull(result);
        this.waiter = requireNonNull(waiter);
    }

    @Override
    public DestroyResult result() {
        return result;
    }

    public interface ProcessWaiter {
        @SuppressWarnings("UnusedReturnValue")
        int waitFor() throws InterruptedException;
        boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;
        boolean isAlive();
        static ProcessWaiter jre(Process process) {
            return new ProcessWaiter() {
                @Override
                public boolean isAlive() {
                    return process.isAlive();
                }

                @Override
                public int waitFor() throws InterruptedException {
                    return process.waitFor();
                }

                @Override
                public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
                    return process.waitFor(timeout, unit);
                }
            };
        }
    }
}
