/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import org.apache.tools.ant.taskdefs.ExecuteWatchdog;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a watchdog providing a method to kill the process
 * it is watching. That method performs the same actions as the superclass's
 * {@link ExecuteWatchdog#timeoutOccured(org.apache.tools.ant.util.Watchdog) }
 * method.
 * @author mchaberski
 */
public class LethalWatchdog extends ExecuteWatchdog {

    protected Process process;
    protected Exception destroyException;
    
    public LethalWatchdog(long timeout) {
        super(timeout);
    }

    @Override
    public synchronized void start(Process process) {
        this.process = checkNotNull(process);
    }

    @Override
    protected synchronized void cleanUp() {
        process = null;
    }
    
    public synchronized void destroy() {
        Process process_ = process;
        if (process_ == null) {
            return;
        }
        try {
            try {
                // We must check if the process was not stopped before being here
                process_.exitValue();
            } catch (IllegalThreadStateException itse) {
                // the process is not terminated, if this is really
                // a timeout and not a manual stop then kill it.
                process_.destroy();
            }
        } catch (NullPointerException e) {
            throw e;
        } catch (Exception e) {
            destroyException = e;
            e.printStackTrace(System.err);
        } finally {
            cleanUp();
        }        
    }

    public Exception getDestroyException() {
        return destroyException;
    }

    public void setDestroyException(Exception destroyException) {
        this.destroyException = destroyException;
    }
    
    
}