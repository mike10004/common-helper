/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import java.util.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import com.github.mike10004.nativehelper.repackaged.org.apache.tools.ant.taskdefs.PumpStreamHandler;

/**
 * Stream handler that provides a method to echo output from the standard
 * output or error streams. Set the 
 * {@link #setStdoutEcho(OutputStreamEcho) stdoutEcho}
 * or  {@link #setStderrEcho(OutputStreamEcho) stderrEcho} 
 * fields in order to hear the output.
 *  @author mchaberski
 */
public class EchoingPumpStreamHandler extends PumpStreamHandler {

    private transient final OutputStream out, err;
    private transient final InputStream in;
    private OutputStreamEcho stdoutEcho, stderrEcho;
    
    public EchoingPumpStreamHandler(OutputStream out, OutputStream err, 
            @Nullable InputStream input, boolean nonBlockingRead) {
        super(checkNotNull(out, "output stream must be non-null"), 
                checkNotNull(err, "error stream must be non-null"), 
                input, nonBlockingRead);
        this.out = out;
        this.err = err;
        this.in = input;
    }

    @Nullable
    public OutputStreamEcho getStderrEcho() {
        return stderrEcho;
    }

    public void setStderrEcho(@Nullable OutputStreamEcho stderrEcho) {
        this.stderrEcho = stderrEcho;
    }

    @Nullable
    public OutputStreamEcho getStdoutEcho() {
        return stdoutEcho;
    }

    public void setStdoutEcho(@Nullable OutputStreamEcho stdoutEcho) {
        this.stdoutEcho = stdoutEcho;
    }
    
    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @param closeWhenExhausted if true close the inputstream.
     * @param nonBlockingIO set it to <code>true</code> to use simulated non
     *                     blocking IO.
     * @return a thread object that does the pumping, subclasses
     * should return an instance of {@link ThreadWithPumper
     * ThreadWithPumper}.
     * @since Ant 1.8.2
     */
    @Override
    protected Thread createPump(InputStream is, final OutputStream os,
                                boolean closeWhenExhausted, boolean nonBlockingIO) {
        EchoingStreamPumper pumper = new EchoingStreamPumper(is, os, 
                closeWhenExhausted, nonBlockingIO);
        final OutputStreamEcho echo;
        final String streamName;
        if (Objects.equals(os, err)) {
            echo = stderrEcho;
            streamName = "stderr";
        } else if (Objects.equals(os, this.out)) {
            echo = stdoutEcho;
            streamName = "stdout";
        } else if (Objects.equals(is, this.in)) {
            // we don't handle this case right now, but maybe in the future we 
            // can add an input echo
            streamName = "stdin";
            echo = null;
        } else {
            throw new IllegalArgumentException("argument output stream is unrecognized; "
                    + "this stream handler instance must be constructed with "
                    + "the argument output stream as output or error stream");
        }
        pumper.getEchoingOutputStream().setEcho(echo);
        final Thread result = new PumpStreamHandler.ThreadWithPumper(pumper);
        String threadName = "PumpStreamHandlerThread-" + streamName + '-' + threadIndex.incrementAndGet();
        result.setName(threadName);
        result.setDaemon(true);
        return result;
    }
    
    private static final AtomicLong threadIndex = new AtomicLong(0L);
}

