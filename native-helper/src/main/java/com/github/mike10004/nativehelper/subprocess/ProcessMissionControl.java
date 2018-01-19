/*
 * A lot of code taken from Execute class in apache-ant and (c) Apache Software Foundation
 */
package com.github.mike10004.nativehelper.subprocess;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

class ProcessMissionControl {

    private static final Logger log = LoggerFactory.getLogger(ProcessMissionControl.class);

    private final ListeningExecutorService terminationWaitingService;
    private final Subprocess program;
    private final ProcessContext processDestroyer;

    public ProcessMissionControl(Subprocess program, ProcessContext processDestroyer, ListeningExecutorService terminationWaitingService) {
        this.program = requireNonNull(program);
        this.processDestroyer = requireNonNull(processDestroyer);
        this.terminationWaitingService = requireNonNull(terminationWaitingService);
    }

    public interface Execution {
        Process getProcess();
        FluentFuture<Integer> getFuture();
    }

    public Execution launch(ProcessStreamEndpoints endpoints) {
        Process process = execute();
        FluentFuture<Integer> future = FluentFuture.from(terminationWaitingService.submit(() -> {
            return follow(process, endpoints);
        }));
        return new Execution() {
            @Override
            public Process getProcess() {
                return process;
            }

            @Override
            public FluentFuture<Integer> getFuture() {
                return future;
            }
        };
    }

    private ImmutableList<String> getCommandLine() {
        ImmutableList.Builder<String> cmdline = ImmutableList.builder();
        cmdline.add(program.getExecutable());
        cmdline.addAll(program.getArguments());
        return cmdline.build();
    }

    private static final boolean NON_BLOCKING_READ = false;

    private Process createProcess(List<String> cmdline) {
        ProcessBuilder pb = new ProcessBuilder()
                .command(cmdline)
                .redirectError(Redirect.PIPE)
                .redirectOutput(Redirect.PIPE)
                .redirectInput(Redirect.PIPE)
                .directory(program.getWorkingDirectory());
        Map<String, String> pbenv = pb.environment();
        pbenv.putAll(program.getEnvironment());
        try {
            return pb.start();
        } catch (IOException e) {
            throw new ProcessStartException(e);
        }
    }

    static class ProcessStartException extends ProcessLaunchException {
        public ProcessStartException(IOException cause) {
            super(cause);
        }
    }

    /**
     * Runs a process and returns its exit status.
     *
     * @return the exit status of the subprocess or null if the process did
     * @exception ProcessException The exception is thrown, if launching
     *            of the subprocess failed.
     */
    @VisibleForTesting
    Process execute() {
        File workingDirectory = program.getWorkingDirectory();
        if (!InvalidWorkingDirectoryException.check(workingDirectory)) {
            throw new InvalidWorkingDirectoryException(workingDirectory);
        }
        final Process process = createProcess(getCommandLine());
        return process;
    }

    static class InvalidWorkingDirectoryException extends ProcessLaunchException {
        public InvalidWorkingDirectoryException(File workingDirectory) {
            super("specified working directory " + workingDirectory + " is not a directory; is file? " + workingDirectory.isFile());
        }

        static boolean check(@Nullable File workingDirectory) {
            return workingDirectory == null || workingDirectory.isDirectory();
        }
    }

    private static class MaybeNullResource<T extends java.io.Closeable> implements java.io.Closeable {
        @Nullable
        public final T resource;

        private MaybeNullResource(@Nullable T closeable) {
            this.resource = closeable;
        }

        @Override
        public void close() throws IOException {
            if (resource != null) {
                resource.close();
            }
        }
    }

    private static MaybeNullResource<InputStream> openMaybeNullInputStream(@Nullable ByteSource source) throws IOException {
        if (source == null) {
            return new MaybeNullResource<>(null);
        } else {
            return new MaybeNullResource<>(source.openStream());
        }
    }

    @VisibleForTesting
    @Nullable
    Integer follow(Process process, ProcessStreamEndpoints endpoints) throws IOException {
        processDestroyer.add(process);
        boolean terminated = false;
        @Nullable Integer exitVal;
        OutputStream processStdin = null;
        InputStream processStdout = null, processStderr = null;
        try (MaybeNullResource<InputStream> inResource = openMaybeNullInputStream(endpoints.stdin);
            OutputStream stdoutDestination = endpoints.stdout.openStream();
            OutputStream stderrDestination = endpoints.stderr.openStream()) {
            ProcessConduit conduit = new ProcessConduit(stdoutDestination, stderrDestination, inResource.resource, NON_BLOCKING_READ);
            processStdin = process.getOutputStream();
            processStdout = process.getInputStream();
            processStderr = process.getErrorStream();
            try (Closeable ignore = conduit.connect(processStdin, processStdout, processStderr)) {
                exitVal = waitFor(process);
                if (exitVal != null) {
                    terminated = true;
                }
            }
        } finally {
            if (!terminated) {
                destroy(process);
            }
            processDestroyer.remove(process);
            closeStreams(processStdin, processStdout, processStderr);
        }
        return exitVal;
    }

    private void destroy(Process process) {
        boolean terminatedNaturally = false;
        try {
            terminatedNaturally = process.waitFor(0, TimeUnit.MILLISECONDS);
        } catch(InterruptedException ignore) {
        } finally {
            if (!terminatedNaturally && process.isAlive()) {
                process.destroy();
            }
        }

    }

    /**
     * Wait for a given process.
     *
     * @param process the process one wants to wait for.
     */
    @Nullable
    private Integer waitFor(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            log.info("interrupted in Process.waitFor");
            return null;
        }
    }

    private static void closeStreams(java.io.Closeable...streams) {
        for (java.io.Closeable stream : streams) {
            try {
                stream.close();
            } catch (IOException ignore) {
            }
        }
    }
}
