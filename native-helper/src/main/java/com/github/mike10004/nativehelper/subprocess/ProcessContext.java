package com.github.mike10004.nativehelper.subprocess;

/**
 * Interface that represents a process context. A process context tracks the creation
 * and destruction of processes.
 */
public interface ProcessContext {

    /**
     * Adds a process to this context instance.
     * @param process the process
     */
    void add(Process process);

    /**
     * Removes a process from this context instance.
     * @param process the process to remove
     * @return true iff the process existed in this process and was removed
     * @throws RuntimeException if process exists in this context and removing it failed
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean remove(Process process);

    /**
     * Gets the count of all processes added over the lifetime of this context.
     * @return the count
     */
    int count();

    /**
     * Gets the count of process that have been added to this context but not removed.
     * @return the count of active processes
     */
    int activeCount();

    /**
     * Creates a process context instance. The default implementation is used.
     * @return a new process context instance
     */
    static ProcessContext create() {
        return new ShutdownHookProcessContext();
    }

}
