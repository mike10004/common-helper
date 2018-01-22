package com.github.mike10004.nativehelper.subprocess;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface representing a process result. A process result is an exit code
 * and an object representing output from the process.
 * @param <SO> type of the captured standard output contents
 * @param <SE> type of the captured standard error contents
 */
public interface ProcessResult<SO, SE> {

    int getExitCode();

    ProcessOutput<SO, SE> getOutput();

    static <SO, SE> ProcessResult<SO, SE> direct(int exitCode, SO stdout, SE stderr) {
        return BasicProcessResult.create(exitCode, stdout, stderr);
    }

//    static <SO, SE> ProcessResult<SO, SE> supplied(int exitCode, Supplier<? extends ProcessOutput<SO, SE>> outputSupplier) {
//
//    }

    static <SO, SE> ProcessResult<SO, SE> direct(int exitCode, ProcessOutput<SO, SE> output) {
        return new BasicProcessResult<>(exitCode, output);
    }

    default <SO2, SE2> ProcessResult<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        return new BasicProcessResult<>(getExitCode(), getOutput().map(stdoutMap, stderrMap));
    }
}
