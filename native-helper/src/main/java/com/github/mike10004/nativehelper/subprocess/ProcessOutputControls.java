package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.ProcessOutputControl.UniformOutputControl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

class ProcessOutputControls {

    private static final ProcessOutput NO_OUTPUT = ProcessOutput.direct(null, null);

    @SuppressWarnings("unchecked")
    static <SO, SE> ProcessOutput<SO, SE> noOutput() {
        return NO_OUTPUT;
    }

    public static ProcessOutputControl<? extends OutputContext, Void, Void> sinkhole() {
        return ProcessOutputControl.predefined(PredefinedOutputContext.nullWithNullInput(), ProcessOutputControls::noOutput);
    }

    public static UniformOutputControl<? extends OutputContext, ByteSource> memoryByteSources(@Nullable ByteSource stdin) {
        return byteArrays(stdin).map(ByteSource::wrap);
    }

    @SuppressWarnings("unused")
    public static UniformOutputControl<? extends OutputContext, CharSource> memoryCharSources(Charset charset, @Nullable ByteSource stdin) {
        requireNonNull(charset);
        return memoryByteSources(stdin).map(byteSource -> byteSource.asCharSource(charset));
    }

    public static UniformOutputControl<? extends OutputContext, File> outputTempFiles(Path directory, @Nullable ByteSource stdin) {
        return new FileOutputControl() {
            @Override
            public FileContext produceContext() throws IOException {
                File stdoutFile = File.createTempFile("FileOutputControl_stdout", ".tmp", directory.toFile());
                File stderrFile = File.createTempFile("FileOutputControl_stderr", ".tmp", directory.toFile());
                return new FileContext(stdoutFile, stderrFile, stdin);
            }
        };
    }

    public static UniformOutputControl<? extends OutputContext, File> outputFiles(File stdoutFile, File stderrFile, @Nullable ByteSource stdin) {
        return new FileOutputControl() {
            @Override
            public FileContext produceContext() {
                return new FileContext(stdoutFile, stderrFile, stdin);
            }
        };
    }

    @VisibleForTesting
    static class BucketContext extends PredefinedOutputContext {

        public final ByteBucket stdout;
        public final ByteBucket stderr;

        public BucketContext(@Nullable ByteSource stdin) {
            this(ByteBucket.withInitialCapacity(256), ByteBucket.withInitialCapacity(256), stdin);
        }

        public BucketContext(ByteBucket stdout, ByteBucket stderr, @Nullable ByteSource stdin) {
            super(stdout, stderr, stdin);
            this.stdout = requireNonNull(stdout);
            this.stderr = requireNonNull(stderr);
        }


    }

    public static UniformOutputControl<? extends OutputContext, byte[]> byteArrays(@Nullable ByteSource stdin) {
        return new UniformOutputControl<BucketContext, byte[]>() {
            @Override
            public BucketContext produceContext() {
                return new BucketContext(stdin);
            }

            @Override
            public ProcessOutput<byte[], byte[]> transform(int exitCode, BucketContext ctx) {
                return ProcessOutput.direct(ctx.stdout.dump(), ctx.stderr.dump());
            }
        };
    }

    public static UniformOutputControl<? extends OutputContext, String> strings(Charset charset, @Nullable ByteSource stdin) {
        requireNonNull(charset);
        return byteArrays(stdin).map(bytes -> new String(bytes, charset));
    }

    public static ProcessOutputControl<? extends OutputContext, Void, Void> inheritOutputs() {
        return ProcessOutputControl.predefined(PredefinedOutputContext.builder().inheritStderr().inheritStdout().build(), ProcessOutputControls::noOutput);    }

    public static ProcessOutputControl<? extends OutputContext, Void, Void> inheritAll() {
        return ProcessOutputControl.predefined(PredefinedOutputContext.builder().inheritStdin().inheritStderr().inheritStdout().build(), ProcessOutputControls::noOutput);
    }

    public static class FileContext implements OutputContext {
        private final File stdoutFile, stderrFile;
        @Nullable
        private final ByteSource stdin;

        public FileContext(File stdoutFile, File stderrFile, @Nullable ByteSource stdin) {
            this.stdoutFile = requireNonNull(stdoutFile);
            this.stderrFile = requireNonNull(stderrFile);
            this.stdin = stdin;
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return new FileOutputStream(stdoutFile);
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return new FileOutputStream(stderrFile);
        }

        @Nullable
        @Override
        public InputStream openStdinSource() throws IOException {
            return stdin == null ? null : stdin.openStream();
        }
    }

    public static abstract class FileOutputControl implements UniformOutputControl<FileContext, File> {

        @Override
        public ProcessOutput<File, File> transform(int exitCode, FileContext context) {
            return ProcessOutput.direct(context.stdoutFile, context.stderrFile);
        }

    }

}
