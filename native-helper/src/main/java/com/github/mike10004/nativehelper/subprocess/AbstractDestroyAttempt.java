package com.github.mike10004.nativehelper.subprocess;

import static java.util.Objects.requireNonNull;

public class AbstractDestroyAttempt implements DestroyAttempt {

    protected final DestroyResult result;
    protected final Process process;

    public AbstractDestroyAttempt(DestroyResult result, Process process) {
        this.result = requireNonNull(result);
        this.process = requireNonNull(process);
    }

    @Override
    public DestroyResult result() {
        return result;
    }

}
