package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class ByteBucket extends ByteSink {

    private final ByteArrayOutputStream collector;

    public ByteBucket(ByteArrayOutputStream collector) {
        this.collector = collector;
    }

    @Override
    public OutputStream openStream() throws IOException {
        return collector;
    }

    public byte[] dump() {
        return collector.toByteArray();
    }

    public static ByteBucket withInitialCapacity(int capacity) {
        return new ByteBucket(new ByteArrayOutputStream(capacity));
    }

    public static ByteBucket create() {
        return new ByteBucket(new ByteArrayOutputStream());
    }
}
