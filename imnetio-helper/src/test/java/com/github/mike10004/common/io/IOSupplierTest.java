package com.github.mike10004.common.io;

import org.junit.Assert;
import org.junit.Test;

public class IOSupplierTest {

    @Test
    public void memoize() throws Exception {
        IOSupplier<Object> s = IOSupplier.memoize(Object::new);
        Object first = s.get();
        Object second = s.get();
        Assert.assertSame(first, second);
    }

}