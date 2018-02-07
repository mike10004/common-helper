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
package com.github.mike10004.common.net;

import com.github.mike10004.common.net.HttpRequests.ResponseData;
import com.google.common.base.Functions;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpRequestsTest {
    
    @Test
    public void HttpRequester_hostResolutionFailure() throws Exception {
        System.out.println("HttpRequester_hostResolutionFailure");
        final HttpClient client = EasyMock.createMock(HttpClient.class);
        EasyMock.expect(client.execute(EasyMock.anyObject(HttpUriRequest.class), EasyMock.anyObject(ResponseHandler.class))).andThrow(new UnknownHostException()).anyTimes();
        HttpRequests.HttpRequester unknownHostThrowingRequester = new HttpRequests.DefaultHttpRequester(Functions.<HttpClient>constant(client), new HttpRequests.DefaultHttpRequester.HttpGetRequestFactory());
        EasyMock.replay(client);
        
        ResponseData responseData = unknownHostThrowingRequester.retrieve(URI.create("http://localhost:12522/some/file.txt"));
        System.out.println("response: " + responseData);
        assertEquals("responseData.data", 0, responseData.data.length);
        assertTrue("exception present", responseData.exception != null);
    }
    
}
