package com.github.mike10004.nativehelper;

class BuildException extends RuntimeException {
    public BuildException(String message) {
        super(message);
    }

    public BuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuildException(Throwable cause) {
        super(cause);
    }
}
