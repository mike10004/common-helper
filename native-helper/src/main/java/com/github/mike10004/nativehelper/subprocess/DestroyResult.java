package com.github.mike10004.nativehelper.subprocess;

public enum DestroyResult {

    /**
     * Constant that indicates the process has stopped, either naturally or because of the signal.
     */
    TERMINATED,

    /**
     * Constant that indicates the process is still alive.
     */
    STILL_ALIVE;

}
