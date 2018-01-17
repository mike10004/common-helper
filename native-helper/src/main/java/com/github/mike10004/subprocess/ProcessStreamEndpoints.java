package com.github.mike10004.subprocess;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import javax.annotation.Nullable;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

public class ProcessStreamEndpoints {

    public final ByteSink stdout;
    public final ByteSink stderr;
    @Nullable
    public final ByteSource stdin;

    public ProcessStreamEndpoints(ByteSink stdout, ByteSink stderr, @Nullable ByteSource stdin) {
        this.stdin = stdin;
        this.stdout = requireNonNull(stdout);
        this.stderr = requireNonNull(stderr);
    }

    private ProcessStreamEndpoints(Builder builder) {
        stdout = builder.stdout;
        stderr = builder.stderr;
        stdin = builder.stdin;
    }


    private static FilterOutputStream nonclosing(OutputStream outputStream) {
        return new FilterOutputStream(outputStream) {
            @Override
            public void close() {
            }
        };
    }

    private static FilterInputStream nonclosing(InputStream inputStream) {
        return new FilterInputStream(inputStream) {
            @Override
            public void close() {
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final ByteSink NULL_SINK = new ByteSink() {
        @Override
        public final OutputStream openStream() {
            return ByteStreams.nullOutputStream();
        }

        @Override
        public final OutputStream openBufferedStream() {
            return openStream();
        }
    };

    private static final ProcessStreamEndpoints NULL_WITH_NULL_INPUT = builder().build();
    private static final ProcessStreamEndpoints NULL_WITH_EMPTY_INPUT = builder().stdin(ByteSource.empty()).build();

    public static final class Builder {
        private ByteSink stdout = NULL_SINK;
        private ByteSink stderr = NULL_SINK;
        @Nullable
        private ByteSource stdin;

        private Builder() {
        }

        public Builder stdout(ByteSink val) {
            stdout = requireNonNull(val);
            return this;
        }

        public Builder stderr(ByteSink val) {
            stderr = requireNonNull(val);
            return this;
        }

        public Builder stdin(@Nullable ByteSource val) {
            stdin = val;
            return this;
        }

        public ProcessStreamEndpoints build() {
            return new ProcessStreamEndpoints(this);
        }
    }
}
