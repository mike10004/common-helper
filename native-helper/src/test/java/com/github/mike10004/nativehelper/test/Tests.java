package com.github.mike10004.nativehelper.test;

import com.github.mike10004.nativehelper.test.Poller.PollOutcome;
import com.github.mike10004.nativehelper.test.Poller.StopReason;
import com.google.common.base.Suppliers;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;

public class Tests {

    private Tests() {}

    public static File getTestSourcesDir() {
        return new File(getProperties().getProperty("project.basedir"), "src/test");
    }

    public static File getPythonTestSourcesDir() {
        File f = new File(getTestSourcesDir(), "python");
        checkState(f.isDirectory(), "not a directory: %s", f);
        return f;
    }

    public static File getPythonFile(String relativePath) {
        return getPythonTestSourcesDir().toPath().resolve(relativePath).toFile();
    }

    private static final Supplier<Properties> propertiesSupplier = Suppliers.memoize(() -> {
        Properties p = new Properties();
        String resourcePath = "/test.properties";
        try (InputStream in = Tests.class.getResourceAsStream(resourcePath)) {
            checkState(in != null, "not found: classpath:" + resourcePath);
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkState(!p.isEmpty(), "no properties loaded");
        return p;
    });

    public static Properties getProperties() {
        return propertiesSupplier.get();
    }

    public static File pyReadInput() {
        return getPythonFile("nht_read_input.py");
    }

    public static String joinPlus(String delimiter, Iterable<String> items) {
        return String.join(delimiter, items) + delimiter;
    }

    public static File pyCat() {
        return getPythonFile("nht_cat.py");
    }

    public static File pySignalListener() {
        return getPythonFile("nht_signal_listener.py");
    }

    public static String readWhenNonempty(File file) throws InterruptedException {
        PollOutcome<String> outcome = new Poller<String>() {

            @Override
            protected PollAnswer<String> check(int pollAttemptsSoFar) {
                if (file.length() > 0) {
                    try {
                        return resolve(Files.asCharSource(file, StandardCharsets.US_ASCII).read());
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                        return abortPolling();
                    }
                }
                return continuePolling();
            }
        }.poll(250, 20);
        if (outcome.reason == StopReason.RESOLVED) {
            return outcome.content;
        }
        throw new IllegalStateException("polling for nonempty file failed: " + file);
    }
}
