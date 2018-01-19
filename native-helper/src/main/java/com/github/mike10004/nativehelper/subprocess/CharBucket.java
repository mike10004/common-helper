package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.CharSink;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

class CharBucket extends CharSink {

    private final StringWriter collector;

    public CharBucket(StringWriter collector) {
        this.collector = collector;
    }

    @Override
    public Writer openStream() {
        return collector;
    }

    public String dump() {
        return collector.toString();
    }

    public static CharBucket withInitialCapacity(int capacity) {
        return new CharBucket(new StringWriter(capacity));
    }

    public static CharBucket create() {
        return new CharBucket(new StringWriter());
    }

    public String toString() {
        return "ByteBucket[" + collector.getBuffer().length() + "]";
    }
}
