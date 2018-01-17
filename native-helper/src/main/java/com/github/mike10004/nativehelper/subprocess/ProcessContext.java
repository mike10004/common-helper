package com.github.mike10004.nativehelper.subprocess;

public interface ProcessContext {

    void add(Process process);
    @SuppressWarnings("UnusedReturnValue")
    boolean remove(Process process);

    static ProcessContext create() {
        return new ShutdownHookProcessContext();
    }

}
