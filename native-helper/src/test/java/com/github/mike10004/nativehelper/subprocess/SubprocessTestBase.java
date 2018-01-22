package com.github.mike10004.nativehelper.subprocess;

import org.junit.After;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public abstract class SubprocessTestBase {

    protected static final ProcessContext CONTEXT = ProcessContext.create();

    @After
    public final void checkProcesses() {
        int active = CONTEXT.activeCount();
        assertEquals("num active", 0, active);
    }

    protected static <T> Supplier<T> nullSupplier() {
        return () -> null;
    }

}