package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

public class EchoByteSink extends ByteSink {

    private PipedInputStream pipedInputStream;
    private PipedOutputStream pipedOutputStream;
    private transient final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public OutputStream openStream() throws IOException {
        pipedInputStream = new PipedInputStream();
        if (pipedOutputStream != null) {
            return pipedOutputStream;
        }
        pipedOutputStream = new PipedOutputStream(pipedInputStream);
        latch.countDown();
        return pipedOutputStream;
    }

    @SuppressWarnings("RedundantThrows")
    public InputStream connect() throws IOException, InterruptedException {
        latch.await();
        checkState(pipedInputStream != null, "BUG: input stream not yet created");
        return pipedInputStream;
    }

    @SuppressWarnings("RedundantThrows")
    public InputStream connect(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException {
        boolean succeeded = latch.await(timeout, timeUnit);
        if (!succeeded) {
            throw new IOException("waited for pipe to connect " + timeout + " " + timeUnit + " but it did not; this means that the stream was never opened");
        }
        checkState(pipedInputStream != null, "BUG: input stream not yet created");
        return pipedInputStream;
    }
}
