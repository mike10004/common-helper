/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.nativehelper;

import org.apache.tools.ant.taskdefs.ExecuteWatchdog;

import java.util.logging.Logger;

public class NotifyingWatchdog extends ExecuteWatchdog {

    private transient ProcessListener processListener;

    public NotifyingWatchdog(long timeout) {
        super(timeout);
    }

    public interface ProcessListener {
        void started(Process process);
    }

    @Override
    public synchronized void start(Process process) {
        super.start(process);
        ProcessListener processListener_ = processListener;
        if (processListener_ != null) {
            processListener_.started(process);
        } else {
            Logger.getLogger(getClass().getName()).info("process listener not set");
        }
    }

    void setProcessListener(ProcessListener processListener) {
        this.processListener = processListener;
    }
}
