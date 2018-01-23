/*
 * (c) 2012 IBG LLC
 */
package com.github.mike10004.nativehelper;

import com.github.mike10004.nativehelper.Program.StandardInputSource;
import com.github.mike10004.nativehelper.subprocess.ProcessMonitor;
import com.github.mike10004.nativehelper.subprocess.ProcessResult;
import com.github.mike10004.nativehelper.subprocess.ProcessTracker;
import com.github.mike10004.nativehelper.subprocess.StreamContent;
import com.github.mike10004.nativehelper.subprocess.StreamContext;
import com.github.mike10004.nativehelper.subprocess.StreamControl;
import com.github.mike10004.nativehelper.subprocess.Subprocess;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.tools.ant.BuildException;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Task that runs a program in an external process and echos output. Provides 
 * hooks to read output as it arrives on the process's standard output and 
 * error streams. Uses a custom {@link EchoableRedirector Redirector} that
 * creates a {@link EchoingPumpStreamHandler stream handler} that provides a 
 * hook to hear output as it arrives. Allows a task to be aborted 
 * prematurely, if the {@code destructible} flag is true.
 * 
 * <p>The better way to do this would be to override the {@code Redirector}'s 
 * {@code Redirector#createStreams()} method, but there are
 * many private members used in that method, so it would take a lot of code
 * to override the class's setter methods to obtain visible references to 
 * them. 
 *   @author mchaberski
 * @deprecated use {@link com.github.mike10004.nativehelper.subprocess.Subprocess} API instead
 */
public class ExposedExecTask {

    private static final Logger log = Logger.getLogger(ExposedExecTask.class.getName());

    private static final ProcessTracker GLOBAL_TRACKER = ProcessTracker.create();
    private final LethalWatchdog watchdog;
    private transient ProcessMonitor<?, ?> processMonitor;
    private transient Subprocess.Launcher subprocessLauncher;

    public ExposedExecTask() {
        watchdog = new LethalWatchdog();
    }

    /**
     * Attempts to destroy the process.
     * If this task's process has not been set, then this method does nothing. The process is set
     * some time after {@link #execute()} is invoked. Therefore, this method's result is not very
     * predictable, so you should prefer to capture the process yourself using {@link #executeProcess(Consumer)}
     * and destroy it directly. At some point in the future, this method will be removed.
     * <p>This calls {@link Processes#destroy(Process, long, TimeUnit)} with a timeout of one second.
     * @return true if this task was never executed or it was successfully killed
     * @deprecated prefer using {@link #executeProcess(Consumer)} and doing what you want with that
     * process object
     */
    @Deprecated
    public boolean abort() {
        ProcessMonitor<?, ?> processMonitor = this.processMonitor;
        if (processMonitor != null) {
            processMonitor.destructor().sendTermSignal().timeout(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).kill();
            return !processMonitor.process().isAlive();
        }
        return false;
    }

    private final static int DEFAULT_TIMEOUT_MILLIS = 1000;

    public void execute() {
        executeProcess(process -> {});
    }

    static class AlreadyKilledException extends IllegalStateException {
        public AlreadyKilledException() {
            super("not executing because abort() was already called on this instance");
        }
    }

    LethalWatchdog getWatchdog() {
        return watchdog;
    }

    /**
     * Executes the process, providing the process object in a callback that executes on a
     * different thread.
     */
    public void executeProcess(Consumer<? super Process> processCallback) {
        checkState(subprocessLauncher != null, "subprocess launcher not present; call task.configure(program) first");
        ProcessMonitor<?, ?> pm;
        try {
            pm = this.processMonitor = subprocessLauncher.launch();
        } catch (com.github.mike10004.nativehelper.subprocess.ProcessLaunchException e) {
            throw new BuildException(e);
        }
        watchdog.start(pm.process());
        processCallback.accept(pm.process());
        try {
            pm.await();
        } catch (InterruptedException e) {
            throw new ProcessMonitorWaitInterruptedException(e);
        }
    }

    static class ProcessMonitorWaitInterruptedException extends BuildException {
        public ProcessMonitorWaitInterruptedException(Throwable cause) {
            super(cause);
        }
    }

    @Nullable
    Process getProcess() {
        ProcessMonitor<?, ?> processMonitor = this.processMonitor;
        if (processMonitor != null) {
            return processMonitor.process();
        }
        return null;
    }

    /**
     * Sets the subprocess launcher for this task. The task can only be executed
     * after the subprocess launcher is set.
     * @param program the program
     */
    void configure(Program<?> program) {
        Subprocess.Builder b = Subprocess.running(program.getExecutable())
                .args(program.getArguments())
                .env(program.getEnvironment());
        if (program.getWorkingDirectory() != null) {
            b.from(program.getWorkingDirectory());
        }
        subprocessLauncher = b.build()
                .launcher(GLOBAL_TRACKER)
                .output(produceStreamContext(program));
    }

    static class ExecTaskStreamControl implements StreamControl {

        private final StandardInputSource standardInputSource;

        ExecTaskStreamControl(StandardInputSource standardInputSource) {
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
            if (standardInputSource.getMemory() != null) {
                return CharSource.wrap(standardInputSource.getMemory()).asByteSource(Charset.defaultCharset()).openStream();
            } else if (standardInputSource.getDisk() != null) {
                return new FileInputStream(standardInputSource.getDisk());
            } else {
                return null;
            }
        }
    }

    static class StringsStreamControl extends ExecTaskStreamControl {
        private final ByteArrayOutputStream stdoutCollector, stderrCollector;
        public StringsStreamControl(StandardInputSource standardInputSource) {
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
    }

    protected StreamContext<?, ?, ?> produceStreamContext(Program program) {
        if (program instanceof ProgramWithOutputStrings) {
            return new StringOutputStreamContext(program);
        } else if (program instanceof ProgramWithOutputFiles) {
            return new FileOutputStreamContext((ProgramWithOutputFiles) program);
        } else if (program instanceof Program.SimpleProgram) {
            return new NoOutputStreamContext(program);
        } else {
            throw new IllegalArgumentException("not sure how to handle " + program);
        }
    }

    private static class NoOutputStreamContext implements StreamContext<ExecTaskStreamControl, Void, Void> {

        private final Program program;

        private NoOutputStreamContext(Program program) {
            this.program = program;
        }

        @Override
        public ExecTaskStreamControl produceControl() throws IOException {
            return new ExecTaskStreamControl(program.getStandardInput());
        }

        @Override
        public StreamContent<Void, Void> transform(int exitCode, ExecTaskStreamControl context) {
            return StreamContent.empty();
        }
    }

    private static class StringOutputStreamContext implements StreamContext<StringsStreamControl, String, String> {

        private final Program program;

        private StringOutputStreamContext(Program program) {
            this.program = program;
        }

        @Override
        public StringsStreamControl produceControl() throws IOException {
            return new StringsStreamControl(program.getStandardInput());
        }

        @Override
        public StreamContent<String, String> transform(int exitCode, StringsStreamControl context) {
            return context.toOutput(Charset.defaultCharset());
        }
    }

    private static class FilesStreamControl extends ExecTaskStreamControl {

        private final Supplier<File> stdoutFileSupplier, stderrFileSupplier;

        public FilesStreamControl(StandardInputSource standardInputSource, Supplier<File> stdoutFileSupplier, Supplier<File> stderrFileSupplier) {
            super(standardInputSource);
            this.stdoutFileSupplier = stdoutFileSupplier;
            this.stderrFileSupplier = stderrFileSupplier;
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return new FileOutputStream(stdoutFileSupplier.get());
        }

        @Override
        public OutputStream openStderrSink() throws IOException {
            return new FileOutputStream(stderrFileSupplier.get());
        }
    }

    private static class FileOutputStreamContext implements StreamContext<FilesStreamControl, File, File> {

        private final ProgramWithOutputFiles program;

        private FileOutputStreamContext(ProgramWithOutputFiles program) {
            this.program = program;
        }

        @Override
        public FilesStreamControl produceControl() throws IOException {
            return new FilesStreamControl(program.getStandardInput(), program.getStdoutFileSupplier(), program.getStderrFileSupplier());
        }

        @Override
        public StreamContent<File, File> transform(int exitCode, FilesStreamControl context) {
            return StreamContent.direct(context.stdoutFileSupplier.get(), context.stderrFileSupplier.get());
        }
    }

    ProcessResult<?, ?> getProcessResultOrDie() {
        return checkNotNull(getProcessResultOrNull(), "process result not yet obtained");
    }

    @Nullable
    ProcessResult<?, ?> getProcessResultOrNull() {
        ProcessMonitor<?, ?> processMonitor = this.processMonitor;
        ListenableFuture<? extends ProcessResult<?, ?>> future = processMonitor.future();
        if (future.isDone()) {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("BUG: already checked isDone()");
            }
        }
        return null;
    }

    public EchoableRedirector getRedirector() {
        throw new UnsupportedOperationException();
    }
}
