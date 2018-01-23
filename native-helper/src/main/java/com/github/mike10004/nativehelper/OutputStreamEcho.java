/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import java.io.ByteArrayOutputStream;

/**
 * Interface whose method is invoked when output from an output stream
 * is echoed. 
 *  @author mchaberski
 * @deprecated use {@link com.github.mike10004.nativehelper.subprocess.Subprocess} API instead
 */
public interface OutputStreamEcho {
    void writeEchoed(byte[] b, int off, int len);
    
    /**
     * Implementation of an output stream echo that collects all echoed bytes.
     */
    public static class BucketEcho implements OutputStreamEcho {

        private final ByteArrayOutputStream bucket;

        public BucketEcho() {
            this(8192);
        }
        
        public BucketEcho(int initialBufferSize) {
            bucket = new ByteArrayOutputStream(initialBufferSize);
        }
        
        @Override
        public synchronized void writeEchoed(byte[] b, int off, int len) {
            bucket.write(b, off, len);
        }

        /**
         * Creates and returns a byte array containing all bytes collected
         * so far.
         * @return a new byte array 
         */
        public synchronized byte[] toByteArray() {
            return bucket.toByteArray();
        }
    }
}
