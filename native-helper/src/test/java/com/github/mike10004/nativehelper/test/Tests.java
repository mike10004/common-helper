package com.github.mike10004.nativehelper.test;

import com.google.common.base.Suppliers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;

public class Tests {

    private Tests() {}

    private static File getPythonTestSourcesDir() {
        File f = new File(getProperties().getProperty("project.basedir"), "src/test/python");
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
}
