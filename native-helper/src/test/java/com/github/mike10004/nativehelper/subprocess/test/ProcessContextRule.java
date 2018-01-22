package com.github.mike10004.nativehelper.subprocess.test;

import com.github.mike10004.nativehelper.subprocess.ProcessContext;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

public class ProcessContextRule extends ExternalResource {

    private final TestWatcher watcher;
    private final AtomicBoolean passage;
    private ProcessContext processContext;

    public ProcessContextRule() {
        passage = new AtomicBoolean(false);
        watcher = new TestWatcher() {
            @Override
            protected void succeeded(Description description) {
                passage.set(true);
            }
        };
    }

    @Override
    public Statement apply(Statement base, Description description) {
        watcher.apply(base, description);
        return super.apply(base, description);
    }

    @Override
    protected void before() {
        processContext = ProcessContext.create();
    }

    @Override
    protected void after() {
        ProcessContext processContext = this.processContext;
        if (processContext != null) {
            boolean testPassed = passage.get();
            if (testPassed) {
                if (processContext.activeCount() > 0) {
                    System.err.format("%d active processes in context%n", processContext.activeCount());
                }
            } else {
                assertEquals("number of active processes in context must be zero", 0, processContext.activeCount());
            }
        }
    }

    public ProcessContext getContext() {
        return processContext;
    }
}
