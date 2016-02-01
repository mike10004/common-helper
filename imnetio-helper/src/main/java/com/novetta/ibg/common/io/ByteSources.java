/*
 * (c) 2015 Mike Chaberski
 */
package com.novetta.ibg.common.io;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that provides static utility methods relating to byte sources.
 * @author mchaberski
 * @see ByteSource
 */
public class ByteSources {
    
    /**
     * Concatenates multiple byte sources, skipping sources for which
     * opening an input stream fails.
     * @param first the first byte source
     * @param others other byte sources
     * @return the concatenated source
     */
    public static ByteSource concatOpenable(ByteSource first, ByteSource...others) {
        return concatOpenable(Lists.asList(first, others));
    }
    /**
     * Concatenates multiple byte sources, skipping sources for which
     * opening an input stream fails.
     * @param possibleBrokenSources an iterable over the possibly-broken sources
     * @return the concatenated source
     */
    public static ByteSource concatOpenable(Iterable<ByteSource> possibleBrokenSources) {
        return ByteSource.concat(Iterables.transform(possibleBrokenSources, newOrEmptyFunction()));
    }
    
    public static Function<ByteSource, ByteSource> newOrEmptyFunction() {
        return new Function<ByteSource, ByteSource>() {
            @Override
            public ByteSource apply(ByteSource input) {
                return orEmpty(input);
            }
        };
    }
    
    public static ByteSource orEmpty(URL resource) {
        if (resource == null) {
            return empty();
        }
        return Resources.asByteSource(resource);
    }

    public static ByteSource orEmpty(ByteSource first) {
        return or(first, empty());
    }

    public static ByteSource broken() {
        return brokenByteSourceInstance;
    }
    
    private static final ByteSource brokenByteSourceInstance = new BrokenByteSource();
    
    private static class BrokenByteSource extends ByteSource {
        
        @Override
        public String toString() {
            return "ByteSources{BROKEN}";
        }

        @Override
        public InputStream openStream() throws IOException {
            throw new IOException("broken");
        }
        
    }
    
    public static ByteSource brokenIfNull(URL resource) {
        return resource == null ? broken() : Resources.asByteSource(resource);
    }
    
    public static ByteSource fromNullable(ByteSource byteSource) {
        return byteSource == null ? broken() : byteSource;
    }
    
    public static ByteSource or(final ByteSource first, ByteSource... others) {
        final List<ByteSource> sources = Lists.asList(first, others);
        return or(sources);
    }    
    
    public static ByteSource or(final Iterable<ByteSource> sources) {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                int numSourcesTried = 0;
                for (ByteSource source : sources) {
                    numSourcesTried++;
                    try {
                        InputStream in = source.openStream();
                        return in;
                    } catch (IOException swallow) {
                    }
                }
                throw new IOException("no streams available from " + numSourcesTried + " sources");
            }
        };
    }

    public static ByteSource empty() {
        return emptyByteSourceInstance;
    }

    private static final byte[] emptyByteArray = new byte[0];
    
    private static class EmptyByteSource extends ByteSource {
        
        private final ByteSource delegate = ByteSource.wrap(emptyByteArray);
        
        private EmptyByteSource() {}

        @Override
        public InputStream openStream() throws IOException {
            return delegate.openStream();
        }

        @Override
        public boolean contentEquals(ByteSource other) throws IOException {
            return other.isEmpty();
        }

        @Override
        public HashCode hash(HashFunction hashFunction) throws IOException {
            return delegate.hash(hashFunction);
        }

        @Override
        public <T> T read(ByteProcessor<T> processor) throws IOException {
            return delegate.read(processor);
        }

        @Override
        public byte[] read() throws IOException {
            return emptyByteArray;
        }

        @Override
        public long copyTo(ByteSink sink) throws IOException {
            return 0L;
        }

        @Override
        public long copyTo(OutputStream output) throws IOException {
            return 0L;
        }

        @Override
        public long size() throws IOException {
            return 0L;
        }

        @Override
        public boolean isEmpty() throws IOException {
            return true;
        }

        @Override
        public ByteSource slice(long offset, long length) {
            return delegate.slice(offset, length);
        }

        @Override
        public CharSource asCharSource(Charset charset) {
            return delegate.asCharSource(charset);
        }

        @Override
        public String toString() {
            return "ByteSources{EMPTY}";
        }
        
    }

    private static final ByteSource emptyByteSourceInstance = new EmptyByteSource();

    private static class GunzippingByteSource extends ByteSource {

        private final ByteSource gzippedByteSource;

        private GunzippingByteSource(ByteSource gzippedByteSource) {
            this.gzippedByteSource = checkNotNull(gzippedByteSource);
        }


        @Override
        public InputStream openStream() throws IOException {
            return new GZIPInputStream(gzippedByteSource.openStream());
        }

        @Override
        public String toString() {
            return "GunzippingByteSource{wrapped=" + gzippedByteSource + "}";
        }
    }

    /**
     * Creates and returns a byte source that decompresses a gzipped byte source.
     * @param gzippedByteSource the gzipped byte source to decompress
     * @return the byte source providing uncompressed data
     */
    public static ByteSource gunzipping(ByteSource gzippedByteSource) {
        return new GunzippingByteSource(gzippedByteSource);
    }

    /**
     * Creates and returns a byte source that decompresses a gzipped resource.
     * @param gzippedResource the gzipped resource
     * @return a byte source that provides uncompressed data
     */
    public static ByteSource gunzipping(URL gzippedResource) {
        return new GunzippingByteSource(Resources.asByteSource(gzippedResource));
    }
}
