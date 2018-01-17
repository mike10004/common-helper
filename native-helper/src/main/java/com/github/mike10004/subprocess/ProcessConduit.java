package com.github.mike10004.subprocess;

import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.util.FileUtils;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProcessConduit {

    private volatile Thread outputThread;
    private volatile Thread errorThread;
    private volatile Thread inputThread;

    private final OutputStream out;
    private final OutputStream err;
    @Nullable
    private final InputStream input;
    private final boolean nonBlockingRead;

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>.
     * @param err the error <code>OutputStream</code>.
     * @param input the input <code>InputStream</code>.
     * @param nonBlockingRead set it to <code>true</code> if the input should be
     *                      read with simulated non blocking IO.
     */
    public ProcessConduit(OutputStream out, OutputStream err,
                             @Nullable InputStream input, boolean nonBlockingRead) {
        this.out = out;
        this.err = err;
        this.input = input;
        this.nonBlockingRead = nonBlockingRead;
    }

    /**
     * Set the <code>InputStream</code> from which to read the
     * standard output of the process.
     * @param is the <code>InputStream</code>.
     */
    private void setProcessOutputStream(InputStream is) {
        createProcessOutputPump(is, out);
    }

    /**
     * Set the <code>InputStream</code> from which to read the
     * standard error of the process.
     * @param is the <code>InputStream</code>.
     */
    private void setProcessErrorStream(InputStream is) {
        if (err != null) {
            createProcessErrorPump(is, err);
        }
    }

    /**
     * Set the <code>OutputStream</code> by means of which
     * input can be sent to the process.
     * @param os the <code>OutputStream</code>.
     */
    private void setProcessInputStream(OutputStream os) {
        if (input != null) {
            inputThread = createPump(input, os, true, nonBlockingRead);
        } else {
            FileUtils.close(os);
        }
    }

    /**
     * Start the <code>Thread</code>s.
     */
    public java.io.Closeable connect(OutputStream stdin, InputStream stdout, InputStream stderr) {
        setProcessInputStream(stdin);
        setProcessErrorStream(stderr);
        setProcessOutputStream(stdout);
        outputThread.start();
        errorThread.start();
        if (inputThread != null) {
            inputThread.start();
        }
        return new Closeable() {
            @Override
            public void close() throws IOException {
                stop();
            }
        };
    }

    /**
     * Stop pumping the streams.
     */
    private void stop() {
        finish(inputThread);

        try {
            err.flush();
        } catch (IOException e) {
            // ignore
        }
        try {
            out.flush();
        } catch (IOException e) {
            // ignore
        }
        finish(outputThread);
        finish(errorThread);
    }

    private static final long JOIN_TIMEOUT = 200;

    /**
     * Waits for a thread to finish while trying to make it finish
     * quicker by stopping the pumper (if the thread is a {@link
     * PumpStreamHandler.ThreadWithPumper ThreadWithPumper} instance) or interrupting
     * the thread.
     *
     * @since Ant 1.8.0
     */
    private final void finish(Thread t) {
        if (t == null) {
            // nothing to terminate
            return;
        }
        try {
            StreamPumper s = null;
            if (t instanceof ThreadWithPumper) {
                s = ((ThreadWithPumper) t).getPumper();
            }
            if (s != null && s.isFinished()) {
                return;
            }
            if (!t.isAlive()) {
                return;
            }

            if (s != null && !s.isFinished()) {
                s.stop();
            }
            t.join(JOIN_TIMEOUT);
            while ((s == null || !s.isFinished()) && t.isAlive()) {
                t.interrupt();
                t.join(JOIN_TIMEOUT);
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Get the error stream.
     * @return <code>OutputStream</code>.
     */
    protected OutputStream getErr() {
        return err;
    }

    /**
     * Get the output stream.
     * @return <code>OutputStream</code>.
     */
    protected OutputStream getOut() {
        return out;
    }

    /**
     * Create the pump to handle process output.
     * @param is the <code>InputStream</code>.
     * @param os the <code>OutputStream</code>.
     */
    protected void createProcessOutputPump(InputStream is, OutputStream os) {
        outputThread = createPump(is, os);
    }

    /**
     * Create the pump to handle error output.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     */
    protected void createProcessErrorPump(InputStream is, OutputStream os) {
        errorThread = createPump(is, os);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @return a thread object that does the pumping.
     */
    protected Thread createPump(InputStream is, OutputStream os) {
        return createPump(is, os, false);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @param closeWhenExhausted if true close the inputstream.
     * @return a thread object that does the pumping, subclasses
     * should return an instance of {@link PumpStreamHandler.ThreadWithPumper
     * ThreadWithPumper}.
     */
    protected Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        return createPump(is, os, closeWhenExhausted, true);
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
     * should return an instance of {@link PumpStreamHandler.ThreadWithPumper
     * ThreadWithPumper}.
     * @since Ant 1.8.2
     */
    protected Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted, boolean nonBlockingIO) {
        StreamPumper pumper = new StreamPumper(is, os, closeWhenExhausted, nonBlockingIO);
        pumper.setAutoflush(true);
        final Thread result = new ThreadWithPumper(pumper);
        result.setDaemon(true);
        return result;
    }

    /**
     * Specialized subclass that allows access to the running StreamPumper.
     *
     * @since Ant 1.8.0
     */
    protected static class ThreadWithPumper extends Thread {
        private final StreamPumper pumper;
        public ThreadWithPumper(StreamPumper p) {
            super(p);
            pumper = p;
        }
        protected StreamPumper getPumper() {
            return pumper;
        }
    }

}
