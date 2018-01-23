/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import org.apache.tools.ant.taskdefs.ExecuteWatchdog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Class that represents a watchdog providing a method to kill the process
 * it is watching. That method performs the same actions as the superclass's
 * {@link ExecuteWatchdog#timeoutOccured(org.apache.tools.ant.util.Watchdog) }
 * method.
 * @author mchaberski
 */
public class LethalWatchdog {

    private static final Logger log = Logger.getLogger(LethalWatchdog.class.getName());

    private final List<ProcessStartListener> processStartListeners;
    
    public LethalWatchdog() {
        processStartListeners = Collections.synchronizedList(new ArrayList<>());
    }

    public interface ProcessStartListener {
        void started(Process process);
    }

    public void removeProcessStartListener(ProcessStartListener processStartListener) {
        processStartListeners.remove(processStartListener);
    }

    public void addProcessStartListener(ProcessStartListener processStartListener) {
        processStartListeners.add(processStartListener);
    }

    public synchronized final void start(Process process) {
        this.processStartListeners.forEach(listener -> listener.started(process));
    }

}
