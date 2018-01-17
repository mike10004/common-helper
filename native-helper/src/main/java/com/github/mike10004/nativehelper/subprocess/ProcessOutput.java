package com.github.mike10004.nativehelper.subprocess;

public interface ProcessOutput<SO, SE> {
    SO getStdout();
    SE getStderr();
}
