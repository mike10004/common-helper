package com.github.mike10004.nativehelper.subprocess;

import java.util.function.Function;

public interface ProcessResult<SO, SE> {

    int getExitCode();

    ProcessOutput<SO, SE> getOutput();

    static <SO, SE> ProcessResult<SO, SE> direct(int exitCode, SO stdout, SE stderr) {
        return BasicProcessResult.create(exitCode, stdout, stderr);
    }

    static <SO, SE> ProcessResult<SO, SE> direct(int exitCode, ProcessOutput<SO, SE> output) {
        return new BasicProcessResult<>(exitCode, output);
    }

    default <SO2, SE2> ProcessResult<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        return new BasicProcessResult<>(getExitCode(), getOutput().map(stdoutMap, stderrMap));
    }
}
