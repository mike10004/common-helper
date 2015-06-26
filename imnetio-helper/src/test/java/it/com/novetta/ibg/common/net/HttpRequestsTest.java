/*
 * (c) 2015 Mike Chaberski
 */

package it.com.novetta.ibg.common.net;

import com.novetta.ibg.common.net.HttpRequests;
import com.novetta.ibg.common.net.HttpRequests.HttpRequester;
import com.novetta.ibg.common.net.HttpRequests.ResponseData;
import java.net.HttpURLConnection;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author mchaberski
 */
public class HttpRequestsTest {
    
    @Test
    public void testDownloadSomething() {
        System.out.println("testDownloadSomething");
        HttpRequester requester = HttpRequests.newRequester();
        ResponseData responseData = requester.retrieve("http://example.com");
        System.out.println("HTTP response " + responseData.code);
        System.out.println(new String(responseData.data));
        assertEquals("expect 200 OK", HttpURLConnection.HTTP_OK, responseData.code);
        assertTrue("expect nonzero data length", responseData.data.length > 0);
        assertFalse("expect no error", responseData.exception.isPresent());
        
    }
    
}
