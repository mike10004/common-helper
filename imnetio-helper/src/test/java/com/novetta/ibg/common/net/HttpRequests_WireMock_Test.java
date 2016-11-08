/*
 * (c) 2015 Mike Chaberski
 */

package com.novetta.ibg.common.net;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.novetta.ibg.common.net.HttpRequests.HttpRequester;
import com.novetta.ibg.common.net.HttpRequests.ResponseData;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author mchaberski
 */
public class HttpRequests_WireMock_Test {
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    
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

    private static final ImmutableMultimap<String, String> noHeaders = ImmutableMultimap.of();

    private ResponseData expectAndVerify(String urlPath, byte[] responseBody, MediaType contentType, int httpStatus, HttpRequester requester) {
        return expectAndVerify(urlPath, noHeaders, responseBody, contentType, httpStatus, requester);
    }

    private ResponseData expectAndVerify(String urlPath, Multimap<String, String> requestHeaders, byte[] responseBody, MediaType contentType, int httpStatus, HttpRequester requester) {
        return expectAndVerify(urlPath, requestHeaders, responseBody, ImmutableMultimap.of(HttpHeaders.CONTENT_TYPE, contentType.toString()), httpStatus, requester);
    }

    private ResponseData expectAndVerify(String urlPath, Multimap<String, String> requestHeaders, byte[] responseBody, Multimap<String, String> responseHeaders, int httpStatus, HttpRequester requester) {
        checkArgument(urlPath.startsWith("/"));
        MappingBuilder mb = get(urlEqualTo(urlPath));
        for (Map.Entry<String, String> header : requestHeaders.entries()) {
            mb.withHeader(header.getKey(), equalTo(header.getValue()));
        }
        ResponseDefinitionBuilder rdb = aResponse().withStatus(httpStatus).withBody(responseBody);
        for (Map.Entry<String, String> header : responseHeaders.entries()) {
            rdb.withHeader(header.getKey(), header.getValue());
        }
        stubFor(mb.willReturn(rdb));
        URI uri = URI.create("http://localhost:" + wireMockRule.port() + urlPath);
        ResponseData responseData = requester.retrieve(uri, requestHeaders);
        System.out.format("HTTP status %d; content-type = %s%n", responseData.code, responseData.getHeaderValues(HttpHeaders.CONTENT_TYPE));
        System.out.println("response body length = " + responseData.data.length);
        assertEquals(httpStatus, responseData.code);
        assertEquals("response data length", responseBody.length, responseData.data.length);
        assertEquals(ImmutableList.copyOf(responseHeaders.get(HttpHeaders.CONTENT_TYPE)), ImmutableList.copyOf(responseData.getHeaderValues(HttpHeaders.CONTENT_TYPE)));
        assertFalse("expect no error", responseData.exception.isPresent());
        return responseData;
    }
    
    @Test
    public void testNotFound() {
        System.out.println("testNotFound");
        HttpRequester requester = HttpRequests.newRequester();
        expectAndVerify("/resource/not/found", "<html><body><h1>404 Not Found</h1></body></html>".getBytes(Charsets.UTF_8), MediaType.HTML_UTF_8, HTTP_NOT_FOUND, requester);
    }

    @Test
    public void sendWithHeaders() throws Exception {
        HttpRequester requester = HttpRequests.newRequester();
        expectAndVerify("/some/path", ImmutableMultimap.of("X-Custom-Header", "foo"), "bar".getBytes(Charsets.UTF_8), MediaType.PLAIN_TEXT_UTF_8, HTTP_OK, requester);
    }
}
