/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.novetta.ibg.common.net;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.novetta.ibg.common.net.HttpRequests.ResponseData;
import java.net.URI;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author mchaberski
 */
public class ResponseDataTest {
    
    @Test
    public void testGetHeaderValues() {
        System.out.println("testGetHeaderValues");
        
        ResponseData responseData = new HttpRequests.ResponseData(URI.create("http://example.com"), 200, new byte[0], ImmutableMultimap.of("some-header", "A", "Some-Header", "B", "another-header", "C"));
        Iterable<String> values = responseData.getHeaderValues("some-header");
        assertEquals(ImmutableSet.of("A", "B"), ImmutableSet.copyOf(values));
        assertEquals(0, Iterables.size(responseData.getHeaderValues("not-present")));
    }
    
}
