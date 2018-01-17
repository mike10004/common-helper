package com.github.mike10004.subprocess;

public interface ProcessContext {

    void add(Process process);
    boolean remove(Process process);

}
