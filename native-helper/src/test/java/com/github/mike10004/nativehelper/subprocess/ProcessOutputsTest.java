package com.github.mike10004.nativehelper.subprocess;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessOutputsTest {

    @Test
    public void testToString() {
        ProcessOutput<?, ?> p = ProcessOutput.direct(new byte[3], new byte[7]);
        assertEquals("toString()", "DirectOutput{stdout=byte[3], stderr=byte[7]}", p.toString());
    }
}