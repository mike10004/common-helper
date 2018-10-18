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

    @SuppressWarnings("unchecked") // EasyMock.anyObject
    @Test
    public void HttpRequester_hostResolutionFailure() throws Exception {
        System.out.println("HttpRequester_hostResolutionFailure");
        final HttpClient client = EasyMock.createMock(HttpClient.class);
        EasyMock.expect(client.execute(EasyMock.anyObject(HttpUriRequest.class), EasyMock.anyObject(ResponseHandler.class))).andThrow(new UnknownHostException()).anyTimes();
        HttpRequests.HttpRequester unknownHostThrowingRequester = new HttpRequests.DefaultHttpRequester(Functions.constant(client), new HttpRequests.DefaultHttpRequester.HttpGetRequestFactory());
        EasyMock.replay(client);
        
        ResponseData responseData = unknownHostThrowingRequester.retrieve(URI.create("http://localhost:12522/some/file.txt"));
        System.out.println("response: " + responseData);
        assertEquals("responseData.data", 0, responseData.data.length);
        assertTrue("exception present", responseData.exception != null);
    }
    
}
