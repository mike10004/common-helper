/*
 * (c) 2015 Mike Chaberski
 */

package com.novetta.ibg.common.net;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.novetta.ibg.common.net.HttpRequests;
import com.novetta.ibg.common.net.HttpRequests.HttpRequester;
import com.novetta.ibg.common.net.HttpRequests.ResponseData;
import java.net.HttpURLConnection;
import java.net.URI;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.IOException;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import java.nio.charset.Charset;

/**
 *
 * @author mchaberski
 */
public class HttpRequestsIT {
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(getWireMockPort());
    
    @Test
    public void testDownloadHtmlFile() {
        System.out.println("testDownloadHtmlFile");
        HttpRequester requester = HttpRequests.newRequester();
        Charset responseCharset = Charsets.UTF_8;
        byte[] responseBody = "<html><body>Hello</body></html>".getBytes(responseCharset);
        expectAndVerify("/index.html", responseBody, MediaType.HTML_UTF_8.withCharset(responseCharset), HTTP_OK, requester);
    }
    
    @Test
    public void testDownloadImageFile() throws IOException {
        System.out.println("testDownloadImageFile");
        HttpRequester requester = HttpRequests.newRequester();
        byte[] responseBody = Resources.toByteArray(getClass().getResource("/images/logo.png"));
        expectAndVerify("/image.png", responseBody, MediaType.PNG, HTTP_OK, requester);
    }

    private ResponseData expectAndVerify(String urlPath, byte[] responseBody, MediaType contentType, int httpStatus, HttpRequester requester) {
        checkArgument(urlPath.startsWith("/"));
        stubFor(get(urlEqualTo(urlPath))
                .willReturn(aResponse().withStatus(httpStatus)
                        .withHeader(HttpHeaders.CONTENT_TYPE, contentType.toString())
                        .withBody(responseBody)));
        ResponseData responseData = requester.retrieve(URI.create("http://localhost:" + wireMockRule.port() + urlPath));
        System.out.format("HTTP status %d; content-type = %s%n", responseData.code, responseData.getHeaderValues(HttpHeaders.CONTENT_TYPE));
        System.out.println("response body length = " + responseData.data.length);
        assertEquals(httpStatus, responseData.code);
        assertEquals("response data length", responseBody.length, responseData.data.length);
        assertEquals(ImmutableList.of(contentType.toString()), ImmutableList.copyOf(responseData.getHeaderValues(HttpHeaders.CONTENT_TYPE)));
        assertFalse("expect no error", responseData.exception.isPresent());
        return responseData;
    }
    
    @Test
    public void testNotFound() {
        System.out.println("testNotFound");
        HttpRequester requester = HttpRequests.newRequester();
        expectAndVerify("/resource/not/found", "<html><body><h1>404 Not Found</h1></body></html>".getBytes(Charsets.UTF_8), MediaType.HTML_UTF_8, HTTP_NOT_FOUND, requester);
    }

    private static int getWireMockPort() {
        String portStr = System.getProperty("wiremock.port");
        if (portStr == null) {
            throw new IllegalStateException("system property wiremock.port is not defined; the build-helper-maven-plugin must execute before this test in order to reserve a network port");
        }
        return Integer.parseInt(portStr);
    }
}
