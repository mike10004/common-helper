/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Flushable;
import java.nio.charset.Charset;

/**
 * Output stream echo that re-echos the bytes it receives as strings.
 * Buffers the bytes it receives until a delimiter appears, then 
 * invokes {@link #consumedLine(java.lang.String) consumedLine()} with the 
 * contents of the buffer. If the end of the input is not a delimiter, then 
 * {@link #flush() } must be called in order to get the remaining contents
 * of the buffer.
 *  @author mchaberski
 */
public abstract class StringReadingEcho implements OutputStreamEcho, Flushable {

    /**
     * Default initial capacity, in characters.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 8192;
    
    private final StringBuilder sb;
    private int numCharsEaten;
    private int numLinesEaten;
    private int numBytesEaten;
    private int numChunksEaten;
    private final Charset charset;
    private final String delimiter;
    
    /**
     * Creates an instances with the default charset and system line 
     * separator as delimiter.
     */
    public StringReadingEcho() {
        this(System.getProperty("line.separator"));
    }
    
    /**
     * Creates an instances with the specified charset and system line 
     * separator as delimiter.
     * @param charset  the charset to use
     */
    public StringReadingEcho(Charset charset) {
        this(charset, System.getProperty("line.separator"), DEFAULT_INITIAL_CAPACITY);
    }
    
    /**
     * Creates an instances with the default charset and using the specified
     * delimiter.
     * @param delimiter  the delimiter
     */
    public StringReadingEcho(String delimiter) {
        this(Charset.defaultCharset(), delimiter, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs an instance with the default initial buffer capacity.
     * @param charset the charset to use
     * @param delimiter  the delimiter to use
     */
    public StringReadingEcho(Charset charset, String delimiter) {
        this(charset, delimiter, DEFAULT_INITIAL_CAPACITY);
    }
    
    /**
     * Constructs an instance using the specified character set and 
     * delimiter that delimits lines. 
     * @param charset the character set to use
     * @param delimiter the delimiter to use
     * @param initialCapacity the initial capacity of the character buffer
     */
    public StringReadingEcho(Charset charset, String delimiter, int initialCapacity) {
        Preconditions.checkArgument(initialCapacity > 0);
        this.charset = charset;
        this.delimiter = delimiter;
        sb = new StringBuilder(initialCapacity);
    }

    /**
     * Method invoked when bytes are received. Constructs a string using
     * this instance's charset and passes it to 
     * {@link #consume(java.lang.String) }.
     * @param b the byte array
     * @param off the offset
     * @param len  the length
     */
    @Override
    public synchronized void writeEchoed(byte[] b, int off, int len) {
        String s = new String(b, off, len, charset);
        numBytesEaten += len;
        ++numChunksEaten;
        numCharsEaten += s.length();
        consume(s);
    }

    protected synchronized void consume(String data) {
        sb.append(data);
        int newlineIndex = sb.indexOf(delimiter);
        if (newlineIndex == -1) {
            return;
        }
        int pos = 0;
        while (newlineIndex != -1) {
            String line = sb.substring(pos, newlineIndex);
            consumedLine(line);
            ++numLinesEaten;
            pos = newlineIndex + delimiter.length();
            if (pos >= sb.length()) {
                break;
            }
            newlineIndex = sb.indexOf(delimiter, pos);
        }
        if (sb.length() > pos) {
            String s = sb.substring(pos);
            sb.setLength(0);
            sb.append(s);
        } else {
            sb.setLength(0);
        }
    }

    public int getNumBytesConsumed() {
        return numBytesEaten;
    }
    
    public int getNumCharsConsumed() {
        return numCharsEaten;
    }

    public int getNumLinesConsumed() {
        return numLinesEaten;
    }

    /**
     * Method invoked whenever a line is consumed. A line is defined as the
     * character sequence in between delimiters or the start/end of the 
     * input characters.
     * @param line the line consumed
     */
    protected abstract void consumedLine(String line);
    
    public int getNumChunksConsumed() {
        return numChunksEaten;
    }
    
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(StringReadingEcho.class)
                .add("chunks", numChunksEaten)
                .add("bytes", numBytesEaten)
                .add("chars", numCharsEaten)
                .add("lines", numLinesEaten)
                .add("bytesPerChunk", String.format("%.0f", (double) numBytesEaten / (double) numChunksEaten)).toString();
    }

    /**
     * Clears the internal buffer, flushing any characters that it contains.
     * If the buffer did contain any characters, they are sent in a string 
     * to {@link #consumedLine(java.lang.String) consumedLine()}.
     * 
     */
    @Override
    public synchronized void flush() {
        String flotsam = sb.toString();
        sb.setLength(0);
        if (!flotsam.isEmpty()) {
            consumedLine(flotsam);
        }
    }
    
}
