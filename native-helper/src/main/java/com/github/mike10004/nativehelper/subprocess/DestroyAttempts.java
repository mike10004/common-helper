package com.github.mike10004.nativehelper.subprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class DestroyAttempts {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(DestroyAttempts.class);

    private static final class AlreadyTerminated implements DestroyAttempt.KillAttempt, DestroyAttempt.TermAttempt {
        @Override
        public DestroyResult result() {
            return DestroyResult.TERMINATED;
        }

        @Override
        public TermAttempt await() {
            return this;
        }

        @Override
        public TermAttempt timeout(long duration, TimeUnit unit) {
            return this;
        }

        @Override
        public void awaitKill() {
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

    @SuppressWarnings("unchecked")
    public static <A extends DestroyAttempt.KillAttempt & DestroyAttempt.TermAttempt> A terminated() {
        return (A) ALREADY_TERMINATED;
    }

    private static final AlreadyTerminated ALREADY_TERMINATED = new AlreadyTerminated();

    private DestroyAttempts() {}

    static class KillAttemptImpl extends AbstractDestroyAttempt implements DestroyAttempt.KillAttempt {

        @SuppressWarnings("unused")
        private static final Logger log = LoggerFactory.getLogger(KillAttemptImpl.class);

        public KillAttemptImpl(DestroyResult result, ProcessWaiter waiter, Supplier<? extends ExecutorService> executorServiceFactory) {
            super(result, waiter, executorServiceFactory);
        }

        @Override
        public void timeoutOrThrow(long duration, TimeUnit timeUnit) throws ProcessStillAliveException {
            boolean succeeded = timeoutKill(duration, timeUnit);
            if (!succeeded) {
                throw new ProcessStillAliveException();
            }
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
                    return DestroyAttempts.terminated();
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
