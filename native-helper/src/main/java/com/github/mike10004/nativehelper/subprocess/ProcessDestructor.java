package com.github.mike10004.nativehelper.subprocess;

/**
 * Service class used to destroy a single process.
 */
public interface ProcessDestructor {

    DestroyAttempt.TermAttempt sendTermSignal();
    DestroyAttempt.KillAttempt sendKillSignal();

}
