package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.test.Tests.ProcessToBeKilled;
import com.github.mike10004.nativehelper.test.Tests.ProcessToBeKilled.PidFailureReaction;
import com.github.mike10004.nativehelper.Program.TaskStage;
import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class) // we repeat this because race conditions are common
public class ProgramKillTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final long KILL_TIMEOUT = 5000L;

    private static final int NUM_TRIALS = 10;

    @Parameterized.Parameters
    public static List<Object[]> params() {
        return Arrays.asList(new Object[NUM_TRIALS][0]);
    }

    @Test(timeout = KILL_TIMEOUT)
    public void deprecatedCancel_awaitCalled() throws Exception {
        testDeprecatedCancel(TaskStage.CALLED, false, PidFailureReaction.IGNORE);
    }

    @Test(timeout = KILL_TIMEOUT)
    public void deprecatedCancel_awaitExecuted() throws Exception {
        testDeprecatedCancel(TaskStage.EXECUTED, true, PidFailureReaction.RAISE);
    }

    private void testDeprecatedCancel(TaskStage stageToAwait, boolean waitForPidfile, PidFailureReaction pidFailureReaction) throws Exception {
        File pidFile = tmp.newFile();
        Program<ProgramWithOutputStringsResult> program = Program.running("python")
                .arg(Tests.getPythonFile("nht_signal_listener.py").getAbsolutePath())
                .args("--pidfile", pidFile.getAbsolutePath())
                .outputToStrings();
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        boolean clean = false;
        try (ProcessToBeKilled ignore1 = new ProcessToBeKilled(pidFile, pidFailureReaction)){
            ProgramFuture<ProgramWithOutputStringsResult> future = program.executeAsync(executorService);
            future.awaitStage(stageToAwait);
            if (waitForPidfile) {
                System.out.format("pidfile contents: %s%n", Tests.readWhenNonempty(pidFile).trim());
            }
            @SuppressWarnings("deprecation")
            boolean cancelResult = future.cancel(true);
            System.out.format("cancelResult: %s%n", cancelResult);
            assertTrue("isDone()", future.isDone());
            try {
                ProgramWithOutputStringsResult result = future.get();
                System.out.println(result);
                Assert.assertTrue("should not be able to get result from cancelled future, " +
                        "but it's possible for the Process.destroy call to kill the program and " +
                        "thus allow the submitted Callable to complete 'naturally'", false);
            } catch (CancellationException ignore2) {
            }
            clean = true;
        } finally {
            List<?> remaining = executorService.shutdownNow();
            if (clean) {
                assertEquals("num remaining", 0, remaining.size());
            }
        }
    }

}
