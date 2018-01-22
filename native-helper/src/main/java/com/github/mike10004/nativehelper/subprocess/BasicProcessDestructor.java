package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.Impl;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.Impl.KillAttemptImpl;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.Impl.ProcessWaiter;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.Impl.TermAttemptImpl;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.KillAttempt;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.TermAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BasicProcessDestructor implements ProcessDestructor {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(BasicProcessDestructor.class);

    private final Process process;
    private final Supplier<? extends ExecutorService> executorServiceFactory;

    public BasicProcessDestructor(Process process, Supplier<? extends ExecutorService> executorServiceFactory) {
        this.process = process;
        this.executorServiceFactory = executorServiceFactory;
    }

    @Override
    public TermAttempt sendTermSignal() {
        if (isAlreadyTerminated()) {
            return Impl.terminated();
        }
        sendSignal(Process::destroy);
        return createTermAttempt();
    }

    private DestroyResult makeCurrentResult() {
        return isAlreadyTerminated() ? DestroyResult.TERMINATED : DestroyResult.STILL_ALIVE;
    }

    private boolean isAlreadyTerminated() {
        try {
            return process.waitFor(0, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ignore) {
        }
        return false;
    }

    private ProcessWaiter waiter() {
        return ProcessWaiter.jre(process);
    }

    protected TermAttempt createTermAttempt() {
        DestroyResult result = makeCurrentResult();
        if (result == DestroyResult.TERMINATED) {
            return DestroyAttempt.Impl.terminated();
        }
        return new TermAttemptImpl(this, waiter(), result, executorServiceFactory);
    }

    protected KillAttempt createKillAttempt() {
        DestroyResult result = makeCurrentResult();
        if (result == DestroyResult.TERMINATED) {
            return DestroyAttempt.Impl.terminated();
        }
        return new KillAttemptImpl(result, waiter(), executorServiceFactory);
    }

    @Override
    public KillAttempt sendKillSignal() {
        if (isAlreadyTerminated()) {
            return Impl.terminated();
        }
        sendSignal(Process::destroyForcibly);
        return createKillAttempt();
    }

    private void sendSignal(Consumer<? super Process> signaller) {
        signaller.accept(process);
    }
}