package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.Subprocess.ProcessExecutionException;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProcessMonitor<SO, SE> {

    private final Process process;
    private final ListenableFuture<ProcessResult<SO, SE>> future;

    ProcessMonitor(Process process, ListenableFuture<ProcessResult<SO, SE>> future) {
        this.future = future;
        this.process = process;
    }

    public ListenableFuture<ProcessResult<SO, SE>> future() {
        return future;
    }

    public ProcessDestructor destructor() {
        return new BasicProcessDestructor(process, ExecutorServices.newSingleThreadExecutorServiceFactory());
    }

    public Process getProcess() {
        return process;
    }

    public static class ProcessExecutionInnerException extends ProcessExecutionException {
        public ProcessExecutionInnerException(Throwable cause) {
            super(cause);
        }
    }

    public ProcessResult<SO, SE> await(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        try {
            return future().get(timeout, unit);
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
