package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import javax.annotation.Nullable;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Class that represents the byte sources and sinks to be attached to
 * a process's standard output, error, and input streams.
 */
class PredefinedStreamControl implements StreamControl {

    /**
     * Sink for bytes read from the process standard output stream.
     */
    private final ByteSink stdout;

    /**
     * Sink for bytes read from the process standard error stream.
     */
    private final ByteSink stderr;

    /**
     * Source of bytes to supply on the process's standard input stream. Can be null
     * if nothing is to be sent to the process.
     */
    @Nullable
    private  final ByteSource stdin;

    public PredefinedStreamControl(ByteSink stdout, ByteSink stderr, @Nullable ByteSource stdin) {
        this.stdin = stdin;
        this.stdout = requireNonNull(stdout);
        this.stderr = requireNonNull(stderr);
    }

    @Override
    public OutputStream openStdoutSink() throws IOException {
        return stdout.openStream();
    }

    @Override
    public OutputStream openStderrSink() throws IOException {
        return stderr.openStream();
    }

    @Nullable
    @Override
    public InputStream openStdinSource() throws IOException {
        return stdin == null ? null : stdin.openStream();
    }

    private PredefinedStreamControl(Builder builder) {
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

    /**
     * Returns an instance defining synthetic sources and sinks that do not capture
     * any output from the process and do not send any input to the process.
     * @return
     */
    public static PredefinedStreamControl nullWithNullInput() {
        return NULL_WITH_NULL_INPUT;
    }

    @SuppressWarnings("unused")
    public static PredefinedStreamControl nullWithEmptyInput() {
        return NULL_WITH_EMPTY_INPUT;
    }

    private static final PredefinedStreamControl NULL_WITH_NULL_INPUT = builder().build();
    private static final PredefinedStreamControl NULL_WITH_EMPTY_INPUT = builder().stdin(ByteSource.empty()).build();

    /**
     * Builder of instances. By default, no output is captured
     * from the process and no input is sent to the process.
     */
    public static final class Builder {
        private ByteSink stdout = NULL_SINK;
        private ByteSink stderr = NULL_SINK;
        @Nullable
        private ByteSource stdin = null;

        private Builder() {
        }

        public Builder noStdin() {
            return stdin(null);
        }

        public Builder emptyStdin() {
            return stdin(ByteSource.empty());
        }

        public Builder stderrToDevNull() {
            return stderr(NULL_SINK);
        }

        public Builder stdoutToDevNull() {
            return stdout(NULL_SINK);
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

        public Builder inheritStdin() {
            return stdin(new ByteSource() {
                @Override
                public InputStream openStream() {
                    return nonclosing(System.in);
                }
            });
        }

        public Builder inheritStderr() {
            return stderr(new ByteSink() {
                @Override
                public OutputStream openStream() {
                    return nonclosing(System.err);
                }
            });
        }

        public Builder inheritStdout() {
            return stdout(new ByteSink() {
                @Override
                public OutputStream openStream() {
                    return nonclosing(System.out);
                }
            });
        }

        public PredefinedStreamControl build() {
            return new PredefinedStreamControl(this);
        }
    }
}