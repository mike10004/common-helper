package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.KillAttempt;
import com.github.mike10004.nativehelper.subprocess.DestroyAttempt.TermAttempt;
import com.github.mike10004.nativehelper.test.Tests;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class SubprocessKillTest extends SubprocessTestBase {

    private static File pySignalFile() {
        return Tests.getPythonFile("nht_signal_listener.py");
    }

    private static Subprocess signalProgram(boolean swallowSigterm, File pidFile) {
        Subprocess.Builder builder = Subprocess.running(pySignalFile());
        builder.args("--pidfile", pidFile.getAbsolutePath());
        if (swallowSigterm) {
            builder.arg("--swallow-sigterm");
        }
        return builder.build();
    }

    private static final long STD_TIMEOUT = 3000L;

    @Test(timeout = STD_TIMEOUT)
    public void destroyWithSigKill() throws Exception {
        File pidFile = File.createTempFile("SubprocessKillTest", ".pid");
        ProcessMonitor<?, ?> monitor = signalProgram(true, pidFile)
                .launcher(CONTEXT)
                .inheritOutputStreams()
                .launch();
        System.out.println("waiting for pid to be printed...");
        String pid = Tests.readWhenNonempty(pidFile) ;// new String(ByteStreams.toByteArray(stderrSink.connect()), StandardCharsets.US_ASCII);
        System.out.format("pid printed: %s%n", pid);
        TermAttempt termAttempt = monitor.destructor().sendTermSignal();
        assertEquals("term attempt result", DestroyResult.STILL_ALIVE, termAttempt.result());
        KillAttempt attempt = termAttempt.kill();
        attempt.awaitKill();
        int exitCode = monitor.await().exitCode();
        if (Platforms.getPlatform().isLinux()) {
            assertEquals("exit code", EXPECTED_SIGKILL_EXIT_CODE, exitCode);
        }
    }

    @Test(timeout = STD_TIMEOUT)
    public void destroyWithSigTerm() throws Exception {
        File pidFile = File.createTempFile("SubprocessKillTest", ".pid");
        ProcessMonitor<?, ?> monitor = signalProgram(false, pidFile)
                .launcher(CONTEXT)
                .inheritOutputStreams()
                .launch();
        System.out.println("waiting for pid to be printed...");
        String pid = Tests.readWhenNonempty(pidFile) ;// new String(ByteStreams.toByteArray(stderrSink.connect()), StandardCharsets.US_ASCII);
        System.out.format("pid printed: %s%n", pid);
        TermAttempt termAttempt = monitor.destructor().sendTermSignal().await();
        assertEquals("term attempt result", DestroyResult.TERMINATED, termAttempt.result());
        int exitCode = monitor.await().exitCode();
        if (Platforms.getPlatform().isLinux()) {
            assertEquals("exit code", EXPECTED_SIGTERM_EXIT_CODE, exitCode);
        }
    }

    private static final int EXPECTED_SIGTERM_EXIT_CODE = 128 + 15;
    private static final int EXPECTED_SIGKILL_EXIT_CODE = 128 + 9;

}
