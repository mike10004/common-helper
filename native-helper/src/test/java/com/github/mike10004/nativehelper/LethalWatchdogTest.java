package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.ProcessUtils.ProcessToBeKilled;
import com.github.mike10004.nativehelper.Processes.DestroyStatus;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class LethalWatchdogTest {

    private static final long DEFAULT_TIMEOUT = 5000L;

    @Test(timeout = DEFAULT_TIMEOUT)
    public void destroy_termable() throws Exception {
        testDestroy(DestroyStatus.TERMINATED);
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void destroy_killRequired() throws Exception {
        testDestroy(DestroyStatus.TERMINATED_FORCIBLY, "--swallow-sigterm");
    }

    private void testDestroy(DestroyStatus expected, String...extraArgs) throws Exception {
        File pidFile = File.createTempFile("LethalWatchdogTest", ".pid");
        File scriptFile = ProcessUtils.pythonScript_mustBeKilled();
        List<String> cmd = new ArrayList<>(Arrays.asList(
                "python",
                scriptFile.getAbsolutePath(),
                "--quiet",
                "--pidfile", pidFile.getAbsolutePath()
        ));
        cmd.addAll(Arrays.asList(extraArgs));
        Process process = new ProcessBuilder()
                .command(cmd)
                .inheritIO()
                .start();
        try (ProcessToBeKilled ignored = new ProcessToBeKilled(pidFile)) {
            String pidFileContents = ProcessUtils.readWhenNonempty(pidFile);
            System.out.format("pidfile contents %s%n", pidFileContents.trim());
            DestroyStatus status = Processes.destroy(process, 500, TimeUnit.MILLISECONDS);
            assertEquals("status", expected, status);
        }
        int exitcode = process.exitValue();
        System.out.format("%d was exit value from %s%n", exitcode, cmd);
        @Nullable Integer expectedExitCode = getExpectedExitCode(expected);
        if (expectedExitCode != null) {
            assertEquals("exit code", expectedExitCode.intValue(), exitcode);
        }
    }

    @Nullable
    private static Integer getExpectedExitCode(DestroyStatus status) {
        switch (status) {
            case TERMINATED: return 128 + 15;
            case TERMINATED_FORCIBLY: return 128 + 9;
            default: return null;
        }
    }
}