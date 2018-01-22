package com.github.mike10004.nativehelper.subprocess;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

/**
 * Base class for subprocess tests. This superclass provides a {@link ProcessTracker} field
 * for test classes to use, and implements a check after each test that confirms that there
 * are no active processes remaining.
 */
public abstract class SubprocessTestBase {

    protected static final ProcessTracker CONTEXT = ProcessTracker.create();

    private volatile boolean testFailed;

    @Rule
    public final TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            testFailed = true;
        }
    };

    @After
    public final void checkProcesses() {
        int active = CONTEXT.activeCount();
        if (testFailed) {
            if (active > 0) {
                System.err.format("%d active processes; ignoring because test failed%n", active);
            }
        } else {
            assertEquals(active + " processes are still active but should have finished or been killed", 0, active);
        }
    }

    protected static <T> Supplier<T> nullSupplier() {
        return () -> null;
    }

}
