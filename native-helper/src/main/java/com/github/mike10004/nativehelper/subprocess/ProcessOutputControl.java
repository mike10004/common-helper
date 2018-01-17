package com.github.mike10004.nativehelper.subprocess;

import com.google.common.util.concurrent.AsyncFunction;

import java.io.IOException;

public interface ProcessOutputControl<T extends ProcessOutput> {

    ProcessStreamEndpoints produceEndpoints() throws IOException;

    AsyncFunction<Integer, ProcessResult<T>> getTransform();

}
