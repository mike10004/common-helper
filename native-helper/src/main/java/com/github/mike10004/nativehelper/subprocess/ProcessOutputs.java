package com.github.mike10004.nativehelper.subprocess;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.function.Function;
import java.util.function.Supplier;

class ProcessOutputs {

    private static int lengthOf(@Nullable String s) {
        return s == null ? 0 : s.length();
    }

    private static String innerFieldsToString(Object stdout, Object stderr) {
        String so = abbrev(stdout);
        String se = abbrev(stderr);
        StringBuilder s = new StringBuilder(lengthOf(so) + lengthOf(se) + 20);
        s.append("stdout=");
        if (so != null) {
            s.append('"').append(so).append('"');
        }
        s.append(", stderr=");
        if (se != null) {
            s.append('"').append(se).append('"');
        }
        return s.toString();
    }

    public static class DirectOutput<SO, SE> implements ProcessOutput<SO, SE> {

        private final SO stdout;
        private final SE stderr;

        public DirectOutput(SO stdout, SE stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override
        public SO getStdout() {
            return stdout;
        }

        @Override
        public SE getStderr() {
            return stderr;
        }

        @Override
        public String toString() {
            return "DirectOutput{" + innerFieldsToString(stdout, stderr) + '}';
        }
    }

    @VisibleForTesting
    static String betterToString(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        if (object.getClass().isArray()) {
            String componentTypeName = object.getClass().getComponentType().getName();
            StringBuilder s = new StringBuilder(32);
            while (object != null && object.getClass().isArray()) {
                componentTypeName = object.getClass().getComponentType().getName();
                int length = Array.getLength(object);
                s.append('[').append(length).append(']');
                if (length > 0) {
                    object = Array.get(object, 0);
                } else {
                    break;
                }
            }
            if (componentTypeName.matches("^java\\.lang\\.[A-Z].*$")) {
                componentTypeName = StringUtils.removeStart(componentTypeName, "java.lang.");
            }
            return componentTypeName + s.toString();
        }
        return object.toString();
    }

    private static String abbrev(Object object) {
        if (object == null) {
            return null;
        }
        String abbreved = StringUtils.abbreviateMiddle(betterToString(object), "...", ABBREV);
        return StringEscapeUtils.escapeJava(abbreved);
    }

    public static class DeferredOutput<SO, SE> implements ProcessOutput<SO, SE> {

        private final Supplier<SO> stdoutSupplier;
        private final Supplier<SE> stderrSupplier;

        public DeferredOutput(Supplier<SO> stdoutSupplier, Supplier<SE> stderrSupplier) {
            this.stdoutSupplier = Suppliers.memoize(stdoutSupplier::get);
            this.stderrSupplier = Suppliers.memoize(stderrSupplier::get);
        }

        @Override
        public SO getStdout() {
            return stdoutSupplier.get();
        }

        @Override
        public SE getStderr() {
            return stderrSupplier.get();
        }

        @Override
        public String toString() {
            return "DeferredOutput{" +
                    "stdoutSupplier=" + stdoutSupplier +
                    ", stderrSupplier=" + stderrSupplier +
                    '}';
        }

        public String toStringExpanded() {
            return "DeferredOutput:Expanded{" + innerFieldsToString(getStdout(), getStderr()) + '}';
        }

    }

    private static final int ABBREV = 64;

    public static class MappedOutput<SO, SE> extends DeferredOutput<SO, SE> {

        public <SO0, SE0> MappedOutput(ProcessOutput<SO0, SE0> original, Function<? super SO0, SO> stdoutMap, Function<? super SE0, SE> stderrMap) {
            super(() -> stdoutMap.apply(original.getStdout()), () -> stderrMap.apply(original.getStderr()));
        }

        @Override
        public String toString() {
            return toStringExpanded();
        }
    }

    @SuppressWarnings("unchecked")
    public static <SO, SE> ProcessOutput<SO, SE> bothNull() {
        return BOTH_NULL;
    }

    private static final ProcessOutput BOTH_NULL = new ProcessOutput() {
        @Override
        public Void getStdout() {
            return null;
        }

        @Override
        public Void getStderr() {
            return null;
        }

        @Override
        public String toString() {
            return "NoOutput{}";
        }
    };
}
