package com.github.mike10004.nativehelper.subprocess;

import org.junit.After;

import static org.junit.Assert.assertEquals;

public class SubprocessTestBase {

    protected static final ProcessContext CONTEXT = ProcessContext.create();

    @After
    public final void checkProcesses() {
        int active = CONTEXT.activeCount();
        assertEquals("num active", 0, active);
    }

}
