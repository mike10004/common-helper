package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.ProcessOutputs.MappedOutput;

import java.util.function.Function;

/**
 * Interface defining methods for obtaining the captured content of
 * standard output and error streams of a process
 * @param <SO> type of captured standard output contents
 * @param <SE> type of captured standard error contents
 */
public interface ProcessOutput<SO, SE> {

    /**
     * Returns the content written to standard output by a process.
     * @return the standard output content
     */
    SO getStdout();

    /**
     * Returns the content written to standard error by a process.
     * @return the standard error content
     */
    SE getStderr();

    static <SO, SE> ProcessOutput<SO, SE> direct(SO stdout, SE stderr) {
        return new ProcessOutputs.DirectOutput<>(stdout, stderr);
    }

    default <SO2, SE2> ProcessOutput<SO2, SE2> map(Function<? super SO, SO2> stdoutMap, Function<? super SE, SE2> stderrMap) {
        return new MappedOutput<>(this, stdoutMap, stderrMap);
    }
}
