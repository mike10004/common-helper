/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Redirector;

import java.io.File;

/**
 * Class that implements a redirector that can be configured to echo the data
 * being redirected.
 *  @author mchaberski
 */
public class EchoableRedirector extends Redirector {

    private OutputStreamEcho stdoutEcho, stderrEcho;
    private boolean hasInputFile, hasInputString;

    public EchoableRedirector(org.apache.tools.ant.Task managingTask) {
        super(managingTask);
    }

    public OutputStreamEcho getStderrEcho() {
        return stderrEcho;
    }

    public void setStderrEcho(OutputStreamEcho stderrEcho) {
        this.stderrEcho = stderrEcho;
    }

    public OutputStreamEcho getStdoutEcho() {
        return stdoutEcho;
    }

    public void setStdoutEcho(OutputStreamEcho stdoutEcho) {
        this.stdoutEcho = stdoutEcho;
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     * 
     * @return the execute stream handler to manage the input, output and error
     *         streams.
     * 
     * @throws BuildException
     *             if the execute stream handler cannot be created.
     */
    @Override
    public ExecuteStreamHandler createHandler() throws BuildException {
        createStreams();
        boolean nonBlockingRead = isNonBlockingIO();
        EchoingPumpStreamHandler handler = new EchoingPumpStreamHandler(
                getOutputStream(), getErrorStream(),
                getInputStream(), nonBlockingRead);
        handler.setStdoutEcho(stdoutEcho);
        handler.setStderrEcho(stderrEcho);
        return handler;
    }

    @Override
    public void setInput(File input) {
        super.setInput(input);
        hasInputFile = input != null;
    }

    @Override
    public void setInput(File[] input) {
        super.setInput(input);
        hasInputFile = input != null;
    }

    @Override
    public void setInputString(String inputString) {
        super.setInputString(inputString);
        hasInputString = inputString != null;
    }
    
    protected boolean isNonBlockingIO() {
        return !hasInputFile && !hasInputString;
    }

}