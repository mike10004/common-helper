package com.github.mike10004.nativehelper.subprocess;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

class ShutdownHookProcessContext implements ProcessContext {

    private final ProcessDestroyer destroyer = new ProcessDestroyer();

    private final AtomicInteger allCount = new AtomicInteger();
    private final AtomicInteger activeCount = new AtomicInteger();

    @Override
    public void add(Process process) {
        boolean added = destroyer.add(process);
        checkState(added, "add failed");
        allCount.incrementAndGet();
        activeCount.incrementAndGet();
    }

    @Override
    public boolean remove(Process process) {
        boolean removed = destroyer.remove(process);
        if (removed) {
            activeCount.decrementAndGet();
        }
        return removed;
    }

    @Override
    public int count() {
        return allCount.get();
    }

    @Override
    public int activeCount() {
        return activeCount.get();
    }
}
