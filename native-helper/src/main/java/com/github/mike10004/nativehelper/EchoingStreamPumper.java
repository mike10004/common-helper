/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import java.io.InputStream;
import java.io.OutputStream;
import org.apache.tools.ant.taskdefs.StreamPumper;

/**
 * Class that 
 *  @author mchaberski
 */
public class EchoingStreamPumper extends StreamPumper {

    private final EchoingOutputStream os;
    
    public EchoingStreamPumper(InputStream is, OutputStream os, 
            boolean closeWhenExhausted, boolean useAvailable) {
        this(is, new EchoingOutputStream(os), closeWhenExhausted, useAvailable);
    }
    
    private EchoingStreamPumper(InputStream is, EchoingOutputStream os, 
            boolean closeWhenExhausted, boolean useAvailable) {
        super(is, os, closeWhenExhausted, useAvailable);
        this.os = os;
    }

    public EchoingOutputStream getEchoingOutputStream() {
        return os;
    }
}


