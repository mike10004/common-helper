package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.subprocess.ProcessOutputs.DirectOutput;

import static java.util.Objects.requireNonNull;

class BasicProcessResult<SO, SE> implements ProcessResult<SO, SE> {

    private final int exitCode;
    private final ProcessOutput<SO, SE> output;

    public BasicProcessResult(int exitCode, ProcessOutput<SO, SE> output) {
        this.exitCode = exitCode;
        this.output = requireNonNull(output);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public ProcessOutput<SO, SE> getOutput() {
        return output;
    }

    public static <SO, SE> BasicProcessResult<SO, SE> withNoOutput(int exitCode) {
        return new BasicProcessResult<>(exitCode, ProcessOutputs.bothNull());
    }

    public static <SO, SE> BasicProcessResult<SO, SE> create(int exitCode, SO stdout, SE stderr) {
        return new BasicProcessResult<>(exitCode, new DirectOutput<>(stdout, stderr));
    }
}
