package com.github.mike10004.nativehelper.subprocess;

public interface ProcessResult<SO, SE> {

    int getExitCode();
    ProcessOutput<SO, SE> getOutput();

}
