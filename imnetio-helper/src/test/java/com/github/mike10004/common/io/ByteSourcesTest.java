package com.github.mike10004.common.io;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static com.github.mike10004.common.io.ByteSources.broken;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ByteSourcesTest {
    
    private static byte[] toByteArray(int...bytes) {
        byte[] byteBytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i];
            byte valueAsByte = (byte) value;
            if (value != valueAsByte) {
                throw new IllegalArgumentException("bytes[" + i + "] = " + value + " overflows");
            }
            byteBytes[i] = valueAsByte;
        }
        return byteBytes;
    }
    
    private static ByteSource wrap(int...bytes) {
        return wrap(toByteArray(bytes));
    }
    
    private static ByteSource wrap(byte...bytes) {
        return ByteSource.wrap(bytes);
    }
    
    /**
     * Test of concatOpenable method, of class ByteSources.
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void testConcatOpenable() throws Exception {
        System.out.println("concatOpenable");
        Map<Iterable<ByteSource>, byte[]> testCases = ImmutableMap.<Iterable<ByteSource>, byte[]>builder()
                .put(Arrays.asList(), new byte[0])
                .put(asList(wrap(1)), toByteArray(1))
                .put(asList(broken()), new byte[0])
                .put(asList(broken(), broken()), new byte[0])
                .put(asList(broken(), wrap(1)), toByteArray(1))
                .put(asList(wrap(1), broken(), wrap(2), broken(), wrap(3), broken()), toByteArray(1, 2, 3))
                .build();
        
        for (Iterable<ByteSource> input : testCases.keySet()) {
            byte[] expectedResult = testCases.get(input);
            ByteSource actualResultSource = ByteSources.concatOpenable(input);
            byte[] actualResultBytes = actualResultSource.read();
            assertArrayEquals(expectedResult, actualResultBytes);
        }
    }

    @Test
    public void concatOpenable_varargs() throws Exception {
        Charset charset = StandardCharsets.UTF_8;
        ByteSource a = CharSource.wrap("foo").asByteSource(charset);
        ByteSource b = broken();
        ByteSource c = CharSource.wrap("bar").asByteSource(charset);
        ByteSource concatenated = ByteSources.concatOpenable(a, b, c);
        assertEquals("foobar", concatenated.asCharSource(charset).read());
    }

    /**
     * Test of orEmpty method, of class ByteSources.
     */
    @Test
    public void testOrEmpty_URL() throws Exception {
        System.out.println("orEmpty(URL)");
        byte[] expResult = new byte[0];
        byte[] actualResult = ByteSources.orEmpty((URL) null).read();
        assertArrayEquals(expResult, actualResult);
        
        URL resource = getClass().getResource("/hello.txt");
        String expResultStr = "hello";
        String actualResultStr = ByteSources.orEmpty(resource).asCharSource(Charsets.US_ASCII).read();
        assertEquals(expResultStr, actualResultStr);
    }

    /**
     * Test of orEmpty method, of class ByteSources.
     */
    @Test
    public void testOrEmpty_ByteSource() throws IOException {
        System.out.println("orEmpty");
        ByteSource result = ByteSources.orEmpty(ByteSources.broken());
        assertEquals(0, result.size());
    }

    /**
     * Test of broken method, of class ByteSources.
     */
    @Test(expected = IOException.class)
    public void testBroken() throws IOException {
        System.out.println("broken");
        ByteSource result = ByteSources.broken();
        try (InputStream ignore = result.openStream()) {
            fail("should have thrown exception.");
        }
    }

    /**
     * Test of brokenIfNull method, of class ByteSources.
     */
    @Test(expected = IOException.class)
    public void testBrokenIfNull() throws IOException {
        System.out.println("brokenIfNull");
        ByteSource result = ByteSources.brokenIfNull(null);
        try (InputStream ignore = result.openStream()) {
            fail("Should have thrown exception on openStream");
        }
    }

    /**
     * Test of fromNullable method, of class ByteSources.
     */
    @Test(expected = IOException.class)
    public void testFromNullable() throws IOException {
        System.out.println("fromNullable");
        ByteSource result = ByteSources.fromNullable(null);
        try (InputStream ignore = result.openStream()) {
            fail("should have thrown exception");
        }
        
    }

    /**
     * Test of or method, of class ByteSources.
     */
    @Test
    public void testOr_ByteSource_ByteSourceArr() throws IOException {
        System.out.println("or");
        ByteSource first = ByteSources.broken();
        byte[] bytes = { (byte) 1, (byte) 2, (byte) 3, (byte) 4};
        ByteSource other = ByteSource.wrap(bytes);
        ByteSource result = ByteSources.or(first, other);
        assertArrayEquals(bytes, result.read());
        
    }

    @Test
    public void testEmpty() throws IOException {
        System.out.println("empty");
        //noinspection deprecation
        ByteSource result = ByteSources.empty();
        assertEquals(0, result.size());
    }

    @Test
    public void testGunzipping_ByteSource() throws Exception {
        System.out.println("testGunzipping");
        URL uncompressed = getClass().getResource("/hello.txt");
        URL compressed = getClass().getResource("/hello.txt.gz");
        ByteSource uncompressedSource = Resources.asByteSource(uncompressed);
        ByteSource compressedSource = Resources.asByteSource(compressed);
        testGunzipping(uncompressedSource, compressedSource, ByteSources::gunzipping);
    }

    @Test
    public void testGunzipping_URL() throws Exception {
        System.out.println("testGunzipping");
        URL uncompressed = getClass().getResource("/hello.txt");
        URL compressed = getClass().getResource("/hello.txt.gz");
        testGunzipping(Resources.asByteSource(uncompressed), compressed, ByteSources::gunzipping);
    }

    private <T> void testGunzipping(ByteSource uncompressedSource, T compressed, Function<T, ByteSource> bsFunction) throws IOException {
        String uncompressedContent = uncompressedSource.asCharSource(Charsets.US_ASCII).read();
        System.out.println("uncompressed: " + uncompressedContent);
        ByteSource decompressedSource = bsFunction.apply(compressed);
        String decompressedContent = decompressedSource.asCharSource(Charsets.US_ASCII).read();
        System.out.println("decompressed: " + decompressedContent);
        assertEquals(uncompressedContent, decompressedContent);
    }
}
