package com.github.mike10004.nativehelper.subprocess;

import com.github.mike10004.nativehelper.Platforms;
import com.github.mike10004.nativehelper.test.Tests;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.mike10004.nativehelper.subprocess.Subprocess.running;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubprocessTest extends SubprocessTestBase {

    @Test
    public void launch_true() throws Exception {
        int exitCode = running(pyTrue()).build()
                .launcher(CONTEXT)
                .launch().await().exitCode();
        assertEquals("exit code", 0, exitCode);
    }

    @Test
    public void launch_exit_3() throws Exception {
        int expected = 3;
        int exitCode = running(pyExit())
                .arg(String.valueOf(expected))
                .build()
                .launcher(CONTEXT)
                .launch().await().exitCode();
        assertEquals("exit code", expected, exitCode);
    }

    private static File pyEcho() {
        return Tests.getPythonFile("nht_echo.py");
    }

    private static File pyTrue() {
        return Tests.getPythonFile("nht_true.py");
    }

    private static File pyExit() {
        return Tests.getPythonFile("nht_exit.py");
    }

    @Test
    public void launch_echo() throws Exception {
        String arg = "hello";
        ProcessResult<String, String> processResult = running(pyEcho())
                .arg(arg).build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().await();
        int exitCode = processResult.exitCode();
        assertEquals("exit code", 0, exitCode);
        String actualStdout = processResult.content().stdout();
        String actualStderr = processResult.content().stderr();
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
                running(Tests.getPythonFile("nht_stereo.py"))
                .args(args)
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII)
                .launch().await();
        String actualStdout = result.content().stdout();
        String actualStderr = result.content().stderr();
        assertEquals("stdout", Tests.joinPlus(linesep, stdout), actualStdout);
        assertEquals("stderr", Tests.joinPlus(linesep, stderr), actualStderr);
    }

    private static final String linesep = System.lineSeparator();

    @Test
    public void launch_cat_stdin() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        int LENGTH = 2 * 1024 * 1024; // overwhelm StreamPumper's buffer
        byte[] bytes = new byte[LENGTH];
        random.nextBytes(bytes);
        ProcessResult<byte[], byte[]> result =
                running(Tests.pyCat())
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().await();
        System.out.println(result);
        assertEquals("exit code", 0, result.exitCode());
        assertArrayEquals("stdout", bytes, result.content().stdout());
        assertEquals("stderr length", 0, result.content().stderr().length);
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
                running(Tests.pyCat())
                        .arg(dataFile.getAbsolutePath())
                        .build()
                        .launcher(CONTEXT)
                        .outputInMemory(ByteSource.wrap(bytes))
                        .launch().await();
        System.out.println(result);
        assertEquals("exit code", 0, result.exitCode());
        checkState(Arrays.equals(bytes, Files.asByteSource(dataFile).read()));
        assertArrayEquals("stdout", bytes, result.content().stdout());
        assertEquals("stderr length", 0, result.content().stderr().length);
        //noinspection ResultOfMethodCallIgnored
        dataFile.delete();
    }

    @Test
    public void launch_readInput_predefined() throws Exception {
        String expected = String.format("foo%nbar%n");
        ProcessResult<String, String> result = running(Tests.pyReadInput())
                .build()
                .launcher(CONTEXT)
                .outputStrings(US_ASCII, CharSource.wrap(expected + System.lineSeparator()).asByteSource(US_ASCII))
                .launch().await();
        System.out.println(result);
        assertEquals("output", expected, result.content().stdout());
        assertEquals("exit code", 0, result.exitCode());
    }

    /**
     * Runs echo, printing to the JVM stdout stream.
     * This is not an automated test, as it requires visual inspection in the console
     * to confirm expected behavior, but it's useful for diagnosing issues.
     */
    @Test
    public void launch_echo_inherit() throws Exception {
        int exitCode = Subprocess.running(pyEcho())
                .arg("hello, world")
                .build()
                .launcher(CONTEXT)
                .inheritOutputStreams()
                .launch().await().exitCode();
        checkState(exitCode == 0);
        System.out.println();
        System.out.flush();
    }

    @Test
    public void launch_stereo_inherit() throws Exception {
        PrintStream JVM_STDOUT = System.out, JVM_STDERR = System.err;
        ByteBucket stdoutBucket = ByteBucket.create(), stderrBucket = ByteBucket.create();
        ProcessResult<Void, Void> result;
        try (PrintStream tempOut = new PrintStream(stdoutBucket.openStream(), true);
            PrintStream tempErr = new PrintStream(stderrBucket.openStream(), true)) {
            System.setOut(tempOut);
            System.setErr(tempErr);
            result = Subprocess.running(Tests.getPythonFile("nht_stereo.py"))
                    .args("foo", "bar")
                    .build()
                    .launcher(CONTEXT)
                    .inheritOutputStreams()
                    .launch().await();
            assertEquals("exit code", 0, result.exitCode());
        } finally {
            System.setOut(JVM_STDOUT);
            System.setErr(JVM_STDERR);
        }
        System.out.format("result: %s%n", result);
        String actualStdout = stdoutBucket.decode(US_ASCII);
        String actualStderr = stderrBucket.decode(US_ASCII);
        assertEquals("stdout", "foo" + System.lineSeparator(), actualStdout);
        assertEquals("stderr", "bar" + System.lineSeparator(), actualStderr);
    }

    private static CharMatcher alphanumeric() {
        return CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('0', '9'));
    }

    @Test(expected = ProcessLaunchException.class)
    public void launch_notAnExecutable() throws Exception {
        String executable  = "e" + alphanumeric().retainFrom(UUID.randomUUID().toString());
        Subprocess.running(executable).build().launcher(CONTEXT).launch();
    }

    private static final String homeVarName = Platforms.getPlatform().isWindows() ? "UserProfile" : "HOME";

    @Test
    public void launch_env_noSupplements() throws Exception {
        String expectedUser = System.getProperty("user.name");
        String expectedHome = System.getProperty("user.home");
        Map<String, String> result = launchEnv(ImmutableMap.of(), "USER", homeVarName);
        assertEquals("user", expectedUser, result.get("USER"));
        assertEquals("home", expectedHome, result.get(homeVarName));
    }

    @Test
    public void launch_env_override() throws Exception {
        String expectedUser = System.getProperty("user.name");
        String expectedHome = FileUtils.getTempDirectory().getAbsolutePath();
        Map<String, String> result = launchEnv(ImmutableMap.of(homeVarName, expectedHome), "USER", homeVarName);
        assertEquals("user", expectedUser, result.get("USER"));
        assertEquals("home", expectedHome, result.get(homeVarName));
    }

    @Test
    public void launch_env_withSupplements() throws Exception {
        String expectedUser = System.getProperty("user.name");
        String expectedHome = System.getProperty("user.home");
        String expectedFoo = "bar";
        Map<String, String> result = launchEnv(ImmutableMap.of("FOO", expectedFoo), "USER", homeVarName, "FOO");
        assertEquals("user", expectedUser, result.get("USER"));
        assertEquals("home", expectedHome, result.get(homeVarName));
        assertEquals("foo", expectedFoo, result.get("FOO"));
    }

    private Map<String, String> launchEnv(Map<String, String> env, String...varnamesArray) throws InterruptedException {
        List<String> varnames = Arrays.asList(varnamesArray);
        ProcessResult<String, String> result = Subprocess.running(Tests.getPythonFile("nht_env.py"))
                .env(env)
                .args(varnames)
                .build().launcher(CONTEXT).outputStrings(Charset.defaultCharset()).launch().await();
        System.out.format("result: %s%n", result);
        List<String> lines = Splitter.on(System.lineSeparator()).omitEmptyStrings().splitToList(result.content().stdout());
        assertEquals("exit code", 0, result.exitCode());
        assertEquals("num lines in output", varnames.size(), lines.size());
        Map<String, String> defs = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            defs.put(varnames.get(i), lines.get(i));
        }
        return defs;
    }

    @Test(timeout = 5000L)
    public void awaitWithTimeout() throws Exception {
        ProcessMonitor<?, ?> monitor = Subprocess.running(Tests.pySignalListener())
                .build().launcher(CONTEXT).launch();
        ProcessResult<?, ?> result = null;
        try {
            result = monitor.await(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignore) {
        }
        assertNull("result should not have been assigned", result);
        DestroyAttempt term = monitor.destructor().sendTermSignal().await();
        assertEquals("term result", DestroyResult.TERMINATED, term.result());
    }

    @Test
    public void killRemovesFromTracker() throws Exception {
        Map<Process, Boolean> processes = Collections.synchronizedMap(new HashMap<>());

        ProcessTracker localContext = new ProcessTracker() {
            @Override
            public void add(Process process) {
                processes.put(process, true);
            }

            @Override
            public boolean remove(Process process) {
                return processes.put(process, false) != null;
            }

            @Override
            public int count() {
                return processes.size();
            }

            @Override
            public int activeCount() {
                return Ints.checkedCast(processes.entrySet().stream().filter(Entry::getValue).count());
            }

        };
        ProcessMonitor<?, ?> monitor = Subprocess.running(Tests.pySignalListener())
                .build().launcher(localContext).launch();
        ProcessResult<?, ?> result = null;
        try {
            result = monitor.await(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignore) {
        }
        checkState(result == null);
        DestroyAttempt term = monitor.destructor().sendTermSignal().await();
        checkState(DestroyResult.TERMINATED == term.result());
        Set<Process> pset = ImmutableSet.copyOf(processes.keySet());
        checkState(pset.size() == 1);
        Process process = pset.iterator().next();
        checkState(!process.isAlive(), "process still alive");
        assertEquals("all count", 1, localContext.count());
        assertEquals("active count", 0, localContext.activeCount());

    }
}