package com.github.mike10004.nativehelper.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
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

        @SuppressWarnings("unused")
        private static final Logger log = LoggerFactory.getLogger(Impl.class);

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

        public static <A extends DestroyAttempt.KillAttempt & DestroyAttempt.TermAttempt> A terminated() {
            return (A) ALREADY_TERMINATED;
        }

        private static final AlreadyTerminated ALREADY_TERMINATED = new AlreadyTerminated();

        private Impl() {}

        public interface ProcessWaiter {
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

        public static class AbstractDestroyAttempt implements DestroyAttempt {

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

        }

        static class KillAttemptImpl extends AbstractDestroyAttempt implements DestroyAttempt.KillAttempt {

            @SuppressWarnings("unused")
            private static final Logger log = LoggerFactory.getLogger(KillAttemptImpl.class);

            public KillAttemptImpl(DestroyResult result, ProcessWaiter waiter, Supplier<? extends ExecutorService> executorServiceFactory) {
                super(result, waiter, executorServiceFactory);
            }

            @Override
            public void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException {
                boolean succeeded = timeoutKill(duration, timeUnit);

            }

            @Override
            public void awaitKill() throws InterruptedException {
                waiter.waitFor();
            }

            @Override
            public boolean timeoutKill(long duration, TimeUnit unit) {
                try {
                    return waiter.waitFor(duration, unit);
                } catch (InterruptedException e) {
                    log.info("interrupted: " + e);
                    return false;
                }
            }
        }

        static class TermAttemptImpl extends AbstractDestroyAttempt implements DestroyAttempt.TermAttempt {

            @SuppressWarnings("unused")
            private static final Logger log = LoggerFactory.getLogger(TermAttemptImpl.class);

            private final ProcessDestructor destructor;

            public TermAttemptImpl(ProcessDestructor destructor, ProcessWaiter waiter, DestroyResult result, Supplier<? extends ExecutorService> executorServiceFactory) {
                super(result, waiter, executorServiceFactory);
                this.destructor = requireNonNull(destructor);
            }

            @Override
            public TermAttempt await() {
                if (result == DestroyResult.TERMINATED) {
                    return this;
                }
                try {
                    waiter.waitFor();
                } catch (InterruptedException e) {
                    log.debug("interrupted while waiting on process termination: " + e);
                }
                DestroyResult result = waiter.isAlive() ? DestroyResult.STILL_ALIVE : DestroyResult.TERMINATED;
                return new TermAttemptImpl(destructor, waiter, result, executorServiceFactory);
            }

            @Override
            public TermAttempt timeout(long duration, TimeUnit unit) {
                if (result == DestroyResult.TERMINATED) {
                    return this;
                }
                try {
                    boolean finished = waiter.waitFor(duration, unit);
                    if (finished) {
                        return Impl.terminated();
                    }
                } catch (InterruptedException e) {
                    log.info("interrupted: " + e);
                }
                return this;
            }

            @Override
            public KillAttempt kill() {
                if (result == DestroyResult.TERMINATED) {
                    return terminated();
                }
                return destructor.sendKillSignal();
            }

        }
    }

}
