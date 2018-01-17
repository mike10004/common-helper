package com.github.mike10004.nativehelper.subprocess;

public interface ProcessOutput<SO, SE> {
    SO getStdout();
    SE getStderr();

    static <SO, SE> ProcessOutput<SO, SE> direct(SO stdout, SE stderr) {
        return new ProcessOutputs.DirectOutput<>(stdout, stderr);
    }

    default <SO2, SE2> ProcessOutput<SO2, SE2> map(Function<? super >)
}
