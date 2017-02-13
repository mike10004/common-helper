/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nullable;

/**
 * Class that provides a method to echo whatever data is written to the 
 * underlying stream.
 *  @author mchaberski
 */
public class EchoingOutputStream extends FilterOutputStream {

    private OutputStreamEcho echo;
    
    public EchoingOutputStream(OutputStream out) {
        super(checkNotNull(out));
    }

    /**
     * Gets the echo.
     * @return  the echo
     */
    public @Nullable OutputStreamEcho getEcho() {
        return echo;
    }

    /**
     * Sets the echo.
     * @param echo the echo
     */
    public void setEcho(@Nullable OutputStreamEcho echo) {
        this.echo = echo;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        OutputStreamEcho echo_ = getEcho();
        if (echo_ != null) {
            echo_.writeEchoed(b, off, len);
        }
    }
}