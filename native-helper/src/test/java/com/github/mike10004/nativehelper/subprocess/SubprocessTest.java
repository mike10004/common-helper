package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static com.github.mike10004.nativehelper.subprocess.Subprocess.running;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SubprocessTest {

    private static final ProcessContext CONTEXT = ProcessContext.create();

    @After
    public void checkProcesses() {
        int active = CONTEXT.activeCount();
        assertEquals("num active", 0, active);
    }

    @Test
    public void launch_true() throws Exception {
        int exitCode = running(pyTrue()).build()
                .launcher(CONTEXT)
                .launch().get().getExitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void execute_true() {
        int exitCode = running(pyTrue()).build()
                .launcher(CONTEXT)
                .execute().getExitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void launch_exit_3() throws Exception {
        int expected = 3;
        int exitCode = running(pyExit())
                .arg(String.valueOf(expected))
                .build()
                .launcher(CONTEXT)
                .launch().get().getExitCode();
        assertEquals("exit code", expected, exitCode);
    }

    private static File pyEcho() {
        return Tests.getPythonFile("bin_echo.py");
    }

    private static File pyTrue() {
        return Tests.getPythonFile("bin_true.py");
    }

    private static File pyExit() {
        return Tests.getPythonFile("bin_exit.py");
    }

    private static File pyCat() {
        return Tests.getPythonFile("bin_cat.py");
    }

    private static File pyReadInput() {
        return Tests.getPythonFile("read_input.py");
    }

    @Test
    public void launch_echo() throws Exception {
        String arg = "hello";
        ProcessResult<String, String> processResult = running(pyEcho())
                .arg(arg).build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().get();
        int exitCode = processResult.getExitCode();
        assertEquals("exit code", 0, exitCode);
        String actualStdout = processResult.getOutput().getStdout();
        String actualStderr = processResult.getOutput().getStderr();
        System.out.format("output: \"%s\"%n", StringEscapeUtils.escapeJava(actualStdout));
        assertEquals("stdout", arg, actualStdout);
        assertEquals("stderr", "", actualStderr);
    }

    @Test
    public void launch_stereo() throws Exception {
        List<String> stdout = Arrays.asList("foo", "bar", "baz"), stderr = Arrays.asList("gaw", "gee");
        List<String> args = new ArrayList<>();
        for (int i = 0; i < Math.max(stdout.size(), stderr.size()); i++) {
            if (i < stdout.size()) {
                args.add(stdout.get(i));
            }
            if (i < stderr.size()) {
                args.add(stderr.get(i));
            }
        }
        ProcessResult<String, String> result =
                running(Tests.getPythonFile("stereo.py"))
                .args(args)
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().get();
        String actualStdout = result.getOutput().getStdout();
        String actualStderr = result.getOutput().getStderr();
        assertEquals("stdout", joinPlus(linesep, stdout), actualStdout);
        assertEquals("stderr", joinPlus(linesep, stderr), actualStderr);
    }

    private static final String linesep = System.lineSeparator();

    private static String joinPlus(String delimiter, Iterable<String> items) {
        return String.join(delimiter, items) + delimiter;
    }

    @Test
    public void launch_cat_stdin() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 2 * 1024 * 1024; // overwhelm StreamPumper's buffer
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        ProcessResult<byte[], byte[]> result =
                running(pyCat())
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().get();
        System.out.println(result);
        assertEquals("exit code", 0, result.getExitCode());
        assertArrayEquals("stdout", bytes, result.getOutput().getStdout());
        assertEquals("stderr length", 0, result.getOutput().getStderr().length);
    }

    @Test
    public void launch_cat_file() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 256 * 1024;
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        File dataFile = File.createTempFile("SubprocessTest", ".dat");
        Files.asByteSink(dataFile).write(bytes);
        ProcessResult<byte[], byte[]> result =
                running(pyCat())
                        .arg(dataFile.getAbsolutePath())
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().get();
        System.out.println(result);
        assertEquals("exit code", 0, result.getExitCode());
        checkState(Arrays.equals(bytes, Files.asByteSource(dataFile).read()));
        assertArrayEquals("stdout", bytes, result.getOutput().getStdout());
        assertEquals("stderr length", 0, result.getOutput().getStderr().length);
        //noinspection ResultOfMethodCallIgnored
        dataFile.delete();
    }

    @Test
    public void launch_readInput_predefined() throws Exception {
        String expected = String.format("foo%nbar%n");
        ProcessResult<String, String> result = running(pyReadInput())
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII, CharSource.wrap(expected + System.lineSeparator()).asByteSource(US_ASCII))
                .launch().get();
        System.out.println(result);
        assertEquals("output", expected, result.getOutput().getStdout());
        assertEquals("exit code", 0, result.getExitCode());
    }

    @Test(timeout = 5000L)
    public void launch_readInput_piped() throws Exception {
        Charset charset = UTF_8;
        EchoByteSource pipe = new EchoByteSource();
        ListenableFuture<ProcessResult<String, String>> resultFuture = running(pyReadInput())
                .build()
                .launcher(CONTEXT)
                .outputStrings(charset, pipe.asByteSource())
                .launch();
        List<String> lines = Arrays.asList("foo", "bar", "baz", "");
        PrintWriter printer = new PrintWriter(new OutputStreamWriter(pipe.connect(), charset));
        for (String line : lines) {
            printer.println(line);
            printer.flush();
        }
        ProcessResult<String, String> result = resultFuture.get();
        System.out.format("get() returned %s%n", result);
        assertEquals("exit code", 0, result.getExitCode());
        String expected = joinPlus(System.lineSeparator(), lines.subList(0, 3));
        assertEquals("output", expected, result.getOutput().getStdout());
    }

    private static final List<String> poemLines = Arrays.asList(
            "April is the cruellest month, breeding",
            "Lilacs out of the dead land, mixing",
            "Memory and desire, stirring",
            "Dull roots with spring rain.",
            "Winter kept us warm, covering",
            "Earth in forgetful snow, feeding",
            "A little life with dried tubers.");

    private static <T> Supplier<T> nullSupplier() {
        return () -> null;
    }

    private static File writePoemToFile() throws IOException {
        File wastelandFile = File.createTempFile("SubprocessTest", ".txt");
        Files.asCharSink(wastelandFile, UTF_8).writeLines(poemLines);
        return wastelandFile;
    }

    @SuppressWarnings("Duplicates")
    @Test(timeout = 5000L)
    public void listen_pipeClass() throws Exception {
        org.apache.commons.io.input.TeeInputStream.class.getName();
        File wastelandFile = writePoemToFile();
        ByteBucket stderrBucket = ByteBucket.create();
        EchoByteSink stdoutPipe = new EchoByteSink();
        ProcessStreamEndpoints endpoints = ProcessStreamEndpoints.builder()
                .stderr(stderrBucket)
                .stdout(stdoutPipe)
                .noStdin() // read from file passed as argument
                .build();
        ProcessOutputControl<Void, String> outputControl = ProcessOutputControls.predefined(endpoints, nullSupplier(), () -> stderrBucket.decode(Charset.defaultCharset()));
        ListenableFuture<ProcessResult<Void, String>> future = Subprocess.running(pyCat())
                .arg(wastelandFile.getAbsolutePath())
                .build()
                .launcher(CONTEXT)
                .output(outputControl)
                .launch();
        Charset charset = Charset.defaultCharset();
        List<String> actualLines;
        try (Reader reader = new InputStreamReader(stdoutPipe.connect(), charset)) {
            actualLines = CharStreams.readLines(reader);
        }
        ProcessResult<Void, String> result = future.get();
        System.out.format("result: %s%n", result);
        System.out.format("lines:%n%s%n", String.join(System.lineSeparator(), actualLines));
        assertEquals("actual", poemLines, actualLines);
        assertEquals("exit code", 0, result.getExitCode());
    }

//    @Test
//    public void launch_readWrite_interleaved() throws Exception {
//        EchoByteSink echo = new EchoByteSink();
//        Charset charset = UTF_8;
//        ListenableFuture<ProcessResult<String, String>> resultFuture = running(pyReadInput())
//                .build()
//                .launcher(CONTEXT)
//                .outputStrings(charset, )
//                .launch();
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Futures.addCallback(resultFuture, new AlwaysCallback<Object>() {
//            @Override
//            protected void always(@Nullable Object result, @Nullable Throwable t) {
//                System.out.println("program ended: " + result);
//            }
//        }, executor);
//        List<String> lines = Arrays.asList("foo", "bar", "baz", "");
//        PrintWriter printer = new PrintWriter(new OutputStreamWriter(pipeOut, charset));
//        for (String line : lines) {
//            printer.println(line);
//            printer.flush();
//        }
//        ProcessResult<String, String> result = resultFuture.get();
//        assertEquals("exit code", 0, result.getExitCode());
//        String expected = joinPlus(System.lineSeparator(), lines.subList(0, 3));
//        assertEquals("output", expected, result.getOutput().getStdout());
//        executor.awaitTermination(5, TimeUnit.SECONDS);
//        executor.shutdown();
//    }
//

}