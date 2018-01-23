/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ProcessTracker;
import com.github.mike10004.nativehelper.subprocess.StreamContent;
import com.github.mike10004.nativehelper.subprocess.StreamContext;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.github.mike10004.nativehelper.subprocess.Subprocess.Launcher;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a program whose output is captured in memory as strings.
 * @author mchaberski
 */
@Deprecated
public class ProgramWithOutputStrings extends ProgramWithOutput<ProgramWithOutputStringsResult> {

    @Deprecated
    public static final String STDOUT_PROPERTY_NAME = ProgramWithOutputStrings.class.getName() + ".stdout";
    @Deprecated
    public static final String STDERR_PROPERTY_NAME = ProgramWithOutputStrings.class.getName() + ".stderr";
    
    private final Charset charset;
    
    protected ProgramWithOutputStrings(String executable, String standardInput, File standardInputFile, File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Charset charset) {
        super(executable, standardInput, standardInputFile, workingDirectory, environment, arguments);
        this.charset = checkNotNull(charset);
    }

    @Override
    protected SubprocessBridge<?> buildSubprocessBridge() {
        return new SubprocessBridge<String>() {
            @Override
            public Launcher<String, String> buildLauncher(Subprocess subprocess, ProcessTracker tracker) {
                return subprocess.launcher(tracker)
                        .output(new StringOutputStreamContext());
            }

            @Override
            public ProgramWithOutputStringsResult buildResult(ProcessResult<String, String> subprocessResult) {
                return new ProgramWithOutputStringsResult(subprocessResult.exitCode(), subprocessResult.content().stdout(), subprocessResult.content().stderr(), charset);
            }
        };

    }

    private class StringOutputStreamContext implements StreamContext<StringsStreamControl, String, String> {

        private StringOutputStreamContext() {
        }

        @Override
        public StringsStreamControl produceControl() throws IOException {
            return new StringsStreamControl(getStandardInput());
        }

        @Override
        public StreamContent<String, String> transform(int exitCode, StringsStreamControl context) {
            return context.toOutput(Charset.defaultCharset());
        }
    }

    static class StringsStreamControl extends ExecTaskStreamControl {
        private final ByteArrayOutputStream stdoutCollector, stderrCollector;
        public StringsStreamControl(ImmutablePair<String, File> standardInputSource) {
            super(standardInputSource);
            stdoutCollector = new ByteArrayOutputStream(256);
            stderrCollector = new ByteArrayOutputStream(256);
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return stdoutCollector;
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return stderrCollector;
        }

        public StreamContent<String, String> toOutput(Charset charset) {
            byte[] stdout = stdoutCollector.toByteArray();
            byte[] stderr = stderrCollector.toByteArray();
            return StreamContent.direct(new String(stdout, charset), new String(stderr, charset));
        }
    }}
