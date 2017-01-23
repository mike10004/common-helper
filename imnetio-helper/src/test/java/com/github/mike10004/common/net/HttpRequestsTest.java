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
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.*;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
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
        assertTrue("exception present", responseData.exception.isPresent());
    }
    
    @Test
    public void ResponseData_getHeaderValues() {
        System.out.println("ResponseData_getHeaderValues");
        ResponseData responseData = new HttpRequests.ResponseData(URI.create("http://example.com"), 200, new byte[0], ImmutableMultimap.of("some-header", "A", "Some-Header", "B", "another-header", "C"));
        Iterable<String> values = responseData.getHeaderValues("some-header");
        assertEquals(ImmutableSet.of("A", "B"), ImmutableSet.copyOf(values));
        assertEquals(0, Iterables.size(responseData.getHeaderValues("not-present")));
    }

    private static byte[] bytes(int...values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = UnsignedBytes.checkedCast(values[i]);
        }
        return b;
    }

    private static TestCase encodeTestCase(Charset charset, byte[] bytes) {
        String expected = new String(bytes, charset);
        String contentType = PLAIN_TEXT_UTF_8.withoutParameters().withCharset(charset).toString();
        return new TestCase(bytes, contentType, expected);
    }

    private static List<String> toHexStrings(byte[] bytes) {
        return Lists.transform(Bytes.asList(bytes), new Function<Byte, String>(){
            @Override
            public String apply(Byte input) {
                return UnsignedBytes.toString(input.byteValue(), 16);
            }
        });
    }

    private static Multimap<String, String> headers(String header1Name, String header1Value, String...others) {
        ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
        List<String> parts = Lists.asList(header1Name, header1Value, others);
        for (int i = 0; i < parts.size(); i += 2) {
            String name = parts.get(i);
            String value = parts.get(i+1);
            if (value != null) {
                b.put(name, value);
            }
        }
        return b.build();
    }

    private static class TestCase {
        public final byte[] bytes;
        public final String contentTypeHeaderValue;
        public final Charset fallbackCharset;
        public final String expected;

        public TestCase(byte[] bytes, String contentTypeHeaderValue, String expected) {
            this(bytes, contentTypeHeaderValue, Charset.defaultCharset(), expected);
        }

        public TestCase(byte[] bytes, MediaType contentTypeHeaderValue, Charset fallbackCharset, String expected) {
            this(bytes, contentTypeHeaderValue.toString(), fallbackCharset, expected);
        }

        public TestCase(byte[] bytes, MediaType contentTypeHeaderValue, String expected) {
            this(bytes, contentTypeHeaderValue.toString(), expected);
        }

        public TestCase(byte[] bytes, String contentTypeHeaderValue, Charset fallbackCharset, String expected) {
            this.bytes = bytes;
            this.contentTypeHeaderValue = contentTypeHeaderValue;
            this.fallbackCharset = fallbackCharset;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "bytes=" + toHexStrings(bytes) +
                    ", contentTypeHeaderValue='" + contentTypeHeaderValue + '\'' +
                    ", fallbackCharset=" + fallbackCharset +
                    ", expected='" + expected + '\'' +
                    '}';
        }
    }

    private static final MediaType PLAIN_TEXT = PLAIN_TEXT_UTF_8.withoutParameters();

    @Test
    public void ResponseData_getDataAsString() throws Exception {
        String EUR = "€";
        String UM = "ä";
        byte[] isoEuroBytes = UM.getBytes(ISO_8859_1);
        checkState(UM.equals(new String(isoEuroBytes, ISO_8859_1)), "symbol %s", UM); // sanity check

        // does not currently include charset mismatch edge cases
        List<TestCase> testCases = Arrays.asList(
                // empty
                new TestCase(new byte[0], (String) null, ""),
                new TestCase(new byte[0], PLAIN_TEXT_UTF_8, ""),
                new TestCase(new byte[0], PLAIN_TEXT_UTF_8.withCharset(ISO_8859_1), ""),
                new TestCase(new byte[0], "", ""),
                new TestCase(new byte[0], "", ""),
                new TestCase(new byte[0], "", ""),

                // euro symbol
                new TestCase(bytes(0xE2, 0x82, 0xAC), (String) null, UTF_8, EUR),
                new TestCase(bytes(0xE2, 0x82, 0xAC), PLAIN_TEXT.withCharset(UTF_8), ISO_8859_1, EUR),
                encodeTestCase(UTF_8, bytes(0xE2, 0x82, 0xAC)),

                // umlaut with different content-type and default values
                new TestCase(bytes(0xe4), PLAIN_TEXT.withCharset(ISO_8859_1), UM),
                new TestCase(bytes(0xC3, 0xA4), PLAIN_TEXT_UTF_8, UM),
                new TestCase(bytes(0xe4), PLAIN_TEXT, ISO_8859_1, UM),
                new TestCase(bytes(0xC3, 0xA4), PLAIN_TEXT, UTF_8, UM),
                new TestCase(bytes(0xe4), (String) null, ISO_8859_1, UM),
                new TestCase(bytes(0xC3, 0xA4), (String) null, UTF_8, UM),
                new TestCase(bytes(0xe4), "", ISO_8859_1, UM),
                new TestCase(bytes(0xC3, 0xA4), "", UTF_8, UM),

                // invalid charset value "UFT-8" (typo of UTF-8)
                new TestCase(bytes(0xC3, 0xA4), "text/html; charset=UFT-8", UTF_8, UM),
                new TestCase(bytes(0xE4), "text/html; charset=UFT-8", ISO_8859_1, UM)
        );
        URI url = URI.create("http://localhost:12345/hello.txt");
        for (TestCase testCase : testCases) {
            byte[] bytes = testCase.bytes;
            String contentType = testCase.contentTypeHeaderValue;
            String expected = testCase.expected;
            System.out.format("%s with content-type: %s -> \"%s\"%n", toHexStrings(bytes), contentType, expected);
            ResponseData responseData = new ResponseData(url, HttpStatus.SC_OK, bytes, headers(HttpHeaders.CONTENT_TYPE, contentType));
            String actual = responseData.getDataAsString(testCase.fallbackCharset);
            assertEquals(testCase.toString(), expected, actual);
        }
    }
}
