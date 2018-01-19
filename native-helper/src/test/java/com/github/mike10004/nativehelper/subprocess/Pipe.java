package com.github.mike10004.nativehelper.subprocess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

public abstract class Pipe<F, T> {

    private F pipedInputStream;
    private T pipedOutputStream;
    private transient final CountDownLatch latch = new CountDownLatch(1);

    protected abstract F createFrom() throws IOException;
    protected abstract T createTo(F from) throws IOException;

    public T openTo() throws IOException {
        pipedInputStream = createFrom();
        if (pipedOutputStream != null) {
            return pipedOutputStream;
        }
        pipedOutputStream = createTo(pipedInputStream);
        latch.countDown();
        return pipedOutputStream;
    }

    @SuppressWarnings("RedundantThrows")
    public F connect() throws IOException, InterruptedException {
        latch.await();
        checkState(pipedInputStream != null, "BUG: input stream not yet created");
        return pipedInputStream;
    }

    @SuppressWarnings("RedundantThrows")
    public F connect(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException {
        boolean succeeded = latch.await(timeout, timeUnit);
        if (!succeeded) {
            throw new IOException("waited for pipe to connect " + timeout + " " + timeUnit + " but it did not; this means that the stream was never opened");
        }
        checkState(pipedInputStream != null, "BUG: input stream not yet created");
        return pipedInputStream;
    }
}
