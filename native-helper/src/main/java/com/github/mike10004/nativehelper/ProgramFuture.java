package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.Program.TaskStage;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Future that represents aynchronous execution of a program.
 * @param <T>
 */
public class ProgramFuture<T> extends SimpleForwardingListenableFuture<T> {

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
     * Aborts the task and cancels the future. Invokes {@link ExposedExecTask#abort()}
     * before cancelling the future. Task is aborted regardless of the {@code mayInterruptIfRunning}
     * parameter value.
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        execTask.abort();
        return super.cancel(mayInterruptIfRunning);
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
