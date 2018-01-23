/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class that represents a watchdog providing a method to kill the process
 * it is watching. That method performs the same actions as the superclass's
 * {@code ExecuteWatchdog#timeoutOccured(org.apache.tools.ant.util.Watchdog) }
 * method.
 * @author mchaberski
 * @deprecated use {@link com.github.mike10004.nativehelper.subprocess.Subprocess} API instead
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
