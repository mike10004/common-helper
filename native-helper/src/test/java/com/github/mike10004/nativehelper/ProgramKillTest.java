package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.ProcessUtils.ProcessToBeKilled;
import com.github.mike10004.nativehelper.ProcessUtils.ProcessToBeKilled.PidFailureReaction;
import com.github.mike10004.nativehelper.Program.TaskStage;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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

    private static final int NUM_TRIALS = 5;

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
                .arg(ProcessUtils.pythonScript_mustBeKilled().getAbsolutePath())
                .args("--pidfile", pidFile.getAbsolutePath())
                .outputToStrings();
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        boolean clean = false;
        try (ProcessToBeKilled ignore1 = new ProcessToBeKilled(pidFile, pidFailureReaction)){
            ProgramFuture<ProgramWithOutputStringsResult> future = program.executeAsync(executorService);
            future.awaitStage(stageToAwait);
            if (waitForPidfile) {
                System.out.format("pidfile contents: %s%n", ProcessUtils.readWhenNonempty(pidFile).trim());
            }
            @SuppressWarnings("deprecation")
            boolean cancelResult = future.cancel(true);
            System.out.format("cancelResult: %s%n", cancelResult);
            assertTrue("isDone()", future.isDone());
            try {
                ProgramWithOutputStringsResult result = future.get();
                System.out.println(result);
                fail("should not be able to get result from cancelled future");
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

    enum TestProcessState {
        WAS_RUNNING,
        WAS_NOT_RUNNING
    }

    //    static List<byte[]> split(byte[] bytes, Predicate<? super Byte> split) {
//        List<byte[]> arrays = new ArrayList<>();
//        if (bytes.length == 0) {
//            arrays.add(bytes);
//            return arrays;
//        }
//        int i = 0;
//        do {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
//            if (split.test(bytes[i])) {
//                arrays.add(baos.toByteArray());
//                baos.reset();
//            }
//            baos.write(bytes[i]);
//            i++;
//            if (i == bytes.length) {
//                arrays.add(baos.toByteArray());
//            }
//        } while(i < bytes.length);
//    }

}
