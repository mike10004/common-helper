package com.github.mike10004.nativehelper.subprocess;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AbstractDestroyAttempt implements DestroyAttempt {

    protected final DestroyResult result;
    protected final ProcessWaiter waiter;
    protected final Supplier<? extends ExecutorService> executorServiceFactory;

    public AbstractDestroyAttempt(DestroyResult result, ProcessWaiter waiter, Supplier<? extends ExecutorService> executorServiceFactory) {
        this.result = requireNonNull(result);
        this.waiter = requireNonNull(waiter);
        this.executorServiceFactory = requireNonNull(executorServiceFactory);
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
