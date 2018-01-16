package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.LethalWatchdog.DestroyStatus;
import com.github.mike10004.nativehelper.Program.TaskStage;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Future that represents aynchronous execution of a program.
 * @param <T>
 */
public class ProgramFuture<T> extends SimpleForwardingListenableFuture<T> {

    private static final Logger log = Logger.getLogger(ProgramFuture.class.getName());

    private static final ImmutableSet<TaskStage> ALL_TASK_STAGES = ImmutableSet.copyOf(TaskStage.values());

    private final ExposedExecTask execTask;
    private final ListenableFuture<T> wrappedFuture;
    private final CountdownLatchSet<TaskStage> countdownLatchSet;

    ProgramFuture(ExposedExecTask execTask, ListenableFuture<T> wrappedFuture, CountdownLatchSet<TaskStage> countdownLatchSet) {
        super(wrappedFuture);
        this.execTask = requireNonNull(execTask);
        this.wrappedFuture = requireNonNull(wrappedFuture);
        this.countdownLatchSet = requireNonNull(countdownLatchSet);
        checkArgument(this.countdownLatchSet.keySet().equals(ALL_TASK_STAGES), "countdown latch match must contain a latch for all task stages");
    }

    /**
     * Cancels the future, aborting the execution of the program. The results of this
     * method are somewhat racy and unpredictable due to (a) the timing of process
     * destruction and future task completion and (b) ambiguity in what it means for
     * an asynchronous task to have completed when the process it was waiting on
     * terminated due to a SIGTERM or SIGKILL. Also, using this method with
     * {@code mayInterrupteIfRunning = true} could have unfortunate side effects
     * because only the process exit waiting thread will be interrupted, even if
     * the process itself could not be terminated.
     * @see java.util.concurrent.Future#cancel(boolean)
     * @deprecated prefer {@link #cancelAndKill(long, TimeUnit)}, {@link #cancelWithoutInterrupting()},
     * or {@link #killProcess(long, TimeUnit)}, depending on what you really want to do
     */
    @Override
    @Deprecated
    public boolean cancel(boolean mayInterruptIfRunning) {
        execTask.abort();
        return super.cancel(mayInterruptIfRunning);
    }

    public boolean cancelWithoutInterrupting() {
        return super.cancel(false);
    }

    /**
     * Cancels the future task and kills the process if it has been executed.
     * This currently calls {@link #cancelWithoutInterrupting()} and then
     * {@link #killProcess(long, TimeUnit)}.
     * @param timeout time to wait for kill
     * @param unit unit of killing wait time
     * @return the kill result
     */
    public KillResult cancelAndKill(long timeout, TimeUnit unit) {
        cancelWithoutInterrupting();
        return killProcess(timeout, unit);
    }

    public enum KillAction {
        PERFORMED, NOT_PERFORMED
    }

    /**
     * Class that represents the result of a process kill request.
     * @see #killProcess(long, TimeUnit)
     */
    public static class KillResult {

        /**
         * The kill action.
         */
        public final KillAction action;

        /**
         * Result of destroy call. Null if kill was not performed.
         */
        public final DestroyStatus status;

        private KillResult(KillAction action, DestroyStatus status) {
            this.action = action;
            this.status = status;
            checkArgument(action != KillAction.PERFORMED || status != null, "if KillAction was performed, DestroyStatus must be non-null");
        }

        public Optional<DestroyStatus> status() {
            return Optional.ofNullable(status);
        }
    }

    /**
     * Kills the process if it has been executed. Does not cancel task submission. Therefore,
     * you probably don't want to use this unless you are <i>sure</i> that the process has
     * been executed, meaning you have already called
     * {@link #awaitStage(TaskStage) awaitStage(TaskStage.EXECUTED)}. This will have no effect
     * if the task has not reached that stage, and it may still execute in the future.
     * You're probably better off calling {@link #cancelAndKill(long, TimeUnit)}.
     * @param timeout time to wait for kill
     * @param unit unit of killing wait time
     * @return the kill result
     * @see #cancelWithoutInterrupting()
     * @see #cancelAndKill(long, TimeUnit)
     * @see java.util.concurrent.ExecutorService#submit(Callable)
     */
    public KillResult killProcess(long timeout, TimeUnit unit) {
        if (getStage() == TaskStage.EXECUTED) {
            DestroyStatus status = LethalWatchdog.destroy(execTask.getProcess(), timeout, unit);
            checkState(status != null, "status null even though process known to be started");
            return new KillResult(KillAction.PERFORMED, status);
        } else {
            return new KillResult(KillAction.NOT_PERFORMED, null);
        }
    }

    public TaskStage getStage() {
        return countdownLatchSet.current();
    }

    public void awaitStage(TaskStage stage) throws InterruptedException {
        requireNonNull(stage, "stage");
        CountDownLatch latch = countdownLatchSet.getLatch(stage);
        checkState(latch != null, "no latch for stage %s", stage);
        latch.await();
    }

    public void awaitStage(TaskStage stage, long timeout, TimeUnit unit) throws InterruptedException {
        requireNonNull(stage, "stage");
        CountDownLatch latch = countdownLatchSet.getLatch(stage);
        checkState(latch != null, "no latch for stage %s", stage);
        latch.await(timeout, unit);
    }

    @Override
    public String toString() {
        return "ProgramFuture{wrapped=" + wrappedFuture +
                ", stage=" + countdownLatchSet.current() +
                ", task=" + execTask +
                "}";
    }

    static class CountdownLatchSet<T> {

        private final AtomicReference<T> reference;
        private final ImmutableMap<T, CountDownLatch> latches;

        public CountdownLatchSet(Set<T> keys, int count) {
            latches = ImmutableMap.copyOf(Maps.asMap(keys, key -> new CountDownLatch(count)));
            reference = new AtomicReference<>();
        }

        public T current() {
            return reference.get();
        }

        public ImmutableSet<T> keySet() {
            return latches.keySet();
        }

        @SuppressWarnings("UnusedReturnValue")
        @Nullable
        public T tick(T key) {
            T previous = reference.getAndSet(key);
            CountDownLatch latch = latches.get(key);
            checkArgument(latch != null, "not in the latch set: %s", key);
            latch.countDown();
            return previous;
        }

        @Nullable
        public CountDownLatch getLatch(T key) {
            return latches.get(key);
        }

    }
}
