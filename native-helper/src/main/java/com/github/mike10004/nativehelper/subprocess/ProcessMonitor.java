package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.Subprocess.Launcher;
import com.github.mike10004.nativehelper.subprocess.Subprocess.ProcessExecutionException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Class that represents a process monitor. A process monitor is returned by
 * {@link Subprocess#launch(ProcessTracker, StreamContext)} or
 * {@link Launcher#launch()}. Process execution is inherently asynchronous, so
 * launching a monitor returns an object that allows you to decide how to
 * handle changes (or lack thereof) in the process's state.
 * @param <SO> type of captured standard output contents
 * @param <SE> type of captured standard error contents
 */
public class ProcessMonitor<SO, SE> {

    private final Process process;
    private final ListenableFuture<ProcessResult<SO, SE>> future;
    private final Supplier<? extends ListeningExecutorService> killExecutorServiceFactory;

    ProcessMonitor(Process process, ListenableFuture<ProcessResult<SO, SE>> future, Supplier<? extends ListeningExecutorService> killExecutorServiceFactory) {
        this.future = requireNonNull(future);
        this.process = requireNonNull(process);
        this.killExecutorServiceFactory = requireNonNull(killExecutorServiceFactory);
    }

    public ListenableFuture<ProcessResult<SO, SE>> future() {
        return future;
    }

    public ProcessDestructor destructor() {
        return new BasicProcessDestructor(process, killExecutorServiceFactory);
    }

    public Process process() {
        return process;
    }

    public static class ProcessExecutionInnerException extends ProcessExecutionException {
        public ProcessExecutionInnerException(Throwable cause) {
            super(cause);
        }
    }

    public ProcessResult<SO, SE> await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        try {
            boolean ok = process.waitFor(timeout, unit);
            if (!ok) {
                throw new TimeoutException("process.waitFor timeout elapsed");
            }
            return future().get();
        } catch (ExecutionException e) {
            throw new ProcessExecutionInnerException(e.getCause());
        }
    }

    public ProcessResult<SO, SE> await() throws ProcessException, InterruptedException {
        try {
            return future().get();
        } catch (ExecutionException e) {
            throw new ProcessExecutionInnerException(e.getCause());
        }
    }
}
