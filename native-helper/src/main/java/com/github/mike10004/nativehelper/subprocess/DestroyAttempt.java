package com.github.mike10004.nativehelper.subprocess;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

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

    class Impl {

        private static final class AlreadyTerminated implements DestroyAttempt.KillAttempt, DestroyAttempt.TermAttempt {
            @Override
            public DestroyResult result() {
                return DestroyResult.TERMINATED;
            }

            @Override
            public TermAttempt await() throws InterruptedException {
                return this;
            }

            @Override
            public TermAttempt timeout(long duration, TimeUnit unit) {
                return this;
            }

            @Override
            public void awaitKill() throws InterruptedException {
            }

            @Override
            public boolean timeoutKill(long duration, TimeUnit timeUnit) {
                return true;
            }

            @Override
            public void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException {
            }

            @Override
            public KillAttempt kill() {
                return this;
            }
        }

        public static <A extends DestroyAttempt.KillAttempt & DestroyAttempt.TermAttempt> A alreadyTerminated() {
            return (A) ALREADY_TERMINATED;
        }

        private static final AlreadyTerminated ALREADY_TERMINATED = new AlreadyTerminated();

        private Impl() {}

        public interface ProcessWaiter {
            int waitFor() throws InterruptedException;
            boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;
            static ProcessWaiter jre(Process process) {
                return new ProcessWaiter() {
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

        public static class AbstractDestroyAttempt implements DestroyAttempt {

            private final DestroyResult result;
            private final ProcessWaiter waiter;
            private final Supplier<? extends ExecutorService> executorServiceFactory;

            public AbstractDestroyAttempt(DestroyResult result, ProcessWaiter waiter, Supplier<? extends ExecutorService> executorServiceFactory) {
                this.result = requireNonNull(result);
                this.waiter = requireNonNull(waiter);
                this.executorServiceFactory = requireNonNull(executorServiceFactory);
            }

            @Override
            public DestroyResult result() {
                return result;
            }

        }

        static class KillAttempt extends AbstractDestroyAttempt implements DestroyAttempt.KillAttempt {

            public KillAttempt(DestroyResult result, ProcessWaiter waiter, Supplier<? extends ExecutorService> executorServiceFactory) {
                super(result, waiter, executorServiceFactory);
            }

            @Override
            public void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException {
                throw new UnsupportedOperationException("KillAttempt.timeoutOrThrow");
            }

            @Override
            public void awaitKill() throws InterruptedException {
                throw new UnsupportedOperationException("KillAttempt.awaitKill");
            }

            @Override
            public boolean timeoutKill(long duration, TimeUnit unit) {
                throw new UnsupportedOperationException("KillAttempt.timeoutKill");
            }
        }

        static class TermAttempt extends AbstractDestroyAttempt implements DestroyAttempt.TermAttempt {

            private final ProcessDestructor destructor;

            public TermAttempt(ProcessDestructor destructor, ProcessWaiter waiter, DestroyResult result, Supplier<? extends ExecutorService> executorServiceFactory) {
                super(result, waiter, executorServiceFactory);
                this.destructor = requireNonNull(destructor);
            }

            @Override
            public TermAttempt await() {
                if (result() == DestroyResult.TERMINATED) {
                    return ALREADY_TERMINATED;
                }
                throw new UnsupportedOperationException("TermAttempt.await");
            }

            @Override
            public TermAttempt timeout(long duration, TimeUnit unit) {
                if (result() == DestroyResult.TERMINATED) {
                    return ALREADY_TERMINATED;
                }
                throw new UnsupportedOperationException("TermAttempt.timeout");
            }

            @Override
            public KillAttempt kill() {
                if (result() == DestroyResult.TERMINATED) {
                    return ALREADY_TERMINATED;
                }
                return destructor.sendKillSignal();
            }

        }
    }

}
