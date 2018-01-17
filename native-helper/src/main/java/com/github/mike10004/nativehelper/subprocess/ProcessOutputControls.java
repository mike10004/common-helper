package com.github.mike10004.nativehelper.subprocess;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.util.Objects.requireNonNull;

public class ProcessOutputControls {

    public static ProcessOutputControl<Void, Void> sinkhole() {
        return new ProcessOutputControl<Void, Void>() {
            @Override
            public ProcessStreamEndpoints produceEndpoints() {
                return ProcessStreamEndpoints.nullWithNullInput();
            }

            @Override
            public AsyncFunction<Integer, ProcessResult<Void, Void>> getTransform() {
                //noinspection NullableProblems
                return new AsyncFunction<Integer, ProcessResult<Void, Void>>() {
                    @Override
                    public ListenableFuture<ProcessResult<Void, Void>> apply(Integer input) {
                        requireNonNull(input, "exitCode");
                        return Futures.immediateFuture(BasicProcessResult.withNoOutput(input));
                    }
                };
            }
        };
    }

    public static ProcessOutputControl<ByteSource, ByteSource> memory() {
        ByteBucket stdout = ByteBucket.withInitialCapacity(256);
        ByteBucket stderr = ByteBucket.withInitialCapacity(256);
        ProcessStreamEndpoints endpoints = ProcessStreamEndpoints.builder()
                .stderr(stderr)
                .stdout(stdout)
                .build();
        return new ProcessOutputControl<ByteSource, ByteSource>() {
            @Override
            public ProcessStreamEndpoints produceEndpoints() throws IOException {
                return endpoints;
            }

            @Override
            public AsyncFunction<Integer, ProcessResult<ByteSource, ByteSource>> getTransform() {
                return AsyncFunctions.asAsync(exitCode -> {
                    return BasicProcessResult.create(exitCode, ByteSource.wrap(stdout.dump()), ByteSource.wrap(stderr.dump()));
                });
            }
        };
    }

    public static ProcessOutputControl<CharSource, CharSource> memoryDecoded(Charset charset) {

    }
}
