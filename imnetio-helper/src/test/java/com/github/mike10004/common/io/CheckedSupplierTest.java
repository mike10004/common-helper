package com.github.mike10004.common.io;

import org.junit.Assert;
import org.junit.Test;

public class CheckedSupplierTest {

    @Test
    public void memoize() throws Exception {
        CheckedSupplier<Object, Exception> s = CheckedSupplier.memoize(Object::new);
        Object first = s.get();
        Object second = s.get();
        Assert.assertSame(first, second);
    }
}