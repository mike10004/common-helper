package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class StreamPipeSource extends StreamPipe<OutputStream, InputStream> {

    @Override
    protected ComponentPair<OutputStream, InputStream> createComponents() throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        return ComponentPair.of(out, in);
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
