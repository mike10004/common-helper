package com.github.mike10004.nativehelper.subprocess;

import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;

public class ProcessFuture<T extends ProcessOutput> extends SimpleForwardingListenableFuture<ProcessResult<T>> {

    public final Process process;

    ProcessFuture(Process process, ListenableFuture<ProcessResult<T>> delegate) {
        super(delegate);
        this.process = process;
    }

}
