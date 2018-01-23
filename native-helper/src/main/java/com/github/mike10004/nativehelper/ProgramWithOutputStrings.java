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
import org.apache.tools.ant.taskdefs.ExecTask;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Class that represents a program whose output is captured in memory as strings.
 * @author mchaberski
 * @deprecated use {@link com.github.mike10004.nativehelper.subprocess.Subprocess} API instead
 */
public class ProgramWithOutputStrings extends ProgramWithOutput<ProgramWithOutputStringsResult> {

    @SuppressWarnings("unused")
    public static final String STDOUT_PROPERTY_NAME = ProgramWithOutputStrings.class.getName() + ".stdout";
    @SuppressWarnings("unused")
    public static final String STDERR_PROPERTY_NAME = ProgramWithOutputStrings.class.getName() + ".stderr";
    
    private final Charset charset;
    
    protected ProgramWithOutputStrings(String executable, StandardInputSource stdinSource, File workingDirectory, Map<String, String> environment, Iterable<String> arguments, Supplier<? extends ExposedExecTask> taskFactory, Charset charset) {
        super(executable, stdinSource, workingDirectory, environment, arguments, taskFactory);
        this.charset = checkNotNull(charset);
    }

    @Override
    protected ProgramWithOutputStringsResult produceResultFromExecutedTask(ExposedExecTask task, Map<String, Object> executionContext) {
        @SuppressWarnings("unchecked")
        ProcessResult<String, String> result = (ProcessResult<String, String>) task.getProcessResultOrNull();
        checkState(result != null, "result not available; task not yet executed maybe");
        return new ProgramWithOutputStringsResult(result.getExitCode(), result.getOutput().getStdout(), result.getOutput().getStderr(), charset);
    }

}
