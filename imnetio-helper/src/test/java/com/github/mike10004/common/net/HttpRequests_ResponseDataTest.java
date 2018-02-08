package com.github.mike10004.common.net;

import com.github.mike10004.common.net.HttpRequests.ResponseData;
import com.google.common.collect.*;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HttpRequests_ResponseDataTest {

    @Test
    public void getHeaderValues() throws Exception {
        System.out.println("getHeaderValues");
        ResponseData responseData = ResponseData.builder(URI.create("http://example.com"), SC_OK)
                .header("some-header", "A")
                .header("Some-Header", "B")
                .header("another-header", "C")
                .build();
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
        return Bytes.asList(bytes).stream()
                .map(input -> UnsignedBytes.toString(input.byteValue(), 16))
                .map(code -> String.format("0x%s", code))
                .collect(Collectors.toList());
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
    public void getDataAsString() throws Exception {
        System.out.println("getDataAsString");
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
            ResponseData responseData = new ResponseData(url, SC_OK, bytes, headers(HttpHeaders.CONTENT_TYPE, contentType));
            String actual = responseData.getDataAsString(testCase.fallbackCharset);
            assertEquals(testCase.toString(), expected, actual);
        }
    }

    @Test
    public void getFirstHeaderValue_absent() throws Exception {
        ResponseData response = ResponseData.builder(URI.create("https://example.com/"), 200)
                .build();
        assertFalse("content-length value", response.getFirstHeaderValue("content-length").isPresent());
    }

    @Test
    public void getFirstHeaderValue_caseInsensitive() throws Exception {
        ResponseData response = ResponseData.builder(URI.create("https://example.com/"), 200)
                .header("content-type", "text/plain")
                .data("hello, world".getBytes(StandardCharsets.US_ASCII))
                .build();
        String contentType = response.getFirstHeaderValue("Content-Type").orElse(null);
        assertEquals("content type header value", "text/plain", contentType);
        String content = response.getDataAsString(StandardCharsets.US_ASCII);
        assertEquals("content", "hello, world", content);
    }
}
