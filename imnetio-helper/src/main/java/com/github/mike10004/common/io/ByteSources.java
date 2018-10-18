package com.github.mike10004.common.io;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that provides static utility methods relating to byte sources.
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
        //noinspection StaticPseudoFunctionalStyleMethod
        return ByteSource.concat(Iterables.transform(possibleBrokenSources, ByteSources::orEmpty));
    }

    @Deprecated
    public static Function<ByteSource, ByteSource> newOrEmptyFunction() {
        return ByteSources::orEmpty;
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

    /**
     * Returns an empty byte source.
     * @deprecated use {@link ByteSource#empty()}
     * @return an empty byte source
     */
    @Deprecated
    public static ByteSource empty() {
        return ByteSource.empty();
    }

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
