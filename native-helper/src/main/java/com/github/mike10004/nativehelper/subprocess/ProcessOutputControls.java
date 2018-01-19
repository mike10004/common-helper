package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.ProcessOutputControl.UniformOutputControl;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ProcessOutputControls {

    private static <SO, SE> Function<Integer, ProcessResult<SO, SE>> noOutputTransform() {
        return new Function<Integer, ProcessResult<SO, SE>>() {
            @Override
            public ProcessResult<SO, SE> apply(Integer exitCode) {
                requireNonNull(exitCode, "exitCode");
                return BasicProcessResult.withNoOutput(exitCode);
            }
        };
    }

    public static ProcessOutputControl<Void, Void> sinkhole() {
        return predefined(ProcessStreamEndpoints.nullWithNullInput(), noOutputTransform());
    }

    public static UniformOutputControl<ByteSource> memoryByteSources() {
        return memoryByteSources(null);
    }

    public static UniformOutputControl<ByteSource> memoryByteSources(@Nullable ByteSource stdin) {
        return byteArrays(stdin).map(ByteSource::wrap);
    }

    public static ProcessOutputControl<CharSource, CharSource> memoryCharSources(Charset charset) {
        requireNonNull(charset);
        return memoryCharSources(charset, null);
    }

    public static ProcessOutputControl<CharSource, CharSource> memoryCharSources(Charset charset, @Nullable ByteSource stdin) {
        requireNonNull(charset);
        return memoryByteSources(stdin).map(byteSource -> byteSource.asCharSource(charset));
    }

    public static UniformOutputControl<byte[]> byteArrays() {
        return byteArrays(null);
    }

    public static UniformOutputControl<byte[]> byteArrays(@Nullable ByteSource stdin) {
        ByteBucket stdout = ByteBucket.withInitialCapacity(256);
        ByteBucket stderr = ByteBucket.withInitialCapacity(256);
        ProcessStreamEndpoints endpoints = ProcessStreamEndpoints.builder()
                .stderr(stderr)
                .stdout(stdout)
                .stdin(stdin)
                .build();
        return new UniformOutputControl<byte[]>() {
            @Override
            public ProcessStreamEndpoints produceEndpoints() {
                return endpoints;
            }

            @Override
            public Function<Integer, ProcessResult<byte[], byte[]>> getTransform() {
                return exitCode -> BasicProcessResult.create(exitCode, stdout.dump(), stderr.dump());
            }
        };
    }

    public static UniformOutputControl<String> strings(Charset charset) {
        return strings(charset, null);
    }

    public static UniformOutputControl<String> strings(Charset charset, @Nullable ByteSource stdin) {
        requireNonNull(charset);
        return byteArrays(stdin).map(bytes -> new String(bytes, charset));
    }

    public static <SO, SE> ProcessOutputControl<SO, SE> predefinedNoOutput(ProcessStreamEndpoints endpoints) {
        return predefined(endpoints, noOutputTransform());
    }

    public static <SO, SE> ProcessOutputControl<SO, SE> predefined(ProcessStreamEndpoints endpoints, Supplier<SO> stdoutProvider, Supplier<SE> stderrProvider) {
        return predefined(endpoints, () -> ProcessOutput.direct(stdoutProvider.get(), stderrProvider.get()));
    }

    public static <SO, SE> ProcessOutputControl<SO, SE> predefined(ProcessStreamEndpoints endpoints, Supplier<ProcessOutput<SO, SE>> outputter) {
        Function<Integer, ProcessResult<SO, SE>> transform = exitCode -> ProcessResult.direct(exitCode, outputter.get());
        return predefined(endpoints, transform);
    }

    public static <SO, SE> ProcessOutputControl<SO, SE> predefined(ProcessStreamEndpoints endpoints, Function<Integer, ProcessResult<SO, SE>> transform) {
        return new ProcessOutputControl<SO, SE>() {
            @Override
            public ProcessStreamEndpoints produceEndpoints() {
                return endpoints;
            }

            @Override
            public Function<Integer, ProcessResult<SO, SE>> getTransform() {
                return transform;
            }
        };
    }

    public static ProcessOutputControl<Void, Void> inheritOutputs() {
        return predefined(ProcessStreamEndpoints.builder().inheritStderr().inheritStdout().build(), noOutputTransform());
    }

    public static ProcessOutputControl<Void, Void> inheritAll() {
        return predefined(ProcessStreamEndpoints.builder().inheritStdin().inheritStderr().inheritStdout().build(), noOutputTransform());
    }
}
