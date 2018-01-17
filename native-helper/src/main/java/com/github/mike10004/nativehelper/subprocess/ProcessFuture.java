package com.github.mike10004.nativehelper.subprocess;

import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;

import static java.util.Objects.requireNonNull;

public class ProcessFuture<SO, SE> extends SimpleForwardingListenableFuture<ProcessResult<SO, SE>> {

    private final Process process;

    ProcessFuture(Process process, ListenableFuture<ProcessResult<SO, SE>> delegate) {
        super(delegate);
        this.process = requireNonNull(process);
    }

    public Process getProcess() {
        return process;
    }
}
