package com.github.mike10004.subprocess;

public interface ProcessResult<T extends ProcessOutput> {

    Integer getExitCode();
    T getOutput();

}
