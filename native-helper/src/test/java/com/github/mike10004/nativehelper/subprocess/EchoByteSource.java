package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class EchoByteSource extends Pipe<PipedOutputStream, PipedInputStream> {

    @SuppressWarnings("RedundantThrows")
    @Override
    protected PipedOutputStream createFrom() throws IOException {
        return new PipedOutputStream();
    }

    @Override
    protected PipedInputStream createTo(PipedOutputStream from) throws IOException {
        return new PipedInputStream(from);
    }

    public ByteSource asByteSource() {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return openTo();
            }
        };
    }
}
