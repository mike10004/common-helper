package com.github.mike10004.subprocess;

public interface ProcessOutput<SO, SE> {
    SO getStdout();
    SE getStderr();
}
