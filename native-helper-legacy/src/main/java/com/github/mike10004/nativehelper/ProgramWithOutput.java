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

import com.github.mike10004.nativehelper.subprocess.StreamControl;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Abstract superclass for programs whose output is captured.
 * @author mchaberski
 * @see ProgramWithOutputResult
 */
public abstract class ProgramWithOutput<R extends ProgramWithOutputResult> extends Program<R> {

    protected ProgramWithOutput(String executable, String standardInput, File standardInputFile, File workingDirectory, Map<String, String> environment, Iterable<String> arguments) {
        super(executable, standardInput, standardInputFile, workingDirectory, environment, arguments);
    }

    protected static class ExecTaskStreamControl implements StreamControl {

        private final ImmutablePair<String, File> standardInputSource;

        ExecTaskStreamControl(ImmutablePair<String, File> standardInputSource) {
            this.standardInputSource = standardInputSource;
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return ByteStreams.nullOutputStream();
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return ByteStreams.nullOutputStream();
        }

        @Nullable
        @Override
        public InputStream openStdinSource() throws IOException {
            if (standardInputSource.getLeft() != null) {
                return CharSource.wrap(standardInputSource.getLeft()).asByteSource(Charset.defaultCharset()).openStream();
            } else if (standardInputSource.getRight() != null) {
                return new FileInputStream(standardInputSource.getRight());
            } else {
                return null;
            }
        }
    }
}
